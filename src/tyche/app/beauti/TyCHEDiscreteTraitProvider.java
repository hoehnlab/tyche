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
import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.datatype.UserDataType;
import beast.base.evolution.sitemodel.SiteModel;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TraitSet;
import beast.base.inference.State;
import beast.base.inference.StateNode;
import beast.base.inference.parameter.BooleanParameter;
import beast.base.inference.parameter.IntegerParameter;
import beast.base.inference.parameter.RealParameter;
import beast.base.parser.PartitionContext;
import beastclassic.app.beauti.TraitDialog;
import beastclassic.evolution.alignment.AlignmentFromTrait;
import tyche.evolution.likelihood.AncestralTypeLikelihood;
import beastclassic.evolution.substitutionmodel.SVSGeneralSubstitutionModel;
import beastfx.app.inputeditor.BeautiAlignmentProvider;
import beastfx.app.inputeditor.BeautiDoc;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Jessie Fielding
 * This file is part of the TyCHE package - https://github.com/hoehnlab/tyche
 */

/**
 * "Add TyCHE Trait" menu entry. Builds a new discrete-trait partition,
 * opens the per-taxon trait editor, and resizes the linked substitution
 * model's rates, indicator, and frequencies to match the number of
 * distinct trait values entered.
 */
public class TyCHEDiscreteTraitProvider extends BeautiAlignmentProvider {

