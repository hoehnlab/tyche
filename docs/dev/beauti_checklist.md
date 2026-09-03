# TyCHE BEAUti — Smoke Tests

Companion to `tyche_beauti_decisions.md`. Use the checklist after any fxtemplate or
BEAUti-editor change, before trusting a build, and update if the GUI 
or expected output _should_ change. 

---

## Smoke test

Minimum test case: one sequence partition (any FASTA/NEXUS with a tip
named `Germline`, at least 4 taxa), imported via **Import Germline-Root
Alignment**. Add a TyCHE trait partition (**Add TyCHE Trait**) linked to
that tree, enter at least 2 distinct trait values plus the default
`newTrait` name unchanged. Pick **Tyche Expected Occupancy** as the clock
model for the sequence partition, link it to the trait partition. Save.
Check the saved XML for all of the following. Any single failure means
stop and fix before testing further down the list — most later items
depend on earlier ones actually having worked.

### Tree and tree prior
- [ ] `Tree.t:<partition>` is `spec="tyche.evolution.tree.GermlineRootTree"`.
- [ ] No `YuleModel`/`YuleBirthRatePrior`/Yule operators anywhere in the file.
- [ ] `GRTBayesianSkyline` present, its `treeIntervals` is
  `spec="tyche.evolution.tree.GRTIntervals"` (not plain `TreeIntervals`).
- [ ] Skyline operators present: `GRTBactrianScaleOperator` (`rootOnly="true"`),
  `GRTSubtreeSlide`, `GRTExchange` (×2, one `isNarrow="false"`),
  `GRTWilsonBalding`.
- [ ] `EpochFlexOperator` (×2), `TreeStretchOperator`, `BactrianNodeOperator`
  are present **unswapped** (plain, not `GRT*`) — swapping these would
  be a regression, not a fix.
- [ ] `obs.prior` present, `monophyletic="true"`, taxon set excludes the
  tip named `Germline`.
- [ ] `Germline.prior` present, `tipsonly="true"`, taxon set contains only
  `Germline`, has a `Uniform` `distr` child.

### Trait partition
- [ ] `traitSet.<trait>` (`traitname` = whatever the user set, default
  `tycheType`) is nested **inside** `Tree.t:<partition>`'s own `<trait>`
  list, alongside the date trait if tip dates are used.
- [ ] `nodeTypes.<trait>` dimension equals the tree's actual node count
  (leaf + internal), not `100`.
- [ ] `expectedOccupancies.c:<partition>` dimension equals `nodeTypes`'
    dimension (the tree's real node count) — not `100`.
- [ ] Four operators present: `nodeTypesUniformTreeOperator`,
  `nodeTypesSubtreeTypeSwitchOperator`, `nodeTypeHeightOperator`,
  `rootHeightAndTypeOperator` — each `traitName` equal to the *real*
  `traitname` on `traitSet`, not the partition name.
- [ ] `rateIndicator.s:<trait>` and `relativeGeoRates.s:<trait>` have the
  **same** dimension as each other, equal to `n(n-1)` (asymmetric
  default) for the actual number of distinct trait values entered —
  not `n(n-1)/2`, not left at the template's placeholder size.
- [ ] `traitfrequencies.s:<trait>` dimension equals the number of distinct
  trait values, values sum to 1 (uniform, e.g. `0.5 0.5` for 2 states).
- [ ] `symmetric` attribute is **not present** anywhere in the saved file
  (confirms the checkbox is suppressed, not just defaulted).

### Clock model
- [ ] `tycheBranchRates.c:<partition>` is nested directly inside
  `treeLikelihood.<partition>` (no connector needed for this — confirm
  it's there, don't go looking for a connect rule).
- [ ] Its `nodeTypes`, `typeSwitchClockRate`, `substitutionModel` all point
  at the **real** trait partition's objects (matching IDs with no
  `.tycheClockPlaceholder` suffix) — not the clock model's own
  placeholders.
- [ ] `typeLinkedRates.c:<partition>` dimension equals the number of
  distinct trait values.
- [ ] `typeLinkedRatesIndicator.c:<partition>` present, `estimate="false"`
  on the indicator itself, dimension matches `typeLinkedRates`.
- [ ] `typeLinkedRatesScaler.c:<partition>` operator present, references
  the indicator above.
- [ ] `typeLinkedRatesPrior.c:<partition>` present as a `<prior>` wrapping
  `spec="tyche.inference.distribution.ElementwiseParametricDistribution"`
  — and its `id` and every nested `<distribution>`'s `id` start with
  the class's own simple name (`ElementwiseParametricDistribution...`,
  `Normal...`), **not** `typeLinkedRates...`. This is the ID-truncation
  rule from `tyche_beauti_decisions.md` — get it wrong and the dropdown renders
  empty and "change distribution" throws on save.
- [ ] `treeWithTraitLogger.<trait>` (bare `$(n)`, not `.t:`) present, its
  inner `TreeWithTraitLogger` log item contains **both**
  `traitedtreeLikelihood.<trait>` and `expectedOccupancies.c:<partition>`
  as `metadata` entries.

### GUI behavior (can't grep for these — check by hand)
- [ ] Switching the Clock Model tab's "Trait partition" combo correctly
  rebuilds the rate rows, internal-node dropdown, and transition-rate
  panel for the newly selected trait — no stale rows from the
  previous partition.
- [ ] "Relative State Transition Rates" panel: toggling one direction's
  checkbox in **symmetric** mode also toggles the paired reverse-
  direction row; in **asymmetric** mode (the default) it does not.
  Unchecking a row visibly disables (greys) its rate field.
- [ ] Both edit-dot buttons (Type Linked Rates title, Relative State
  Transition Rates title) open a native parameter dialog and target
  the *correct current* parameter after switching trait partitions —
  not a stale one captured at panel-build time.
- [ ] No console warnings of the form `cannot determine ambiguous tips
  without a traitName that matches a traitset` — presence means the
  traitname-sync or tree-attachment step silently didn't run.
- [ ] No `Could not find beastObject with id ...` errors tied to anything
  we actually expect to exist (some benign ones — e.g. probing an
  unrelated partition's `traitedtreeLikelihood` — are expected noise;
  see `tyche_beauti_decisions.md`'s ID-scoping section for which ones are real).

