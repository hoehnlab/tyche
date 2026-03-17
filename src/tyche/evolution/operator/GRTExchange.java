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
import beast.base.evolution.operator.Exchange;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.util.Randomizer;
import tyche.evolution.tree.GRTNode;
import tyche.evolution.tree.GermlineRootTree;

/**
 * @author Jessie Fielding
 * This class is part of the TyCHE package - https://github.com/hoehnlab/tyche
 */

/**
 * ExchangeOperator that will appropriately handle if the provided Tree is a GermlineRootTree
 */
@Description("ExchangeOperator that will appropriately handle if the provided Tree is a GermlineRootTree.")
@Citation(value="Fielding, J. J., Wu, S., Melton, H. J., Fisk, N., du Plessis, L., & Hoehn, K. B. (2025).\n" +
        "TyCHE enables time-resolved lineage tracing of heterogeneously-evolving populations.\n" +
        "bioRxiv https://doi.org/10.1101/2025.10.21.683591 (2025) doi:10.1101/2025.10.21.683591.",
        year = 2025, firstAuthorSurname = "Fielding", DOI="10.1101/2025.10.21.683591")
public class GRTExchange extends Exchange implements GRTCompatibleOperator {

    /**
     * WARNING: Assumes strictly bifurcating beast.tree.
     * @param tree
     */
    @Override
    public double wide(final Tree tree) {
        if (!(tree instanceof GermlineRootTree) || ((GermlineRootTree) tree).getGermlineNum() < 0) {
            return super.wide(tree);
        }

        final int nodeCount = tree.getNodeCount();

        Node i = tree.getRoot();

        while (i.isRoot()) {
            i = tree.getNode(Randomizer.nextInt(nodeCount));
        }

        Node j = i;
        while (j.getNr() == i.getNr() || j.isRoot()) {
            j = tree.getNode(Randomizer.nextInt(nodeCount));
        }

        final Node p = i.getParent();
        final Node jP = j.getParent();

        // TODO for future version for code simplicity: test if we need this
        //  don't even make the change if i or j is the germline child of root
        if (((i.getID() + " ").toUpperCase().contains("germline".toUpperCase()) && i.getParent().isRoot())
                || ((j.getID() + " ").toUpperCase().contains("germline".toUpperCase()) && j.getParent().isRoot())) return Double.NEGATIVE_INFINITY;

        if ((p != jP) && (i != jP) && (j != p)
                && (j.getHeight() < p.getHeight())
                && (i.getHeight() < jP.getHeight())) {
            exchangeNodes(i, j, p, jP);

            // All the nodes on the path from i/j to the common ancestor of i/j parents had a topology change,
            // so they need to be marked FILTHY.
            if( markCladesInput.get() ) {
                Node iup = p;
                Node jup = jP;
                while (iup != jup) {
                    if( iup.getHeight() < jup.getHeight() ) {
                        assert !iup.isRoot();
                        iup = iup.getParent();
                        iup.makeDirty(Tree.IS_FILTHY);
                    } else {
                        assert !jup.isRoot();
                        jup = jup.getParent();
                        jup.makeDirty(Tree.IS_FILTHY);
                    }
                }
            }
            return 0;
        }

        // Randomly selected nodes i and j are not valid candidates for a wide exchange.
        // reject instead of counting (like we do for narrow).
        return Double.NEGATIVE_INFINITY;
    }

    /**
     * handle proposal appropriately if the provided Tree is a GermlineRootTree
     */
    @Override
    public double doGRTProposal() {
        if (treeInput.get() instanceof GermlineRootTree && ((GermlineRootTree) treeInput.get()).getGermlineNum() > 0) {
            boolean wasStartingStructureGRT = isStructureGRT(treeInput);
            double toReturn = super.proposal();
            if (wasStartingStructureGRT && !isStructureGRT(treeInput)) {
                return Double.NEGATIVE_INFINITY;
            }
            return toReturn;
        }
        else {
            return super.proposal();
        }
    }

    /**
     * Change the parameter.
     *
     * @return Double.NEGATIVE_INFINITY if proposal should not be accepted
     */
    @Override
    public double proposal() {
        return doGRTProposal();
    }
}