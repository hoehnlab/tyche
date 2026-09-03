# BEAUti Design Patterns for TyCHE (and beyond)

A guide to how BEAUti actually works, written from what broke while
building TyCHE's integration and what fixed it. Companion to
`docs/dev/SKILL.md` (same facts, agent-terse) and `docs/dev/beauti_decisions.md`
(TyCHE-specific choices). This file is the *general* mechanics; read it
once, then use the skill file as your quick-reference while working.

Every fact here was confirmed by reading the actual BeastFX/beast2 source,
not inferred from a docstring or a class name. If you're working against a
different BeastFX version, re-check anything marked **(confirmed against
BEAST 2.7.x)** before trusting it.

---

## 1. The three layers

A BEAUti integration is three separate things, and almost every confusing
bug this session came from mixing them up:

1. **Your model classes** — plain BEASTObjects (`TycheExpectedOccupancyClockModel`,
   `AncestralTypeLikelihood`, etc.). BEAUti never sees these directly; it
   only sees whatever XML gets built from them.
2. **The fxtemplate** — XML that describes *what objects to construct* and
   *how to wire them together*, declaratively, once. This is not "the GUI" —
   it's closer to a template for generating the analysis XML BEAST2 will
   actually run.
3. **`InputEditor` classes** — the actual GUI code, one per BEASTObject
   type, that renders a panel and lets the user edit *already-constructed*
   objects.

The fxtemplate builds the object graph. The `InputEditor` edits it. Neither
one knows about the other directly — they meet only through the objects
sitting in `BeautiDoc.pluginmap`, found by ID.

## 2. What an fxtemplate actually is

An fxtemplate file is XML containing some mix of:

