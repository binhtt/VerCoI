package vercoi.generator;

import vercoi.model.CombiningAlgorithm;
import vercoi.model.PolicyRule;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Implements Algorithm 1: parse rules first, group them by resource, then generate a PolicySet. */
public final class XacmlGenerator {
    private XacmlGenerator() {}

    /**
     * Generates an XACML 3.0 PolicySet. Rules are grouped into one Policy per resource,
     * matching Algorithm 1 in the article.
     */
    public static String generate(String policySetId, CombiningAlgorithm alg, List<PolicyRule> rules) {
        Map<String, List<PolicyRule>> byResource = new LinkedHashMap<>();
        for (PolicyRule r : rules) byResource.computeIfAbsent(r.resource(), k -> new ArrayList<>()).add(r);

        StringBuilder x = new StringBuilder();
        x.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        x.append("<PolicySet xmlns=\"urn:oasis:names:tc:xacml:3.0:core:schema:wd-17\" PolicySetId=\"")
         .append(xml(policySetId)).append("\" Version=\"1.0\" PolicyCombiningAlgId=\"")
         .append(policyUrn(alg)).append("\">\n");
        x.append("  <Target/>\n");

        for (var entry : byResource.entrySet()) {
            String resource = entry.getKey();
            x.append("  <Policy PolicyId=\"P_").append(xmlId(resource)).append("\" Version=\"1.0\" RuleCombiningAlgId=\"")
             .append(ruleUrn(alg)).append("\">\n");
            x.append("    <Target/>\n");
            for (PolicyRule r : entry.getValue()) appendRule(x, r);
            x.append("  </Policy>\n");
        }
        x.append("</PolicySet>\n");
        return x.toString();
    }

    private static void appendRule(StringBuilder x, PolicyRule r) {
        x.append("    <Rule RuleId=\"").append(xml(r.ruleId())).append("\" Effect=\"")
         .append(r.effect()).append("\">\n");
        x.append("      <Target>\n        <AnyOf>\n");
        // Each AllOf is one complete (subject, resource, action) alternative.
        for (String a : r.actions()) {
            x.append("          <AllOf>\n");
            match(x, "access-subject", r.subjectAttributeId(), r.subject(), "            ");
            match(x, "resource", "resource", r.resource(), "            ");
            match(x, "action", "action", a, "            ");
            x.append("          </AllOf>\n");
        }
        x.append("        </AnyOf>\n      </Target>\n    </Rule>\n");
    }

    private static void match(StringBuilder x, String cat, String id, String value, String indent) {
        x.append(indent).append("<Match MatchId=\"urn:oasis:names:tc:xacml:1.0:function:string-equal\">\n")
         .append(indent).append("  <AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#string\">")
         .append(xml(value)).append("</AttributeValue>\n")
         .append(indent).append("  <AttributeDesignator Category=\"urn:oasis:names:tc:xacml:3.0:attribute-category:")
         .append(cat).append("\" AttributeId=\"").append(xml(id))
         .append("\" DataType=\"http://www.w3.org/2001/XMLSchema#string\" MustBePresent=\"false\"/>\n")
         .append(indent).append("</Match>\n");
    }

    private static String ruleUrn(CombiningAlgorithm a) {
        return switch (a) {
            case DENY_OVERRIDES -> "urn:oasis:names:tc:xacml:1.0:rule-combining-algorithm:deny-overrides";
            case PERMIT_OVERRIDES -> "urn:oasis:names:tc:xacml:1.0:rule-combining-algorithm:permit-overrides";
            case FIRST_APPLICABLE -> "urn:oasis:names:tc:xacml:1.0:rule-combining-algorithm:first-applicable";
        };
    }

    private static String policyUrn(CombiningAlgorithm a) {
        return switch (a) {
            case DENY_OVERRIDES -> "urn:oasis:names:tc:xacml:1.0:policy-combining-algorithm:deny-overrides";
            case PERMIT_OVERRIDES -> "urn:oasis:names:tc:xacml:1.0:policy-combining-algorithm:permit-overrides";
            case FIRST_APPLICABLE -> "urn:oasis:names:tc:xacml:1.0:policy-combining-algorithm:first-applicable";
        };
    }

    private static String xmlId(String s) {
        String out = s.replaceAll("[^A-Za-z0-9_.-]", "_");
        return out.isBlank() ? "resource" : out;
    }

    private static String xml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
