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
import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.operator.TreeOperator;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.inference.parameter.IntegerParameter;
import beast.base.inference.parameter.Parameter;
import beast.base.inference.util.InputUtil;
import beast.base.util.Randomizer;

import java.util.Arrays;

/**
 * @author Jessie Fielding
 * This class is part of the TyCHE package - https://github.com/hoehnlab/tyche
 */

/**
 * Tree Operator that operates on types associated with internal nodes and ambiguous tips by switching a node and its subtree to the new type.
 */
@Description("Tree Operator that operates on types associated with internal nodes and ambiguous tips by switching a node and its subtree to the new type.")
@Citation(value="Fielding, J. J., Wu, S., Melton, H. J., Fisk, N., du Plessis, L., & Hoehn, K. B. (2025).\n" +
        "TyCHE enables time-resolved lineage tracing of heterogeneously-evolving populations.\n" +
        "bioRxiv https://doi.org/10.1101/2025.10.21.683591 (2025) doi:10.1101/2025.10.21.683591.",
        year = 2025, firstAuthorSurname = "Fielding", DOI="10.1101/2025.10.21.683591")
public class SubtreeTypeSwitchOperator extends LeafConsciousTypeTreeOperator {

    /**
     * empty constructor to facilitate construction by XML + initAndValidate
     */
    public SubtreeTypeSwitchOperator() {
    }

    public SubtreeTypeSwitchOperator(Tree tree) {
        try {
            initByName(treeInput.getName(), tree);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            throw new RuntimeException("Failed to construct Trait Operator.");
        }
    }

    /**
     * Initialize and validate the operator.
     */
    @Override
    public void initAndValidate() {
        super.initAndValidate();
    }

    /**
     * Change the parameter.
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
        } while (node.isLeaf()); // subtree operator shouldn't pick a subtree that starts at the leaves
        int newValue = Randomizer.nextInt(upperInt - lowerInt + 1) + lowerInt; // from 0 to n-1, n must > 0,
        setSubtree(node, newValue);

        if (markCladesInput.get()) {
            node.makeAllDirty(Tree.IS_DIRTY);
        }

        return 0.0;
    }

    /**
     * Set all nodeType values in the subtree to a new value.
     * @param node the node that is the root of the subtree we are setting to a new value
     * @param newValue the new value we are setting every member of the subtree equal to
     */
    private void setSubtree(Node node, int newValue) {
        int nodeNum = node.getNr();
        if (node.isLeaf()) {
            if (isAmbiguous[nodeNum]) {
                nodeTypes.setValue(nodeNum, newValue);
            }
            return;
        }
        nodeTypes.setValue(nodeNum, newValue);
        for (Node childNode : node.getChildren()) {
            setSubtree(childNode, newValue);
        }
    }
}
