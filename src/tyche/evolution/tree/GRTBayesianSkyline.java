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

package tyche.evolution.tree;

import beast.base.core.Citation;
import beast.base.core.Description;
import beast.base.evolution.tree.TreeIntervals;
import beast.base.inference.StateNode;
import beast.base.spec.domain.PositiveInt;
import beast.base.spec.evolution.tree.coalescent.BayesianSkyline;
import beast.base.spec.inference.parameter.IntSimplexParam;
import beast.base.spec.type.IntSimplex;
import tyche.TycheCitation;

/**
 * @author Jessie Fielding
 * This file is part of the TyCHE package - https://github.com/hoehnlab/tyche
 */

/**
 * Bayesian Skyline that derives its group sizes automatically from the
 * number of coalescent events in a GermlineRootTree, splitting them as
 * evenly as possible across groups -- so group sizes never need to be
 * specified by hand for a germline-rooted analysis.
 */
@Description("Bayesian Skyline tree prior that auto-derives group sizes from a GermlineRootTree's coalescent events.")
@Citation(value = TycheCitation.TEXT, year = TycheCitation.YEAR, firstAuthorSurname = TycheCitation.FIRST_AUTHOR, DOI = TycheCitation.DOI)
public class GRTBayesianSkyline extends BayesianSkyline {

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void initAndValidate() {
        super.initAndValidate();
        if (treeInput.get() != null) {
            throw new IllegalArgumentException("only tree intervals (not tree) should not be specified");
        }

        TreeIntervals intervals = treeIntervalsInput.get();
        if (intervals instanceof GRTIntervals) {
            // the number of coalescent events of a germline-rooted tree is not its internal node count, so the
            // equal split the parent class made from the node count is replaced by one from the event count
            int events = ((GRTIntervals) intervals).getTotalCoalescentEvents();
            IntSimplex<? extends PositiveInt> groupSizes = groupSizeParamInput.get();
            int paramDim2 = groupSizes.size();
            int eventsEach = events / paramDim2;
            int eventsExtras = events % paramDim2;

            int[] values = new int[paramDim2];
            for (int i = 0; i < paramDim2; i++) {
                values[i] = i < eventsExtras ? eventsEach + 1 : eventsEach;
            }
            IntSimplexParam parameter = new IntSimplexParam(values, groupSizes.getDomain(), events);
            if (groupSizes instanceof StateNode stateNode) {
                stateNode.assignFromWithoutID(parameter);
            }
            prepare();
        }
    }
}
