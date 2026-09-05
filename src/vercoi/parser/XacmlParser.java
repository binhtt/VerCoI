package vercoi.parser;

import vercoi.model.*;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Parser for the XACML subset supported by VerCoI. */
public final class XacmlParser {
    private XacmlParser() {}

    public static ParsedPolicy parse(Path path) throws Exception { return parse(Files.readString(path)); }

    public static ParsedPolicy parse(String xml) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        f.setFeature("http://xml.org/sax/features/external-general-entities", false);
        f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        f.setXIncludeAware(false);
        f.setExpandEntityReferences(false);
        try { f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, ""); } catch (IllegalArgumentException ignored) {}
        try { f.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, ""); } catch (IllegalArgumentException ignored) {}

        Document d = f.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        Element root = d.getDocumentElement();
        String local = localName(root);
        if ("PolicySet".equals(local)) return parsePolicySet(root);
        if ("Policy".equals(local)) return parseSinglePolicy(root);
        throw new IllegalArgumentException("Expected XACML PolicySet or Policy root, found: " + local);
    }

    public static ParsedPolicy parseAll(List<Path> paths, CombiningAlgorithm combiningAlgorithm) throws Exception {
        if (paths == null || paths.isEmpty()) throw new IllegalArgumentException("At least one XACML file is required");
        List<ParsedPolicy> parsed = new ArrayList<>();
        for (Path path : paths) parsed.add(parse(path));
        return merge(parsed, combiningAlgorithm);
    }

    public static ParsedPolicy merge(List<ParsedPolicy> inputs, CombiningAlgorithm combiningAlgorithm) {
        if (inputs == null || inputs.isEmpty()) throw new IllegalArgumentException("At least one policy is required");
        List<ResourcePolicy> policies = new ArrayList<>();
        List<PolicyRule> rules = new ArrayList<>();
        for (ParsedPolicy p : inputs) {
            policies.addAll(p.policies());
            rules.addAll(p.rules());
        }
        return new ParsedPolicy("MergedPolicySet", combiningAlgorithm, policies, rules);
    }

    private static ParsedPolicy parsePolicySet(Element root) {
        String id = attr(root, "PolicySetId", "PolicySet");
        CombiningAlgorithm setAlg = CombiningAlgorithm.parse(attr(root, "PolicyCombiningAlgId", "first-applicable"));
        List<ResourcePolicy> policies = new ArrayList<>();
        List<PolicyRule> all = new ArrayList<>();

        for (Element p : directChildren(root, "Policy")) {
            ResourcePolicy rp = parsePolicyElement(p);
            policies.add(rp);
            all.addAll(rp.rules());
        }
        if (policies.isEmpty()) {
            // Tolerate a PolicySet containing direct Rules as a legacy input, but label it explicitly.
            List<PolicyRule> direct = parseDirectRules(root);
            if (!direct.isEmpty()) {
                ResourcePolicy rp = new ResourcePolicy(id + "_direct", setAlg, direct);
                policies.add(rp);
                all.addAll(direct);
            }
        }
        return new ParsedPolicy(id, setAlg, policies, all);
    }

    private static ParsedPolicy parseSinglePolicy(Element root) {
        ResourcePolicy rp = parsePolicyElement(root);
        return new ParsedPolicy(rp.policyId(), rp.combiningAlgorithm(), List.of(rp), rp.rules());
    }

    private static ResourcePolicy parsePolicyElement(Element policy) {
        String id = attr(policy, "PolicyId", "Policy");
        CombiningAlgorithm alg = CombiningAlgorithm.parse(attr(policy, "RuleCombiningAlgId", "first-applicable"));
        return new ResourcePolicy(id, alg, parseDirectRules(policy));
    }

    private static List<PolicyRule> parseDirectRules(Element parent) {
        List<PolicyRule> rules = new ArrayList<>();
        for (Element rule : directChildren(parent, "Rule")) rules.add(parseRule(rule));
        return List.copyOf(rules);
    }

    private static PolicyRule parseRule(Element rule) {
        String id = requireAttr(rule, "RuleId");
        Effect effect = Effect.parse(requireAttr(rule, "Effect"));

        LinkedHashSet<String> subjects = new LinkedHashSet<>();
        LinkedHashSet<String> subjectAttrs = new LinkedHashSet<>();
        LinkedHashSet<String> resources = new LinkedHashSet<>();
        LinkedHashSet<String> actions = new LinkedHashSet<>();

        NodeList matches = rule.getElementsByTagNameNS("*", "Match");
        for (int i = 0; i < matches.getLength(); i++) {
            Element m = (Element) matches.item(i);
            Element des = first(m, "AttributeDesignator");
            Element val = first(m, "AttributeValue");
            if (des == null || val == null) continue;
            String category = des.getAttribute("Category");
            String aid = des.getAttribute("AttributeId");
            String v = val.getTextContent().trim();
            if (v.isEmpty()) continue;
            if (endsCategory(category, "access-subject")) {
                subjects.add(v);
                subjectAttrs.add(aid.isBlank() ? "role" : aid);
            } else if (endsCategory(category, "resource")) {
                resources.add(v);
            } else if (endsCategory(category, "action")) {
                actions.add(v);
            }
        }

        if (subjects.size() != 1 || subjectAttrs.size() != 1 || resources.size() != 1 || actions.isEmpty()) {
            throw new IllegalArgumentException("Rule " + id + " is outside the supported target subset: exactly one subject, one subject attribute, one resource, and at least one action are required");
        }
        return new PolicyRule(id, subjectAttrs.iterator().next(), subjects.iterator().next(),
                resources.iterator().next(), actions, effect);
    }

    private static boolean endsCategory(String category, String suffix) {
        return category != null && (category.endsWith(":" + suffix) || category.equals(suffix) || category.endsWith(suffix));
    }

    private static List<Element> directChildren(Element parent, String local) {
        List<Element> out = new ArrayList<>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n instanceof Element e && local.equals(localName(e))) out.add(e);
        }
        return out;
    }

    private static String localName(Element e) {
        String n = e.getLocalName();
        if (n != null) return n;
        n = e.getNodeName();
        int p = n.indexOf(':');
        return p >= 0 ? n.substring(p + 1) : n;
    }

    private static Element first(Element parent, String local) {
        NodeList n = parent.getElementsByTagNameNS("*", local);
        return n.getLength() == 0 ? null : (Element) n.item(0);
    }
    private static String attr(Element e, String n, String def) {
        String v = e.getAttribute(n); return v == null || v.isBlank() ? def : v;
    }
    private static String requireAttr(Element e, String n) {
        String v = e.getAttribute(n); if (v == null || v.isBlank()) throw new IllegalArgumentException("Missing " + n); return v;
    }
}
