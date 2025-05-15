package beast.base.evolution.branchratemodel;

import beast.base.core.Function;
import beast.base.core.Input;
import beast.base.evolution.tree.Node;
import beast.base.inference.parameter.IntegerParameter;
import beast.base.inference.parameter.RealParameter;

public abstract class AbstractTraitLinkedBranchRateModel extends BranchRateModel.Base {
    public Input<RealParameter> traitRatesInput = new Input<RealParameter>("traitRates", "the mutation rate for each trait");

    public Input<IntegerParameter> nodeTraitsInput = new Input<IntegerParameter>("nodeTraits", "the trait for each node");

    RealParameter traitRates;

    IntegerParameter nodeTraits;

    Function muParameter;

    public void initAndValidate() {
        System.out.println("Init trait clock");
        nodeTraits = nodeTraitsInput.get();
        muParameter = meanRateInput.get();

        traitRates = traitRatesInput.get();
//        TODO: validate that traitRates has the correct number of values - jf
    }

    public double getTraitRate(int trait) {
        // get the rate that corresponds to that integer position in the rate array/list
        return traitRates.getArrayValue(trait);
    }

    @Override
    public abstract double getRateForBranch(Node node);

    public boolean requiresRecalculation() {
        return true;
    }
}
