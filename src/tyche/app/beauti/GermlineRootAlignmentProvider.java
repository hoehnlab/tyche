/*
 *  Copyright (C) 2025 Hoehn Lab, Dartmouth College
 *
 * This file is part of TyCHE.
 *
 * TyCHE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * TyCHE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with TyCHE.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package tyche.app.beauti;

import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.alignment.Taxon;
import beast.base.evolution.alignment.TaxonSet;
import beast.base.evolution.operator.Exchange;
import beast.base.evolution.operator.ScaleOperator;
import beast.base.evolution.operator.SubtreeSlide;
import beast.base.evolution.operator.WilsonBalding;
import beast.base.evolution.operator.kernel.BactrianScaleOperator;
import beast.base.evolution.tree.MRCAPrior;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TreeDistribution;
import beast.base.evolution.tree.TreeIntervals;
import beast.base.inference.Distribution;
import beast.base.parser.PartitionContext;
import beastfx.app.inputeditor.BeautiAlignmentProvider;
import beastfx.app.inputeditor.BeautiDoc;
import beastfx.app.inputeditor.BeautiSubTemplate;
import tyche.evolution.operator.*;
import tyche.evolution.tree.GRTBayesianSkyline;
import tyche.evolution.tree.GermlineRootTree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Jessie Fielding
 * This file is part of the TyCHE package - https://github.com/hoehnlab/tyche
 */

/**
 * "Import Germline-Root Alignment" menu entry. Builds the completely
 * standard partition (via the same StandardPartitionTemplate every normal
 * import uses), then converts that partition's tree to a GermlineRootTree,
 * swaps its operators and Bayesian Skyline model (if any) to their
 * GRT-compatible forms, and adds a monophyletic MRCAPrior constraining the
 * tip named "Germline" to be the outgroup. All of this happens once, at
 * partition-creation time -- there is no later toggle.
 */
public class GermlineRootAlignmentProvider extends BeautiAlignmentProvider {

    private static final Map<Class<?>, Class<?>> PLAIN_TO_GRT = new HashMap<>();
    static {
        PLAIN_TO_GRT.put(WilsonBalding.class, GRTWilsonBalding.class);
        PLAIN_TO_GRT.put(Exchange.class, GRTExchange.class);
        PLAIN_TO_GRT.put(BactrianScaleOperator.class, GRTBactrianScaleOperator.class);
        PLAIN_TO_GRT.put(ScaleOperator.class, GRTScaleOperator.class);
        PLAIN_TO_GRT.put(SubtreeSlide.class, GRTSubtreeSlide.class);
    }

    @Override
    protected void addAlignments(BeautiDoc doc, List<BEASTInterface> selectedBEASTObjects) {
        super.addAlignments(doc, selectedBEASTObjects);   // builds the standard partition, unchanged
        doc.scrubAll(true, false);   // force the standard partition's <connect> rules to run first

        for (BEASTInterface beastObject : selectedBEASTObjects) {
            if (!(beastObject instanceof Alignment)) continue;
            Alignment alignment = (Alignment) beastObject;

            BEASTInterface treeObj = doc.pluginmap.get("Tree.t:" + alignment.getID());
            if (!(treeObj instanceof Tree)) continue;
            Tree tree = (Tree) treeObj;

            Tree grtTree = convertToGermlineRootTree(doc, tree);
            replaceTreePriorWithGRTBayesianSkyline(doc, grtTree);
            addOutgroupPriors(doc, grtTree);
            doc.scrubAll(true, false);
        }
    }

