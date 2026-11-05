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
import beast.base.inference.Scalable;
import beast.base.spec.inference.operator.ScaleOperator;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.inference.parameter.RealVectorParam;
import tyche.TycheCitation;

@Description("RelativeParameterOperator that will operate on a multiplier and apply the multiplier to the affected parameter.")
@Citation(value = TycheCitation.TEXT, year = TycheCitation.YEAR, firstAuthorSurname = TycheCitation.FIRST_AUTHOR, DOI = TycheCitation.DOI)
public class RelativeParameterOperator extends ScaleOperator {

    public final Input<RealVectorParam<?>> affectedParameterInput = new Input<>("affectedParameter", "the parameter operated on is assumed to be the multiplier to change the relationship between the values of this affected parameter",
            Input.Validate.REQUIRED);

    protected RealVectorParam<?> affectedParameter;
    protected boolean singleMultiplier = false;

    @Override
    public void initAndValidate() {
        super.initAndValidate();
        affectedParameter = affectedParameterInput.get();
        int multiplierDimension = dimensionOf(parameterInput.get());
        if (multiplierDimension == 1) {
            singleMultiplier = true;
        } else if (multiplierDimension != affectedParameter.size() - 1) {
            throw new IllegalArgumentException(this.getName() + " Error: parameter dimension must be 1 or affectedParameter dimension minus 1.");
        }
    }

    private static int dimensionOf(Scalable multiplier) {
        if (multiplier instanceof RealScalarParam<?>) {
            return 1;
        }
        return ((RealVectorParam<?>) multiplier).size(); // ScaleOperator only accepts scalar or vector real parameters
    }

    private static double valueOf(Scalable multiplier, int i) {
        if (multiplier instanceof RealScalarParam<?> scalar) {
            return scalar.get();
        }
        return ((RealVectorParam<?>) multiplier).get(i);
    }

    protected void updateAffectedParameter(Scalable multiplier) {
        double startValue = affectedParameter.get(0);
        for (int i = 0; i < affectedParameter.size() - 1; i++) {
            double currMultiplier = singleMultiplier ? valueOf(multiplier, 0) : valueOf(multiplier, i);
            affectedParameter.set(i + 1, startValue * currMultiplier);
        }
    }

    @Override
    public double proposal() {
        double toReturn = super.proposal();
        updateAffectedParameter(parameterInput.get());
        return toReturn;
    }
}
