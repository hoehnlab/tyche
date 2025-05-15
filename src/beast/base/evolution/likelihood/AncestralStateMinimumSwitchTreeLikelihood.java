package beast.base.evolution.likelihood;


import beagle.Beagle;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.datatype.DataType;
import beast.base.evolution.datatype.UserDataType;
import beast.base.evolution.sitemodel.SiteModel;
import beast.base.evolution.substitutionmodel.GeneralSubstitutionModel;
import beast.base.evolution.substitutionmodel.SubstitutionModel;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TreeInterface;
import beast.base.inference.parameter.IntegerParameter;
import beast.base.inference.parameter.RealParameter;
import beastclassic.evolution.likelihood.LeafTrait;
import beastclassic.evolution.tree.TreeTrait;
import beastclassic.evolution.tree.TreeTraitProvider;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Marc A. Suchard
 * @author Alexei Drummond
 */
@Description("Ancestral State Tree Likelihood")
public class AncestralStateMinimumSwitchTreeLikelihood extends TreeLikelihood implements TreeTraitProvider {
    public static final String STATES_KEY = "states";

    public Input<String> tagInput = new Input<String>("tag","label used to report trait", Validate.REQUIRED);
    public Input<Boolean> useMAPInput = new Input<Boolean>("useMAP","whether to use maximum aposteriori assignments or sample", false);
    public Input<Boolean> returnMLInput = new Input<Boolean>("returnML", "report integrate likelihood of tip data", true);

    public Input<Boolean> useJava = new Input<Boolean>("useJava", "prefer java, even if beagle is available", true);

    public Input<Boolean> sampleTipsInput = new Input<Boolean>("sampleTips", "if tips have missing data/ambigous values sample them for logging (default true)", true);

    public Input<IntegerParameter> nodeTraitsInput = new Input<IntegerParameter>("nodeTraits", "the trait for each node", Validate.REQUIRED);
    public Input<RealParameter> occupancyInput = new Input<RealParameter>("occupancy", "the occupancy time in second state (=1)", Validate.REQUIRED);

    public Input<List<LeafTrait>> leafTriatsInput = new Input<List<LeafTrait>>("leaftrait", "list of leaf traits",
            new ArrayList<LeafTrait>());

    int[][] storedTipStates;

    /** parameters for each of the leafs **/
    IntegerParameter[] parameters;

    /** and node number associated with parameter **/
    int[] leafNr;

    int traitDimension;
    IntegerParameter nodeTraits;
    RealParameter occupancy;

    double[][] qMatrix;

    /**
     * Constructor.
     * Now also takes a DataType so that ancestral states are printed using data codes
     *
     * @param patternList     -
     * @param treeModel       -
     * @param siteModel       -
     * @param branchRateModel -
     * @param useAmbiguities  -
     * @param storePartials   -
     * @param dataType        - need to provide the data-type, so that corrent data characters can be returned
     * @param tag             - string label for reconstruction characters in tree log
     * @param forceRescaling  -
     * @param useMAP          - perform maximum aposteriori reconstruction
     * @param returnML        - report integrate likelihood of tip data
     */
    int patternCount;
    int stateCount;

    int[][] tipStates; // used to store tip states when using beagle

