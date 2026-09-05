package vercoi.experiment;

import vercoi.generator.XacmlGenerator;
import vercoi.model.*;
import vercoi.parser.JavaPolicyParser;
import vercoi.parser.XacmlParser;
import vercoi.verification.ComplianceChecker;
import vercoi.verification.ConflictDetector;
import vercoi.verification.ConflictResolver;
import java.nio.file.Path;
import java.util.*;

public final class RQ4ScalabilityExperiment {
    private RQ4ScalabilityExperiment(){}

    public static void run(Path results)throws Exception{
        int[] sizes={50,100,200,500,1000,2000};
        int reps=20;
        List<String[]> raw=new ArrayList<>();
        raw.add(new String[]{"scenario","rules","repetition","transform_ms","parse_ms","conflict_ms","compliance_ms","conflicts","resolved_rules"});
        List<String[]> summary=new ArrayList<>();
        summary.add(new String[]{"scenario","rules","repetitions","transform_mean_ms","transform_sd_ms","parse_mean_ms","parse_sd_ms","conflict_mean_ms","conflict_sd_ms","compliance_mean_ms","compliance_sd_ms","mean_conflicts","mean_resolved_rules"});
        for(boolean heavy:new boolean[]{false,true}){
            String scenario=heavy?"paired-conflicts":"conflict-free";
            for(int n:sizes){
                for(int w=0;w<3;w++) measure("W",n,heavy);
                List<Measure> ms=new ArrayList<>();
                for(int r=1;r<=reps;r++){
                    Measure m=measure("S",n,heavy);ms.add(m);
                    raw.add(new String[]{scenario,""+n,""+r,f(m.transform),f(m.parse),f(m.conflict),f(m.compliance),""+m.conflicts,""+m.resolvedRules});
                }
                summary.add(new String[]{scenario,""+n,""+reps,
                        f(mean(ms,0)),f(sd(ms,0)),f(mean(ms,1)),f(sd(ms,1)),
                        f(mean(ms,2)),f(sd(ms,2)),f(mean(ms,3)),f(sd(ms,3)),
                        f(meanConflicts(ms)),f(meanResolvedRules(ms))});
            }
        }
        Csv.write(results.resolve("rq4_scalability.csv"),raw);
        Csv.write(results.resolve("rq4_scalability_summary.csv"),summary);
    }

    private static Measure measure(String sys,int n,boolean heavy)throws Exception{
        List<PolicyRule> rules=BenchmarkFactory.scalabilityRules(sys,n,heavy);
        long t0=System.nanoTime();
        String xml=XacmlGenerator.generate("P",CombiningAlgorithm.FIRST_APPLICABLE,rules);
        long t1=System.nanoTime();
        List<PolicyRule> parsed=XacmlParser.parse(xml).rules();
        long t2=System.nanoTime();
        int conflicts=ConflictDetector.findConflicts(parsed).size();
        long t3=System.nanoTime();
        List<PolicyRule> resolved=ConflictResolver.resolveToConflictFree(parsed,CombiningAlgorithm.FIRST_APPLICABLE);
        String java=BenchmarkFactory.javaSource(resolved);
        ComplianceChecker.check(resolved,JavaPolicyParser.parse(java));
        long t4=System.nanoTime();
        return new Measure(ms(t1-t0),ms(t2-t1),ms(t3-t2),ms(t4-t3),conflicts,resolved.size());
    }

    private static double mean(List<Measure> xs,int field){double s=0;for(Measure m:xs)s+=value(m,field);return s/xs.size();}
    private static double sd(List<Measure> xs,int field){double mu=mean(xs,field),s=0;for(Measure m:xs){double d=value(m,field)-mu;s+=d*d;}return Math.sqrt(s/(xs.size()-1));}
    private static double value(Measure m,int f){return switch(f){case 0->m.transform;case 1->m.parse;case 2->m.conflict;default->m.compliance;};}
    private static double meanConflicts(List<Measure> xs){double s=0;for(Measure m:xs)s+=m.conflicts;return s/xs.size();}
    private static double meanResolvedRules(List<Measure> xs){double s=0;for(Measure m:xs)s+=m.resolvedRules;return s/xs.size();}
    private static double ms(long ns){return ns/1_000_000.0;}
    private static String f(double d){return String.format(Locale.ROOT,"%.3f",d);}
    private record Measure(double transform,double parse,double conflict,double compliance,int conflicts,int resolvedRules){}
}
