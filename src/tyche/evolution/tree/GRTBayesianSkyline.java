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
import beast.base.evolution.tree.coalescent.BayesianSkyline;
import beast.base.inference.parameter.IntegerParameter;

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
@Citation(value="Fielding, J. J., Wu, S., Melton, H. J., Fisk, N., du Plessis, L., & Hoehn, K. B. (2025).\n" +
        "TyCHE enables time-resolved lineage tracing of heterogeneously-evolving populations.\n" +
        "bioRxiv https://doi.org/10.1101/2025.10.21.683591 (2025) doi:10.1101/2025.10.21.683591.",
        year = 2025, firstAuthorSurname = "Fielding", DOI="10.1101/2025.10.21.683591")
public class GRTBayesianSkyline extends BayesianSkyline {

    @Override
    public void initAndValidate() {
        super.initAndValidate();

        if (treeInput.get() != null) {
            throw new IllegalArgumentException("only tree intervals (not tree) should not be specified");
        }
        TreeIntervals intervals = treeIntervalsInput.get();
        if (intervals instanceof GRTIntervals) {
            // make sure that the sum of groupsizes == number of coalescent events
            int events = ((GRTIntervals) intervals).getTotalCoalescentEvents();

            IntegerParameter groupSizes = groupSizeParamInput.get();
            int paramDim2 = groupSizes.getDimension();


            // We assume that the XML has not
            // specified initial group sizes because
            // the super function will have broken if that's the case
            int eventsEach = events / paramDim2;
            int eventsExtras = events % paramDim2;
            Integer[] values = new Integer[paramDim2];
            for (int i = 0; i < paramDim2; i++) {
                if (i < eventsExtras) {
                    values[i] = eventsEach + 1;
                } else {
                    values[i] = eventsEach;
                }
            }
            IntegerParameter parameter = new IntegerParameter(values);
            parameter.setBounds(1, Integer.MAX_VALUE);
            groupSizes.assignFromWithoutID(parameter);
            prepare();
        }
    }
}
