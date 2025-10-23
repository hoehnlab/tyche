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

package tyche.evolution.substitutionmodel;

import beast.base.core.Citation;
import beast.base.core.Description;
import beast.base.core.Log;
import beast.base.evolution.tree.Node;
import beast.base.inference.parameter.BooleanParameter;
import beastclassic.evolution.substitutionmodel.SVSGeneralSubstitutionModel;

import java.util.Arrays;
import beast.base.util.MachineAccuracy;


/**
 * @author Jessie Fielding
 * This class is part of the TyCHE package - https://github.com/hoehnlab/tyche
 */
@Description("Extends SVSGeneralSubstitutionModel so that the rate matrix is stored and restored after rejected proposals.")
@Citation(value="Fielding, J. J., Wu, S., Melton, H. J., Fisk, N., du Plessis, L., & Hoehn, K. B. (2025).\n" +
        "TyCHE enables time-resolved lineage tracing of heterogeneously-evolving populations.\n" +
        "bioRxiv https://doi.org/10.1101/2025.10.21.683591 (2025) doi:10.1101/2025.10.21.683591.",
        year = 2025, firstAuthorSurname = "Fielding", DOI="10.1101/2025.10.21.683591")
public class TycheSVSGeneralSubstitutionModel extends SVSGeneralSubstitutionModel {
    private double[][] storedRateMatrix;
    private BooleanParameter rateIndicator;

    @Override
    public void initAndValidate() {
        super.initAndValidate();
        storedRateMatrix = new double[nrOfStates][nrOfStates];
        rateIndicator = indicator.get();
    }

    @Override
    public void getTransitionProbabilities(Node node, double startTime, double endTime, double rate, double[] matrix) {

        super.getTransitionProbabilities(node, startTime, endTime, rate, matrix);
        int stateCount = getStateCount();

//      Due to floating point/machine accuracy errors, values are sometimes close to but not quite zero when they should be.
//      Since we have rateIndicators, we can correct for these inaccuracies easily to improve overall accuracy of our model.
//      Preliminary investigation on our end suggests that these non-zero values are occurring during matrix inversion.


        int count = 0;
        for (int i = 0; i < stateCount; i++) {
            for (int j = 0; j < stateCount; j++) {
                int index = i*stateCount+j;
                if (i == j) {
                    continue;
                }
                if (!rateIndicator.getValue(count)) {
                    if (matrix[index] < -MachineAccuracy.EPSILON || matrix[index] > MachineAccuracy.EPSILON) {
                        Log.warning.println("Non-zero probability calculated for transition where rate indicator is 0: " + matrix[index]);
                    }
                    matrix[index] = 0;
                }
                count++;
            }
        }
    }


    @Override
    public void store() {
        for (int i = 0; i < nrOfStates; i++) {
            System.arraycopy(rateMatrix[i], 0, storedRateMatrix[i], 0, nrOfStates);
        }
        super.store();
    }
    @Override
    public void restore() {
        for (int i = 0; i < nrOfStates; i++) {
            System.arraycopy(storedRateMatrix[i], 0, rateMatrix[i], 0, nrOfStates);
        }
        super.restore();
    }
}
