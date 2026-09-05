package vercoi.verification;

import vercoi.model.Conflict;
import vercoi.model.PolicyRule;
import java.util.*;

/** Pairwise conflict detection for the supported rule model. */
public final class ConflictDetector {
    private ConflictDetector() {}

    public static List<Conflict> findConflicts(List<PolicyRule> rules) {
        List<Conflict> out = new ArrayList<>();
        for (int i = 0; i < rules.size(); i++) {
            for (int j = i + 1; j < rules.size(); j++) {
                PolicyRule a = rules.get(i), b = rules.get(j);
                if (a.conflictsWith(b)) {
                    Set<String> overlap = new TreeSet<>(a.actions());
                    overlap.retainAll(b.actions());
                    out.add(new Conflict(a, b, Set.copyOf(overlap)));
                }
            }
        }
        return List.copyOf(out);
    }
}
