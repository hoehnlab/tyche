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
import beast.base.evolution.operator.WilsonBalding;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.inference.util.InputUtil;
import tyche.evolution.tree.GRTNode;
import tyche.evolution.tree.GermlineRootTree;
import tyche.TycheCitation;

/**
 * WilsonBalding Operator that will appropriately handle if the provided Tree is a GermlineRootTree
 */
@Description("WilsonBalding Operator that will appropriately handle if the provided Tree is a GermlineRootTree.")
@Citation(value = TycheCitation.TEXT, year = TycheCitation.YEAR, firstAuthorSurname = TycheCitation.FIRST_AUTHOR, DOI = TycheCitation.DOI)
public class GRTWilsonBalding extends WilsonBalding implements GRTCompatibleOperator {

    /**
     * handle proposal appropriately if the provided Tree is a GermlineRootTree
     * @return the log Hastings ratio for the proposal, or Double.NEGATIVE_INFINITY if it should be rejected
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
