# M9A reproducible evaluation harness

M9A establishes measurements without changing the production algorithm, queries, indexes, caching, or database design. A **test** verifies a known implementation property, an **experiment** measures behavior in a defined synthetic scenario, and a **benchmark** measures latency/scaling under a defined workload. Passing tests alone is not evidence of academic superiority.

## Research questions and Trust method

1. Is the production crowdsourced Trust estimator more robust than an arithmetic mean under repeated contributors, stale observations, and sparse evidence?
2. How does end-to-end backend latency scale with feedback volume?
3. Does the offline-first repository recover from controlled transient failures without duplication?

The evaluation-only baseline is `sum(scores) / count`; it has no prior, recency, contributor cap, or confidence weighting and is never exposed by an endpoint. The proposed estimator is not reimplemented: the test invokes the package-visible production `TrustAggregationService.aggregate`. Production uses prior mean 3 with weight 2, 30-day half-life (`0.5^(ageDays/30)`), and caps each contributor's total effective weight at 1. Confidence is the authoritative `ConfidenceLevel` based on unique contributors.

## Deterministic Trust experiments

All experiments use a fixed UTC clock, deterministic UUIDs and timestamps, and emit tidy `M9_RESULT,experiment_id,variant,metric,value` records.

- **E1 Normal consensus:** twenty fresh independent ratings, evenly split between 3 and 4, measure shrinkage from their 3.5 mean.
- **E2 Repeated-contributor manipulation:** compare a controlled reference value of 4 from twenty contributors with an attacked set containing twenty score-1 reports from one account; measure score distortion and computed distortion reduction.
- **E3 Stale versus recent state:** ten old independent score-2 observations and ten recent independent score-4 observations at 30, 60, and 90 days; measure recency sensitivity against the controlled current-state reference of 4.
- **E4 Sparse extreme evidence:** one fresh score-5 observation; measure attenuation toward neutral 3. This demonstrates conservatism, not accuracy without field-derived truth.
- **E5 Contributor diversity:** compare twenty score-5 observations from one contributor with the same arithmetic mean from twenty contributors.
- **E6 Confidence growth:** measure 1, 2, 3, 5, and 6 independent contributors and regression-test LOW/MEDIUM/HIGH transitions.

Run from the repository root on Windows PowerShell:

```powershell
./evaluation/run-trust-evaluation.ps1
```

The runner overwrites `results/trust-results.csv` and creates `trust-summary.md` only after a successful real execution. Relationships are assertions as well as measurements; documentation does not hardcode measured improvements.

## End-to-end performance benchmark

```powershell
./evaluation/run-performance.ps1
# Short smoke workload:
./evaluation/run-performance.ps1 -DatasetSizes 100,1000 -Warmups 2 -Iterations 5
```

The sequential single-client benchmark uses the real Spring Boot HTTP stack and PostgreSQL 17, defaults to 100/1,000/5,000/10,000 rows, five warmups and thirty measured requests per endpoint. It measures authenticated GETs for Trust heatmap, the area resolved from its first valid cell, feedback sync, admin overview, filtered reporting summary, and filtered paged admin feedback. Each response must succeed. Timing uses high-resolution `Stopwatch`; min, arithmetic average, nearest-rank p50/p95, and max retain full precision until CSV output.

Before timing each size, correctness gates require the overview and filtered summary counts to equal the exact seed size, a nonempty heatmap and resolvable area, and expected filtered admin records. A failed gate aborts rather than recording misleading latency. Deterministic SQL creates 100 valid evaluation contributors, cycles scores and recent timestamps, and distributes Bucharest coordinates across nearby grid cells.

### Isolation and secrets safety

The runner permits only `crowd_feedback_eval`, explicitly refuses `crowd_feedback`, starts (but does not replace) the Compose PostgreSQL service, creates the separate database, builds a temporary backend named `crowdtransportfeedback-m9-backend` on port 8081, and removes it in `finally`. Reset SQL independently checks the connected database before deleting only M9 evaluation identities. It never invokes `docker compose down -v`, drops a database, truncates development data, or writes credentials.

Existing `.env` values supply database/JWT configuration and `APP_ADMIN_EMAIL`/`APP_ADMIN_PASSWORD`; missing values fail clearly. AdminBootstrap provisions the admin. Passwords and returned tokens are held only in memory and are neither printed nor included in results. The runner writes non-sensitive environment metadata and measurements only after successful collection. Final dissertation BEFORE/AFTER measurements must be made on the same developer machine and software setup; cloud/container measurements are not substitutes.

## Reliability evaluation

```powershell
./evaluation/run-reliability.ps1
```

`M9ReliabilityEvaluationTest` uses in-memory Room and a deterministic fake `FeedbackApi` for controlled repository-level fault injection. Each scenario runs 30 iterations: **R1** offline create then recovery (one server row and synchronized local row), **R2** transient owner-delete then recovery (eventual removal without resurrection), and **R3** five repeated synchronizations after recovery (stable state and no duplicate POST/Room row). It asserts all attempts succeed and duplicates remain zero, and emits machine-readable results. The runner targets only this instrumentation class and refuses to fabricate output when no authorized emulator/device is connected.

## Outputs and interpretation

Generated files are ignored by Git: `trust-results.csv`, `trust-summary.md`, `performance-results.csv`, `performance-environment.txt`, and `reliability-results.csv`. Runners warn that stable paths are overwritten. Preserve a result set with its commit SHA and environment externally before comparing M9B.

A measured positive distortion reduction is scenario-specific evidence of robustness, not universal superiority. Synthetic reference values are controlled reference values, not real ground truth. Sparse prior shrinkage demonstrates conservatism and does not establish accuracy.

## Threats to validity

- Crowdsourcing scenarios and contributors are synthetic.
- Controlled reference values are not field-derived ground truth.
- Performance is a single-machine benchmark and depends on hardware, OS, JVM, Docker, and PostgreSQL versions.
- Sequential requests characterize single-client latency, not concurrent load or saturation.
- JVM/database warmup and background machine activity can affect results despite the fixed warmup policy.
- Dataset coordinates and score distributions are deterministic approximations of use, not a representative population sample.
- Repository-level network fault injection is controlled and is not equivalent to real cellular network behavior, process death, radio transitions, or long-duration field use.

These limits prevent claims of broad generalizability; later work should replicate measurements and add field-derived and concurrent-load studies.
