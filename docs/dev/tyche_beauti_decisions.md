# TyCHE BEAUti Integration — Decision Log

Internal record of *why* the BEAUti integration is built the way it is. Code is
ground truth for *what*; this file is ground truth for *why*. If this log and
the code disagree, trust the code and fix this file.

Written in decision order. Each entry: what we chose, why (once), and any
fact worth remembering so you don't have to re-derive it.

---

## Germline root tree: partition-import, not a live toggle

**Decision:** "Germline Root Tree" is a choice made once, at partition
creation (`GermlineRootAlignmentProvider`, an `Import Germline-Root
Alignment` menu entry), not a toggle that converts an existing document.

**Why:** BEAUti has no native mechanism to swap a `Tree` StateNode's
*class* after creation — every other swappable component (tree prior, subst
model, clock model) has one; the tree itself doesn't. A live toggle needs a
full `getOutputs()`-based reference-repointing pass, has to fight
`StateNodeInitialiser` collisions (`RandomTree` is also `instanceof Tree`),
and can't be made to reliably revert on removal. Doing the conversion once,
right after the standard partition template builds a normal partition, has
none of these problems — the tree's whole reference graph is fresh and
fully known at that moment.

**What it does, in order** (`GermlineRootAlignmentProvider.addAlignments()`):
1. `super.addAlignments()` — build the *standard* partition (unchanged).
2. `convertToGermlineRootTree()` — swap `Tree` → `GermlineRootTree`, using
   `repointReferences()` (walks `original.getOutputs()`, not a full-document
   scan — mirrors `BeautiDoc`'s own private `replaceInputs()`, which is not
   accessible from outside its package).
3. `replaceTreePriorWithGRTBayesianSkyline()` — swap the default Yule prior
   for a dedicated `GRTBayesianSkyline` subtemplate (see below). Must run
   *after* step 2, so the skyline's own `TreeIntervals` gets built pointing
   at the already-converted tree.
4. The four `nodeTypes`/tree operators and `traitSet`→tree attachment are
   *not* handled here — they belong to whichever `AncestralTypeLikelihood`
   trait partition ends up linked to this tree, and are handled in
   `TyCHEDiscreteTraitProvider`/`TyCHETraitInputEditor` instead (see below).
5. `addOutgroupPriors()` — add `obs.prior` (monophyletic on every taxon
   except one named "Germline") and `Germline.prior` (tips-only,
   `Uniform(-10000,10000)` height calibration on that one tip).

**Accepted limitation:** deleting the resulting partition does not revert
anything. No reliable "this object is now gone, undo the conversion" hook
exists in BEAUti's connector system for this. Not fixed; accepted.

## Tree-object and object-graph swapping mechanics

- **`removeSubNet(template, context)` before replacing an object**, not
  after. It disconnects every connector belonging to the *old* template
  while that template can still resolve — deleting the old object from
  `pluginmap` first (before calling this) permanently breaks its own
  connectors' ability to ever disconnect, orphaning dependents forever.
- **`createSubNet(context, list, item, init)` (the list+index overload) is
  unsafe here** — it calls `removeSubNet` internally, which can shrink the
  *same* list you're about to `.set()` into, using a now-stale index →
  `IndexOutOfBoundsException`. Use the standalone `createSubNet(context,
  init)` overload and do `list.remove(...)`/`list.add(...)` yourself.
- **`doc.determinePartitions()` before `doc.scrubAll(true, false)`**,
  matching `BeautiDoc.addAlignmentWithSubnet()`'s own internal pairing —
  call both together after any manual object-graph surgery, not `scrubAll`
  alone.
- **`repointReferences()` uses `original.getOutputs()`**, not a full
  `pluginmap` scan — cheaper, and matches how BEAST2 itself already tracks
  reverse references via `Input.setValue()`.

## Bayesian Skyline: a dedicated GRT subtemplate, not runtime conversion

**Decision:** `GRTBayesianSkyline` is its own real subtemplate in
`priors.xml` (`class='beast.base.evolution.tree.coalescent.Coalescent'` —
matching the same odd convention the *plain* `CoalescentBayesianSkyline`
subtemplate uses, needed purely for tree-prior category matching), swapped
in via `template.createSubNet(...)` — not "swap in plain Skyline, then
convert it in Java."

**Why:** the two-step version worked, but broke on save (`removeSubNet`
under-evaluated `posteriorPredecessors` for the newly-swapped object,
disconnecting its own just-added operators). Building the whole thing
correctly in one step, the same way BEAUti's own tree-prior combo does it,
has none of this fragility.

