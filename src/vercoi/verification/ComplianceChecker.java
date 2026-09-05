package vercoi.verification;

import vercoi.model.*;
import java.util.*;

/**
 * Static tuple-level compliance checker for the explicitly supported Java authorization representation.
 * Precondition: the expected XACML rule set has already been made conflict-free by Algorithm 2.
 */
public final class ComplianceChecker {
    private ComplianceChecker() {}

    private record Key(String subject, String resource, String action) { }

    public static List<ComplianceViolation> check(List<PolicyRule> expected, List<JavaAuthorizationRule> actual) {
        Map<Key, Effect> exp = new LinkedHashMap<>();
        for (PolicyRule r : expected) {
            for (String a : r.actions()) {
                Key k = new Key(r.subject(), r.resource(), a);
                Effect previous = exp.putIfAbsent(k, r.effect());
                if (previous != null && previous != r.effect()) {
                    throw new IllegalArgumentException("Expected policy contains an unresolved conflict for " + k
                            + ". Run conflict resolution before compliance verification.");
                }
            }
        }

        Map<Key, Effect> act = new LinkedHashMap<>();
        List<ComplianceViolation> out = new ArrayList<>();
        for (JavaAuthorizationRule r : actual) {
            Key k = new Key(r.subject(), r.resource(), r.action());
            Effect previous = act.putIfAbsent(k, r.effect());
            if (previous != null && previous != r.effect()) {
                out.add(new ComplianceViolation(ComplianceViolation.Type.IMPLEMENTATION_CONFLICT,
                        k.subject(), k.resource(), k.action(), exp.get(k), r.effect(),
                        "Java source contains contradictory authorization effects for the same tuple"));
            }
        }

        for (var e : exp.entrySet()) {
            Key k = e.getKey();
            Effect got = act.get(k);
            if (got == null) {
                out.add(new ComplianceViolation(ComplianceViolation.Type.MISSING_RULE,
                        k.subject(), k.resource(), k.action(), e.getValue(), null,
                        "Expected authorization tuple is absent from recognized Java authorization code"));
            } else if (got != e.getValue()) {
                out.add(new ComplianceViolation(ComplianceViolation.Type.EFFECT_MISMATCH,
                        k.subject(), k.resource(), k.action(), e.getValue(), got,
                        "Recognized Java authorization effect differs from XACML"));
            }
        }
        for (var e : act.entrySet()) {
            if (!exp.containsKey(e.getKey())) {
                Key k = e.getKey();
                out.add(new ComplianceViolation(ComplianceViolation.Type.UNEXPECTED_RULE,
                        k.subject(), k.resource(), k.action(), null, e.getValue(),
                        "Authorization tuple appears in recognized Java code but not in XACML"));
            }
        }
        return List.copyOf(out);
    }
}
