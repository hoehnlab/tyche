package beast.base.evolution.branchratemodel;

import beast.base.core.Description;
import beast.base.evolution.tree.Node;

/**
 * @author Jessie Fielding
 */

@Description("Defines a type-linked rate for each branch in the beast.tree, where the branch rate is calculated from the expected occupancy in each type.")
public class TypeLinkedExpectedOccupancyClockModel extends AbstractTycheTypeLinkedClockModel {

    @Override
    public boolean isExpectedOccupancy() {
        return true;
    }

    @Override
    public double getBranchRate(final Node node) {

        if (node.isRoot()) {
            return 1.0;
        }

        int type = (int) nodeTypes.getArrayValue(node.getNr());
        int parentType = (int) nodeTypes.getArrayValue(node.getParent().getNr());
        double typeTime = node.getLength() * typeSwitchClockRate.getArrayValue();
        double[] occupancy = getOccupancy(parentType, type, typeTime, node.getNr());
        return (getTypeLinkedRate(0)*occupancy[0] + getTypeLinkedRate(1)*occupancy[1]);
    }

}