    @Override
    public List<BEASTInterface> getAlignments(BeautiDoc doc) {
        try {
            List<String> trees = new ArrayList<String>();
            doc.scrubAll(true, false);
            State state = (State) doc.pluginmap.get("state");
            for (StateNode node : state.stateNodeInput.get()) {
                if (node instanceof Tree) { // && ((Tree) node).m_initial.get() != null) {
                    trees.add(BeautiDoc.parsePartition(((Tree) node).getID()));
                }
            }
            TraitDialog dlg = new TraitDialog(doc, trees);
            if (dlg.showDialog("Create new trait")) {
                String tree = dlg.getTree();
                String name = dlg.getName();
                PartitionContext context = new PartitionContext(name, name, name, tree);
                Alignment alignment = (Alignment) doc.addAlignmentWithSubnet(context, template.get());
                List<BEASTInterface> list = new ArrayList<BEASTInterface>();
                list.add(alignment);
                editAlignment(alignment, doc);
                return list;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public int matches(Alignment alignment) {
        for (BEASTInterface output : alignment.getOutputs()) {
            if (output instanceof AncestralTypeLikelihood) {
                return 10;
            }
        }
        return 0;
    }


    @Override
    public void editAlignment(Alignment alignment, BeautiDoc doc) {
        TyCHETraitInputEditor editor = new TyCHETraitInputEditor(doc);
        AncestralTypeLikelihood likelihood = null;
        for (BEASTInterface output : alignment.getOutputs()) {
            if (output instanceof AncestralTypeLikelihood) {
                likelihood = (AncestralTypeLikelihood) output;
                editor.initPanel(likelihood);

                Dialog dlg = new Dialog();
                DialogPane pane = new DialogPane();
                pane.setContent(editor);
                pane.getButtonTypes().add(ButtonType.CLOSE);
                dlg.setDialogPane(pane);
                dlg.setTitle("TyCHE trait editor");
                pane.setId("TyCHETraitEditor");
                dlg.setResizable(true);
                dlg.showAndWait();
                editor.syncOperatorTraitNames();

                try {
                    AlignmentFromTrait traitData = (AlignmentFromTrait) likelihood.dataInput.get();
                    int stateCount = ((UserDataType) traitData.userDataTypeInput.get()).stateCountInput.get();
                    SVSGeneralSubstitutionModel substModel = (SVSGeneralSubstitutionModel)
                            ((SiteModel.Base) likelihood.siteModelInput.get()).substModelInput.get();
                    boolean isSymmetric = substModel.isSymmetricInput.get();
                    int nRates = isSymmetric ? stateCount * (stateCount - 1) / 2 : stateCount * (stateCount - 1);

                    BooleanParameter rateIndicatorParam = substModel.indicator.get();
                    if (rateIndicatorParam.getDimension() != nRates) {
                        final int oldDim = rateIndicatorParam.getDimension();
                        String indicatorValueString = IntStream.range(0, nRates)
                                .mapToObj(i -> i < oldDim ? rateIndicatorParam.getValue(i) : Boolean.TRUE)
                                .map(String::valueOf)
                                .collect(Collectors.joining(" "));
                        rateIndicatorParam.setDimension(nRates);
                        rateIndicatorParam.valuesInput.setValue(indicatorValueString, rateIndicatorParam);
                        rateIndicatorParam.initAndValidate();
                    }

                    RealParameter relativeGeoRates = (RealParameter) substModel.ratesInput.get();
                    if (relativeGeoRates.getDimension() != nRates) {
                        final int oldDim = relativeGeoRates.getDimension();
                        String ratesValueString = IntStream.range(0, nRates)
                                .mapToObj(i -> i < oldDim ? relativeGeoRates.getValue(i) : 1.0)
                                .map(String::valueOf)
                                .collect(Collectors.joining(" "));
                        relativeGeoRates.setDimension(nRates);
                        relativeGeoRates.valuesInput.setValue(ratesValueString, relativeGeoRates);
                        relativeGeoRates.initAndValidate();
                    }
                    RealParameter freqs = substModel.frequenciesInput.get().frequenciesInput.get();
                    if (freqs.getDimension() != stateCount) {
                        double uniformFreq = 1.0 / stateCount;
                        String freqValueString = IntStream.range(0, stateCount)
                                .mapToObj(i -> String.valueOf(uniformFreq))
                                .collect(Collectors.joining(" "));
                        freqs.setDimension(stateCount);
                        freqs.valuesInput.setValue(freqValueString, freqs);
                        freqs.initAndValidate();
                    }

                    PartitionContext context = new PartitionContext(likelihood);
                    // resize parameters for tree
                    resizeNodeTypesForTree(likelihood);

                    // attach the real TraitSet to the tree
                    Tree tree = (Tree) likelihood.treeInput.get();
                    TraitSet traitSet = traitData.traitInput.get();
                    if (traitSet != null && !tree.m_traitList.get().contains(traitSet)) {
                        tree.m_traitList.get().add(traitSet);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

                return;
            }
        }
    }


    /**
     * Resizes nodeTypes to match the tree's node count. The fxtemplate only
     * gives it a fixed placeholder dimension (100); AncestralTypeLikelihood
     * itself never calls setDimension, only setValue at existing indices, so
     * a tree with more than 100 nodes would otherwise throw at runtime.
     * @param likelihood the AncestralTypeLikelihood whose nodeTypes parameter should be resized to match its tree's node count
     */
    private void resizeNodeTypesForTree(AncestralTypeLikelihood likelihood) {
        System.out.println("\n RESIZING NODES !!!!! \n");
        IntegerParameter nodeTypes = likelihood.nodeTypesInput.get();
        Tree tree = (Tree) likelihood.treeInput.get();
        if (nodeTypes == null || tree == null) return;
        System.out.println("\n RESIZING NODES 2 electric boogaloo !!!!! \n");

        int nNodes = tree.getNodeCount();
        if (nodeTypes.getDimension() == nNodes) return;
        System.out.println("\n RESIZING NODES 3 wee hee !!!!! \n");

        final int oldDim = nodeTypes.getDimension();
        String valueString = IntStream.range(0, nNodes)
                .mapToObj(i -> i < oldDim ? nodeTypes.getValue(i) : 0)
                .map(String::valueOf)
                .collect(Collectors.joining(" "));

        nodeTypes.setDimension(nNodes);
        nodeTypes.valuesInput.setValue(valueString, nodeTypes);
        nodeTypes.initAndValidate();
    }

}
