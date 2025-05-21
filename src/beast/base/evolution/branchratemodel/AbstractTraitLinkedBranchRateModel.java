package beast.base.evolution.branchratemodel;

import beast.base.core.Function;
import beast.base.core.Input;
import beast.base.evolution.substitutionmodel.SVSGeneralSubstitutionModelNew;
import beast.base.evolution.tree.Node;
import beast.base.inference.parameter.IntegerParameter;
import beast.base.inference.parameter.RealParameter;

/**
 * @author Jessie Fielding
 */

public abstract class AbstractTraitLinkedBranchRateModel extends BranchRateModel.Base {
    public Input<RealParameter> traitRatesInput = new Input<RealParameter>("traitRates", "the mutation rate for each trait", Input.Validate.REQUIRED);

    public Input<IntegerParameter> nodeTraitsInput = new Input<IntegerParameter>("nodeTraits", "the trait for each node", Input.Validate.REQUIRED);

    public Input<Function> traitClockRateInput = new Input<Function>("traitClockRate", "the clock rate for the Ancestral Reconstruction Tree Likelihood");
    public Input<SVSGeneralSubstitutionModelNew> svsInput = new Input<SVSGeneralSubstitutionModelNew>("substitutionModel", "testing the substitution model input");
    Function traitClockRate;
    SVSGeneralSubstitutionModelNew svs;
    double[][] qMatrix;

    RealParameter traitRates;

    IntegerParameter nodeTraits;

    Function muParameter;

    public void initAndValidate() {
        System.out.println("Init trait clock");
        nodeTraits = nodeTraitsInput.get();
        muParameter = meanRateInput.get();

        traitRates = traitRatesInput.get();
//        TODO: validate that traitRates has the correct number of values - jf
        if (isExpectedOccupancy()) {
            svsInput.setRule(Input.Validate.REQUIRED);
            traitClockRateInput.setRule(Input.Validate.REQUIRED);
            traitClockRate = traitClockRateInput.get();
            svs = svsInput.get();

//        TODO(jf): confirm q matrix is actually being updated here? would prefer not to get it every single branch but
            qMatrix = svs.getRateMatrix();
        }
    }

    public double getTraitRate(int trait) {
        // get the rate that corresponds to that integer position in the rate array/list
        return traitRates.getArrayValue(trait);
    }

    public double[] getOccupancy(final int parentTrait, final int currentTrait, final Double time) {

        // TODO(jf): check all this math, and that we're always setting occupancy[0] to the occupancy in trait 0
        double alpha = qMatrix[0][1];
        double beta = qMatrix[1][0];
        double k = alpha + beta;
        double expkt = Math.exp(-k * time);
        double[] occupancy = new double[2];

        if (parentTrait == 0 && currentTrait == 0) {
            occupancy[0] = (1 / k) * (
                    (Math.pow(beta, 2) * time + 2 * alpha * beta / k * (1 - expkt) + Math.pow(alpha, 2) * time * expkt) /
                            (beta + alpha * expkt)
            ) / time;
        } else if ((parentTrait == 0 && currentTrait == 1) || (parentTrait == 1 && currentTrait == 0)) {
            occupancy[0] = (1 / k) * (
                    (beta * time - alpha * time * expkt + (alpha - beta) / k * (1 - expkt)) /
                            (1 - expkt)
            ) / time;
        } else if (parentTrait == 1 && currentTrait == 1) {
            occupancy[0] = (1 / k) * (
                    (alpha * beta * time - 2 * alpha * beta / k * (1 - expkt) + alpha * beta * time * expkt) /
                            (alpha + beta * expkt)
            ) / time;
        }
        occupancy[1] = 1 - occupancy[0];
        return occupancy;
    }

    @Override
    public abstract double getRateForBranch(Node node);

    public boolean isExpectedOccupancy() { return false; }

    public boolean requiresRecalculation() {
        return true;
    }
}