**Operator swap set**, matching `GermlineRootTree.isIncompatibleOperator()`
exactly — this is the authoritative rule, not something to re-derive:
- `Exchange`, `SubtreeSlide` (plain), `WilsonBalding` → always incompatible.
- `ScaleOperator` → only when `rootOnly=true`.
- `kernel.BactrianSubtreeSlide` → **also incompatible**, even though it's
  not caught by the model's own `instanceof SubtreeSlide` check (it extends
  `TreeOperator` directly). Its own docs admit it "can exceed the root and
  become a new root." The model's self-check has a gap here; the fxtemplate
  swaps it anyway (`GRTSubtreeSlide`, plain, not a Bactrian-kernel variant —
  no `GRTBactrianSubtreeSlide` exists; accepted efficiency cost).
- `EpochFlexOperator`, `TreeStretchOperator`, `BactrianNodeOperator` →
  confirmed safe by their own proposal logic (root excluded, or height-only
  scaling with no topology change). Left unswapped.
- Fragment: `GRTTreeOperators` in `priors.xml`, added "whenever an
  `AncestralTypeLikelihood` exists," independent of whether the TyCHE clock
  is even chosen — the four `nodeTypes`/tree operators belong to the trait,
  not the clock.

## ID scoping — the one rule that explains most of this session's bugs

**`.t:$(n)`, `.s:$(n)`, `.c:$(n)` are not decoration.** Confirmed directly in
`BeautiDoc.translatePartitionNames`: the marker picks which field of
`PartitionContext` (`tree`/`siteModel`/`clockModel`/`partition`) `$(n)`
resolves to. A trait partition sharing another partition's tree has a
*different* `partition.partition` than `partition.tree` — using the wrong
marker silently binds to the wrong object.

**Rule used throughout:** anything conceptually tied to the *tree* (like
`nodeTypes` used to be, briefly) uses `.t:`; anything permanent to *this*
partition/clock model uses bare `$(n)`. Concretely:
- `nodeTypes.$(n)` — bare, **not** `.t:$(n)`. Two trait partitions can share
  one tree but must never share one `nodeTypes` array.
- `typeLinkedRates.c:$(n)`, `expectedOccupancies.c:$(n)` — bare/`.c:`,
  permanent to the clock model, never swapped, only resized.
- Clock model placeholders (`svs`, `nodeTypes`, `traitClockRate`) —
  suffixed `.$(n).tycheClockPlaceholder`, so they can never collide with a
  *real* trait partition's own objects regardless of what it's named
  (previously relied on the accidental fact that the default trait name is
  `"newTrait"` — broke the instant a user picked a different name).
- `treeWithTraitLogger.$(n)` (bare) — deliberately **not** `.t:$(n)`. Tried
  `.t:$(n)` first; it caused the logger to be silently disconnected on save
  (see next section). Bare `$(n)` for the outer `Logger`, `.t:$(n)` for the
  inner `TreeWithTraitLogger`/its `metadata` connectors — this split is
  intentional, not an inconsistency.

**Combo/remove-template lookups also truncate at the first `.`** (recovers
a class name from `id2.substring(0, id2.indexOf('.'))`). Any object whose ID
is later resolved this way — anything offered in a "choose implementation"
dropdown — must have its *class name* (or a name matching a real
subtemplate) before the first dot. This bit us three separate times:
- `ElementwisePrior`/`Uniform` placeholders — fixed by naming
  `d.getClass().getSimpleName() + "." + ownerLabel + "." + i`.
- `typeLinkedRatesPrior`'s nested `ElementwiseParametricDistribution`/
  `Normal` objects — same fix, class name first.
- Not yet hit but worth remembering: any *new* nested swappable object
  needs this same convention from the start.

## `Parameter.Base` has two copies of its data — the recurring bug

`values[]` (live, used by `getValue`/`setValue(int,T)`, runtime-only) and
`valuesInput` (the declared `List`, what `XMLProducer` actually serializes
on save) are separate. `setDimension()` only touches `values[]` *and*
`dimensionInput` — never `valuesInput`. `initAndValidate()` then does
`Math.max(dimensionInput.get(), valuesString.length)`, so skipping
`setDimension()` silently pads a resize right back to the old size.

**The only correct resize sequence**, used everywhere a parameter's
dimension changes at runtime (`typeLinkedRates`, `expectedOccupancies`, `nodeTypes`,
`rateIndicator`, `relativeGeoRates`, `frequencies`):
```java
String valueString = IntStream.range(0, newDim)
        .mapToObj(i -> i < oldDim ? param.getValue(i) : defaultValue)
        .map(String::valueOf).collect(Collectors.joining(" "));
param.setDimension(newDim);                       // updates dimensionInput + values[]
param.valuesInput.setValue(valueString, param);    // clears + rebuilds the declared List
param.initAndValidate();                            // re-syncs values[] from that List
```
Capture `oldDim` **before** `setDimension()` — calling it after (as an
earlier draft did) makes `oldDim` always equal the new dimension, silently
skipping the "use the default for new slots" branch. Harmless there only
because `setDimension()`'s own tiling already filled every slot; don't copy
that ordering elsewhere.

