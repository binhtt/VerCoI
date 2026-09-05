# Reproducibility guide

## Environment used in the manuscript experiments

The manuscript describes experiments executed with Java 17 from Eclipse IDE on a Dell Inspiron 14 5420 with a 12th Gen Intel Core i5-1235U processor (1.30 GHz), 12 GB RAM, and 64-bit Windows 11.

## Running the complete suite

Run:

```text
vercoi.experiment.ExperimentRunner
```

By default, result CSV files are written to `results/`. To avoid overwriting the supplied reference results, provide another output directory as the first argument, for example `results-local`.

## Individual experiment classes

- `vercoi.experiment.RQ1Experiment`
- `vercoi.experiment.RQ2Experiment`
- `vercoi.experiment.RQ3Experiment`
- `vercoi.experiment.RQ4ScalabilityExperiment`

## Regression test

Run:

```text
vercoi.test.RegressionSelfTest
```

The expected completion message is:

```text
All VerCoI V2 regression checks passed.
```

## Important provenance note

The current HACS-sized, FMS-sized, and SIS-sized regression fixtures are synthetic datasets preserving legacy case-study sizes. They are not the unrecovered original HACS/FMS/SIS raw inputs.

## Timing experiments

RQ4 uses 3 warm-up executions and 20 measured repetitions per configuration. Absolute timing values vary with hardware, operating system, JVM state, and background load. Compare algorithmic trends and rerun under a stable environment rather than expecting byte-for-byte timing equality.
