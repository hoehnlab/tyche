---
name: beauti-integration
description: Use when writing or debugging a BEAUti GUI integration for a BEAST2 package -- fxtemplates, InputEditors, BeautiAlignmentProvider/PriorProvider, or anything involving connectors, subtemplates, or parameter resizing in a BeautiDoc-driven document.
---

# BEAUti Integration

BEAUti's internals are largely undocumented and behave unintuitively.
**Verify every mechanic below against the actual BeastFX/beast2 source in
the target repo before relying on it** -- these are confirmed true against
one specific version (BeastFX bundled with BEAST 2.7.x, as used by TyCHE),
not guaranteed stable across versions. Where a fact is version-fragile,
it's marked. Never assume a mechanism works the way a docstring or method
name implies; grep the actual implementation first.

## Architecture, in one pass

- `BeautiDoc` -- the live document. Owns `pluginmap` (`Map<String,
  BEASTInterface>`, every object by ID) and `beautiConfig`.
- `BeautiConfig` -- owns `subTemplates` (every registered
  `BeautiSubTemplate`, flat list, not namespaced by merge point) and
  `suppressBEASTObjects` (a single global `Set<String>`, see below).
- `BeautiSubTemplate` -- one `<subtemplate>` element. `createSubNet(...)`
  parses its CDATA into `pluginmap`; `removeSubNet(...)` disconnects its
  connectors.
- `BeautiConnector` -- one `<connect>` element. Five `if=` predicate forms
  only: `inposterior(id)`, `inlikelihood(id)`, `nooperator(id)`,
  `isInitializing`, `id/inputName=value` (string equality via
  `.toString()`). **No `instanceof`/type-check predicate exists.** For
  anything needing real logic, use `method="fully.qualified.Class.method"`
  (signature `static void method(BeautiDoc doc)`) -- invoked unconditionally
  every sync, for side effects; bypasses the normal connect/disconnect path
  entirely (`isRegularConnector = false`).
- `InputEditorFactory` -- builds the panel for one `Input`. Picks an
  `InputEditor` by walking the *input's value's* class up its superclass
  chain against a registered map -- exact runtime class wins first, most
  specific match, not the declared field type.