`expectedOccupancies` is resized in `TycheClockInputEditor.updateModelsFromPartition()` (clock-model side), not TyCHEDiscreteTraitProvider (trait-partition side) — it reuses nodeTypes' already-correct dimension rather than re-deriving the tree's node count independently.

`frequencies` is the one exception: reset to uniform on any dimension
change rather than preserving old values, since frequencies must sum to 1
and "keep old, default new" would usually break that.

## SVS substitution model: asymmetric by default, `symmetric` hidden

**Decision:** `symmetric="false"` set explicitly in both places
`TycheSVSGeneralSubstitutionModel` is declared (real trait's `svs`, clock
model's placeholder `svs`). The `symmetric` input itself is suppressed via
`suppressInputs` in *both* declaring subtemplates (`TycheTraitTest` in
`temp.xml`, `TycheExpectedOccupancy` in `clockModels.xml` — both needed,
since suppression only persists as long as at least one declaring
subtemplate remains active).

**Why:** symmetric mode makes `rateIndicator`/`rates` dimension
`n(n-1)/2`, with H→N and N→H sharing one bit — biologically wrong for a
model that should support directional-only transitions by default.
Making it user-toggleable live was considered and rejected: nothing
re-runs the resize logic when the checkbox flips, so toggling it live
leaves `rateIndicator`/`rates` at the wrong dimension with no error until
the next MCMC step. Hiding it avoids the whole failure class rather than
building a listener to guard against it.

**Index formula, confirmed against `SVSGeneralSubstitutionModel`'s and
`GeneralSubstitutionModel`'s own `setupRateMatrix()`** — don't re-derive:
```java
// symmetric: upper-triangular pair count, i<j
// asymmetric: i*(n-1) + (j<i ? j : j-1)
```
`rateIndicator` and `rates` always share this same index space and
dimension as each other, regardless of mode — enforced by resizing both to
one shared `nRates` value, never independently.

## State-transitions UI: `TraitTransitionRatesPanel`, shared by two editors

Row-building logic for the H→N / N→H checkbox+rate-field UI lives once, in
`TraitTransitionRatesPanel` (package-private, not an `InputEditor`, no
`version.xml` entry needed), used by both:
- `TycheClockInputEditor` — the clock model's own panel, title "Relative
  State Transition Rates". Edit-dot on the title looks up `relativeGeoRates` **fresh on
  every click**, not once at panel-build time — this object's identity
  changes across partition switches, unlike `typeLinkedRates`, which
  doesn't and can be captured once.
- `TycheSVSInputEditor` — a dedicated editor for
  `TycheSVSGeneralSubstitutionModel` itself, registered so it's reachable
  as its own `InputEditor`. Its own edit-dot opens the native
  `BEASTObjectDialog` for `rates` specifically, not `indicator` (fully
  covered by the checkboxes already).

**Symmetric-pairing behavior**: in symmetric mode, toggling/editing one
direction's row updates the paired reverse-direction row to match (same
underlying bit/value); in asymmetric mode, rows are fully independent. This
branches on `substModel.isSymmetricInput.get()` read live, not assumed.

**Checkbox unchecked → rate field disabled**, via
`rateField.disableProperty().bind(allowedBox.selectedProperty().not())` —
a binding, not a listener, so it stays correct for the paired row in
symmetric mode automatically.

## `typeLinkedRates`: estimate checkboxes, one shared bound pair

- One `BooleanParameter` indicator per rate (`typeLinkedRatesIndicator`),
  feeding a single `ScaleOperator`'s own `indicator` input — replacing an
  earlier two-operator "comment one out to fix a rate" design, which had
  nothing to do with the SVS rate indicator despite similar naming.
- Checking/unchecking auto-toggles `typeLinkedRates.isEstimatedInput`: false
  only when *every* indicator entry is false, true otherwise.
- Upper/lower bounds: **no custom fields** — a title-bar edit-dot opens the
  native `BEASTObjectDialog` for `typeLinkedRates` directly, same mechanism
  as the SVS rates button. `upper` left unset in the fxtemplate
  deliberately (`RealParameter`'s own documented default is `+Infinity`;
  not a special case to handle).