    @Override
    public void initAndValidate() {
        if (dataInput.get().getSiteCount() == 0) {
            return;
        }


        String sJavaOnly = null;
        if (useJava.get()) {
            sJavaOnly = System.getProperty("java.only");
            System.setProperty("java.only", "" + true);
        }
        super.initAndValidate();
        if (useJava.get()) {
            if (sJavaOnly != null) {
                System.setProperty("java.only", sJavaOnly);
            } else {
                System.clearProperty("java.only");
            }
        }

        this.tag = tagInput.get();
        TreeInterface treeModel = treeInput.get();
        patternCount = dataInput.get().getPatternCount();
        dataType = dataInput.get().getDataType();
        stateCount = dataType.getStateCount();


        reconstructedStates = new int[treeModel.getNodeCount()][patternCount];
        storedReconstructedStates = new int[treeModel.getNodeCount()][patternCount];

        this.useMAP = useMAPInput.get();
        this.returnMarginalLogLikelihood = returnMLInput.get();
        System.out.println("returnML: " + this.returnMarginalLogLikelihood);

        nodeTraits = nodeTraitsInput.get();
        occupancy = occupancyInput.get();

        treeTraits.addTrait(STATES_KEY, new TreeTrait.IA() {
            public String getTraitName() {
                return tag;
            }

            public Intent getIntent() {
                return Intent.NODE;
            }

            public int[] getTrait(TreeInterface tree, Node node) {
                return getStatesForNode(tree,node);
            }

            public String getTraitString(TreeInterface tree, Node node) {
                return formattedState(getStatesForNode(tree,node), dataType);
            }
        });

//        if (m_useAmbiguities.get()) {
//            Logger.getLogger("dr.evomodel.treelikelihood").info("Ancestral reconstruction using ambiguities is currently "+
//            "not support without BEAGLE");
//            System.exit(-1);
//        }
        if (beagle != null) {
            if (!(siteModelInput.get() instanceof SiteModel.Base)) {
                throw new IllegalArgumentException ("siteModel input should be of type SiteModel.Base");
            }
            m_siteModel = (SiteModel.Base) siteModelInput.get();
            substitutionModel = (SubstitutionModel.Base) m_siteModel.substModelInput.get();
            int nStateCount = dataInput.get().getMaxStateCount();
            probabilities = new double[(nStateCount + 1) * (nStateCount + 1)];
        }

        int tipCount = treeModel.getLeafNodeCount();
        tipStates = new int[tipCount][];

        Alignment data = dataInput.get();
        for (Node node : treeInput.get().getExternalNodes()) {
            String taxon = node.getID();
            int taxonIndex = data.getTaxonIndex(taxon);
            if (taxonIndex == -1) {
                if (taxon.startsWith("'") || taxon.startsWith("\"")) {
                    taxonIndex = data.getTaxonIndex(taxon.substring(1, taxon.length() - 1));
                }
                if (taxonIndex == -1) {
                    throw new RuntimeException("Could not find sequence " + taxon + " in the alignment");
                }
            }
            tipStates[node.getNr()] = new int[patternCount];
            if (!m_useAmbiguities.get()) {
                likelihoodCore.getNodeStates(node.getNr(), tipStates[node.getNr()]);
            } else {
                int [] states = tipStates[node.getNr()];
                for (int i = 0; i < patternCount; i++) {
                    int code = data.getPattern(taxonIndex, i);
                    int[] statesForCode = data.getDataType().getStatesForCode(code);
                    if (statesForCode.length==1)
                        states[i] = statesForCode[0];
                    else
                        states[i] = code; // Causes ambiguous states to be ignored.
                }

            }
        }

        if (m_siteModel.getCategoryCount() > 1)
            throw new RuntimeException("Reconstruction not implemented for multiple categories yet.");

        if (substitutionModel instanceof GeneralSubstitutionModel) {
            qMatrix = ((GeneralSubstitutionModel) substitutionModel).getRateMatrix();
        } else {
            qMatrix = new double[][] {{1, -1}, {-1, 1}};
        }
        System.out.println(qMatrix);


        // stuff for dealing with ambiguities in tips
        if (!m_useAmbiguities.get() && leafTriatsInput.get().size() == 0) {
            return;
        }
        traitDimension = tipStates[0].length;

        leafNr = new int[leafTriatsInput.get().size()];
        parameters = new IntegerParameter[leafTriatsInput.get().size()];

        List<String> taxaNames = dataInput.get().getTaxaNames();
        for (int i = 0; i < leafNr.length; i++) {
            LeafTrait leafTrait = leafTriatsInput.get().get(i);
            parameters[i] = leafTrait.parameter.get();
            // sanity check
            if (parameters[i].getDimension() != traitDimension) {
                throw new IllegalArgumentException("Expected parameter dimension to be " + traitDimension + ", not "
                        + parameters[i].getDimension());
            }
            // identify node
            String taxon = leafTrait.taxonName.get();
            int k = 0;
            while (k < taxaNames.size() && !taxaNames.get(k).equals(taxon)) {
                k++;
            }
            leafNr[i] = k;
            // sanity check
            if (k == taxaNames.size()) {
                throw new IllegalArgumentException("Could not find taxon '" + taxon + "' in tree");
            }
            // initialise parameter value from states
            Integer[] values = new Integer[tipStates[k].length];
            for (int j = 0; j < tipStates[k].length; j++) {
                values[j] = tipStates[k][j];
            }
            IntegerParameter p = new IntegerParameter(values);
            p.setLower(0);
            p.setUpper(dataType.getStateCount()-1);
            parameters[i].assignFromWithoutID(p);
        }

        storedTipStates = new int[tipStates.length][traitDimension];
        for (int i = 0; i < tipStates.length; i++) {
            System.arraycopy(tipStates[i], 0, storedTipStates[i], 0, traitDimension);
        }
        // TODO: why are we not getting here?
        System.out.println("HERE2");
    }

