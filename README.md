# VerCoI

VerCoI is a Java 17 research prototype for static verification of XACML-based access-control policies from specification to code. This repository contains the implementation and the reproducible experimental package used to evaluate policy transformation, conflict detection and resolution, Spec-to-Code compliance verification, and scalability.

## Main capabilities

1. **Policy transformation** — converts the supported controlled textual rule format to XACML and supports reverse/round-trip validation used in the experiments.
2. **Conflict detection and resolution** — detects contradictory rules using subject-attribute identifier, subject, resource, overlapping actions, and opposite effects; supports `deny-overrides`, `permit-overrides`, and `first-applicable`.
3. **Spec-to-Code compliance verification** — compares resolved policy rules with statically identifiable Java authorization declarations in the supported `allow(...)` / `deny(...)` idiom.
4. **Reproducible experiments** — provides executable RQ1-RQ4 experiments and CSV result files.

## Requirements

- JDK 17 or newer
- Eclipse IDE for Java Developers (optional; the project can also be compiled from the command line)
- No external Java libraries

## Eclipse setup

1. Open Eclipse.
2. Select `File -> Import -> General -> Existing Projects into Workspace`.
3. Select the repository directory.
4. Run one of the following classes:
   - `vercoi.gui.VerCoIApp` — graphical interface
   - `vercoi.experiment.ExperimentRunner` — all experiments
   - `vercoi.test.RegressionSelfTest` — regression checks

## Command-line build

From the repository root:

```bash
mkdir -p bin
javac -encoding UTF-8 -d bin $(find src -name "*.java")
java -cp bin vercoi.test.RegressionSelfTest
java -cp bin vercoi.experiment.ExperimentRunner results-local
```

On Windows PowerShell, Eclipse is the simplest way to run the project. Alternatively, compile the Java sources with JDK 17 and run the same main classes.

## Supported controlled-text format

Canonical format, one rule per line:

```text
RuleId | Subject | Resource | Action1,Action2 | Permit|Deny
```

Example:

```text
R1 | DOCTOR | MEDICAL_RECORD | read,update | Permit
```

Extended form preserving the subject-attribute identifier:

```text
RuleId | SubjectAttributeId | Subject | Resource | Action1,Action2 | Permit|Deny
```

When the subject-attribute identifier is omitted, the supported default is `role`.

## Experimental package

The `results/` directory contains the reference CSV outputs supplied with this artifact.

### RQ1 — Transformation

The synthetic regression fixtures preserve the sizes of the three legacy case-study configurations:

| Dataset | Rules | Correct round-trip |
|---|---:|---:|
| HACS-sized | 50 | 50/50 |
| FMS-sized | 45 | 45/45 |
| SIS-sized | 40 | 40/40 |
| **Total** | **135** | **135/135** |

See `results/rq1_transformation.csv` and `results/rq1_input_robustness.csv`.

### RQ2 — Conflict detection and resolution

Conflict-detection benchmark:

| Method | TP | FP | FN | TN | Precision | Recall | F1 | Specificity |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| VerCoI V2 | 90 | 0 | 0 | 90 | 1.000 | 1.000 | 1.000 | 1.000 |
| Internal recovered RuleId heuristic | 45 | 36 | 45 | 54 | 0.556 | 0.500 | 0.526 | 0.600 |

Resolution benchmark: 30/30 correct cases for each of `deny-overrides`, `permit-overrides`, and `first-applicable` (90/90 total).

See `results/rq2_conflict_cases.csv`, `results/rq2_conflicts.csv`, and `results/rq2_resolution.csv`.

### RQ3 — Spec-to-Code compliance

The mutation benchmark contains 180 faulty implementations and 90 clean controls. In the supplied reference run:

- TP = 180
- FP = 0
- FN = 0
- TN = 90

Accordingly, precision, recall, F1-score, specificity, and accuracy are all 1.000 for this controlled benchmark.

See `results/rq3_compliance_cases.csv`, `results/rq3_compliance.csv`, `results/rq3_by_operator.csv`, and `results/rq3_scope_boundary.csv`.

### RQ4 — Scalability

Scalability is evaluated for 50, 100, 200, 500, 1000, and 2000 rules under conflict-free and paired-conflict scenarios. Each configuration uses 3 warm-up runs followed by 20 measured repetitions. The supplied reference CSV reports transformation, parsing, conflict-detection, and compliance times together with sample standard deviations.

See `results/rq4_scalability.csv` and `results/rq4_scalability_summary.csv`.

Timing values are environment-dependent. The CSV files should be treated as the reference run included with this artifact; rerunning the experiment on another machine may produce different absolute times.

## Scope and limitations

VerCoI intentionally supports a defined subset of the full implementation-analysis problem. In particular, the Java recognizer handles explicitly supported authorization declarations such as:

```java
allow("Nurse", "MedicalRecord", "Read");
deny("Nurse", "MedicalRecord", "Write");
```

It is **not** a general Java semantic analyzer and does not claim to resolve arbitrary reflection, dynamic loading, complex aliasing, runtime-generated authorization decisions, or other unsupported program-analysis patterns.

## Dataset provenance

The original raw HACS, FMS, and SIS inputs are not included in this package. The 50/45/40-rule datasets used by the current executable regression benchmark are **legacy-sized synthetic regression fixtures** and must not be described as the original case-study datasets. Historical reported results, where retained for traceability, are explicitly separated from reproduced results.

See `data/original/README.md` and `results/legacy_reported_results.csv`.

## Repository structure

```text
VerCoI/
├── src/                 Java source code
├── data/                dataset provenance notes / optional original-data location
├── results/             reference experimental CSV outputs
├── sample_rules.txt     example controlled-text rules
├── .project             Eclipse project metadata
├── .classpath           Eclipse Java 17 classpath
└── .settings/           Eclipse compiler settings
```

## Reproducibility check

A clean build of this package with JDK 17 should complete:

```text
All VerCoI V2 regression checks passed.
```

No third-party Java dependencies are required.

## License

No open-source license is included yet. Until a license is added, normal copyright rules apply. A license can be added later if redistribution or reuse permissions are to be granted explicitly.
