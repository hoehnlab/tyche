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

@Description("Defines a type-linked rate for each branch in the beast.tree, where the branch is assumed to be entirely in the child state.")
public class TycheInstantSwitchClockModel extends AbstractTycheTypeLinkedClockModel {

    public double getBranchRate(final Node node) {

        if (node.isRoot()) {
            return 1.0;
        }
//        get this node's type
        int type = (int) nodeTypes.getArrayValue(node.getNr());
        return getTypeLinkedRate(type);

    }
}
