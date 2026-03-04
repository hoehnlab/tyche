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


/**
 * @author Jessie Fielding
 * This class is part of the TyCHE package - https://github.com/hoehnlab/tyche
 */

import beast.base.core.Citation;
import beast.base.core.Input;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import tyche.evolution.tree.GRTNode;

/**
 * Interface to implement when a class agrees to handle rootOnly scale proposals in such a way that the minimum possible
 * height of the root ignores the height of the germline. Compatible with GermlineRootTree tree classes.
 */
@Citation(value="Fielding, J. J., Wu, S., Melton, H. J., Fisk, N., du Plessis, L., & Hoehn, K. B. (2025).\n" +
        "TyCHE enables time-resolved lineage tracing of heterogeneously-evolving populations.\n" +
        "bioRxiv https://doi.org/10.1101/2025.10.21.683591 (2025) doi:10.1101/2025.10.21.683591.",
        year = 2025, firstAuthorSurname = "Fielding", DOI="10.1101/2025.10.21.683591")
public interface GRTCompatibleOperator {

    /**
     * handle proposal appropriately if the provided Tree is a GermlineRootTree
     */
    abstract double doGRTProposal();

    default boolean isStructureGRT(Input<Tree> treeInput) {
        Node root = treeInput.get().getRoot();
        if (root instanceof GRTNode) {
            GRTNode rootGRT = (GRTNode) root;
            boolean hasGermlineChild = (root.getLeft().getID() + root.getRight().getID()).toUpperCase().contains("germline".toUpperCase());
            boolean hasGermline = rootGRT.hasGermline();
            if (hasGermlineChild && !hasGermline) {
                System.out.println("Why is there no germline associated with this node??" + rootGRT.getNr() + " " + rootGRT.hasGermline() + (root.getLeft().getID() + " " + root.getRight().getID()));
            } else if (hasGermline && !hasGermlineChild) {
                System.out.println("This is why I want to be able to remove the associated germline");
                return false;
            } else if (!hasGermline && !hasGermlineChild) {
                return false;
            } else return true;

        } else return false;
        return false;
    }
}
