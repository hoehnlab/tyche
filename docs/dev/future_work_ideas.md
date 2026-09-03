
## Stretch goals / future improvements

Ordered roughly by how self-contained each one is, not by priority.

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