- `PartitionContext` -- four separate name fields: `partition`,
  `siteModel`, `clockModel`, `tree`. `$(n)` substitution picks the field by
  the marker immediately before it: `.t:$(n)`→`tree`, `.s:$(n)`→`siteModel`,
  `.c:$(n)`→`clockModel`, bare `$(n)`→`partition`. Confirmed in
  `BeautiDoc.translatePartitionNames`. These can differ for the same
  object (e.g. a trait partition sharing another partition's tree).

## Registration -- where does X get discovered?

| You built... | Register via | Notes |
|---|---|---|
| `InputEditor` subclass | `version.xml`, service `beastfx.app.inputeditor.InputEditor` | Constructor `(BeautiDoc doc)` required, or discovery throws `NoSuchMethodException` silently caught -- editor just never appears. |
| `PriorProvider` implementation | `version.xml`, service `beastfx.app.beauti.PriorProvider` | Plain `newInstance()` -- **no-arg constructor only**, no `(BeautiDoc)` overload. Putting it under the `InputEditor` service instead fails the same silent way. |
| `BeautiAlignmentProvider` implementation | `version.xml`, service `beast.base.core.BEASTInterface`, **and** a `<alignmentProvider id="..." spec="..." template="@X"/>` element in an fxtemplate | The XML element is what makes it appear in the Alignments "+" menu; the service entry alone does not. |
| Plain Java helper/utility class (no `type()`, no BEASTObject) | Nothing | Reflectively invoked by fully-qualified name (`method="..."` connectors) or called directly -- no registry involved. |
| New `BeautiSubTemplate` choice for an existing dropdown (tree prior, clock model, subst model, parametric distribution) | `<mergewith point='...'>` in any fxtemplate, matching a real `<mergepoint id='...'/>` in the base template | Wrong/typo'd point name: silently dropped (`Log.warning`, no exception). Matching is by *class assignability* against the target `Input`'s declared type -- **not** scoped to which merge point it arrived through. |
| A `<connect>`/`<subtemplate>` you want active regardless of any specific object | N/A -- no such thing exists cleanly | See "no wildcard connector" below. |

## Two structural traps that cost the most time

**1. `Parameter.Base` has two copies of its data.** `values[]` (live,
`getValue`/`setValue(int,T)`) and `valuesInput` (declared `List`, what
`XMLProducer` serializes on save) are independent.
`param.dimensionInput.setValue(n, param)` alone touches *neither* the live
array *nor* the saved list correctly -- looks like it worked (no error),
silently doesn't persist, or gets clobbered back by `initAndValidate()`
(`Math.max(dimensionInput.get(), valuesString.length)`). The only correct
resize:
```java
final int oldDim = param.getDimension();               // capture BEFORE setDimension
String valueString = IntStream.range(0, newDim)
        .mapToObj(i -> i < oldDim ? param.getValue(i) : defaultValue)
        .map(String::valueOf).collect(Collectors.joining(" "));
param.setDimension(newDim);                              // live values[] + dimensionInput
param.valuesInput.setValue(valueString, param);           // clears + rebuilds the declared List
param.initAndValidate();                                   // re-syncs values[] from that List
```
Symmetric for `RealParameter`/`IntegerParameter`/`BooleanParameter`. If a
parameter must sum to 1 (frequencies), don't preserve old values on
resize -- reset to uniform instead.

**2. Object IDs get truncated at the first `.` for "which implementation is
this" matching.** Two separate call sites do this: `addComboBox` (combo
preselection) and `removeSubNet(Object)` (package-private lookup when
tearing down before a swap). Both do
`id.substring(0, id.indexOf('.'))` and compare against a subtemplate's
`shortClassName` or main-ID pattern. Any object meant to be *selectable* or
*swappable* via one of BEAUti's own combos needs its ID to start with its
own class's simple name before the first dot --
`ClassName.ownerLabel.index`, never `ownerLabel.ClassName.index` or
`ownerLabel.dist0`. Get this backwards and you get an empty dropdown that
renders fine until the user tries to change it, then throws on
`removeSubNet`.

## Shared-context disconnects

`applyBeautiRules` gates *every* connector in a subtemplate on
`pluginmap.get(translatePartitionNames(mainid, context)) != null`, evaluated
once per `(template, context)` pair, every sync. A bare `mainid='$(n)'`
resolves via `context.partition` -- identical to a plain `Alignment`'s own
ID, so the subtemplate's connectors also get evaluated under *every*
unrelated partition sharing that bare namespace. This alone is harmless.
It becomes a real bug only combined with a second thing: a connector whose
`srcID`/`targetID` uses `.t:`/`.s:`/`.c:` (resolves via a field two
partitions can share, e.g. one tree) while its `if=` resolves differently
per partition. The legitimate context connects the object; the spurious
context resolves to the *same* target ID but a *false* condition, and
disconnects what the legitimate context just added.

- **Breaks:** connector targets `thing.t:$(n)` (shared-tree marker). Two
  partitions share a tree; whichever one's context is "spurious" for this
  subtemplate still resolves `.t:$(n)` to the same tree, and its own false
  condition disconnects the real one's object.
- **Doesn't break:** same connector, bare `thing.$(n)` instead. Spurious
  context resolves to a different (nonexistent) target; disconnect no-ops.
- **Confirmed fix, independent of either:** `if='isInitializing'`. See
  below -- `applyBeautiRules` never disconnects these at all.

If you change `mainid` for any reason, also check every caller relying on
`createSubNet`/`addAlignmentWithSubnet`'s *return value* matching the old
one -- it returns whatever matches the new `mainid`, possibly a different
class now.