    @Override
    public void store() {
        super.store();

        for (int i = 0; i < reconstructedStates.length; i++) {
            System.arraycopy(reconstructedStates[i], 0, storedReconstructedStates[i], 0, reconstructedStates[i].length);
        }

        storedAreStatesRedrawn = areStatesRedrawn;
        storedJointLogLikelihood = jointLogLikelihood;


        // deal with ambiguous tips
        if (leafNr != null) {
            for (int i = 0; i < leafNr.length; i++) {
                int k = leafNr[i];
                System.arraycopy(tipStates[k], 0, storedTipStates[k], 0, traitDimension);
            }
        }
    }

    @Override
    public void restore() {

        super.restore();

        int[][] temp = reconstructedStates;
        reconstructedStates = storedReconstructedStates;
        storedReconstructedStates = temp;

        areStatesRedrawn = storedAreStatesRedrawn;
        jointLogLikelihood = storedJointLogLikelihood;

        // deal with ambiguous tips
        if (leafNr != null) {
            for (int i = 0; i < leafNr.length; i++) {
                int k = leafNr[i];
                int[] tmp = tipStates[k];
                tipStates[k] = storedTipStates[k];
                storedTipStates[k] = tmp;
                // Does not handle ambiguities or missing taxa
                likelihoodCore.setNodeStates(k, tipStates[k]);
            }
        }
    }

    @Override
    protected boolean requiresRecalculation() {
        likelihoodKnown = false;

        super.requiresRecalculation();
        return true;

//        if (nodeTraits.isDirty(nodeTraits.getLastDirty())) {
//            // TODO (jf): add a check that getLastDirty is in fact an internal node and not a leaf
//            isDirty = true;
//            return isDirty;
//        }
//        if (occupancy.isDirty(occupancy.getLastDirty())) {
//            // TODO (jf): add a check that getLastDirty is in fact an internal node and not a leaf
//            isDirty = true;
//            return isDirty;
//        }
//
//        if (!m_useAmbiguities.get()) {
//            return isDirty;
//        }
//
//
//        int hasDirt = Tree.IS_CLEAN;
//
//        // check whether any of the leaf trait parameters changed
//        for (int i = 0; i < leafNr.length; i++) {
//            if (parameters[i].somethingIsDirty()) {
//                int k = leafNr[i];
//                for (int j = 0; j < traitDimension; j++) {
//                    tipStates[k][j] = parameters[i].getValue(j);
//                }
//                likelihoodCore.setNodeStates(k, tipStates[k]);
//                isDirty = true;
//                // mark leaf's parent node as dirty
//                Node leaf = treeInput.get().getNode(k);
//                // leaf.makeDirty(Tree.IS_DIRTY);
//                leaf.getParent().makeDirty(Tree.IS_DIRTY);
//                hasDirt = Tree.IS_DIRTY;
//            }
//        }
//        isDirty |= super.requiresRecalculation();
//        this.hasDirt |= hasDirt;
//        if (isDirty) {
//            System.out.println("Likelihood isDirty");
//        }
//
//        return isDirty;
//        return true;
    }

//    protected void handleModelChangedEvent(Model model, Object object, int index) {
//        super.handleModelChangedEvent(model, object, index);
//        fireModelChanged(model);
//    }