- Title-bar layout: `TitledPane.setGraphic()` alone left-aligns before the
  title text; `ContentDisplay.RIGHT` reorders but doesn't reach the far
  edge and can truncate the title. Working solution: empty `TitledPane`
  title, a manually-built `HBox` (label + `Region` spacer with
  `Priority.ALWAYS` + button) as the graphic, with the `HBox`'s
  `prefWidthProperty()` bound to `pane.widthProperty().subtract(40)` (40px
  ≈ the built-in expand arrow's width).

## Trait partition mechanics

- **`traitSet`→tree attachment is Java, not a connector.** Confirmed by
  reading `TipDatesInputEditor` itself: BEAST2's own date-trait feature uses
  `tree.m_traitList.get().add(traitSet)` directly, not any `<connect>`
  rule, and no such trait is ever statically nested in the core partition
  template's CDATA either. Every attempt to do this via `<connect>` failed
  for a structural reason (see "mainid collisions" below) — this isn't a
  workaround, it's the same pattern BEAST2's own equivalent feature uses.
  Lives in `TyCHEDiscreteTraitProvider.editAlignment()`.
- **`traitname` is user-editable** (`TyCHETraitInputEditor`'s `traitEntry`
  field, default `"tycheType"`), not a fixed constant. The four
  `nodeTypes`/tree operators' `traitName` must match it exactly
  (`getTipMetaDataNames().contains(traitName)`) — kept in sync via
  `syncOperatorTraitNames()`, called both from `traitEntry`'s
  `setOnKeyReleased` listener (live edits) and once from
  `TyCHEDiscreteTraitProvider.editAlignment()` after the dialog closes
  (covers the case where the user never touches the field).
- **`TycheTraitTest`'s `mainid='$(n)'` collides with any plain sequence
  `Alignment`'s own bare ID.** Confirmed: this causes `applyBeautiRules` to
  spuriously re-evaluate this subtemplate's connectors under an unrelated
  sequence partition's context on every sync — the actual root cause
  behind the `treeWithTraitLogger` naming split above, and the reason a
  `traitSet`→tree `<connect>` rule could never be made reliable. Changing
  `mainid` to something collision-free (e.g.
  `traitedtreeLikelihood.$(n)`) is a real, correct fix, but requires
  updating `TyCHEDiscreteTraitProvider.getAlignments()` to stop relying on
  `addAlignmentWithSubnet()`'s return value matching the old `mainid`
  (fetch the `Alignment` from `pluginmap` independently instead). **Not yet
  done** — flagged, not fixed, since the Java-side attachment above made it
  unnecessary for the immediate bug.
- **`editAlignment()` genuinely re-runs on re-edit**, confirmed by tracing
  `AlignmentListInputEditor`'s double-click handler: it re-evaluates
  `matches()` across every registered provider and calls `editAlignment()`
  on whichever scores highest. `TyCHEDiscreteTraitProvider.matches()`
  returns `10` for any alignment with an `AncestralTypeLikelihood` output;
  the base class default is `1`. So all the resize logic inside
  `editAlignment()` correctly re-runs if a user later adds a state to an
  existing trait, not just at first creation.
- **Guess-from-taxon-name, ambiguous fallback**: `TyCHETraitInputEditor`'s
  own `guess()` already degraded gracefully (skips non-matching taxa)
  unlike `TipDatesInputEditor`'s equivalent (which aborts the whole
  operation on the first non-match — a real BeastFX bug, not ours, not
  fixed since it's core code). Extended ours so non-matching taxa get `?`
  explicitly rather than being omitted.
- **Building the codeMap must exclude literal `"?"` from the
  "assign this value a fresh state code" scan** — it already has a
  reserved, separately-appended universal-ambiguity entry; without the
  exclusion, typing `?` as a value got its own ordinary code *and* the
  universal entry, producing a malformed duplicate-`?` codeMap.

## Node-type initialization dropdown

`TycheClockInputEditor`'s "Initialization value for internal nodes"
dropdown is populated from the linked trait's `codeMap` (via
`getStateCount`/`getTraitFromInt`, the same helpers the rate rows use), not
a `TraitSet`. Selecting a value writes that trait's integer code into every
node index from `tree.getLeafNodeCount()` to `tree.getNodeCount()-1` on the
*shared* `nodeTypes` parameter (the same object `AncestralTypeLikelihood`
owns — the clock model only ever points at it, never owns a copy).

## Command-line import — researched, not built

No flag exists, or can be cleanly added, to BEAUti's interactive launcher
(`Beauti.main()` → `BeautiTabPane.initialise()` → `BeautiDoc.parseArgs()` —
confirmed no provider-selection mechanism in either). The real extension
point is `BeautiAlignmentProvider.getAlignments(BeautiDoc, File[],
String[])`, documented for exactly this ("from a scripting environment, see
CompactAnalysis in BEASTLabs" — that class was not available to inspect).
`GermlineRootAlignmentProvider`'s own pipeline has no GUI dialogs, so it's
headless-safe as-is; `TyCHEDiscreteTraitProvider` is not (needs the modal
trait-entry dialog). Would need its own small standalone tool, not a BEAUti
launcher flag. See stretch-goals doc.