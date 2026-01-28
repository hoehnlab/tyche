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
import beast.base.evolution.operator.ScaleOperator;
import beast.base.inference.util.InputUtil;
import tyche.evolution.tree.GRTNode;
import tyche.evolution.tree.GermlineRootTree;


/**
 * @author Jessie Fielding
 * This class is part of the TyCHE package - https://github.com/hoehnlab/tyche
 */

/**
 * ScaleOperator that will handle rootOnly scale appropriately if the provided Tree is a GermlineRootTree
 */
@Description("ScaleOperator that will handle rootOnly scale appropriately if the provided Tree is a GermlineRootTree.")
@Citation(value="Fielding, J. J., Wu, S., Melton, H. J., Fisk, N., du Plessis, L., & Hoehn, K. B. (2025).\n" +
        "TyCHE enables time-resolved lineage tracing of heterogeneously-evolving populations.\n" +
        "bioRxiv https://doi.org/10.1101/2025.10.21.683591 (2025) doi:10.1101/2025.10.21.683591.",
        year = 2025, firstAuthorSurname = "Fielding", DOI="10.1101/2025.10.21.683591")
public class GRTScaleOperator extends ScaleOperator implements GRTCompatibleOperator {

    /**
     * handle rootOnly scale appropriately if the provided Tree is a GermlineRootTree
     */
    @Override
    public double doGRTProposal() {
        final double scale = getScaler();
        // do our own scaler
        final GermlineRootTree tree = (GermlineRootTree) InputUtil.get(treeInput, this);
        final GRTNode root = (GRTNode) tree.getRoot();
        final double newHeight = root.getHeight() * scale;

        if (newHeight < root.getMinimumHeight()) {
            return Double.NEGATIVE_INFINITY;
        }
        root.setHeight(newHeight);
        return -Math.log(scale);
    }


    /**
     * Change the parameter.
     *
     * @return Double.NEGATIVE_INFINITY if proposal should not be accepted
     */
    @Override
    public double proposal() {
        try {
            if (isTreeScaler() && rootOnlyInput.get() && treeInput.get() instanceof GermlineRootTree) {
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
}
