package vercoi.experiment;

import vercoi.model.*;
import java.util.*;

final class BenchmarkFactory {
    private BenchmarkFactory() {}
    static List<PolicyRule> rules(String system,int n) {
        List<PolicyRule> out=new ArrayList<>();
        String[] roles={"DOCTOR","NURSE","AUDITOR","OFFICER","STUDENT","ADMIN"};
        String[] resources={"RECORD","ACCOUNT","REPORT","PROFILE","LOAN","COURSE"};
        String[] actions={"read","write","approve","audit","create","update"};
        for(int i=0;i<n;i++) out.add(new PolicyRule(system+"_R"+(i+1),"role",roles[i%roles.length],resources[(i/roles.length)%resources.length],List.of(actions[i%actions.length]), i%7==0?Effect.Deny:Effect.Permit));
        return out;
    }

    static List<PolicyRule> scalabilityRules(String system, int n, boolean conflictHeavy) {
        List<PolicyRule> out = new ArrayList<>();
        String[] actions={"read","write","approve","audit","create","update"};
        for (int i=0;i<n;i++) {
            int key = conflictHeavy ? i/2 : i;
            String subject = "ROLE_" + key;
            String resource = "RES_" + (key % 50);
            String action = actions[key % actions.length];
            Effect effect = conflictHeavy && i%2==1 ? Effect.Deny : Effect.Permit;
            out.add(new PolicyRule(system+"_R"+(i+1),"role",subject,resource,List.of(action),effect));
        }
        return out;
    }

    static String javaSource(List<PolicyRule> rules) {
        StringBuilder b=new StringBuilder("public class Authorization { void configure(){\n");
        for(PolicyRule r:rules) for(String a:r.actions()) b.append("  ").append(r.effect()==Effect.Permit?"allow":"deny").append("(\"").append(r.subject()).append("\",\"").append(r.resource()).append("\",\"").append(a).append("\");\n");
        return b.append("} void allow(String s,String r,String a){} void deny(String s,String r,String a){} }\n").toString();
    }
}