    public DataType getDataType() {
        return dataType;
    }

    public int[] getStatesForNode(TreeInterface tree, Node node) {
        if (tree != treeInput.get()) {
            throw new RuntimeException("Can only reconstruct states on treeModel given to constructor");
        }

        if (!likelihoodKnown) {
            try {
                calculateLogP();
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        }

        return reconstructedStates[node.getNr()];
    }

//    private boolean checkConditioning = true;


    @Override
    public double calculateLogP() {
////
//        double marginalLogLikelihood = super.calculateLogP();
//        likelihoodKnown = true;
//        if (returnMarginalLogLikelihood) {
//            return logP;
//        }
//        // redraw states and return joint density of drawn states
//        redrawAncestralStates();
        jointLogLikelihood = 0;
        TreeInterface tree = treeInput.get();
        traverseSampleNew(tree, tree.getRoot());
        logP = jointLogLikelihood;
        return logP;
    }

    protected Helper treeTraits = new Helper();

    public TreeTrait[] getTreeTraits() {
        return treeTraits.getTreeTraits();
    }

    public TreeTrait getTreeTrait(String key) {
        return treeTraits.getTreeTrait(key);
    }


    private static String formattedState(int[] state, DataType dataType) {
        StringBuffer sb = new StringBuffer();
        sb.append("\"");
        if (dataType instanceof UserDataType) {
            boolean first = true;
            for (int i : state) {
                if (!first) {
                    sb.append(" ");
                } else {
                    first = false;
                }

                sb.append(dataType.getCode(i));
            }

        } else {
            for (int i : state) {
                sb.append(dataType.getChar(i));
            }
        }
        sb.append("\"");
        return sb.toString();
    }


    public void getStates(int tipNum, int[] states)  {
        // Saved locally to reduce BEAGLE library access
        System.arraycopy(tipStates[tipNum], 0, states, 0, states.length);
    }

    public void getPartials(int number, double[] partials) {
        int cumulativeBufferIndex = Beagle.NONE;
        /* No need to rescale partials */
        beagle.getBeagle().getPartials(beagle.getPartialBufferHelper().getOffsetIndex(number), cumulativeBufferIndex, partials);
    }

    public void getTransitionMatrix(int matrixNum, double[] probabilities) {
        beagle.getBeagle().getTransitionMatrix(beagle.getMatrixBufferHelper().getOffsetIndex(matrixNum), probabilities);
    }

    public void traverseSampleNew(TreeInterface tree, Node node) {
        int nodeNum = node.getNr();

        Node parent = node.getParent();

        // This function DOES NOT assume that all partial likelihoods have already been calculated
        // If the node is internal, then sample its state given the state of its parent (pre-order traversal).

        if (!node.isLeaf()) {
            for (int j= 0; j < patternCount; j++) {
                reconstructedStates[nodeNum][j] = (int) nodeTraits.getArrayValue(node.getNr());
            }
        } else {
            for (int j= 0; j < patternCount; j++) {
                reconstructedStates[nodeNum][j] = tipStates[nodeNum][j];
                if (dataType.isAmbiguousCode(reconstructedStates[nodeNum][j])) {
                    // TODO (jf): handle this better or more accurately, if this is how we handle it, make sure the
                    // operator operates correctly
                    reconstructedStates[nodeNum][j] = (int) nodeTraits.getArrayValue(node.getNr());
                }
            }
        }

        if (parent == null) {
            // TODO (jf): what do we do here? do we do anything?
            jointLogLikelihood += 0;
        } else {
            int parentState = nodeTraits.getValue(parent.getNr());
            int j = 0;
            int currentState = reconstructedStates[nodeNum][j];
            double currentOccupancy = occupancy.getArrayValue(nodeNum);

            // This is an internal node or external leaf, but not the root
            final double branchRate = branchRateModel.getRateForBranch(node);
            final double branchTime = node.getLength() * branchRate;
            double[] transition_probabilities = new double[stateCount*stateCount];
            final double heightDifference = parent.getHeight() - node.getHeight();

            // figure out the minimum switches
            int minimum_switches;
            if (currentState != parentState) {
                minimum_switches = 1;
            }
            else if (currentOccupancy == 0 || currentOccupancy == 1) {
                minimum_switches = 0;
            }
            else {
                minimum_switches = 2;
            }

            // to get transition[parent][child], we need parent*statecount + child

            double probability = 0;
            switch (minimum_switches) {
                case 0:
                    substitutionModel.getTransitionProbabilities(node, parent.getHeight(), node.getHeight(), branchRate, transition_probabilities);
                    probability = transition_probabilities[parentState * stateCount + currentState];
                    break;
                case 1:
                    double timeB = heightDifference * currentOccupancy;
                    double timeA = heightDifference - timeB;
                    double timeParent, timeChild;
                    if (parentState == 0) {
                        timeParent = timeA;
                        timeChild = timeB;
                    } else {
                        timeParent = timeB;
                        timeChild = timeA;
                    }
                    substitutionModel.getTransitionProbabilities(node, timeParent, 0, branchRate, transition_probabilities);
                    probability = transition_probabilities[parentState * stateCount + currentState];
                    substitutionModel.getTransitionProbabilities(node, timeChild, 0, branchRate, transition_probabilities);
                    probability = probability * transition_probabilities[currentState * stateCount + currentState];
                    break;
                case 2:
                    double timeX = heightDifference * (1 - occupancy.getArrayValue(nodeNum)) / 2;
                    int otherState = stateCount - reconstructedStates[nodeNum][j] - 1;
                    double[][] q = ((GeneralSubstitutionModel) substitutionModel).getRateMatrix();
                    boolean isZeroInQ = false;
                    for (int i = 0; i < q.length; i++) {
                        for (int k = 0; k < q.length; k++) {
                            if (q[i][k] == 0) {
                                probability = 0;
                                isZeroInQ = true;
                                break;
                            }
                        }
                    }
                    if (isZeroInQ) {
                        break;
                    }
                    substitutionModel.getTransitionProbabilities(node, timeX, 0, branchRate, transition_probabilities);
                    probability = transition_probabilities[1] * transition_probabilities[2];
                    substitutionModel.getTransitionProbabilities(node, heightDifference * occupancy.getArrayValue(nodeNum), 0, branchRate, transition_probabilities);
                    probability = probability * transition_probabilities[otherState * stateCount + otherState];
                    break;
            }
            jointLogLikelihood += Math.log(probability);
        }

        if (!node.isLeaf()) {

            // Traverse down the two child nodes
            Node child1 = node.getChild(0);
            traverseSampleNew(tree, child1);

            Node child2 = node.getChild(1);
            traverseSampleNew(tree, child2);
        }
    }


    @Override
    public void log(final long sample, final PrintStream out) {
        // useful when logging on a fixed tree in an AncestralTreeLikelihood that is logged, but not part of the posterior
        // TODO (jf): what is the point of this and do we need it?
        hasDirt = Tree.IS_FILTHY;
        calculateLogP();
//        super.calculateLogP();
//        System.out.println("returnML: " + returnMarginalLogLikelihood);
        out.print(getCurrentLogP() + "\t");
    }


    protected DataType dataType;
    private int[][] reconstructedStates;
    private int[][] storedReconstructedStates;

    private String tag;
    private boolean areStatesRedrawn = false;
    private boolean storedAreStatesRedrawn = false;

    private boolean useMAP = false;
    private boolean returnMarginalLogLikelihood = true;

    private double jointLogLikelihood;
    private double occupancyLogLikelihood;
    private double storedJointLogLikelihood;

    boolean likelihoodKnown = false;
}
