package beast.base.evolution.operator;

import beast.base.core.Description;
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

/**
 * @author Jessie Fielding
 */
@Description("Tree Operator that operates on types associated with internal nodes and ambiguous tips but does not operate on known leaf types.")
public class LeafConsciousTraitTreeOperator extends TreeOperator {
    final public Input<IntegerParameter> typesInput = new Input<>("type", "a real or integer parameter to sample individual values for", Input.Validate.REQUIRED, Parameter.class);
    final public Input<Alignment> dataInput = new Input<>("data", "type data for the tips", Input.Validate.OPTIONAL);

    IntegerParameter types;
    int lowerInt, upperInt;

    boolean[] isAmbiguous;


    // empty constructor to facilitate construction by XML + initAndValidate
    public LeafConsciousTraitTreeOperator() {
    }

    public LeafConsciousTraitTreeOperator(Tree tree) {
        try {
            initByName(treeInput.getName(), tree);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to construct LeafConsciousTraitTreeOperator.");
        }
    }

    @Override
    public void initAndValidate() {
        types = typesInput.get();

        lowerInt = types.getLower();
        upperInt = types.getUpper();

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
                // this only handles one pattern
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
        } while ((node.isLeaf() && !isAmbiguous[node.getNr()]));
        int newValue = Randomizer.nextInt(upperInt - lowerInt + 1) + lowerInt; // from 0 to n-1, n must > 0,
        types.setValue(node.getNr(), newValue);

        if (markCladesInput.get()) {
            node.makeAllDirty(Tree.IS_DIRTY);
        }

        return 0.0;
    }
}
