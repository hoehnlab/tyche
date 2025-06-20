package beast.base.evolution.branchratemodel;

import beast.base.core.Description;
import beast.base.evolution.tree.Node;

/**
 * @author Jessie Fielding
 */

@Description("Defines a mean rate for each branch in the beast.tree.")
public class TraitLinkedInstantSwitchStrictClockModel extends AbstractTraitLinkedBranchRateModel {

    public double getBranchRate(final Node node) {

        if (node.isRoot()) {
            return 1.0;
        }
//        get this node's trait
        int trait = (int) nodeTraits.getArrayValue(node.getNr());
        return getTraitRate(trait);

    }

}
