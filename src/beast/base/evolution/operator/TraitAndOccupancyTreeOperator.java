package beast.base.evolution.operator;

import beast.base.core.Input;
import beast.base.evolution.substitutionmodel.GeneralSubstitutionModel;
import beast.base.evolution.substitutionmodel.SubstitutionModel;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.inference.parameter.IntegerParameter;
import beast.base.inference.parameter.Parameter;
import beast.base.inference.parameter.RealParameter;
import beast.base.inference.util.InputUtil;
import beast.base.util.Randomizer;

import java.util.List;

public class TraitAndOccupancyTreeOperator extends TreeOperator {
    final public Input<IntegerParameter> traitsInput = new Input<>("trait", "a real or integer parameter to sample individual values for", Input.Validate.REQUIRED, Parameter.class);
    final public Input<RealParameter> occupancyInput = new Input<>("occupancy", "a real or integer parameter to sample individual values for", Input.Validate.REQUIRED, Parameter.class);

    final public Input<SubstitutionModel> substModelInput =
            new Input<>("substModel", "substitution model along branches in the beast.tree", null, Input.Validate.REQUIRED);

    final public Input<Boolean> includeLeavesInput = new Input<>("includeleaves", "whether to sample the leaves or only internal nodes (default false)", false);

    IntegerParameter traits;
    RealParameter occupancy;
    int lowerInt, upperInt;
    double lowerDouble, upperDouble;
    boolean includeLeaves;

    GeneralSubstitutionModel substitutionModel;

    // empty constructor to facilitate construction by XML + initAndValidate
    public TraitAndOccupancyTreeOperator() {
    }

    public TraitAndOccupancyTreeOperator(Tree tree) {
        try {
            initByName(treeInput.getName(), tree);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            throw new RuntimeException("Failed to construct Trait Operator.");
        }
    }

    @Override
    public void initAndValidate() {
        traits = traitsInput.get();
        occupancy = occupancyInput.get();
        includeLeaves = includeLeavesInput.get();
        if (substModelInput.get() instanceof GeneralSubstitutionModel) {
            substitutionModel = (GeneralSubstitutionModel) substModelInput.get();
        } else {
            throw new IllegalArgumentException("Substitution Model must be an instance of or extend class GeneralSubstitutionModel");
        }

        lowerInt = traits.getLower();
        upperInt = traits.getUpper();

    }

    private void updateOccupancy(Node parent, Node child) {
        if (parent == null || child == null) {
            return;
        }
        int childValue = traits.getValue(child.getNr());
        int parentValue = traits.getValue(parent.getNr());
        if (childValue == parentValue) {
            // if parent and child are the same, and we get here, then occupancy time must be constrained to 0 or 1
            // if they're type 0, occupancy time in type 1 should be 0
            // if they're type 1, occupancy time in type 1 should be 1, so
            occupancy.setValue(child.getNr(), Double.valueOf(childValue));
        } else {
            double newOccupancy = Randomizer.nextDouble() * (occupancy.getUpper() - occupancy.getLower()) + occupancy.getLower();
            // we know this value has changed if we're here, and now it's not equal to the parent anymore,
            // so we should remove the 0/1 constraint that would've been on this before
            occupancy.setValue(child.getNr(), newOccupancy);
        }
    }

    /**
     * change the parameter and return the hastings ratio.
     *
     * @return log of Hastings Ratio, or Double.NEGATIVE_INFINITY if proposal should not be accepted *
     */
    @Override
    public double proposal() {
        final Tree tree = (Tree) InputUtil.get(treeInput, this);

        // randomly select internal node
        final int nodeCount = tree.getNodeCount();

        // Abort if no non-root internal nodes
        if (tree.getInternalNodeCount() == 1)
            return Double.NEGATIVE_INFINITY;

        Node node;
        do {
            final int nodeNr = nodeCount / 2 + 1 + Randomizer.nextInt(nodeCount / 2);
            node = tree.getNode(nodeNr);
        } while ((node.isLeaf() && !includeLeaves));
        int oldValue = traits.getValue(node.getNr());
        int newValue = Randomizer.nextInt(upperInt - lowerInt + 1) + lowerInt; // from 0 to n-1, n must > 0,
        traits.setValue(node.getNr(), newValue);

        if (oldValue == newValue) {
            // if we haven't actually changed the trait, we can leave everything alone
            return 0.0;
        }

        // check to see if the current q matrix is such that occupancy times for some state pairs must be 0 or 1
        boolean isConstrained = false;
        double[][] q = substitutionModel.getRateMatrix();
        for (double[] row : q) {
            for (double v : row) {
                if (v == 0) {
                    isConstrained = true;
                    break;
                }
            }
        }
         if (!isConstrained) return 0.0; // shouldn't need to change any occupancy times

        Node parent = node.getParent();
        List<Node> children = node.getChildren();

        // this should handle parent being null
        updateOccupancy(parent, node);

        for (Node child : children) {
            updateOccupancy(node, child);
        }

        if (markCladesInput.get()) {
            node.makeAllDirty(Tree.IS_DIRTY);
        }
//        tree.getRoot().makeAllDirty(Tree.IS_FILTHY);

        return 0.0;
    }
}
