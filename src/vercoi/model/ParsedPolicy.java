package vercoi.model;

import java.util.List;

/** Parsed XACML Policy or PolicySet. Rules are also exposed as a flattened ordered list. */
public record ParsedPolicy(String policyId,
                           CombiningAlgorithm combiningAlgorithm,
                           List<ResourcePolicy> policies,
                           List<PolicyRule> rules) {
    public ParsedPolicy {
        if (policyId == null || policyId.isBlank()) throw new IllegalArgumentException("policyId must not be empty");
        if (combiningAlgorithm == null) throw new IllegalArgumentException("combiningAlgorithm must not be null");
        policies = List.copyOf(policies);
        rules = List.copyOf(rules);
    }

    /** Compatibility constructor for a single XACML Policy. */
    public ParsedPolicy(String policyId, CombiningAlgorithm combiningAlgorithm, List<PolicyRule> rules) {
        this(policyId, combiningAlgorithm,
                List.of(new ResourcePolicy(policyId, combiningAlgorithm, rules)), rules);
    }
}
