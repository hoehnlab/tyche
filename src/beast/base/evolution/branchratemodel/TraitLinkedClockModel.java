package beast.base.evolution.branchratemodel;

import beast.base.core.Description;
import beast.base.core.Function;
import beast.base.core.Input;
import beast.base.evolution.tree.Node;
import beast.base.inference.parameter.IntegerParameter;
import beast.base.inference.parameter.RealParameter;

/**
 * @author Jessie Fielding
 */

@Description("Defines a mean rate for each branch in the beast.tree.")
public class TraitLinkedClockModel extends BranchRateModel.Base {

    public Input<RealParameter> traitRatesInput = new Input<RealParameter>("traitRates", "the mutation rate for each trait");

    public Input<IntegerParameter> nodeTraitsInput = new Input<IntegerParameter>("nodeTraits", "the trait for each node");

    public Input<RealParameter> occupancyInput = new Input<RealParameter>("occupancy", "the occupancy time in second state (=1)");
    RealParameter occupancy;

    RealParameter traitRates;

//    IntegerParameter nodeTraits;

    Function muParameter;

    @Override
    public void initAndValidate() {
//        nodeTraits = nodeTraitsInput.get();
        muParameter = meanRateInput.get();
        System.out.println("Init trait clock");

        traitRates = traitRatesInput.get();
//        TODO: validate that traitRates has the correct number of values - jf
        occupancy = occupancyInput.get();
    }

    private double getTraitRate(final int trait) {
        // get the rate that corresponds to that integer position in the rate array/list
        return traitRates.getArrayValue(trait);
    }


    @Override
    public double getRateForBranch(final Node node) {

        if (node.isRoot()) {
            return mu;
        }
        double currOccupancy = occupancy.getArrayValue(node.getNr());
        return (1-currOccupancy)*getTraitRate(0) + currOccupancy*getTraitRate(1);

    }

    @Override
    public boolean requiresRecalculation() {
        mu = muParameter.getArrayValue();
        return true;
    }

    @Override
    protected void restore() {
        mu = muParameter.getArrayValue();
        super.restore();
    }

    @Override
    protected void store() {
        mu = muParameter.getArrayValue();
        super.store();
    }

    private double mu = 1.0;
}
