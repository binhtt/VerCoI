package vercoi.experiment;

import vercoi.model.PolicyRule;
import java.util.List;

/** Reproduces the recovered prototype's RuleId-prefix heuristic for internal baseline comparison. */
final class LegacyConflictHeuristic {
    private LegacyConflictHeuristic() {}
    static boolean detects(List<PolicyRule> pair) {
        if (pair.size() != 2) return false;
        PolicyRule a = pair.get(0), b = pair.get(1);
        String sa = suffix(a.ruleId()), sb = suffix(b.ruleId());
        return sa != null && sa.equals(sb) && a.effect() != b.effect();
    }
    private static String suffix(String id) {
        if (id.startsWith("Permit")) return id.substring("Permit".length());
        if (id.startsWith("Deny")) return id.substring("Deny".length());
        return null;
    }
}
