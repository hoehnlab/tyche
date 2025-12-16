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

package tyche.evolution.operator;

import beast.base.core.Citation;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Log;
import beast.base.evolution.operator.TreeOperator;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.inference.parameter.IntegerParameter;
import beast.base.inference.parameter.Parameter;
import beast.base.inference.util.InputUtil;
import beast.base.util.Randomizer;
import tyche.evolution.tree.GRTNode;
import tyche.evolution.tree.GermlineRootTree;
import tyche.evolution.tree.MetadataTree;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author Jessie Fielding
 * This class is part of the TyCHE package - https://github.com/hoehnlab/tyche
 */

/**
 * Tree Operator that operates on types associated with internal nodes and ambiguous tips but does not operate on known leaf types.
 */
@Description("Tree Operator that operates on types associated with internal nodes and ambiguous tips but does not operate on known leaf types.")
@Citation(value="Fielding, J. J., Wu, S., Melton, H. J., Fisk, N., du Plessis, L., & Hoehn, K. B. (2025).\n" +
        "TyCHE enables time-resolved lineage tracing of heterogeneously-evolving populations.\n" +
        "bioRxiv https://doi.org/10.1101/2025.10.21.683591 (2025) doi:10.1101/2025.10.21.683591.",
        year = 2025, firstAuthorSurname = "Fielding", DOI="10.1101/2025.10.21.683591")
public class LeafConsciousTypeTreeOperator extends TreeOperator {
    /**
     * input object for the node types parameter to operate on
     */
    final public Input<IntegerParameter> nodeTypesInput = new Input<>("nodeTypes", "a real or integer parameter to sample individual values for", Input.Validate.REQUIRED, Parameter.class);

    /**
     * input object for the traitName if the original tip traits are stored on the tree -- for checking ambiguity
     */
    final public Input<String> traitNameInput = new Input<>("traitName", "a string of the traitname", Input.Validate.OPTIONAL);
    /**
     * the node types parameter to operate on
     */
    IntegerParameter nodeTypes;
    int lowerInt, upperInt;

    int germlineNum = -1;

    String traitName;

    /**
     * an array to keep track of which nodes are ambiguous, especially important for ambiguous tips
     */
    boolean[] isAmbiguous;

    boolean isGermlineRoot = false;


    /**
     * empty constructor to facilitate construction by XML + initAndValidate
     */
    public LeafConsciousTypeTreeOperator() {
    }


    public LeafConsciousTypeTreeOperator(Tree tree) {
        try {
            initByName(treeInput.getName(), tree);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to construct LeafConsciousTypeTreeOperator.");
        }
    }

    protected void getAmbiguousTips(String traitName, MetadataTree metadataTree) {

        for (Node node : metadataTree.getExternalNodes()) {
            String taxon = node.getID();
            int nodeNum = node.getNr();

            Object currentTrait = metadataTree.getTipMetaData(traitName, taxon);
            if (currentTrait != null && Objects.equals(currentTrait, "?")) {
                isAmbiguous[nodeNum] = true;
            }
        }

    }

    /**
     * Initialize and validate the operator.
     */
    @Override
    public void initAndValidate() {
        nodeTypes = nodeTypesInput.get();

        lowerInt = nodeTypes.getLower();
        upperInt = nodeTypes.getUpper();

        traitName = traitNameInput.get();

        isAmbiguous = new boolean[treeInput.get().getNodeCount()];
        Arrays.fill(isAmbiguous, false);

        Tree tree = treeInput.get();
        if (tree instanceof MetadataTree) {
            MetadataTree metadataTree = (MetadataTree) tree;
            if (traitName != null && metadataTree.getTipMetaDataNames().contains(traitName)) {
                getAmbiguousTips(traitName, metadataTree);
            } else {
                Log.warning("\nWARNING: Operator " + this.getID() + " of type " + this.getClass().getSimpleName() + " cannot determine ambiguous tips without a traitName that matches a traitset provided to the tree. Make sure traitname in the trait set matches traitName in the operator exactly.\n");
            }
        } else {
            Log.warning("\nWARNING: Operator " + this.getID() + " of type " + this.getClass().getSimpleName() + " cannot determine ambiguous tips and will not operate on them. Consider using a tyche.evolution.tree.MetadataTree or tyche.evolution.tree.GermlineRootTree.\n");
        }
        if (!(tree instanceof GermlineRootTree)) {
            if (germlineNum > 0) {
                Log.warning("Operator " + this.getID() + " of type " + this.getClass().getSimpleName() + " will operate on germline and root independently. Use GermlineRootTree if you'd like them to be treated as one.");
            }
        } else {
            germlineNum = ((GermlineRootTree) tree).getGermlineNum();
            isGermlineRoot = true;
        }
    }

    /**
     * Change the parameter and return the hastings ratio.
     *
     * @return log of Hastings Ratio, or Double.NEGATIVE_INFINITY if proposal should not be accepted
     */
    @Override
    public double proposal() {
        final Tree tree = (Tree) InputUtil.get(treeInput, this);

        // randomly select internal node
        final int nodeCount = tree.getNodeCount();

        // Abort if no non-root internal nodes
        if (tree.getInternalNodeCount() == 1)
            return Double.NEGATIVE_INFINITY;

        Node node;
        do {
            final int nodeNr = Randomizer.nextInt(nodeCount);
            node = tree.getNode(nodeNr);
        } while ((node.isLeaf() && !isAmbiguous[node.getNr()]));
        int newValue = Randomizer.nextInt(upperInt - lowerInt + 1) + lowerInt; // from 0 to n-1, n must > 0,
        nodeTypes.setValue(node.getNr(), newValue);
        if (isGermlineRoot && germlineNum > 0) {
            nodeTypes.setValue(germlineNum, newValue);
            nodeTypes.setValue(tree.getRoot().getNr(), newValue);
        }

        if (markCladesInput.get()) {
            node.makeAllDirty(Tree.IS_DIRTY);
        }

        return 0.0;
    }
}