- **`<subtemplate>`** — a named, reusable recipe for constructing one or
  more objects. Has an `id` (how *other* subtemplates refer to it), a
  `class` (used for matching it against a target `Input`'s type — see §5),
  a `mainid` (which constructed object represents "this subtemplate," for
  gating — see §6), and CDATA (the actual XML fragment to parse). Optionally
  `suppressInputs` (§7) and nested `<connect>` elements.
- **`<connect>`** — one wiring rule: "if `if=` holds, put `srcID` into
  `targetID`'s `inputName`." Lives either directly under a
  `<beauticonfig>` (rare, mostly the base template's own) or nested inside a
  `<subtemplate>` (the common case — the connectors that belong to that
  subtemplate's own object).
- **`<mergewith point='X'>`** — "splice this content into wherever the
  base template declares `<mergepoint id='X'/>`." This is how a package
  contributes to BEAUti *without editing BEAUti's own files*. The `point`
  name only affects *where the text lands structurally* — it does **not**
  scope who can later use it (see §5).
- **`<fragment>`** — a named, reusable XML snippet referenced by
  `$(m)`-style templates elsewhere (used for TyCHE's `GRTTreeOperators`,
  shared between the germline-compatible tree prior and — if reused —
  anywhere else that needs the same operator set).

**A worked example**, TyCHE's own clock-model subtemplate, annotated:

```xml
<subtemplate id='TycheExpectedOccupancy'
             class='tyche.evolution.branchratemodel.TycheExpectedOccupancyClockModel'
             mainid='tycheBranchRates.c:$(n)'
             suppressInputs='...comma-separated Class.inputName list...'>
    <![CDATA[
        <plugin spec='...TycheExpectedOccupancyClockModel' id="tycheBranchRates.c:$(n)">
            <input name="substitutionModel" idref="svs.s:$(n).tycheClockPlaceholder"/>
            ...
        </plugin>
        <parameter ... id="nodeTypes.$(n).tycheClockPlaceholder" .../>
    ]]>
    <connect srcID='...' targetID='mcmc' inputName='operator' if='...'/>
</subtemplate>
```

Picking this from a dropdown parses the CDATA (constructing the clock
model *and* its placeholder objects), registers everything by ID into
`pluginmap`, and from then on the connectors decide, every sync, whether
anything else should be wired to it.

## 3. What an `InputEditor` actually is

Every `InputEditor` is keyed to a Java type via `type()`. When BEAUti needs
to render a panel for some `Input`, `InputEditorFactory` looks at the
*actual runtime class* of that input's current value, and walks **up the
superclass chain** checking a registered map until it finds a match — so a
subclass without its own registered editor falls back to whatever its
nearest registered ancestor has, silently. This is why registering the
*wrong* editor for a class you don't own can hijack rendering for anything
that extends it, and why forgetting to register your own editor at all
just produces a generic fallback with no error.

Most editors extend `BEASTObjectInputEditor`, which does the standard
"combo box to pick an implementation, plus an expand button showing that
implementation's own inputs" chrome for free — call `super.init(...)` to
get it, then append your own content. Two things about this base class are
easy to get wrong:

- **It has two init paths** depending on `isExpandOption`:
  `expandedInit()` (builds the combo + a `VBox` of the object's own
  sub-editors) or `simpleInit()` (label + combo + edit button, no `VBox` at
  all). Code that assumes an expansion box always exists will break for any
  caller that passes `ExpandOption.FALSE` — which happens more often than
  you'd expect, since several internal callers default to it. If you're
  building an editor meant to always show its own custom content, force
  `ExpandOption.TRUE` yourself rather than trusting the caller.
- **`itemNr >= 0` means "this input is one item of a list, not the whole
  list."** `expandedInit()` correctly special-cases this in its combo-box
  code, but its `addInputs(box, (BEASTInterface) input.get(), ...)` call
  right after does **not** — it blindly casts `input.get()` to
  `BEASTInterface`, which throws `ClassCastException` if `input.get()` is
  actually a `List`. If you ever need "one editor per list element" (we did,
  for `ElementwisePrior`'s sub-distributions), don't pass the list-typed
  input with an item index — wrap the single element in your own
  synthetic, scalar `Input` (constructor overload that takes an explicit
  `Class<?>`, so `Input.getType()` doesn't need its own reflection-based
  field lookup to succeed — that lookup only works for inputs that are
  *actually* declared fields on the owning object, which a synthetic
  wrapper isn't).

## 4. How the two layers connect: discovery

Nothing in an `InputEditor` or a `BeautiAlignmentProvider` "just works" —
each has its own, different discovery mechanism, and mixing them up is a
guaranteed silent failure (see the table in `SKILL.md`). The two most
common wrong assumptions this session:

- Assuming a `PriorProvider` needs the same `(BeautiDoc)` constructor every
  `InputEditor` needs. It doesn't — plain `newInstance()`, no-arg only.
  Registering it as an `InputEditor` instead produces a caught-and-ignored
  `NoSuchMethodException`, with the class silently never appearing anywhere.
- Assuming registering a class under `beast.base.core.BEASTInterface` in
  `version.xml` is *sufficient* to make it selectable somewhere in the GUI.
  It only makes the class generally discoverable/instantiable — actually
  appearing in a specific dropdown or menu needs the *matching* mechanism
  for that specific spot (a `<subtemplate>` for tree-prior/clock/subst-model
  dropdowns, a `<alignmentProvider>` XML element for the Alignments "+"
  menu, a `PriorProvider` service entry for "+Add Prior").

## 5. Matching: how BEAUti decides "this fits here"

`BeautiConfig.getInputCandidates(parent, input, type)` is the whole
mechanism behind every "choose an implementation" dropdown: it walks
**every** registered `BeautiSubTemplate` (a single flat list — not scoped
by which merge point contributed it) and keeps any whose `_class` is
assignable to the target `Input`'s declared type, then checks
`input.canSetValue(...)` as a second gate. This has a real consequence:
**a `ParametricDistribution` subclass will show up in *every*
`ParametricDistribution`-typed dropdown anywhere in the document**, not
just the one you had in mind when you wrote it. This is by design (it's
how `Dirichlet` — a joint distribution over a whole vector — ends up
offered in the same dropdown as `Normal`, even though its shape is
completely different), but it means adding a new subtemplate is a global
change to every matching dropdown, not a local one.

**A concrete consequence worth internalizing:** if you want a new class to
appear in the *same* dropdown as `Normal`/`Uniform`/`Gamma` (i.e. wherever
a `ParametricDistribution` is expected), it has to genuinely extend
`ParametricDistribution` — extending `Distribution` directly and hoping it
gets picked up the same way will not work, because the *type* being
matched against, not just superficial similarity, is what's checked. Real
example: `ElementwisePrior` (extends `Distribution`, used standalone, in
the top-level "prior" list) and `ElementwiseParametricDistribution`
(extends `ParametricDistribution`, used *inside* a `Prior`, appears in the
per-parameter dropdown) are two different classes with overlapping logic —
not one class serving both roles — precisely because the matching
mechanism cares about the declared type, not the intent.

## 6. Shared-context disconnects

A subtemplate's connectors are evaluated once per `(template, context)`
pair, every sync — not once per subtemplate. Whether a *given* context
counts as "this subtemplate is active" depends only on whether `mainid`
resolves to something real under that context. A bare `mainid='$(n)'`
(resolving via `context.partition`) is true under *every* partition that
shares that bare namespace — e.g. a plain sequence `Alignment`'s own ID
is also just a bare partition name, so a subtemplate with `mainid='$(n)'`
gets evaluated again under that partition's context too, not only the one
it was written for. This isn't really about `mainid` being chosen badly —
it's a common, ordinary choice — it only becomes a problem in combination
with the next fact.

Whether that extra evaluation actually breaks anything depends on
something separate: whether the connector's own `srcID`/`targetID`
resolves to the *same string* under both contexts, while its `if=`
condition resolves *differently*. `.t:$(n)`/`.s:$(n)`/`.c:$(n)` resolve
via `context.tree`/`siteModel`/`clockModel` — fields two different
partitions can genuinely share (a trait partition linked to another
partition's tree, say). Conditions, meanwhile, usually check something
partition-specific (`inposterior(someLikelihood.$(n))`, bare — different
per partition). So: the legitimate context evaluates its condition true
and connects the object. Any *other* context that also gets to evaluate
this same connector — through exactly the mainid collision above —
resolves to the *same* target ID (because the connector's own marker made
it partition-independent) but finds its *own* condition false, and
disconnects the object the legitimate context just connected.

**Toy example, breaks:** subtemplate `Foo`, `mainid='$(n)'`, connector
`srcID='thing.t:$(n)' targetID='mcmc' inputName='logger' if='...'`.
Partitions `traitA` (trait) and `seqB` (sequence), sharing one tree.
Real pass, context `traitA`: `mainid` resolves to the real `Foo` object,
connector's `if=` true, `thing.t:$(n)` → `context.tree` = `"seqB"` →
connects `thing.t.seqB`. Spurious pass, context `seqB`: `mainid` *also*
resolves (to the plain `seqB` alignment — nothing to do with `Foo`).
`thing.t:$(n)` → `context.tree` = `"seqB"` again — same string. `if=`
false under this context → disconnects `thing.t.seqB`, the object the
real pass just added.

**Toy example, doesn't break:** same setup, connector uses bare
`thing.$(n)` instead. Real pass: `thing.$(n)` → `context.partition` =
`"traitA"` → connects `thing.traitA`. Spurious pass: `thing.$(n)` →
`context.partition` = `"seqB"` — a *different* string. Disconnect
targets `thing.seqB`, which was never constructed. No-op; `thing.traitA`
survives.

**A confirmed, robust fix independent of any of this:** `if='isInitializing'`
on the connector. `applyBeautiRules` never lets an `atInitialisationOnly()`
connector reach the `disconnect()` branch at all — on every ordinary sync
it's either connected (if `isInitial`) or skipped entirely. Paired with
`connect()`'s own built-in duplicate guard (already refuses to re-add
something already present in a list target), this gives "create or
match, never delete" — safe for a target several different partitions
might all legitimately want to ensure exists. **Not fully explored yet:**
what happens to an `isInitializing`-only connection when the partition
that added it is later removed (does it linger, orphaned?), and whether
`createSubNet`'s own per-instance firing of these connectors has other
edge cases. Worth a dedicated look before treating this as a universal
answer.

## 7. `suppressInputs`, precisely

The tempting mental model — "this subtemplate hides these inputs while
it's active" — is half right. What's actually true: `suppressInputs`
writes into **one global set** shared by the whole document
(`doc.beautiConfig.suppressBEASTObjects`), keyed by
`"fully.qualified.ClassName.inputName"` — not by which object, not by which
subtemplate declared it. It's added once when the declaring subtemplate is
*constructed*, and removed only when that same subtemplate is fully torn
down via `removeSubNet` (not merely edited). If you want an input hidden
for as long as *any* instance of a class might exist, and more than one
subtemplate can construct that class, add the same suppression string to
*every* one of them — otherwise deleting the "wrong" one un-hides it for
everything else too. This is exactly why TyCHE hides `symmetric` (its SVS
substitution model's mode flag) by listing it in `suppressInputs` on
*both* the trait-partition subtemplate and the clock-model subtemplate —
either one alone would un-suppress it the moment its own declarer is
deleted while the other is still present.

## 8. ID naming — the rule that explains three separate bugs

Two completely different BEAUti mechanisms both truncate an object's ID at
its *first* period and use what's left to decide "what is this," and both
assume the same convention: **the thing before the first dot is the class
name** (or matches a subtemplate's own short name/main-ID pattern),
everything after is disambiguating detail.

- `addComboBox` uses this to *preselect* the right entry in a "choose
  implementation" dropdown.
- `removeSubNet(Object o)` (package-private, only reachable indirectly) uses
  this to *find which subtemplate built this object*, so it can disconnect
  that subtemplate's connectors before a swap.

Every naming bug we hit this session was the same mistake in different
clothes: naming something `ownerLabel.ClassName.index` instead of
`ClassName.ownerLabel.index`. Concretely:
- `ElementwisePrior`'s auto-added `Uniform` placeholders were originally
  named `ownerLabel + ".dist" + i` — truncating gave back `ownerLabel`
  (which happened to equal the *containing* class's own name), so the
  dropdown silently self-selected "ElementwiseParametricDistribution"
  for a `Uniform` object. Fix: `d.getClass().getSimpleName() + "." +
  ownerLabel + "." + i`.
- `typeLinkedRatesPrior`'s nested `ElementwiseParametricDistribution`/
  `Normal` objects had the identical bug, one level deeper — empty
  dropdown, then a hard `RuntimeException: Cannot find template for
  removing ...` the moment someone tried to *change* one (that's
  `removeSubNet` failing the same lookup).

**Rule of thumb:** any object that will ever appear in, or be swappable
through, one of BEAUti's own "choose an implementation" mechanisms needs
an ID starting with its own simple class name before the first dot. Plain
parameters, taxon sets, and anything only ever referenced by a fixed
`idref` — not subject to this at all — can be named however you like.

## 9. `$(n)` substitution — four different answers to "which partition"

`PartitionContext` carries four independent name fields
(`partition`/`siteModel`/`clockModel`/`tree`), and the two characters right
before `$(n)` decide which one you get: `.t:`→tree, `.s:`→siteModel,
`.c:`→clockModel, nothing→partition. These genuinely differ whenever a
trait partition is linked to a tree that belongs to a *different*
partition — which for TyCHE is the normal case, not an edge case.

This split explains why `nodeTypes.$(n)` deliberately uses the bare form
(it must be unique *per trait*, and two traits can share one tree, so
tying it to `.t:$(n)` would make two different traits collide on one
array) while the same file's tree-with-trait logger deliberately splits
across *both* forms on purpose: the outer `Logger` object uses bare
`$(n)` (so it's the trait's own, not shared), while the inner
`TreeWithTraitLogger` log item and its `metadata` connectors use `.t:$(n)`
(so multiple traits sharing one tree correctly write into the *same*
logger object rather than each trying to build their own).

This split matters for a reason distinct from §6's disconnect risk. The
*metadata* connector — attaching `expectedOccupancies` into the log item
— lives in the clock model's own subtemplate (`mainid='tycheBranchRates.c:$(n)'`,
never bare, never at risk of the §6 problem at all). The connector that
*was* at risk is the outer `Logger`'s attachment to `mcmc`, which lives in
the trait subtemplate (bare `mainid='$(n)'`). Using `.t:$(n)` for that one
specific connector's target reproduced §6's toy example exactly — a
sequence partition sharing the same tree could spuriously disconnect it.
Bare `$(n)` for that one connector's target avoided it, for the same
reason the second toy example does. `if='isInitializing'` (§6) would be
worth revisiting here too, once its other edge cases are better
understood.

## 10. The two-copies parameter bug, in full

Every `Parameter.Base` (`RealParameter`, `IntegerParameter`,
`BooleanParameter`) keeps two representations of its values:

- **`values[]`** — a live array. `getValue(i)`/`setValue(i, v)` read and
  write this. This is what MCMC operators actually see at runtime.
- **`valuesInput`** — a declared `List<T>` (`Input`, name `"value"`).
  `XMLProducer` reads *only* this when serializing a save — never the live
  array.

`setDimension(n)` resizes `values[]` (tiling from the old array) and
updates a *third* thing, `dimensionInput` (a scalar `"dimension"` Input) —
but never touches `valuesInput`. Meanwhile `initAndValidate()` does
`dimension = Math.max(dimensionInput.get(), valuesString.length)` — so if
you resize by hand and don't also fix `valuesInput`, the very next
`initAndValidate()` call silently pads your resize right back up to
whatever `dimensionInput` (or the stale declared list) still says.

The practical upshot, seen twice this session in two different disguises:
calling `param.dimensionInput.setValue(n, param)` alone *looks* like it
worked (no exception), and even reads back correctly if you print the
same object you just mutated — but the saved file shows the old dimension,
because nothing ever touched `valuesInput`. The only reliable fix is the
full sequence in `SKILL.md` §2 — resize the live array, rebuild the
declared list from a string (which correctly `clear()`s it first, unlike
passing a raw non-String value to `Input.setValue`, which *appends* to a
`List`-typed Input instead of replacing it — yet another version of the
same "which copy did I actually touch" trap), then re-run
`initAndValidate()` so both copies agree.

## 11. Making a panel that refreshes correctly

The working pattern, used for every "rebuild this when the user changes
something elsewhere" panel in TyCHE's clock-model editor: keep the
`TitledPane` (or whatever container) as a field, built exactly once, and
on every refresh call `pane.setContent(buildContentFresh())` — never
rebuild or re-parent the `TitledPane` itself, and never touch the parent's
child list again after the first build. This sidesteps needing to know
which index in a list to replace, and avoids orphaned nodes accumulating
in the scene graph.

One subtlety worth knowing before you build a title-bar button for a
panel like this: **whether the button needs to look itself up fresh
depends on whether the *object it targets* survives a refresh.**
`typeLinkedRates` belongs permanently to the clock model and is only ever
resized, never swapped — its edit button can safely close over it once, at
build time. `relativeGeoRates` belongs to whichever trait is currently
*linked*, and that link can change when the user picks a different trait
partition from a combo — its edit button has to re-resolve the linked
trait and re-fetch the parameter *inside* its own `onAction`, every click,
or it'll silently keep editing the previous trait's parameter after a
switch.

## 12. Attaching to a `Tree`'s own state, from outside

`Tree.m_traitList` (a real `Input<List<TraitSet>>`, name `"trait"`) looks
like something you should be able to reach via a `<connect
srcID='...' targetID='Tree.t:$(n)' inputName='trait'/>`. A confirmed,
reliable way to do this instead: mirror BEAST2's own mechanism for the
identical problem (attaching a date trait to a tree) — plain Java,
`tree.setDateTrait(traitSet)`, called from `TipDatesInputEditor`. There's
no equivalent convenience method for an arbitrary, non-"date" trait, but
the underlying operation is the same: `tree.m_traitList.get().add(yourTraitSet)`,
called once, at the moment your object genuinely exists (for TyCHE: right
after the trait-entry dialog closes).

## 13. Where to actually go read the source

When something doesn't behave the way this guide says it should, these
are the exact files that were authoritative this session, in the order
worth checking:

- `beastfx.app.inputeditor.BeautiDoc` — `translatePartitionNames`,
  `applyBeautiRules`, `scrubAll`, `parseArgs`, `pluginmap`.
- `beastfx.app.inputeditor.BeautiSubTemplate` — `createSubNet` (all
  overloads), `removeSubNet`, `suppressedInputs` handling.
- `beastfx.app.inputeditor.BeautiConnector` — the five `if=` forms,
  `method=` handling.
- `beastfx.app.inputeditor.InputEditorFactory` / `BeautiConfig` —
  editor/candidate matching by class.
- `beastfx.app.inputeditor.BEASTObjectInputEditor` — `expandedInit`,
  `simpleInit`, `createEditButton`, `addComboBox`.
- `beast.base.inference.parameter.Parameter` — `setDimension`,
  `initAndValidate`, the `values[]`/`valuesInput` split.
- `beast.base.core.Input` — `setValue`, `setStringValue` (the
  append-vs-replace trap).
