package beast.base.evolution.branchratemodel;

import beast.base.core.Description;
import beast.base.evolution.tree.Node;

/**
 * @author Jessie Fielding
 */

@Description("Defines a type-linked rate for each branch in the beast.tree, where the branch is assumed to be entirely in the child state.")
public class TypeLinkedInstantSwitchStrictClockModel extends AbstractTycheTypeLinkedClockModel {

    public double getBranchRate(final Node node) {

        if (node.isRoot()) {
            return 1.0;
        }
//        get this node's type
        int type = (int) nodeTypes.getArrayValue(node.getNr());
        return getTypeLinkedRate(type);

    }
}