## `suppressInputs` semantics

`doc.beautiConfig.suppressBEASTObjects` is one global `Set<String>` of
`"fully.qualified.Class.inputName"` strings -- not scoped to the
declaring subtemplate's own instances. Added once at `createSubNet`,
removed only by `removeSubNet` on the *declaring* subtemplate (full
teardown, not edit). To hide an input permanently for the life of a
session: add it to `suppressInputs` on every subtemplate whose deletion
could plausibly remove the last thing keeping it suppressed.

## Create-or-match, never-delete connectors -- confirmed

For a target several different partitions might all independently want to
ensure exists (a shared, tree-wide logger, say), `if='isInitializing'`
gives exactly this, confirmed by reading both halves directly:
```java
// BeautiDoc.applyBeautiRules
if (connector.atInitialisationOnly()) {
    if (isInitial) connect(connector, context);
    // no else -- disconnect() is unreachable for this connector, ever
} else if (isActivated(...)) { connect(...); } else { disconnect(...); }
```
```java
// BeautiDoc.connect -- already idempotent
if (o instanceof List) {
    if (((List<?>) o).contains(srcBEASTObject)) return;  // already there
}
```
`createSubNet` fires each subtemplate instance's own `isInitializing`
connectors once, at *that instance's* construction (its own local `init`
flag) -- so every partition gets a chance to ensure the shared object
exists; the first to run adds it, every later one's attempt is a no-op via
the guard above, and none of them can ever remove it on a later sync.
**Not yet explored:** behavior when the partition that added it is later
removed (does the connection linger, orphaned?) -- verify before relying
on this for anything that must clean up on removal.

There is still no `if=` form for "run this connector regardless of what
currently exists *and* regardless of mainid" -- the mechanism above still
requires *some* context under which the subtemplate's mainid resolves.
For genuinely unconditional logic, do the work in Java instead (partition
creation, dialog close, listener on a GUI field) -- see recipes below.

## Recipes

**Add a custom InputEditor.** `extends BEASTObjectInputEditor`, override
`type()` to return the target class, override `init(Input<?>, BEASTInterface,
int, ExpandOption, boolean)`. Call `super.init(...)` first if you want the
standard combo/edit-button chrome; pass `ExpandOption.TRUE` explicitly if
any caller might invoke you with `FALSE` (some do, and `simpleInit()`
builds no expansion box at all, breaking any code that assumes one exists).
Register in `version.xml`.

**Splice extra content into the generated layout.** `m_expansionBox` is
package-private -- can't reference it directly from another package.
Locate it by structure instead: `expandedInit()` guarantees the last child
of `getChildren()` is an `HBox`, whose sole child (if `isExpandOption !=
FALSE`) is the expansion `VBox`. Don't scan the whole tree recursively;
find that one node and append to it.

**Make a panel refresh on selection change.** Split "build the widget" from
"build its content": keep the `TitledPane`/container as a field built once;
rebuild only via `pane.setContent(buildContentFresh())` when the selection
changes. Never rebuild the `TitledPane` itself or re-splice into the parent
-- `setGraphic`/`setContent` in place avoids orphaned nodes and duplicate
children.

**Title-bar edit button, natively styled.** Copy `createEditButton`'s
button (`Button("e")`, 15x15px round, `-fx-background-radius: 10`), but
don't call the inherited method itself -- it closes over `m_input`
(the *editor's own* field), not whatever you pass it. Build your own
`BEASTObjectDialog(target, TargetClass.class, doc)` targeting the exact
object you mean. If that object's *identity* can change (e.g. it belongs to
a linked object that gets swapped when a combo changes), **look it up
fresh inside the button's `onAction`**, not once when the button is built.

