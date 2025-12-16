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
import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.alignment.Taxon;
import beast.base.evolution.operator.TreeOperator;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.inference.parameter.IntegerParameter;
import beast.base.inference.parameter.Parameter;
import beast.base.inference.util.InputUtil;
import beast.base.util.Randomizer;
import tyche.evolution.tree.GRTNode;
import tyche.evolution.tree.GermlineRootTree;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author Jessie Fielding
 * This class is part of the TyCHE package - https://github.com/hoehnlab/tyche
 */

/**
 * Tree Operator that operates on the root's height and type together.
 */
@Description("Tree Operator that operates on the root's height and type together.")
@Citation(value="Fielding, J. J., Wu, S., Melton, H. J., Fisk, N., du Plessis, L., & Hoehn, K. B. (2025).\n" +
        "TyCHE enables time-resolved lineage tracing of heterogeneously-evolving populations.\n" +
        "bioRxiv https://doi.org/10.1101/2025.10.21.683591 (2025) doi:10.1101/2025.10.21.683591.",
        year = 2025, firstAuthorSurname = "Fielding", DOI="10.1101/2025.10.21.683591")
public class CombinedRootOperator extends TreeOperator {
    /**
     * input object for the node types parameter to operate on
     */
    final public Input<IntegerParameter> nodeTypesInput = new Input<>("nodeTypes", "a real or integer parameter to sample individual values for", Input.Validate.REQUIRED, Parameter.class);

    /**
     * the node types parameter to operate on
     */
    protected IntegerParameter nodeTypes;
    protected int lowerInt, upperInt;

    protected int germlineNum = -1;



    /**
     * empty constructor to facilitate construction by XML + initAndValidate
     */
    public CombinedRootOperator() {
    }


    public CombinedRootOperator(Tree tree) {
        try {
            initByName(treeInput.getName(), tree);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to construct LeafConsciousTypeTreeOperator.");
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


        Tree tree = treeInput.get();
        if (!(tree instanceof GermlineRootTree)) {
            Log.warning("Operator " + this.getID() + " of type " + this.getClass().getSimpleName() + " will operate on the root height and type together, but will ignore the germline. If you wish to operate on the root and germline together, please use tyche.evolution.tree.GermlineRootTree.");

        } else {
            germlineNum = ((GermlineRootTree) tree).getGermlineNum();
        }

    }

    private double getRandomScale(double heightRoot, double heightMRCA) {
        double maxScale = heightRoot/heightMRCA;
        double scaleAmount = Randomizer.nextDouble() * maxScale;
        boolean scaleDirection = Randomizer.nextDouble() > 0.5;
        return scaleDirection ? scaleAmount : 1/scaleAmount;
    }

    private double getNewHeight(double heightRoot, double heightMRCA) {
        double scaleFactor = getRandomScale(heightRoot, heightMRCA);
        return scaleFactor * heightRoot;
    }

    private int getRandomType() {
        return Randomizer.nextInt(upperInt - lowerInt + 1) + lowerInt; // from 0 to n-1, n must > 0
    }

    private double adjustRoot(Node root, double newHeight) {
        if (root instanceof GRTNode) {
            // if it's a GRTNode, we want to not set + return negative infinity if height is below the non-germline child
            if (newHeight <= ((GRTNode) root).getMinimumHeight()) {
                return Double.NEGATIVE_INFINITY;
            }
        }
        else if (newHeight <= Math.max(root.getLeft().getHeight(), root.getRight().getHeight())) {
            // if it's not a GRTNode, we want to not set + return negative infinity if height is below either child
            return Double.NEGATIVE_INFINITY;
        }
        // if we haven't returned neg infinity at this point, height is safe to set whether it's a GRTNode or reg Node
        root.setHeight(newHeight);
        return 0.0;
    }

    /**
     * Change the parameter.
     *
     * @return Double.NEGATIVE_INFINITY if proposal should not be accepted
     */
    @Override
    public double proposal() {
        final Tree tree = (Tree) InputUtil.get(treeInput, this);

        // Abort if no non-root internal nodes
        if (tree.getInternalNodeCount() == 1)
            return Double.NEGATIVE_INFINITY;

        Node root = tree.getRoot();
        double heightMRCA;
        if (root instanceof GRTNode) {
            heightMRCA = ((GRTNode) root).getMinimumHeight();
        } else {
            heightMRCA = Math.max(root.getLeft().getHeight(), root.getRight().getHeight());
        }

        double newHeight = getNewHeight(root.getHeight(), heightMRCA);
        int newType = getRandomType();

        nodeTypes.setValue(root.getNr(), newType);
        if (germlineNum > 0) {
            nodeTypes.setValue(germlineNum, newType);
        }

        if (markCladesInput.get()) {
            root.makeAllDirty(Tree.IS_DIRTY);
        }

        return adjustRoot(root, newHeight);
    }
}