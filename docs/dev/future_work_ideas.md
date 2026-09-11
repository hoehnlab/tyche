
## Stretch goals / future improvements

Ordered roughly by how self-contained each one is, not by priority. Many of these
are BEAUti integration and Sphinx/doc related, rather than new methodological 
features, which probably are not yet ready to be public.

### Java domain overload/constructor collisions (cross-reference integrity)

`java_domain.py`'s method/constructor directives are built on Sphinx's
*Python* domain machinery (`PyMethod`), which has no notion of
overloading -- it registers objects under `module.ClassName.methodName`
alone, with no parameter list in the identifier. Any class with multiple
constructors, or an overloaded method sharing a name, collides:
`WARNING: duplicate object description of ...`. Confirmed by inspecting
actual generated output (`TycheSVSInputEditor`, two constructors) that
this doesn't hide content -- both entries still render in full -- but
the second one loses its real, semantic anchor ID and falls back to an
auto-numbered one (`id0`), which is unstable across rebuilds. Any
cross-reference or external link specifically targeting an overload
past the first would be pointing at something that could silently
change identity next regeneration.

Real fix belongs in `java_documenter.py`'s signature-building step, not
in the generated RST by hand (defeats the point of automating this at
all): fold each overload's parameter types into its registered name so
they're never identical, or have the generator auto-add `:no-index:` to
every overload after the first sharing a name. Not a problem today --
nothing is missing, only fragile -- worth doing before anything ever
needs to link directly to a specific overload.

### Fix `TycheTraitTest`'s `mainid` collision properly
`mainid='$(n)'` collides with any plain sequence `Alignment`'s own bare ID,
causing spurious connector re-evaluation on every sync (see
`tyche_beauti_decisions.md`). The Java-side `traitSet` attachment made this
non-blocking, but the underlying collision is still there and could still
bite some other connector in this subtemplate later. Real fix: change
`mainid` to `traitedtreeLikelihood.$(n)`, and update
`TyCHEDiscreteTraitProvider.getAlignments()` to fetch the resulting
`Alignment` from `doc.pluginmap.get(context.partition)` rather than relying
on `addAlignmentWithSubnet()`'s return value (which would then return the
`AncestralTypeLikelihood` instead).

### Headless / scripted import
Researched, not built. No flag exists on BEAUti's interactive
launcher and none can be cleanly added — confirmed by reading
`Beauti.main()` and `BeautiDoc.parseArgs()` directly. Real path:
`BeautiAlignmentProvider.getAlignments(BeautiDoc, File[], String[])`, the
documented scripting entry point (cites BEASTLabs' `CompactAnalysis` as
precedent — not available to inspect this session, worth getting that
source before starting). `GermlineRootAlignmentProvider`'s own pipeline
has no GUI dialogs, so it's a good first candidate; `TyCHEDiscreteTraitProvider`
needs a non-interactive replacement for its modal trait-entry dialog
before a general "any TyCHE partition from the command line" tool is
possible. Also unverified: whether `BeautiDoc` has a direct save-to-XML
method or whether a headless tool needs `XMLProducer` directly.


### `GRTBactrianSubtreeSlide`
`GRTSubtreeSlide` extends plain `SubtreeSlide`, not
`kernel.BactrianSubtreeSlide` — the swap keeps the tree valid but loses the
Bactrian-kernel proposal's efficiency. Would need a new class,
`GRTBactrianSubtreeSlide extends kernel.BactrianSubtreeSlide`, mirroring
how `GRTBactrianScaleOperator extends BactrianScaleOperator` already does.
Worth doing only if this efficiency loss turns out to matter in practice.

### Tip-date auto-configuration for germline-root trees
Blocked by a real BeastFX core bug: `TipDatesInputEditor`'s pattern-based
"Auto-configure" aborts entirely (silent `return`, not a graceful skip) on
the first taxon that doesn't match the regex — unlike TyCHE's own trait
`guess()`, which already degrades gracefully. Not fixed (core code, not
ours). Current workaround is manual date entry; a real fix would mean
either patching that shared dependency or building TyCHE's own tip-date
auto-configuration and GUI.


### Estimate/allowed-transitions checkbox column when `stateCount == 1`
Minor polish, not correctness: with only one trait value, the transition
rows have nothing meaningful to show. Currently just renders zero rows
(the loop skips `i==j`), which is probably fine — flagged only in case a
"select at least 2 trait values" guard or explicit message is ever wanted.