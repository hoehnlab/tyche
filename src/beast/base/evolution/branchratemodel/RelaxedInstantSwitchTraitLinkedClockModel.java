package beast.base.evolution.branchratemodel;

import org.apache.commons.math.MathException;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Log;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.inference.distribution.ParametricDistribution;
import beast.base.inference.parameter.IntegerParameter;
import beast.base.inference.parameter.RealParameter;
import beast.base.util.Randomizer;

/**
 * @author Jessie Fielding
 */

@Description("Defines a trait-linked relaxed molecular clock.")

public class RelaxedInstantSwitchTraitLinkedClockModel extends AbstractTraitLinkedBranchRateModel {
    final public Input<ParametricDistribution> rateDistInput = new Input<>("distr", "the distribution governing the rates among branches. Must have mean of 1. The clock.rate parameter can be used to change the mean rate.");
    final public Input<IntegerParameter> categoryInput = new Input<>("categories", "the rate categories associated with nodes in the tree for sampling of individual rates among branches.", Input.Validate.REQUIRED);

    final public Input<Tree> treeInput = new Input<>("tree", "the tree this relaxed clock is associated with.", Input.Validate.REQUIRED);


    IntegerParameter categories;

    public IntegerParameter getCategories() {return categories;}

    ParametricDistribution distribution; //the distribution of the rates
    public ParametricDistribution getDistribution() {return distribution;}

    Tree tree;
    private int branchCount;//the number of branches of the tree

    int nTraits;
    int categoriesPerTrait;

    public double getRate(int trait, int category) {
        return traitRates.getArrayValue(trait*categoriesPerTrait+category);
    }

    public int[] getCategoriesForBranch(int nodeNum) {
        int[] currentCategories = new int[nTraits];
        for (int i = 0; i < nTraits; i++) {
            currentCategories[i] = categories.getValue(branchCount*i + nodeNum);
        }
        return currentCategories;
    }

    @Override
    public void initAndValidate() {
        super.initAndValidate();
        treeInput.setRule(Input.Validate.REQUIRED);
        tree = treeInput.get();
        branchCount = tree.getNodeCount() - 1;
        categories = categoryInput.get();

        nTraits = (nodeTraits.getUpper() - nodeTraits.getLower()) + 1;
        int nCategoryRates = traitRates.getDimension();
        categoriesPerTrait = nCategoryRates/nTraits;

        distribution = rateDistInput.get();

        //Initialization
        if (categoriesPerTrait <= 0) categoriesPerTrait = branchCount;
        Log.info.println("  Trait Linked Relaxed Clock Model: using " + categoriesPerTrait + " rate " +
                "categories per trait for " + nTraits + " traits" +
                " to approximate rate distribution across branches.");

        int categoriesLength = branchCount*nTraits;
        categories.setDimension(categoriesLength);
        Integer[] initialCategories = new Integer[categoriesLength];
        for (int i = 0; i < categoriesLength; i++) {
            initialCategories[i] = Randomizer.nextInt(categoriesPerTrait);
        }
        // set initial values of rate categories
        IntegerParameter other = new IntegerParameter(initialCategories);
        categories.assignFromWithoutID(other);
        categories.setLower(0);
        categories.setUpper(categoriesPerTrait - 1);

    }

    @Override
    //get the rate for node
    public double getRateForBranch(Node node) {
        if (node.isRoot()) {
            // root has no rate
            return 1;
        }

        int nodeNum = node.getNr();
        int trait = nodeTraits.getValue(nodeNum);
        int category = getCategoriesForBranch(nodeNum)[trait];
        return getRate(trait, category);
    }

    @Override
    public boolean requiresRecalculation() {

// TODO(jf): clean up

//        if (treeInput.get().somethingIsDirty()) {
//        	recompute = true;
//            return true;
//        }
        // we're not using rateDistInput right now
//        if (rateDistInput.get().isDirtyCalculation()) {
//            return true;
//        }
        // NOT processed as trait on the tree, so DO mark as dirty
        if (categoryInput.get() != null && categoryInput.get().somethingIsDirty()) {
            return true;
        }

        return super.requiresRecalculation();
    }
}
