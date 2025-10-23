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
import beast.base.core.Function;
import beast.base.core.Input;
import beast.base.evolution.branchratemodel.BranchRateModel;
import tyche.evolution.substitutionmodel.TycheSVSGeneralSubstitutionModel;
import beast.base.evolution.tree.Node;
import beast.base.inference.parameter.IntegerParameter;
import beast.base.inference.parameter.RealParameter;

/**
 * @author Jessie Fielding
 * This class is part of the TyCHE package - https://github.com/hoehnlab/tyche
 */

@Description("Abstract branch rate model for type-linked mutation rates")
@Citation(value="Fielding, J. J., Wu, S., Melton, H. J., Fisk, N., du Plessis, L., & Hoehn, K. B. (2025).\n" +
        "TyCHE enables time-resolved lineage tracing of heterogeneously-evolving populations.\n" +
        "bioRxiv https://doi.org/10.1101/2025.10.21.683591 (2025) doi:10.1101/2025.10.21.683591.",
        year = 2025, firstAuthorSurname = "Fielding", DOI="10.1101/2025.10.21.683591")
public abstract class AbstractTycheTypeLinkedClockModel extends BranchRateModel.Base {
    public Input<RealParameter> typeLinkedRatesInput = new Input<RealParameter>("typeLinkedRates", "the mutation rate for each type", Input.Validate.REQUIRED);

    public Input<IntegerParameter> nodeTypesInput = new Input<IntegerParameter>("nodeTypes", "the type for each node", Input.Validate.REQUIRED);

    public Input<Function> typeSwitchClockRateInput = new Input<Function>("typeSwitchClockRate", "the clock rate for the Ancestral Reconstruction Tree Likelihood");
    public Input<TycheSVSGeneralSubstitutionModel> svsInput = new Input<TycheSVSGeneralSubstitutionModel>("substitutionModel", "testing the substitution model input");

    public Input<RealParameter> branchRatesInput = new Input<>("branchRates", "a real parameter to log branch rates");
    public Input<RealParameter> occupanciesInput = new Input<>("expectedOccupancy", "a real parameter to log expected occupancy");
    Function typeSwitchClockRate;
    TycheSVSGeneralSubstitutionModel svs;
    double[][] qMatrix;

    RealParameter typeLinkedRates;
    RealParameter branchRates;
    RealParameter occupancies;

    IntegerParameter nodeTypes;

    Function muParameter;

    public void initAndValidate() {
        // get inputs
        nodeTypes = nodeTypesInput.get();
        typeLinkedRates = typeLinkedRatesInput.get();
        branchRates = branchRatesInput.get();

        // ensure we have enough type-linked rates for the types in nodeTypes
        if (nodeTypes.getUpper() != null) {
            if (typeLinkedRates.getDimension() != nodeTypes.getUpper() + 1) {
                throw new IllegalArgumentException("Number of type rates should match possible types. nodeTypes has " + (nodeTypes.getUpper()+1) + " possible values but typeLinkedRates has " + typeLinkedRates.getDimension() + " values. Try setting nodeTypes \"upper\" value or adjusting dimension of typeLinkedRates.");
            }
        }

        // if this is an expected occupancy model, more inputs are required
        if (isExpectedOccupancy()) {
            svsInput.setRule(Input.Validate.REQUIRED);
            typeSwitchClockRateInput.setRule(Input.Validate.REQUIRED);
            typeSwitchClockRate = typeSwitchClockRateInput.get();
            svs = svsInput.get();
            occupancies = occupanciesInput.get();
            if (nodeTypes.getUpper() != 1 && nodeTypes.getLower() != 0) {
                throw new IllegalArgumentException("Node types should have upper of 1 and lower of 0 for expected occupancy models.");
            }
            qMatrix = svs.getRateMatrix();
        }
    }

    public double getTypeLinkedRate(int type) {
        // get the rate that corresponds to that integer position in the rate array/list
        return typeLinkedRates.getArrayValue(type);
    }

    public double[] getOccupancy(final int parentType, final int currentType, final Double time, final int nodeNum) {

        double alpha = qMatrix[0][1];
        double beta = qMatrix[1][0];
        double k = alpha + beta;
        double expmkt = Math.exp(-k * time); // exp minus k * time
        double[] occupancy = new double[2];
        double occupancyTimeA; // occupancy time in state 0, calculated as in [reference when we have this paper]

        // see calculations in [reference]
        if (parentType == 0 && currentType == 0) {
            occupancyTimeA = (1 / k) * (
                    ( beta*beta*time + 2*alpha*beta/k*(1 - expmkt) + alpha*alpha*time*expmkt ) /
                            ( beta + alpha*expmkt )
            );
        } else if (parentType == 1 && currentType == 1) {
            occupancyTimeA = (1 / k) * (
                    ( alpha*beta*time - 2*alpha*beta/k*(1 - expmkt) + alpha*beta*time*expmkt ) /
                            ( alpha + beta*expmkt )
            );
        } else if ((parentType == 0 && currentType == 1) || (parentType == 1 && currentType == 0)) {
            occupancyTimeA = (1 / k) * (
                    ( (beta*time - alpha*time*expmkt)/(1 - expmkt) ) + ( (alpha - beta)/k )
            );
        } else {
            int wrongType = (parentType < 0 || parentType > 1) ? parentType : currentType;
            throw new RuntimeException("Types should be either 0 or 1, not " + wrongType);
        }

        occupancy[0] = occupancyTimeA/time; // get occupancy proportion in state 0

        // set occupancy of second type so that occupancies sum to 1
        occupancy[1] = 1 - occupancy[0];

        // record occupancies if a parameter was provided for logging
        if (occupancies != null) {
            occupancies.setValue(nodeNum, occupancy[0]);
        }
        return occupancy;
    }

    public abstract double getBranchRate(Node node);

    @Override
    public double getRateForBranch(Node node) {
        double branchRate = getBranchRate(node);
        if (branchRates != null) {
            branchRates.setValue(node.getNr(), branchRate);
        }
        return branchRate;
    }



    public boolean isExpectedOccupancy() { return false; }

    public boolean requiresRecalculation() {
        return true;
    }
}
