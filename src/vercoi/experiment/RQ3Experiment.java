package vercoi.experiment;

import vercoi.model.*;
import vercoi.parser.JavaPolicyParser;
import vercoi.verification.ComplianceChecker;
import java.nio.file.Path;
import java.util.*;

public final class RQ3Experiment {
    private RQ3Experiment(){}

    public static void run(Path results)throws Exception {
        int tp=0,fp=0,fn=0,tn=0;
        Map<String,int[]> per=new LinkedHashMap<>();
        List<String[]> cases=new ArrayList<>();
        cases.add(new String[]{"system","case","expected_violation","detected_violation","operator"});
        for(String sys:new String[]{"HACS","FMS","SIS"}){
            List<PolicyRule> expected=BenchmarkFactory.rules(sys,10);
            String clean=BenchmarkFactory.javaSource(expected);
            // 30 semantically equivalent source variants/system (whitespace/comments/order-neutral decoration).
            for(int i=0;i<30;i++){
                String variant=cleanVariant(clean,i);
                boolean det=!ComplianceChecker.check(expected,JavaPolicyParser.parse(variant)).isEmpty();
                if(det)fp++;else tn++;
                cases.add(new String[]{sys,"clean-"+(i+1),"false",""+det,"clean-variant"});
            }
            String[] ops={"effect-inversion","missing-rule","wrong-subject","wrong-resource","wrong-action","extra-unauthorized"};
            for(String op:ops) for(int i=0;i<10;i++){
                String mutant=mutate(clean,expected.get(i%expected.size()),op,i);
                if(mutant.equals(clean)) throw new IllegalStateException("Mutation did not change source: "+op+" "+sys+" "+i);
                boolean det=!ComplianceChecker.check(expected,JavaPolicyParser.parse(mutant)).isEmpty();
                if(det)tp++;else fn++;
                per.computeIfAbsent(op,k->new int[2])[det?0:1]++;
                cases.add(new String[]{sys,op+"-"+(i+1),"true",""+det,op});
            }
        }
        Metrics m=new Metrics(tp,fp,fn,tn);
        Csv.write(results.resolve("rq3_compliance_cases.csv"),cases);
        Csv.write(results.resolve("rq3_compliance.csv"),List.of(
                new String[]{"TP","FP","FN","TN","precision","recall","f1","specificity","accuracy"},
                new String[]{""+tp,""+fp,""+fn,""+tn,f(m.precision()),f(m.recall()),f(m.f1()),f(m.specificity()),f(m.accuracy())}));
        List<String[]> po=new ArrayList<>();
        po.add(new String[]{"operator","detected","missed","detection_rate"});
        for(var e:per.entrySet()){
            int d=e.getValue()[0], miss=e.getValue()[1];
            po.add(new String[]{e.getKey(),""+d,""+miss,f(d/(double)(d+miss))});
        }
        Csv.write(results.resolve("rq3_by_operator.csv"),po);

        // Scope-boundary probes are reported separately and are not mixed into the primary metrics.
        List<String[]> boundary=new ArrayList<>();
        boundary.add(new String[]{"case","recognized_calls","interpretation"});
        String[] sources={
            "void f(String s,String r,String a){ allow(s,r,a); }",
            "void f(){ allow(ROLE_DOCTOR,RESOURCE_RECORD,ACTION_READ); }",
            "void f(){ authorize(\"DOCTOR\",\"RECORD\",\"read\"); }",
            "void f(){ allow(\"DOC\"+\"TOR\",\"RECORD\",\"read\"); }"
        };
        String[] names={"variable-arguments","constant-identifiers","wrapper-method","computed-string"};
        for(int i=0;i<sources.length;i++) boundary.add(new String[]{names[i],String.valueOf(JavaPolicyParser.parse(sources[i]).size()),"outside-supported-explicit-literal-call-idiom"});
        Csv.write(results.resolve("rq3_scope_boundary.csv"),boundary);
    }

    private static String cleanVariant(String src,int i){
        return switch(i%3){
            case 0 -> "// benign comment containing fake text: allow(\"X\",\"Y\",\"Z\");\n"+src;
            case 1 -> src.replace(";\n", ";   // authorization declaration\n");
            default -> "/* benchmark clean variant "+i+" */\n"+src.replace("  allow", "    allow").replace("  deny", "    deny");
        };
    }

    private static String mutate(String src,PolicyRule r,String op,int i){
        String a=r.actions().iterator().next();
        String method=r.effect()==Effect.Permit?"allow":"deny";
        String opposite=r.effect()==Effect.Permit?"deny":"allow";
        String call=method+"(\""+r.subject()+"\",\""+r.resource()+"\",\""+a+"\");";
        return switch(op){
            case "effect-inversion" -> src.replace(call,opposite+call.substring(method.length()));
            case "missing-rule" -> src.replace("  "+call+"\n","");
            case "wrong-subject" -> src.replace(call,method+"(\""+r.subject()+"_WRONG\",\""+r.resource()+"\",\""+a+"\");");
            case "wrong-resource" -> src.replace(call,method+"(\""+r.subject()+"\",\""+r.resource()+"_WRONG\",\""+a+"\");");
            case "wrong-action" -> src.replace(call,method+"(\""+r.subject()+"\",\""+r.resource()+"\",\""+a+"_WRONG\");");
            case "extra-unauthorized" -> src.replace("} void allow", "  allow(\"INTRUDER"+i+"\",\"SECRET\",\"read\");\n} void allow");
            default -> throw new IllegalArgumentException("Unknown operator: "+op);
        };
    }
    private static String f(double d){return String.format(Locale.ROOT,"%.6f",d);}
}
