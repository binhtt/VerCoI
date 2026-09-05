package vercoi.verification;

import vercoi.model.*;
import java.util.*;

/** Resolution semantics for the combining algorithms explicitly discussed in Algorithm 2. */
public final class ConflictResolver {
    private ConflictResolver() {}

    public static Effect resolve(List<PolicyRule> applicable, CombiningAlgorithm alg) {
        if (applicable == null || applicable.isEmpty()) return null;
        return switch (alg) {
            case DENY_OVERRIDES -> applicable.stream().anyMatch(r -> r.effect() == Effect.Deny) ? Effect.Deny : Effect.Permit;
            case PERMIT_OVERRIDES -> applicable.stream().anyMatch(r -> r.effect() == Effect.Permit) ? Effect.Permit : Effect.Deny;
            case FIRST_APPLICABLE -> applicable.get(0).effect();
        };
    }

    public static ConflictAnalysis analyze(List<PolicyRule> orderedRules, CombiningAlgorithm alg) {
        List<Conflict> conflicts = ConflictDetector.findConflicts(orderedRules);
        List<ResolvedConflict> resolutions = new ArrayList<>();
        for (Conflict c : conflicts) {
            Effect effect = resolve(List.of(c.left(), c.right()), alg);
            resolutions.add(new ResolvedConflict(c, alg, effect));
        }
        return new ConflictAnalysis(conflicts, resolutions);
    }

    /**
     * Produces an atomic conflict-free rule set by expanding multi-action rules into
     * (subject, resource, action) tuples and applying the selected combining algorithm.
     */
    public static List<PolicyRule> resolveToConflictFree(List<PolicyRule> orderedRules, CombiningAlgorithm alg) {
        record Key(String attr, String subject, String resource, String action) {}
        Map<Key, List<PolicyRule>> groups = new LinkedHashMap<>();
        for (PolicyRule r : orderedRules) {
            for (String a : r.actions()) {
                Key k = new Key(r.subjectAttributeId(), r.subject(), r.resource(), a);
                groups.computeIfAbsent(k, x -> new ArrayList<>()).add(r);
            }
        }
        List<PolicyRule> out = new ArrayList<>();
        int seq = 1;
        for (var e : groups.entrySet()) {
            Key k = e.getKey();
            Effect resolved = resolve(e.getValue(), alg);
            out.add(new PolicyRule("Resolved_" + seq++, k.attr(), k.subject(), k.resource(), List.of(k.action()), resolved));
        }
        return List.copyOf(out);
    }
}
