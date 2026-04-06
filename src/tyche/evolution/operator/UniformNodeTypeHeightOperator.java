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
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.inference.util.InputUtil;
import beast.base.util.MachineAccuracy;
import beast.base.util.Randomizer;

/**
 * @author Jessie Fielding
 * This class is part of the TyCHE package - https://github.com/hoehnlab/tyche
 */

/**
 * Tree Operator that operates on a node's and its parent's height and that node, its parent, and its sibling's type together.
 */
@Description("Tree Operator that operates on a node's and its parent's height and that node, its parent, and its sibling's type together.")
@Citation(value="Fielding, J. J., Wu, S., Melton, H. J., Fisk, N., du Plessis, L., & Hoehn, K. B. (2025).\n" +
        "TyCHE enables time-resolved lineage tracing of heterogeneously-evolving populations.\n" +
        "bioRxiv https://doi.org/10.1101/2025.10.21.683591 (2025) doi:10.1101/2025.10.21.683591.",
        year = 2025, firstAuthorSurname = "Fielding", DOI="10.1101/2025.10.21.683591")
public class UniformNodeTypeHeightOperator extends LeafConsciousTypeTreeOperator {

    private int getRandomType() {
        return Randomizer.nextInt(upperInt - lowerInt + 1) + lowerInt; // from 0 to n-1, n must > 0
    }

    /**
     * Change the parameter.
     *
     * @return Double.NEGATIVE_INFINITY if proposal should not be accepted
     */
    @Override
    public double proposal() {
        final Tree tree = (Tree) InputUtil.get(treeInput, this);
        // randomly select internal node
        final int nodeCount = tree.getNodeCount();

        // Abort if no non-root internal nodes
        if (tree.getInternalNodeCount()==1)
            return Double.NEGATIVE_INFINITY;

        Node node;
        do {
            final int nodeNr = nodeCount / 2 + 1 + Randomizer.nextInt(nodeCount / 2);
            node = tree.getNode(nodeNr);
        } while (node.isRoot() || node.isLeaf() || node.getParent().isRoot() || node.getParent().getParent().isRoot());

        Node parent = node.getParent();
        Node sibling = parent.getLeft() == node ? parent.getRight() : parent.getLeft();
        final double pUpper = parent.getParent().getHeight();
        final double lower = Math.max(node.getLeft().getHeight(), node.getRight().getHeight());
        final double windowSize = pUpper-lower;
        if (windowSize/10 <= MachineAccuracy.EPSILON) return Double.NEGATIVE_INFINITY;
        final double upper = Math.max(pUpper - 0.01, pUpper - windowSize/10);
        final double oldNodeHeight = node.getHeight();
//        final double upper = node.getParent().getHeight();
        final double newValue = (Randomizer.nextDouble() * (upper - lower)) + lower;
        final double pLower = Math.max(newValue, sibling.getHeight());
        final double newParentValue = (Randomizer.nextDouble()) * (pUpper - pLower) + pLower;
        if (newValue < node.getHeight()) {
            node.setHeight(newValue);
            parent.setHeight(newParentValue);
        } else {
            parent.setHeight(newParentValue);
            node.setHeight(newValue);
        }
//        System.out.println("New value: " + newValue + " parent new value: " + newParentValue);


        setNodeType(node.getNr(), getRandomType());
        setNodeType(parent.getNr(), getRandomType());
        if (!sibling.isLeaf()) setNodeType(sibling.getNr(), getRandomType());

        double hastingsRatio = (pUpper-pLower)/(pUpper - Math.max(sibling.getHeight(), oldNodeHeight));

        return Math.log(hastingsRatio);
    }

}
