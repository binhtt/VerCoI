package vercoi.experiment;

import java.nio.file.Path;

public final class ExperimentRunner {
 public static void main(String[] args)throws Exception{
   Path results=Path.of(args.length>0?args[0]:"results");
   System.out.println("Running VerCoI reproducible experiments...");
   RQ1Experiment.run(results); System.out.println("RQ1 complete");
   RQ2Experiment.run(results); System.out.println("RQ2 complete");
   RQ3Experiment.run(results); System.out.println("RQ3 complete");
   RQ4ScalabilityExperiment.run(results); System.out.println("RQ4 complete");
   System.out.println("Results written to: "+results.toAbsolutePath());
 }
}