    /**
     * Replaces this tree's default tree prior (Yule, from the standard
     * partition template) with a GRT Bayesian Skyline, using the same
     * subtemplate BEAUti's own tree-prior combo uses -- so its parameters
     * and operators are constructed correctly, rather than hand-built here.
     * Does nothing if a skyline is already in place, or if the prior slot
     * or template can't be found.
     */
    private void replaceTreePriorWithGRTBayesianSkyline(BeautiDoc doc, Tree tree) {
        BEASTInterface priorObj = doc.pluginmap.get("prior");
        if (priorObj == null) return;

        List<BEASTInterface> list;
        try {
            @SuppressWarnings("unchecked")
            List<BEASTInterface> l = (List<BEASTInterface>) priorObj.getInput("distribution").get();
            list = l;
        } catch (Exception e) {
            return;
        }

        BEASTInterface currentTreePrior = null;
        for (BEASTInterface d : list) {
            if (d instanceof TreeDistribution && treeDistributionMatchesTree((TreeDistribution) d, tree)) {
                currentTreePrior = d;
                break;
            }
        }
        if (currentTreePrior == null || currentTreePrior instanceof GRTBayesianSkyline) return;

        BeautiSubTemplate skylineTemplate = null;
        for (BeautiSubTemplate t : doc.beautiConfig.subTemplates) {
            if ("GRTBayesianSkyline".equals(t.getID())) {
                skylineTemplate = t;
                break;
            }
        }
        if (skylineTemplate == null) return;

        PartitionContext context = doc.getContextFor(currentTreePrior);
        BEASTInterface newSkyline = skylineTemplate.createSubNet(context, true);   // builds it standalone, touches no list
        // removeSubNet(Object) itself is package-private, but the 2-arg overload
        // it delegates to isn't -- do the same template lookup ourselves and
        // call that, so Yule's own connectors (its birth-rate prior, its
        // operators) actually get disconnected, not just its top-level object.
        BeautiSubTemplate yuleTemplate = findSubTemplateForObject(doc, currentTreePrior);
        if (yuleTemplate != null) {
            skylineTemplate.removeSubNet(yuleTemplate, context);
        }

        list.remove(currentTreePrior);
        list.add(newSkyline);

        // Not enough to remove it from the list alone -- applyBeautiRules gates
        // on whether the object still exists in pluginmap at all, so leaving it
        // there means its own <connect> rules just re-add it on the next sync.
        doc.pluginmap.remove(currentTreePrior.getID());

        // Skyline's own operators (and Yule's dependents, like YuleBirthRatePrior)
        // are wired/torn down by separate <connect> rules, evaluated only during
        // a sync pass -- force one now so both take effect before we try to
        // convert anything.
        doc.determinePartitions();
        doc.scrubAll(true, false);
    }

    /** Mirrors BeautiSubTemplate's own (package-private) removeSubNet(Object) lookup. */
    private BeautiSubTemplate findSubTemplateForObject(BeautiDoc doc, BEASTInterface o) {
        String id = o.getID();
        if (id.indexOf('.') > 0) {
            id = id.substring(0, id.indexOf('.'));
        }
        for (BeautiSubTemplate t : doc.beautiConfig.subTemplatesInput.get()) {
            if (t.matchesName(id)) return t;
        }
        return null;
    }

    private boolean treeDistributionMatchesTree(TreeDistribution d, Tree tree) {
        if (d.treeInput.get() == tree) return true;
        TreeIntervals ti = d.treeIntervalsInput.get();
        return ti != null && ti.treeInput.get() == tree;
    }

    /** Replaces tree with a new GermlineRootTree, copying its inputs and repointing every reference to it. */
    private Tree convertToGermlineRootTree(BeautiDoc doc, Tree tree) {
        try {
            GermlineRootTree newTree = new GermlineRootTree();
            copyMatchingInputs(tree, newTree);
            String id = tree.getID();
            doc.pluginmap.remove(id);
            newTree.setID(id);
            newTree.initAndValidate();
            doc.pluginmap.put(id, newTree);
            repointReferences(tree, newTree);
            return newTree;
        } catch (Exception e) {
            throw new RuntimeException("Could not convert tree " + tree.getID() +
                    " to GermlineRootTree: " + e.getMessage(), e);
        }
    }

