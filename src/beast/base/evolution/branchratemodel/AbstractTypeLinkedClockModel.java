package beast.base.evolution.branchratemodel;

import beast.base.core.Description;
import beast.base.core.Function;
import beast.base.core.Input;
import beast.base.evolution.substitutionmodel.SVSGeneralSubstitutionModelNew;
import beast.base.evolution.tree.Node;
import beast.base.inference.parameter.IntegerParameter;
import beast.base.inference.parameter.RealParameter;

/**
 * @author Jessie Fielding
 */

@Description("Branch rate model for type-linked mutation rates")
public abstract class AbstractTypeLinkedClockModel extends BranchRateModel.Base {
    public Input<RealParameter> typeLinkedRatesInput = new Input<RealParameter>("typeLinkedRates", "the mutation rate for each type", Input.Validate.REQUIRED);

    public Input<IntegerParameter> nodeTypesInput = new Input<IntegerParameter>("nodeTypes", "the type for each node", Input.Validate.REQUIRED);

    public Input<Function> typeSwitchClockRateInput = new Input<Function>("typeSwitchClockRate", "the clock rate for the Ancestral Reconstruction Tree Likelihood");
    public Input<SVSGeneralSubstitutionModelNew> svsInput = new Input<SVSGeneralSubstitutionModelNew>("substitutionModel", "testing the substitution model input");

    public Input<RealParameter> branchRatesInput = new Input<>("branchRates", "a real parameter to log branch rates");
    public Input<RealParameter> occupanciesInput = new Input<>("expectedOccupancy", "a real parameter to log expected occupancy");
    Function typeSwitchClockRate;
    SVSGeneralSubstitutionModelNew svs;
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

//        TODO: test this validation - jf
        // ensure we have enough type-linked rates for the types in nodeTypes
        if (nodeTypes.getUpper() != null) {
            if (typeLinkedRates.getDimension() != nodeTypes.getUpper() + 1) {
                throw new IllegalArgumentException("Type rates should be given for each possible type. nodeTypes has upper value of " + nodeTypes.getUpper() + " but typeLinkedRates has " + typeLinkedRates.getDimension() + " values.");
            }
        }

        // if this is an expected occupancy model, more inputs are required
        if (isExpectedOccupancy()) {
            svsInput.setRule(Input.Validate.REQUIRED);
            typeSwitchClockRateInput.setRule(Input.Validate.REQUIRED);
            typeSwitchClockRate = typeSwitchClockRateInput.get();
            svs = svsInput.get();
            occupancies = occupanciesInput.get();
            // TODO(jf): should we enforce two states for now if it's an expected occupancy model?

//        TODO(jf): confirm q matrix is actually being updated here? would prefer not to get it every single branch but
            qMatrix = svs.getRateMatrix();
        }
    }

    public double getTypeLinkedRate(int type) {
        // get the rate that corresponds to that integer position in the rate array/list
        return typeLinkedRates.getArrayValue(type);
    }

    public double[] getOccupancy(final int parentType, final int currentType, final Double time, final int nodeNum) {

                // TODO(jf): check all this math, and that we're always setting occupancy[0] to the occupancy in type 0
        double alpha = qMatrix[0][1];
        double beta = qMatrix[1][0];
        double k = alpha + beta;
        double expkt = Math.exp(-k * time);
        double[] occupancy = new double[2];

        if (parentType == 0 && currentType == 0) {
            occupancy[0] = (1 / k) * (
                    (Math.pow(beta, 2) * time + 2 * alpha * beta / k * (1 - expkt) + Math.pow(alpha, 2) * time * expkt) /
                            (beta + alpha * expkt)
            ) / time;
        } else if ((parentType == 0 && currentType == 1) || (parentType == 1 && currentType == 0)) {
            occupancy[0] = (1 / k) * (
                    (beta * time - alpha * time * expkt + (alpha - beta) / k * (1 - expkt)) /
                            (1 - expkt)
            ) / time;
        } else if (parentType == 1 && currentType == 1) {
            occupancy[0] = (1 / k) * (
            (alpha * beta * time - 2 * alpha * beta / k * (1 - expkt) + alpha * beta * time * expkt) /
                    (alpha + beta * expkt)
            ) / time;
        }


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
