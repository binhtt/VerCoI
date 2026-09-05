package vercoi.verification;

import vercoi.model.PolicyRule;
import java.util.ArrayList;
import java.util.List;

public final class RuleValidator {
    private RuleValidator() {}
    public static List<String> validate(List<PolicyRule> rules) {
        List<String> issues=new ArrayList<>();
        if (rules.isEmpty()) issues.add("Policy contains no rules");
        for (PolicyRule r:rules) {
            if (r.actions().isEmpty()) issues.add(r.ruleId()+": no actions");
        }
        return issues;
    }
}
