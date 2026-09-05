package vercoi.verification;

import vercoi.model.PolicyRule;
import java.util.*;

public final class DuplicateDetector {
    private DuplicateDetector() {}
    public static List<List<PolicyRule>> findDuplicates(List<PolicyRule> rules) {
        Map<String,List<PolicyRule>> groups=new LinkedHashMap<>();
        for(PolicyRule r:rules) groups.computeIfAbsent(r.normalizedKey(),k->new ArrayList<>()).add(r);
        return groups.values().stream().filter(g->g.size()>1).map(List::copyOf).toList();
    }
}
