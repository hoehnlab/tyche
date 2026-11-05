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
import beast.base.core.Input;
import beast.base.spec.evolution.branchratemodel.Base;
import beast.base.util.MachineAccuracy;
import tyche.evolution.NodeTypeBounds;
import tyche.evolution.substitutionmodel.TycheSVSGeneralSubstitutionModel;
import beast.base.evolution.tree.Node;
import beast.base.spec.domain.NonNegativeReal;
import beast.base.spec.inference.parameter.IntVectorParam;
import beast.base.spec.type.RealVector;
import tyche.TycheCitation;
import beast.base.spec.domain.Real;
import beast.base.spec.type.RealScalar;

/**
 * @author Jessie Fielding
 * This class is part of the TyCHE package - https://github.com/hoehnlab/tyche
 */

/**
 * Abstract branch rate model for type-linked mutation rates
 */
@Description("Abstract branch rate model for type-linked mutation rates")
@Citation(value = TycheCitation.TEXT, year = TycheCitation.YEAR, firstAuthorSurname = TycheCitation.FIRST_AUTHOR, DOI = TycheCitation.DOI)
public abstract class AbstractTycheTypeLinkedClockModel extends Base {

    /**
     * input object for the mutation rate for each type
     */
    public Input<RealVector<? extends NonNegativeReal>> typeLinkedRatesInput = new Input<>("typeLinkedRates", "the mutation rate for each type", Input.Validate.REQUIRED);

    /**
     * input object for the type for each node
     */
    public Input<IntVectorParam<?>> nodeTypesInput = new Input<>("nodeTypes", "the type for each node", Input.Validate.REQUIRED);

    /**
     * input object for the clock rate for the Ancestral Reconstruction Tree Likelihood
     */
    public Input<RealScalar<? extends Real>> typeSwitchClockRateInput = new Input<>("typeSwitchClockRate", "the clock rate for the Ancestral Reconstruction Tree Likelihood");

    /**
     * input object for the substitution model describing type substitutions
     */
    public Input<TycheSVSGeneralSubstitutionModel> svsInput = new Input<TycheSVSGeneralSubstitutionModel>("substitutionModel", "the substitution model input");



    /**
     * the clock rate for the Ancestral Reconstruction Tree Likelihood
     */
    protected RealScalar<? extends Real> typeSwitchClockRate;
    /**
     * the substitution model describing type substitutions
     */
    protected TycheSVSGeneralSubstitutionModel svs;

    /**
     * the Q matrix describing type transitions
     */
    protected double[][] qMatrix;

    /**
     * the mutation rate for each type
     */
    protected RealVector<? extends NonNegativeReal> typeLinkedRates;

    /**
     * the type for each node
     */
    protected IntVectorParam<?> nodeTypes;


    /**
     * Initialize and validate inputs that are required for all TyCHE branch models
     */
    public void initAndValidate() {
        // get inputs
        nodeTypes = nodeTypesInput.get();
        typeLinkedRates = typeLinkedRatesInput.get();

        // ensure we have enough type-linked rates for the types in nodeTypes (when nodeTypes states how many there are)
        if (NodeTypeBounds.hasUpper(nodeTypes)) {
            int possibleTypes = NodeTypeBounds.upper(nodeTypes, null) + 1;
            if (typeLinkedRates.size() != possibleTypes) {
                throw new IllegalArgumentException("Number of type rates should match possible types. nodeTypes has " + possibleTypes + " possible values but typeLinkedRates has " + typeLinkedRates.size() + " values. Try setting nodeTypes \"upper\" value or adjusting dimension of typeLinkedRates.");
            }
        }

        if (isExpectedOccupancy()) {
            svsInput.setRule(Input.Validate.REQUIRED);
            typeSwitchClockRateInput.setRule(Input.Validate.REQUIRED);
            typeSwitchClockRate = typeSwitchClockRateInput.get();
            svs = svsInput.get();
            if (typeLinkedRates.size() != 2 || NodeTypeBounds.lower(nodeTypes) != 0) {
                throw new IllegalArgumentException("Expected occupancy models need exactly two types, numbered 0 and 1: typeLinkedRates must have dimension 2 and nodeTypes must start at 0.");
            }
            qMatrix = svs.getRateMatrix();
        }
    }

    /**
     * Get the rate that corresponds to the type's integer position in the rate list
     * @param type  an integer representing the type whose clock rate should be returned
     * @return      the clock rate at the position in the rate list corresponding to the integer representing the type
     */
    public double getTypeLinkedRate(int type) {
        // get the rate that corresponds to that integer position in the rate array/list
        return typeLinkedRates.get(type);
    }

    /**
     * Get the occupancies in each of two states for a branch
     * @param parentType  an integer representing the type of the parent of this branch
     * @param currentType an integer representing the type of the child of this branch (current node)
     * @param time        a Double representing the timespan of the branch
     * @return      a double array listing the expected occupancy for each type
     */
    public double[] getOccupancy(final int parentType, final int currentType, final Double time) {

        if (time < MachineAccuracy.SQRT_EPSILON) {
            return new double[]{0.5, 0.5};
        }

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
        if (occupancy[0] > 1) {
            if (occupancy[0] - 1 > 0.000001) {
                System.err.printf("Warning: occupancy greater than 1: %f \n parentType is %d and currType is %d \n time is %e", occupancy[0], parentType, currentType, time);
            }
            occupancy[0] = 1;
        }
        if (occupancy[0] < 0) {
            if (occupancy[0] < -0.000001) {
                System.err.printf("Warning: occupancy less than 0: %f \n parentType is %d and currType is %d \n time is %e", occupancy[0], parentType, currentType, time);
            }
            occupancy[0] = 0;
        }

        // set occupancy of second type so that occupancies sum to 1
        occupancy[1] = 1 - occupancy[0];
        return occupancy;
    }

    /**
     * Get the occupancy in each of two types for the branch above a node, for the current state of the model.
     * Calculated on demand and without side effects (nothing is stored), so it is always consistent with the
     * current tree and node types. The default assumes the branch is entirely in the child's type; the expected
     * occupancy models override this.
     * @param node  the current node (child of this branch), not the root
     * @return      a double array listing the occupancy for each type
     */
    public double[] getBranchOccupancy(final Node node) {
        return nodeTypes.get(node.getNr()) == 0 ? new double[]{1.0, 0.0} : new double[]{0.0, 1.0};
    }

    /**
     * Get the proportion of the branch above a node that is expected to be spent in type 0, for the current state.
     * @param node  the node
     * @return      the occupancy of type 0, or 0.0 for the root (which has no branch above it)
     */
    public double getNodeOccupancy(final Node node) {
        return node.isRoot() ? 0.0 : getBranchOccupancy(node)[0];
    }

    /**
     * Get the rate for this branch
     * @param node  the current node (child of this branch)
     * @return      the rate to be used for this branch
     */
    public abstract double getBranchRate(Node node);

    /**
     * Get the rate for this branch by calling helper getBranchRate method
     * @param node  the current node (child of this branch)
     * @return      the rate to be used for this branch
     */
    @Override
    public double getRateForBranch(Node node) {
        return getBranchRate(node);
    }


    /**
     * Return whether this model is an expected occupancy model or not
     * @return      true if this model is an expected occupancy model, otherwise false
     */
    public boolean isExpectedOccupancy() { return false; }

    /**
     * Return whether this model requires recalculation
     * @return      true, so that this model is always recalculated
     */
    public boolean requiresRecalculation() {
        return true;
    }
}
