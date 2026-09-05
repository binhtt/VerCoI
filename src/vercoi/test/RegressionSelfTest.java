package vercoi.test;

import vercoi.generator.XacmlGenerator;
import vercoi.model.*;
import vercoi.parser.*;
import vercoi.verification.*;
import java.util.*;

/** Dependency-free regression checks; run as a normal Java application in Eclipse. */
public final class RegressionSelfTest {
    public static void main(String[] args) throws Exception {
        testAlgorithm1Grouping();
        testConflictDetectionAndResolution();
        testCompliance();
        testCommentFiltering();
        System.out.println("All VerCoI V2 regression checks passed.");
    }

    private static void testAlgorithm1Grouping() throws Exception {
        List<PolicyRule> r=List.of(
                new PolicyRule("R1","role","Doctor","Record",List.of("read"),Effect.Permit),
                new PolicyRule("R2","role","Nurse","Record",List.of("read"),Effect.Permit),
                new PolicyRule("R3","role","Auditor","Log",List.of("audit"),Effect.Permit));
        ParsedPolicy p=XacmlParser.parse(XacmlGenerator.generate("P",CombiningAlgorithm.FIRST_APPLICABLE,r));
        check(p.policies().size()==2,"Algorithm 1 must group three rules into two resource policies");
        check(p.rules().size()==3,"Round trip must preserve all rules");
    }

    private static void testConflictDetectionAndResolution(){
        PolicyRule a=new PolicyRule("A","role","Doctor","Record",List.of("read","update"),Effect.Permit);
        PolicyRule b=new PolicyRule("B","role","Doctor","Record",List.of("read"),Effect.Deny);
        check(ConflictDetector.findConflicts(List.of(a,b)).size()==1,"Expected one conflict");
        check(ConflictResolver.resolve(List.of(a,b),CombiningAlgorithm.DENY_OVERRIDES)==Effect.Deny,"deny-overrides failed");
        check(ConflictResolver.resolve(List.of(a,b),CombiningAlgorithm.PERMIT_OVERRIDES)==Effect.Permit,"permit-overrides failed");
        check(ConflictResolver.resolve(List.of(a,b),CombiningAlgorithm.FIRST_APPLICABLE)==Effect.Permit,"first-applicable failed");
        check(ConflictDetector.findConflicts(ConflictResolver.resolveToConflictFree(List.of(a,b),CombiningAlgorithm.DENY_OVERRIDES)).isEmpty(),"Resolved rule set must be conflict-free");
    }

    private static void testCompliance(){
        List<PolicyRule> exp=List.of(new PolicyRule("R","role","Doctor","Record",List.of("read"),Effect.Permit));
        check(ComplianceChecker.check(exp,JavaPolicyParser.parse("allow(\"Doctor\",\"Record\",\"read\");")).isEmpty(),"Clean implementation should comply");
        check(!ComplianceChecker.check(exp,JavaPolicyParser.parse("deny(\"Doctor\",\"Record\",\"read\");")).isEmpty(),"Effect inversion should be detected");
    }

    private static void testCommentFiltering(){
        String src="// allow(\"X\",\"Y\",\"Z\");\nallow(\"Doctor\",\"Record\",\"read\");\n/* deny(\"A\",\"B\",\"C\"); */";
        check(JavaPolicyParser.parse(src).size()==1,"Commented authorization calls must not be parsed");
    }

    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
