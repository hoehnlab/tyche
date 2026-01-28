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
import beast.base.evolution.operator.SubtreeSlide;
import beast.base.evolution.operator.TreeOperator;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.inference.util.InputUtil;
import beast.base.util.Randomizer;
import tyche.evolution.tree.GermlineRootTree;

import java.text.DecimalFormat;
import java.util.*;


import java.util.ArrayList;
import java.util.List;


/**
 * SubtreeSlide Operator that will appropriately handle if the provided Tree is a GermlineRootTree
 */
@Description("SubtreeSlide Operator that will appropriately handle if the provided Tree is a GermlineRootTree.")
@Citation(value="Fielding, J. J., Wu, S., Melton, H. J., Fisk, N., du Plessis, L., & Hoehn, K. B. (2025).\n" +
        "TyCHE enables time-resolved lineage tracing of heterogeneously-evolving populations.\n" +
        "bioRxiv https://doi.org/10.1101/2025.10.21.683591 (2025) doi:10.1101/2025.10.21.683591.",
        year = 2025, firstAuthorSurname = "Fielding", DOI="10.1101/2025.10.21.683591")
public class GRTSubtreeSlide extends SubtreeSlide implements GRTCompatibleOperator {


    protected boolean isGermline(Node node) {
        if (node.getID() == null) return false;
        return node.getID().toUpperCase().contains("germline".toUpperCase());
    }

    /**
     * Do a probabilistic subtree slide move.
     *
     * @return log of Hastings Ratio, or Double.NEGATIVE_INFINITY if proposal should not be accepted *
     */
    @Override
    public double doGRTProposal() {
        final Tree tree = (Tree) InputUtil.get(treeInput, this);

        double logq;

        Node i;
        final boolean markClades = markCladesInput.get();
        // 1. choose a random node avoiding root
        final int nodeCount = tree.getNodeCount();
        if (nodeCount == 1) {
            // test for degenerate case (https://github.com/CompEvol/beast2/issues/887)
            return Double.NEGATIVE_INFINITY;
        }

        do {
            i = tree.getNode(Randomizer.nextInt(nodeCount));
        } while (i.isRoot());

        final Node p = i.getParent();
        final Node CiP = getOtherChild(p, i);
        final Node PiP = p.getParent();

        // 2. choose a delta to move
        final double delta = getDelta();
        final double oldHeight = p.getHeight();
        final double newHeight = oldHeight + delta;

        // 3. if the move is up
        if (delta > 0) {

            // 3.1 if the topology will change
            if (PiP != null && PiP.getHeight() < newHeight) {
                // find new parent
                Node newParent = PiP;
                Node newChild = p;
                while (newParent.getHeight() < newHeight) {
                    newChild = newParent;
                    if( markClades ) newParent.makeDirty(Tree.IS_FILTHY); // JH
                    newParent = newParent.getParent();
                    if (newParent == null) break;
                }
                // the moved node 'p' would become a child of 'newParent'
                //

                // 3.1.1 JF: if we want to create a new root to allow this change, return Double.NEGATIVE_INFINITY
                // to reject this proposal because moving "newChild" (i.e. the old root) would move the former root to
                // be a child of the new root, and therefore the germline to be the grandchild of the new root,
                // which breaks the premise of the GRT tree
                if (newChild.isRoot()) {
                    return Double.NEGATIVE_INFINITY;
                    // JF: of note, this makes the proposal even, because in a GRT, the root can never move down the tree
                    // see JF notes at 4.0 and 4.1
                }
                // 3.1.2 no new root
                if (!Objects.equals(p, newChild)) {
                    replace(p, CiP, newChild);
                    replace(PiP, p, CiP);
                    replace(newParent, newChild, p);
                }
//                replace(p, CiP, newChild);
//                replace(PiP, p, CiP);
//                replace(newParent, newChild, p);


                p.setHeight(newHeight);

                // 3.1.3 count the hypothetical sources of this destination.
                final int possibleSources = intersectingEdges(newChild, oldHeight, null);
                //System.out.println("possible sources = " + possibleSources);

                logq = -Math.log(possibleSources);

            } else {
                // just change the node height
                p.setHeight(newHeight);
                logq = 0.0;
            }
        }
        // 4 if we are sliding the subtree down.
        else {

            // 4.0 is it a valid move?
            if (i.getHeight() > newHeight) {
                return Double.NEGATIVE_INFINITY;
                // JF: if node i was the germline, newHeight effectively has to be below it, so we cannot move the root
                // down the tree if i = germline (technically, if delta < GRTNode.EPSILON, i guess this is possible but
                // it translates to such a small movement down the tree as to be negligible, and essentially does not change
                // the tree structure)
                // if i was the other child of the root, the root cannot move down its branch with i by design of 4.1,
                // and root cannot move down its branch with germline since germline is a tip, so root cannot move
                // down the tree
            }

            // 4.1 will the move change the topology
            if (CiP.getHeight() > newHeight) {

                final List<Node> newChildren = new ArrayList<>();
                final int possibleDestinations = intersectingEdges(CiP, newHeight, newChildren);

                // if no valid destinations then return a failure
                if (newChildren.size() == 0) {
                    return Double.NEGATIVE_INFINITY;
                }

                // pick a random parent/child destination edge uniformly from options
                final int childIndex = Randomizer.nextInt(newChildren.size());
                final Node newChild = newChildren.get(childIndex);
                final Node newParent = newChild.getParent();

                // 4.1.1 if p was root
                if (p.isRoot()) {
                    // new root is CiP
                    replace(p, CiP, newChild);
                    replace(newParent, newChild, p);

                    CiP.setParent(null);
                    tree.setRoot(CiP);

                } else {
                    replace(p, CiP, newChild);
                    replace(PiP, p, CiP);
                    replace(newParent, newChild, p);
                }

                p.setHeight(newHeight);
                if( markClades ) {
                    // make dirty the path from the (down) moved node back up to former parent.
                    Node n = p;
                    while( n != CiP ) {
                        n.makeDirty(Tree.IS_FILTHY); // JH
                        n = n.getParent();
                    }
                }

                logq = Math.log(possibleDestinations);
            } else {
                p.setHeight(newHeight);
                logq = 0.0;
            }
        }
        return logq;
    }

    /**
     * Do a probabilistic subtree slide move.
     *
     * @return log of Hastings Ratio, or Double.NEGATIVE_INFINITY if proposal should not be accepted *
     */
    @Override
    public double proposal() {
        try {
            if (treeInput.get() instanceof GermlineRootTree) {
                return doGRTProposal();
            }
            else {
                return super.proposal();
            }
        }
        catch (Exception e) {
            // whatever went wrong, we want to abort this operation...
            return Double.NEGATIVE_INFINITY;
        }
    }

    private double getDelta() {
        if (!gaussianInput.get()) {
            return (Randomizer.nextDouble() * size) - (size / 2.0);
        } else {
            return Randomizer.nextGaussian() * size;
        }
    }

    private int intersectingEdges(Node node, double height, List<Node> directChildren) {
        final Node parent = node.getParent();

        if (parent == null) {
            // can happen with non-standard non-mutable trees
            return 0;
        }

        if (parent.getHeight() < height) return 0;

        if (node.getHeight() < height) {
            if (directChildren != null) directChildren.add(node);
            return 1;
        }

        if (node.isLeaf()) {
            return 0;
        } else {
            final int count = intersectingEdges(node.getLeft(), height, directChildren) +
                    intersectingEdges(node.getRight(), height, directChildren);
            return count;
        }
    }



}
