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
import beast.base.core.Input;
import beast.base.evolution.operator.ScaleOperator;
import beast.base.inference.parameter.RealParameter;


/**
 * RelativeParameterOperator that will operate on a multiplier and apply the multiplier to the affected parameter.
 */
@Description("RelativeParameterOperator that will operate on a multiplier and apply the multiplier to the affected parameter.")
@Citation(value="Fielding, J. J., Wu, S., Melton, H. J., Fisk, N., du Plessis, L., & Hoehn, K. B. (2025).\n" +
        "TyCHE enables time-resolved lineage tracing of heterogeneously-evolving populations.\n" +
        "bioRxiv https://doi.org/10.1101/2025.10.21.683591 (2025) doi:10.1101/2025.10.21.683591.",
        year = 2025, firstAuthorSurname = "Fielding", DOI="10.1101/2025.10.21.683591")
public class RelativeParameterOperator extends ScaleOperator {
    public final Input<RealParameter> affectedParameterInput = new Input<>("affectedParameter", "the parameter operated on is assumed to be the multiplier to change the relationship between the values of this affected parameter",
            Input.Validate.REQUIRED);

    protected RealParameter affectedParameter;
    protected boolean singleMultiplier = false;

    @Override
    public void initAndValidate() {
        super.initAndValidate();
        affectedParameter = affectedParameterInput.get();
        RealParameter multiplier = parameterInput.get();
        if (multiplier.getDimension() == 1) {
            singleMultiplier = true;
        }
        else if (multiplier.getDimension() != affectedParameter.getDimension() - 1) {
            throw new IllegalArgumentException(this.getName() + " Error: parameter dimension must be 1 or affectedParameter dimension minus 1.");
        }
    }

    protected void updateAffectedParameter(RealParameter multiplier) {
        double startValue = affectedParameter.getValue(0);
        for (int i = 0; i < affectedParameter.getDimension() - 1; i++) {
            double currMultiplier = singleMultiplier ? multiplier.getValue(0) : multiplier.getValue(i);
            affectedParameter.setValue(i+1, startValue * currMultiplier);
        }
    }

    @Override
    public double proposal() {
        double toReturn = super.proposal();
        updateAffectedParameter(parameterInput.get());
        return toReturn;
    }
}
