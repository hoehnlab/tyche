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
public class OccupancyLinkedClockModel extends AbstractTraitLinkedBranchRateModel {
    public Input<RealParameter> occupancyInput = new Input<RealParameter>("occupancy", "the occupancy time in second state (=1)");
    RealParameter occupancy;


    @Override
    public void initAndValidate() {
        super.initAndValidate();
        occupancy = occupancyInput.get();
    }

    @Override
    public double getBranchRate(final Node node) {

        if (node.isRoot()) {
            return 1.0;
        }
        double currOccupancy = occupancy.getArrayValue(node.getNr());
        return (1-currOccupancy)*getTraitRate(0) + currOccupancy*getTraitRate(1);

    }

}
