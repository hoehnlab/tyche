package tyche.evolution.branchratemodel;

import beast.base.core.Description;
import beast.base.evolution.tree.Node;

/**
 * Defines a type-linked rate for each branch in the beast.tree, where the branch rate is calculated from the expected occupancy in each type on branches with differently typed nodes, and on branches with same-typed nodes is assumed to be entirely in that state.
 */
@Description("Defines a type-linked rate for each branch in the beast.tree, where the branch rate is calculated from the expected occupancy in each type on branches with differently typed nodes, and on branches with same-typed nodes is assumed to be entirely in that state.")
public class TycheMixedSwitchClockModel extends AbstractTycheTypeLinkedClockModel {
    /**
     * Returns true as this is an expected occupancy model.
     * @return true
     */
    @Override
    public boolean isExpectedOccupancy() {
        return true;
    }

    /**
     * Calculates a type-linked rate for this branch, where the branch rate is calculated from the expected occupancy in each type if the branch has differently typed parent and child nodes, or if the branch has same-typed parent and child nodes is assumed to be entirely in that state
     * @param node the current node (child node of the branch)
     * @return the type-linked rate for this branch
     */
    @Override
    public double getBranchRate(final Node node) {

        if (node.isRoot()) {
            return 1.0;
        }

        int type = (int) nodeTypes.getArrayValue(node.getNr());
        int parentType = (int) nodeTypes.getArrayValue(node.getParent().getNr());
        if (type == parentType) {
            return getTypeLinkedRate(type);
        }
        double typeTime = node.getLength() * typeSwitchClockRate.getArrayValue();
        double[] occupancy = getOccupancy(parentType, type, typeTime, node.getNr());
        return (getTypeLinkedRate(0)*occupancy[0] + getTypeLinkedRate(1)*occupancy[1]);
    }

}
