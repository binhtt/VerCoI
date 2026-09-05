package vercoi.model;

import java.util.List;

/** One XACML Policy contained in a PolicySet. */
public record ResourcePolicy(String policyId, CombiningAlgorithm combiningAlgorithm, List<PolicyRule> rules) {
    public ResourcePolicy {
        if (policyId == null || policyId.isBlank()) throw new IllegalArgumentException("policyId must not be empty");
        if (combiningAlgorithm == null) throw new IllegalArgumentException("combiningAlgorithm must not be null");
        rules = List.copyOf(rules);
    }
}
