package tyche.evolution.operator;

import beast.base.core.Description;
import beast.base.evolution.operator.kernel.BactrianScaleOperator;
import beast.base.inference.util.InputUtil;
import tyche.evolution.tree.GRTNode;
import tyche.evolution.tree.GermlineRootTree;

/**
 * @author Jessie Fielding
 * This class is part of the TyCHE package - https://github.com/hoehnlab/tyche
 */

/**
 * BactrianScaleOperator that will handle rootOnly scale appropriately if the provided Tree is a GermlineRootTree
 */
@Description("BactrianScaleOperator that will handle rootOnly scale appropriately if the provided Tree is a GermlineRootTree.")
public class GRTBactrianScaleOperator extends BactrianScaleOperator implements GRTCompatibleOperator {

    @Override
    public double doRootOnlyProposal() {
        final GermlineRootTree tree = (GermlineRootTree) InputUtil.get(treeInput, this);
        final GRTNode root = (GRTNode) tree.getRoot();
        final double scale = getScaler(root.getNr(), root.getHeight());
        final double newHeight = root.getHeight() * scale;

        if (newHeight < root.getMinimumHeight()) {
            return Double.NEGATIVE_INFINITY;
        }
        root.setHeight(newHeight);
        return Math.log(scale);
    }


    @Override
    public double proposal() {
        try {
            if (isTreeScaler() && rootOnlyInput.get() && treeInput.get() instanceof GermlineRootTree) {
                return doRootOnlyProposal();
            }
            else {
                return super.proposal();
            }
        }
        catch (Exception e) {
            // whatever went wrong, we want to abort this operation...
            return Double.NEGATIVE_INFINITY;
        }
    }

}
