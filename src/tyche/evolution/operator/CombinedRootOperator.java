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

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.alignment.Taxon;
import beast.base.evolution.operator.TreeOperator;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.inference.parameter.IntegerParameter;
import beast.base.inference.parameter.Parameter;
import beast.base.inference.util.InputUtil;
import beast.base.util.Randomizer;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author Jessie Fielding
 * This class is part of the TyCHE package - https://github.com/hoehnlab/tyche
 */

/**
 * Tree Operator
 */
@Description("Tree Operator that operates ...")
public class CombinedRootOperator extends TreeOperator {
    /**
     * input object for the node types parameter to operate on
     */
    final public Input<IntegerParameter> nodeTypesInput = new Input<>("nodeTypes", "a real or integer parameter to sample individual values for", Input.Validate.REQUIRED, Parameter.class);

    /**
     * the node types parameter to operate on
     */
    IntegerParameter nodeTypes;
    int lowerInt, upperInt;

    int germlineNum = -1;



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

        for (Node node : treeInput.get().getExternalNodes()) {
            String taxon = node.getID();
            int nodeNum = node.getNr();

            if (Objects.equals(taxon, "Germline")) {
                germlineNum = nodeNum;
                System.out.println("Germline number is: " + nodeNum);
                System.out.println("Root number is: " + treeInput.get().getRoot().getNr());
            }
        }

    }

    private double getRandomScale(double heightRoot, double heightMRCA) {
        double maxScale = heightRoot/heightMRCA;
        double scaleAmount = Randomizer.nextDouble() * maxScale;
        boolean scaleDirection = Randomizer.nextDouble() > 0.5;
        return scaleDirection ? scaleAmount : 1/scaleAmount;
    }

    private double adjustRoot(Node root, double newHeight) {
        if (newHeight <= Math.max(root.getLeft().getHeight(), root.getRight().getHeight())) {
            return Double.NEGATIVE_INFINITY;
        }
        root.setHeight(newHeight);
        return 0.0;
    }

    private double adjustRootAndGermline(Node root, Node germline, double newHeight) {
        double offset = 0.0005;
        if (newHeight > root.getHeight()) {
            // move root first then germline
            double value = adjustRoot(root, newHeight);
            if (value == Double.NEGATIVE_INFINITY) {
                return value;
            }
            germline.setHeight(newHeight - offset);
            return value;
        }
        else if (newHeight < root.getHeight()) {
            // move germline first
            germline.setHeight(newHeight - offset);
            return adjustRoot(root, newHeight);
        }
        else {
            return 0.0;
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

        // Abort if no non-root internal nodes
        if (tree.getInternalNodeCount() == 1)
            return Double.NEGATIVE_INFINITY;

        Node root = tree.getRoot();
        double heightMRCA;
        heightMRCA = Math.max(root.getLeft().getHeight(), root.getRight().getHeight());
        if (germlineNum > 0) {
            if (root.getLeft().getNr() == germlineNum) {
                heightMRCA = root.getRight().getHeight();
            } else if (root.getRight().getNr() == germlineNum) {
                heightMRCA = root.getLeft().getHeight();
            }
        }

        double rootHeight = root.getHeight();
        double scaleFactor = getRandomScale(rootHeight, heightMRCA);

        int newType = Randomizer.nextInt(upperInt - lowerInt + 1) + lowerInt; // from 0 to n-1, n must > 0,

        double newHeight = rootHeight * scaleFactor;

        if (germlineNum > 0) {
            Node germline;
            if (root.getLeft().getNr() == germlineNum) {
                germline = root.getLeft();
            } else if (root.getRight().getNr() == germlineNum) {
                germline = root.getRight();
            } else {
//                System.out.println("No germline child of root?");
                throw new RuntimeException("No germline child of root?");
            }
            double value = adjustRootAndGermline(root, germline, newHeight);
            if (value == Double.NEGATIVE_INFINITY) {
                return value;
            }
            nodeTypes.setValue(germlineNum, newType);
            nodeTypes.setValue(root.getNr(), newType);
        }
        else {
            nodeTypes.setValue(root.getNr(), newType);
            double value = adjustRoot(root, newHeight);
            if (value == Double.NEGATIVE_INFINITY) {
                return value;
            }
        }

        if (markCladesInput.get()) {
            root.makeAllDirty(Tree.IS_DIRTY);
        }

        return 0.0;
    }
}