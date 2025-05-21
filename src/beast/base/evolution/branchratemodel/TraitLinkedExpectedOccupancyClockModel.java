package beast.base.evolution.branchratemodel;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.evolution.tree.Node;

/**
 * @author Jessie Fielding
 */

@Description("Defines a mean rate for each branch in the beast.tree.")
public class TraitLinkedExpectedOccupancyClockModel extends AbstractTraitLinkedBranchRateModel {

    @Override
    public boolean isExpectedOccupancy() {
        return true;
    }

    @Override
    public double getRateForBranch(final Node node) {

        if (node.isRoot()) {
            return 1.0;
        }

        int trait = (int) nodeTraits.getArrayValue(node.getNr());
        int parentTrait = (int) nodeTraits.getArrayValue(node.getParent().getNr());
        double traitTime = node.getLength() * traitClockRate.getArrayValue();
        double[] occupancy = getOccupancy(parentTrait, trait, traitTime);
        return (getTraitRate(0)*occupancy[0] + getTraitRate(1)*occupancy[1]);
    }

}