    /** Replaces every reference to original with replacement, using the object's own back-reference list. */
    private static void repointReferences(BEASTInterface original, BEASTInterface replacement) {
        for (Object output : original.getOutputs().toArray()) {
            BEASTInterface consumer = (BEASTInterface) output;
            for (Input<?> input : consumer.listInputs()) {
                Object value = input.get();
                if (value == null) continue;
                if (value instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> list = (List<Object>) value;
                    int idx = list.indexOf(original);
                    if (idx >= 0) {
                        list.set(idx, replacement);
                    }
                } else if (value == original) {
                    try {
                        input.setValue(replacement, consumer);
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    /**
     * Adds both halves of the "Germline is the outgroup" constraint, matching
     * the paper convention: an MRCAPrior enforcing monophyly on every
     * taxon except "Germline" (the observed sequences), and a separate,
     * tips-only MRCAPrior on "Germline" alone carrying a Uniform(-10000,10000)
     * calibration on its own height. tipsonly is used for the second one
     * because monophyly on a single taxon is meaningless -- this is how a
     * height/date prior gets attached directly to one tip. Unsure we need the
     * germline one with a GermlineRootTree constraint but it can't really hurt
     * in most analyses we're doing, and the user can always edit it.
     */
    private void addOutgroupPriors(BeautiDoc doc, Tree tree) {
        String suffix = partitionSuffix(tree);   // e.g. "t:H5N1"

        List<Taxon> nonGermline = new ArrayList<>();
        Taxon germline = null;
        for (String name : tree.getTaxaNames()) {
            if (name.toUpperCase().contains("GERMLINE")) {
                germline = new Taxon(name);
            } else {
                nonGermline.add(new Taxon(name));
            }
        }
        if (germline == null) return;

        TaxonSet obsSet = new TaxonSet();
        obsSet.taxonsetInput.get().addAll(nonGermline);
        obsSet.setID("obs." + suffix);
        obsSet.initAndValidate();
        doc.pluginmap.put(obsSet.getID(), obsSet);

        MRCAPrior obsPrior = new MRCAPrior();
        obsPrior.treeInput.setValue(tree, obsPrior);
        obsPrior.taxonsetInput.setValue(obsSet, obsPrior);
        obsPrior.isMonophyleticInput.setValue(true, obsPrior);
        obsPrior.setID("obs.prior." + suffix);
        obsPrior.initAndValidate();
        doc.pluginmap.put(obsPrior.getID(), obsPrior);

        TaxonSet germSet = new TaxonSet();
        germSet.taxonsetInput.get().add(germline);
        germSet.setID("germSet." + suffix);
        germSet.initAndValidate();
        doc.pluginmap.put(germSet.getID(), germSet);

        beast.base.inference.distribution.Uniform heightPrior = new beast.base.inference.distribution.Uniform();
        heightPrior.lowerInput.setValue(-10000.0, heightPrior);
        heightPrior.upperInput.setValue(10000.0, heightPrior);
        heightPrior.setID("Uniform.1:Germline." + suffix);
        heightPrior.initAndValidate();
        doc.pluginmap.put(heightPrior.getID(), heightPrior);

        MRCAPrior germPrior = new MRCAPrior();
        germPrior.treeInput.setValue(tree, germPrior);
        germPrior.taxonsetInput.setValue(germSet, germPrior);
        germPrior.onlyUseTipsInput.setValue(true, germPrior);
        germPrior.distInput.setValue(heightPrior, germPrior);
        germPrior.setID("Germline.prior." + suffix);
        germPrior.initAndValidate();
        doc.pluginmap.put(germPrior.getID(), germPrior);

        BEASTInterface prior = doc.pluginmap.get("prior");
        if (prior != null) {
            try {
                @SuppressWarnings("unchecked")
                List<Distribution> list = (List<Distribution>) prior.getInput("distribution").get();
                list.add(obsPrior);
                list.add(germPrior);
            } catch (Exception ignored) {}
        }
    }

    /** "Tree.t:H5N1" -> "t:H5N1". Falls back to the full tree ID if it doesn't match the usual pattern. */
    private String partitionSuffix(Tree tree) {
        String id = tree.getID();
        int idx = id.indexOf(".t:");
        return idx >= 0 ? id.substring(idx + 1) : id;
    }


    private static void copyMatchingInputs(BEASTInterface from, BEASTInterface to) {
        for (Input<?> input : from.listInputs()) {
            if (input.get() == null) continue;
            try {
                to.getInput(input.getName()).setValue(input.get(), to);
            } catch (Exception ignored) {}
        }
    }
}