package vercoi.experiment;

import vercoi.model.*;
import vercoi.verification.ConflictDetector;
import vercoi.verification.ConflictResolver;
import java.nio.file.Path;
import java.util.*;

public final class RQ2Experiment {
    private RQ2Experiment(){}

    public static void run(Path results)throws Exception {
        int tp=0,fp=0,fn=0,tn=0;
        int ltp=0,lfp=0,lfn=0,ltn=0;
        List<String[]> detail=new ArrayList<>();
        detail.add(new String[]{"system","case_id","expected_conflict","detected_v2","detected_legacy","case_type"});
        String[] systems={"HACS","FMS","SIS"};
        for(String s:systems) for(int i=0;i<60;i++){
            boolean positive=i<30;
            String suffix=s+"Case"+i;
            // Half the cases deliberately use neutral IDs so RuleId naming cannot serve as an oracle.
            String aid=i%2==0?"Permit"+suffix:s+"_A"+i;
            String bid=i%2==0?"Deny"+suffix:s+"_B"+i;
            PolicyRule a=new PolicyRule(aid,"role","ROLE"+(i%5),"RES"+(i%7),
                    i%3==0?List.of("act"+(i%4),"shared"):List.of("act"+(i%4)),Effect.Permit);
            PolicyRule b; String type;
            if(positive){
                // Mix exact-action and partial-overlap conflicts.
                List<String> ba=i%3==0?List.of("shared","other"):List.of(a.actions().iterator().next());
                b=new PolicyRule(bid,"role",a.subject(),a.resource(),ba,Effect.Deny);
                type=i%3==0?"partial-action-overlap":"exact-overlap-opposite-effect";
            } else {
                int k=i%5;
                type=switch(k){case 0->"different-subject";case 1->"different-resource";case 2->"disjoint-action";case 3->"same-effect";default->"different-subject-attribute";};
                b=switch(k){
                    case 0->new PolicyRule(bid,"role",a.subject()+"X",a.resource(),a.actions(),Effect.Deny);
                    case 1->new PolicyRule(bid,"role",a.subject(),a.resource()+"X",a.actions(),Effect.Deny);
                    case 2->new PolicyRule(bid,"role",a.subject(),a.resource(),List.of("other"),Effect.Deny);
                    case 3->new PolicyRule(bid,"role",a.subject(),a.resource(),a.actions(),Effect.Permit);
                    default->new PolicyRule(bid,"department",a.subject(),a.resource(),a.actions(),Effect.Deny);
                };
            }
            List<PolicyRule> pair=List.of(a,b);
            boolean detected=!ConflictDetector.findConflicts(pair).isEmpty();
            boolean legacy=LegacyConflictHeuristic.detects(pair);
            if(positive&&detected)tp++;else if(positive)fn++;else if(detected)fp++;else tn++;
            if(positive&&legacy)ltp++;else if(positive)lfn++;else if(legacy)lfp++;else ltn++;
            detail.add(new String[]{s,String.valueOf(i+1),String.valueOf(positive),String.valueOf(detected),String.valueOf(legacy),type});
        }
        Metrics m=new Metrics(tp,fp,fn,tn), lm=new Metrics(ltp,lfp,lfn,ltn);
        Csv.write(results.resolve("rq2_conflict_cases.csv"),detail);
        List<String[]> sum=new ArrayList<>();
        sum.add(new String[]{"method","TP","FP","FN","TN","precision","recall","f1","specificity","accuracy"});
        sum.add(metricRow("VerCoI-V2",m));
        sum.add(metricRow("Recovered-RuleId-Heuristic",lm));
        Csv.write(results.resolve("rq2_conflicts.csv"),sum);

        // Separate oracle for the three combining strategies in Algorithm 2.
        List<String[]> resolution=new ArrayList<>();
        resolution.add(new String[]{"algorithm","cases","correct","accuracy"});
        for(CombiningAlgorithm alg:CombiningAlgorithm.values()){
            int correct=0,total=30;
            for(int i=0;i<total;i++){
                Effect first=i%2==0?Effect.Permit:Effect.Deny;
                Effect second=first==Effect.Permit?Effect.Deny:Effect.Permit;
                PolicyRule a=new PolicyRule("A"+i,"role","DOCTOR","RECORD",List.of("read"),first);
                PolicyRule b=new PolicyRule("B"+i,"role","DOCTOR","RECORD",List.of("read"),second);
                Effect expected=switch(alg){case DENY_OVERRIDES->Effect.Deny;case PERMIT_OVERRIDES->Effect.Permit;case FIRST_APPLICABLE->first;};
                Effect got=ConflictResolver.resolve(List.of(a,b),alg);
                if(got==expected) correct++;
            }
            resolution.add(new String[]{alg.name(),String.valueOf(total),String.valueOf(correct),f(correct/(double)total)});
        }
        Csv.write(results.resolve("rq2_resolution.csv"),resolution);
    }

    private static String[] metricRow(String name,Metrics m){return new String[]{name,""+m.tp(),""+m.fp(),""+m.fn(),""+m.tn(),f(m.precision()),f(m.recall()),f(m.f1()),f(m.specificity()),f(m.accuracy())};}
    private static String f(double d){return String.format(Locale.ROOT,"%.6f",d);}
}
