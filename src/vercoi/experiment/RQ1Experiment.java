package vercoi.experiment;

import vercoi.generator.XacmlGenerator;
import vercoi.model.*;
import vercoi.parser.ControlledTextParser;
import vercoi.parser.XacmlParser;
import java.nio.file.Path;
import java.util.*;

public final class RQ1Experiment {
    private RQ1Experiment(){}

    public static void run(Path results)throws Exception {
        int[][] cfg={{0,50},{1,45},{2,40}};
        String[] names={"HACS","FMS","SIS"};
        List<String[]> rows=new ArrayList<>();
        rows.add(new String[]{"system","dataset_kind","rules","resource_policies","correct_roundtrip","accuracy"});
        for(int[] c:cfg){
            String name=names[c[0]];
            List<PolicyRule> src=BenchmarkFactory.rules(name,c[1]);
            String xml=XacmlGenerator.generate(name,CombiningAlgorithm.FIRST_APPLICABLE,src);
            ParsedPolicy parsed=XacmlParser.parse(xml);
            List<PolicyRule> dst=parsed.rules();
            Map<String,PolicyRule> byId=new HashMap<>();
            for(PolicyRule r:dst) byId.put(r.ruleId(),r);
            int ok=0;
            for(PolicyRule r:src){ PolicyRule d=byId.get(r.ruleId()); if(d!=null && r.structurallyEquals(d)) ok++; }
            rows.add(new String[]{name,"legacy-sized-synthetic-regression",String.valueOf(src.size()),
                    String.valueOf(parsed.policies().size()),String.valueOf(ok),f(ok/(double)src.size())});
        }
        Csv.write(results.resolve("rq1_transformation.csv"),rows);

        // Robustness checks for the controlled grammar. These are not the original case-study data.
        List<String[]> robust=new ArrayList<>();
        robust.add(new String[]{"case","expected_accept","actual_accept"});
        String[] valid={
            "R1 | DOCTOR | RECORD | read | Permit",
            "R2 | role | NURSE | RECORD | read,update | Deny"
        };
        String[] invalid={
            "R3 | DOCTOR | RECORD | Permit",
            "R4 | DOCTOR | RECORD | read | Maybe",
            "R5 | DOCTOR | RECORD |  | Permit"
        };
        int k=1;
        for(String s:valid) robust.add(new String[]{"valid-"+k++,"true",String.valueOf(accepts(s))});
        k=1;
        for(String s:invalid) robust.add(new String[]{"invalid-"+k++,"false",String.valueOf(accepts(s))});
        Csv.write(results.resolve("rq1_input_robustness.csv"),robust);
    }

    private static boolean accepts(String s){try{ControlledTextParser.parse(s);return true;}catch(Exception e){return false;}}
    private static String f(double d){return String.format(Locale.ROOT,"%.6f",d);}
}
