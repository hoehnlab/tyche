package beast.base.evolution.branchratemodel;

import beast.base.evolution.tree.Node;

public class RelaxedExpectedOccupancyTraitLinkedClockModel extends RelaxedInstantSwitchTraitLinkedClockModel {

    @Override
    public boolean isExpectedOccupancy() {
        return true;
    }

    @Override
    //get the rate for node
    public double getRateForBranch(Node node) {
        if (node.isRoot()) {
            // root has no rate
            return 1;
        }

        int nodeNum = node.getNr();
        int[] categories = getCategoriesForBranch(nodeNum);
        int trait = (int) nodeTraits.getArrayValue(nodeNum);
        int parentTrait = (int) nodeTraits.getArrayValue(node.getParent().getNr());
        double traitTime = node.getLength() * traitClockRate.getArrayValue();
        double[] occupancy = getOccupancy(parentTrait, trait, traitTime);
        double rate = 0;
        for (int i = 0; i < nTraits; i++) {
            rate += getRate(i, categories[i])*occupancy[i];
        }
        return rate;
    }
}
