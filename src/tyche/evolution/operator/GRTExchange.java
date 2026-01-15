package tyche.evolution.operator;

import beast.base.evolution.operator.Exchange;
import beast.base.evolution.tree.Node;
import tyche.evolution.tree.GRTNode;

public class GRTExchange extends Exchange implements GRTCompatibleOperator {

    /**
     * handle rootOnly scale appropriately if the provided Tree is a GermlineRootTree
     */
    @Override
    public double doGRTProposal() {
        Node root = treeInput.get().getRoot();
        if (root instanceof GRTNode && ((GRTNode) root).hasGermline()) {
            // then we need the root to still have germline after this proposal, or we return neg inf to reject
            double toReturn = super.proposal();
            if (!((GRTNode) treeInput.get().getRoot()).hasGermline()) {
                return Double.NEGATIVE_INFINITY;
            }
            return toReturn;
        }
        else {
            double toReturn = super.proposal();
            return toReturn;
        }
    }

    /**
     * Change the parameter.
     *
     * @return Double.NEGATIVE_INFINITY if proposal should not be accepted
     */
    @Override
    public double proposal() {
        return doGRTProposal();
    }
}