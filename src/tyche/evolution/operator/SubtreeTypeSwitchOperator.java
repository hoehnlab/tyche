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
import beast.base.core.Description;

/**
 * Tree Operator that operates on types associated with internal nodes and ambiguous tips by changing the types of a node and its subtree.
 */
@Description("Tree Operator that operates on types associated with internal nodes and ambiguous tips by changing the types of a node and its subtree.")
@Citation(value="Fielding, J. J., Wu, S., Melton, H. J., Fisk, N., du Plessis, L., & Hoehn, K. B. (2025).\n" +
        "TyCHE enables time-resolved lineage tracing of heterogeneously-evolving populations.\n" +
        "bioRxiv https://doi.org/10.1101/2025.10.21.683591 (2025) doi:10.1101/2025.10.21.683591.",
        year = 2025, firstAuthorSurname = "Fielding", DOI="10.1101/2025.10.21.683591")
public class SubtreeTypeSwitchOperator extends MultiNodeTypeSwitchOperator {

    @Override
    protected void setTraverseMode() {
        mode = TreeTraverseMode.SUBTREE;
    }

    @Override
    protected void setTypeSwitchMode() {
        typeSwitchMode = TypeSwitchMode.HALF_HOMOGENOUS;
    }

    @Override
    protected void setGenerationsLimit() {
        generationLimit = -1;
    }

    @Override
    protected int getGenerationsForProposal() {
        return -1;
    }
}
