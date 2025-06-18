package beast.base.evolution.operator;

import beast.base.core.Input;
import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.substitutionmodel.GeneralSubstitutionModel;
import beast.base.evolution.substitutionmodel.SubstitutionModel;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.inference.parameter.IntegerParameter;
import beast.base.inference.parameter.Parameter;
import beast.base.inference.parameter.RealParameter;
import beast.base.inference.util.InputUtil;
import beast.base.util.Randomizer;
import beastclassic.evolution.likelihood.LeafTrait;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LeafConsciousTraitTreeOperator extends TreeOperator {
    final public Input<IntegerParameter> traitsInput = new Input<>("trait", "a real or integer parameter to sample individual values for", Input.Validate.REQUIRED, Parameter.class);
    final public Input<Boolean> includeLeavesInput = new Input<>("includeleaves", "whether to sample the leaves (true) or only internal nodes (false) (default false)", false);
    final public Input<Alignment> dataInput = new Input<>("data", "trait data for the tips", Input.Validate.OPTIONAL);

    IntegerParameter traits;
    int lowerInt, upperInt;
    boolean includeLeaves;

    boolean[] isAmbiguous;


    // empty constructor to facilitate construction by XML + initAndValidate
    public LeafConsciousTraitTreeOperator() {
    }

    public LeafConsciousTraitTreeOperator(Tree tree) {
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
        includeLeaves = includeLeavesInput.get();

        lowerInt = traits.getLower();
        upperInt = traits.getUpper();

        isAmbiguous = new boolean[treeInput.get().getNodeCount()];
        Arrays.fill(isAmbiguous, true);

        Alignment data = dataInput.get();
        for (Node node : treeInput.get().getExternalNodes()) {
            String taxon = node.getID();
            int nodeNum = node.getNr();
            if (data == null) {
                isAmbiguous[nodeNum] = false;
            }
            else {
                int taxonIndex = data.getTaxonIndex(taxon);
                if (taxonIndex == -1) {
                    if (taxon.startsWith("'") || taxon.startsWith("\"")) {
                        taxonIndex = data.getTaxonIndex(taxon.substring(1, taxon.length() - 1));
                    }
                    if (taxonIndex == -1) {
                        throw new RuntimeException("Could not find sequence " + taxon + " in the alignment");
                    }
                }
                // TODO(jf): this only handles one trait, as does everything else lol
                isAmbiguous[nodeNum] = data.getDataType().isAmbiguousCode(data.getPattern(taxonIndex, 0));
            }
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
        } while ((node.isLeaf() && !includeLeaves && !isAmbiguous[node.getNr()]));
        int newValue = Randomizer.nextInt(upperInt - lowerInt + 1) + lowerInt; // from 0 to n-1, n must > 0,
        traits.setValue(node.getNr(), newValue);

        if (markCladesInput.get()) {
            node.makeAllDirty(Tree.IS_DIRTY);
        }

        return 0.0;
    }
}
