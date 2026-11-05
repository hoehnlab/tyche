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

import beast.base.core.Citation;
import beast.base.core.Description;
import beast.base.evolution.tree.Node;
import tyche.TycheCitation;

/**
 * Defines a type-linked rate for each branch in the beast.tree, where the branch rate is calculated from the expected occupancy in each type on branches with differently typed nodes, and on branches with same-typed nodes is assumed to be entirely in that state.
 */
@Description("Defines a type-linked rate for each branch in the beast.tree, where the branch rate is calculated from the expected occupancy in each type on branches with differently typed nodes, and on branches with same-typed nodes is assumed to be entirely in that state.")
@Citation(value = TycheCitation.TEXT, year = TycheCitation.YEAR, firstAuthorSurname = TycheCitation.FIRST_AUTHOR, DOI = TycheCitation.DOI)
public class TycheMixedSwitchClockModel extends AbstractTycheTypeLinkedClockModel {
    /**
     * Returns true as this is an expected occupancy model.
     * @return true
     */
    @Override
    public boolean isExpectedOccupancy() {
        return true;
    }

    /**
     * Calculates a type-linked rate for this branch, where the branch rate is calculated from the expected occupancy in each type if the branch has differently typed parent and child nodes, or if the branch has same-typed parent and child nodes is assumed to be entirely in that state
     * @param node the current node (child node of the branch)
     * @return the type-linked rate for this branch
     */
    @Override
    public double getBranchRate(final Node node) {

        if (node.isRoot()) {
            return 1.0;
        }

        int type = nodeTypes.get(node.getNr());
        int parentType = nodeTypes.get(node.getParent().getNr());
        if (type == parentType) {
            return getTypeLinkedRate(type);
        }
        double[] occupancy = getBranchOccupancy(node);
        return (getTypeLinkedRate(0)*occupancy[0] + getTypeLinkedRate(1)*occupancy[1]);
    }

    @Override
    public double[] getBranchOccupancy(final Node node) {
        int type = nodeTypes.get(node.getNr());
        int parentType = nodeTypes.get(node.getParent().getNr());
        if (type == parentType) {
            // same type at both ends: the branch is assumed to be entirely in that type
            return type == 0 ? new double[]{1.0, 0.0} : new double[]{0.0, 1.0};
        }
        double typeTime = node.getLength() * typeSwitchClockRate.get();
        return getOccupancy(parentType, type, typeTime);
    }

}