**Right-align a title-bar button without truncating the title.**
`TitledPane.setGraphic()` alone left-aligns before the text.
`ContentDisplay.RIGHT` reorders but doesn't reach the far edge and can
truncate. Working pattern: empty-string title, build the whole header
yourself as an `HBox` (label + `Region` spacer with `HBox.setHgrow(spacer,
Priority.ALWAYS)` + button), set that as the graphic, and bind
`header.prefWidthProperty()` to `pane.widthProperty().subtract(~40)` (40px
≈ the built-in expand arrow) so the spacer has something to push against.

**Swap an object in a list, in place.** Don't use
`createSubNet(context, list, item, init)` -- it calls `removeSubNet`
*internally*, which can shrink the same list you're about to `.set()` into,
using a now-stale index. Use the standalone `createSubNet(context, init)`
overload, then `list.remove(old); list.add(newObj)` yourself. Call
`removeSubNet(anyTemplateInstance, oldObjsTemplate, context)` *before*
deleting the old object from `pluginmap` -- it reads `template.connectors`
from whichever template you pass as the argument (not `this`), and needs
the old `mainid` still resolvable to disconnect the old object's own
dependents correctly.

**Attach something to a `Tree`'s own state from outside its constructing
template.** Not a connector job. `Tree.m_traitList` (`Input<List<TraitSet>>`,
name `"trait"`) is a real list, but nothing statically nests a second
template's object into a tree built by a different, earlier template.
BEAST2's own tip-dates feature does this in Java
(`tree.setDateTrait(traitSet)`, called live from `TipDatesInputEditor`) --
mirror that: `tree.m_traitList.get().add(yourTraitSet)`, called once your
object actually exists (e.g. right after a modal dialog closes).

**Reconcile a list whose size depends on live GUI state (add/remove
default entries as a linked count changes).** No fxtemplate mechanism for
this either. Do it in Java, at the point you already have both counts:
grow by constructing new default objects and `list.add(...)`; shrink from
the end and log (don't silently discard) what was removed, unless the
values genuinely don't need preserving (e.g. frequencies, which must be
reset to uniform on any change anyway, not preserved).

**Headless/scripted construction.** `BeautiAlignmentProvider.getAlignments
(BeautiDoc, File[], String[])` is a real, documented entry point for
exactly this (cites BEASTLabs' `CompactAnalysis` as precedent) -- the base
implementation just delegates to the two-arg version, ignoring `args`;
override it yourself to do something with them. Works cleanly only if your
`addAlignments()` override never opens a GUI dialog. `Beauti.main()`'s own
CLI parsing (`BeautiDoc.parseArgs`) has no provider-selection mechanism --
don't try to add a flag there.

## Debugging checklist, by symptom

- **Editor never appears / falls back to generic rendering** → check
  `version.xml` service registration exists and matches the constructor
  shape the discovery mechanism expects (no-arg vs `(BeautiDoc)`).
- **Combo dropdown for a nested object is empty; changing it throws on
  save** → ID-truncation naming (see above).
- **A resize "worked" (no error) but reverts, or doesn't survive save** →
  Parameter two-copies bug (see above) -- check for a bare
  `dimensionInput.setValue(...)` with no `valuesInput` rebuild.
- **Something you built gets silently disconnected on the next sync, or
  never connects at all** → check the *declaring subtemplate's* `mainid`
  for a collision (bare `$(n)` matching an unrelated object), and check
  whether the connector needs `.t:`/`.s:`/`.c:` to resolve against the
  right `PartitionContext` field.
- **`IndexOutOfBoundsException` / `ArrayIndexOutOfBoundsException` deep in
  a substitution model or clock model at runtime, not at GUI-build time** →
  a parameter that's only ever written via `setValue(index, ...)`, never
  resized to match a real tree/state count, and a test case finally
  exceeded whatever fixed placeholder dimension the template declared.
- **`Cannot find merge point named X ... MergeWith ignored`** in console →
  typo'd or nonexistent `<mergewith point='...'>` target; check the base
  template for the real `<mergepoint id='...'/>` spelling.
