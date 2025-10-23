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

package tyche.evolution.branchratemodel;

import beast.base.core.Description;
import beast.base.evolution.tree.Node;

/**
 * @author Jessie Fielding
 * This class is part of the TyCHE package - https://github.com/hoehnlab/tyche
 */

/**
 * Defines a type-linked rate for each branch in the beast.tree, where the branch rate is calculated from the expected occupancy in each type.
 */
@Description("Defines a type-linked rate for each branch in the beast.tree, where the branch rate is calculated from the expected occupancy in each type.")
public class TycheExpectedOccupancyClockModel extends AbstractTycheTypeLinkedClockModel {

    /**
     * Returns true as this is an expected occupancy model.
     * @return true
     */
    @Override
    public boolean isExpectedOccupancy() {
        return true;
    }

    /**
     * Calculates a type-linked rate for this branch, where the branch rate is calculated from the expected occupancy in each type.
     * @param node the current node (child node of the branch)
     * @return the type-linked rate for this branch
     */
    @Override
    public double getBranchRate(final Node node) {

        if (node.isRoot()) {
            return 1.0;
        }

        int type = (int) nodeTypes.getArrayValue(node.getNr());
        int parentType = (int) nodeTypes.getArrayValue(node.getParent().getNr());
        double typeTime = node.getLength() * typeSwitchClockRate.getArrayValue();
        double[] occupancy = getOccupancy(parentType, type, typeTime, node.getNr());
        return (getTypeLinkedRate(0)*occupancy[0] + getTypeLinkedRate(1)*occupancy[1]);
    }

}
