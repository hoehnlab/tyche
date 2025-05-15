package beast.base.evolution.branchratemodel;

import beast.base.core.Description;
import beast.base.core.Function;
import beast.base.core.Input;
import beast.base.evolution.substitutionmodel.SVSGeneralSubstitutionModelNew;
import beast.base.evolution.tree.Node;

/**
 * @author Jessie Fielding
 */

@Description("Defines a mean rate for each branch in the beast.tree.")
public class TraitLinkedExpectedOccupancyStrictClockModel extends AbstractTraitLinkedBranchRateModel {

    public Input<Function> geoClockRateInput = new Input<>("geoClockRate", "the clock rate for the Ancestral Reconstruction Tree Likelihood");

    public Input<SVSGeneralSubstitutionModelNew> svsInput = new Input<SVSGeneralSubstitutionModelNew>("substitutionModel", "testing the substitution model input");

    Function geoClockRate;
    SVSGeneralSubstitutionModelNew svs;

    double[][] qMatrix;

    @Override
    public void initAndValidate() {
        super.initAndValidate();
        geoClockRate = geoClockRateInput.get();
        svs = svsInput.get();

//        TODO(jf): confirm q matrix is actually being updated here? would prefer not to get it every single branch but
        qMatrix = svs.getRateMatrix();
    }

    private double[] getOccupancy(final int parentTrait, final int currentTrait, final Double time) {
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
            )/time;
        } else if ((parentTrait == 0 && currentTrait == 1) || (parentTrait == 1 && currentTrait == 0)) {
            occupancy[0] = (1 / k) * (
                    (beta * time - alpha * time * expkt + (alpha - beta) / k * (1 - expkt)) /
                            (1 - expkt)
            )/time;
        } else if (parentTrait == 1 && currentTrait == 1) {
            occupancy[0] = (1 / k) * (
                    (alpha * beta * time - 2 * alpha * beta / k * (1 - expkt) + alpha * beta * time * expkt) /
                            (alpha + beta * expkt)
            )/time;
        }
        occupancy[1] = 1-occupancy[0];
        return occupancy;
    }


    @Override
    public double getRateForBranch(final Node node) {

        if (node.isRoot()) {
            return 1.0;
        }

        int trait = (int) nodeTraits.getArrayValue(node.getNr());
        int parentTrait = (int) nodeTraits.getArrayValue(node.getParent().getNr());
        double traitTime = node.getLength() * geoClockRate.getArrayValue();
        double[] occupancy = getOccupancy(parentTrait, trait, traitTime);
        return (getTraitRate(0)*occupancy[0] + getTraitRate(1)*occupancy[1]);
    }

}
