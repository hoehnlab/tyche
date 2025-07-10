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

import beast.base.core.Description;
import beastclassic.evolution.substitutionmodel.SVSGeneralSubstitutionModel;


/**
 * @author Jessie Fielding
 * This class is part of the TyCHE package - https://github.com/hoehnlab/tyche
 */
@Description("Extends SVSGeneralSubstitutionModel so that the rate matrix is stored and restored after rejected proposals.")
public class TycheSVSGeneralSubstitutionModel extends SVSGeneralSubstitutionModel {
    private double[][] storedRateMatrix;

    @Override
    public void initAndValidate() {
        super.initAndValidate();
        storedRateMatrix = new double[nrOfStates][nrOfStates];
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
