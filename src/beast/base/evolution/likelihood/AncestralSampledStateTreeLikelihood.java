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
import beast.base.inference.parameter.RealParameter;
import beastclassic.evolution.tree.TreeTrait;
import beastclassic.evolution.tree.TreeTraitProvider;
import beast.base.inference.parameter.IntegerParameter;
import beastclassic.evolution.likelihood.LeafTrait;
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
public class AncestralSampledStateTreeLikelihood extends TreeLikelihood implements TreeTraitProvider {
    public static final String STATES_KEY = "states";

    public Input<String> tagInput = new Input<String>("tag","label used to report trait", Validate.REQUIRED);
    public Input<Boolean> useMAPInput = new Input<Boolean>("useMAP","whether to use maximum aposteriori assignments or sample", false);
    public Input<Boolean> returnMLInput = new Input<Boolean>("returnML", "report integrate likelihood of tip data", true);

    public Input<Boolean> useJava = new Input<Boolean>("useJava", "prefer java, even if beagle is available", true);

    public Input<Boolean> sampleTipsInput = new Input<Boolean>("sampleTips", "if tips have missing data/ambigous values sample them for logging (default true)", true);

    public Input<IntegerParameter> nodeTraitsInput = new Input<IntegerParameter>("nodeTraits", "the trait for each node");

    IntegerParameter nodeTraits;

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

        this.useMAP = useMAPInput.get();
        this.returnMarginalLogLikelihood = returnMLInput.get();
        System.out.println("returnML: " + this.returnMarginalLogLikelihood);

        nodeTraits = nodeTraitsInput.get();

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
        int[][] tipStates = new int[tipCount][];

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
                int currentState = tipStates[node.getNr()][0];
                if (!dataType.isAmbiguousCode(currentState)) {
                    nodeTraits.setValue(node.getNr(), tipStates[node.getNr()][0]);
                }
            } else {
                int [] states = tipStates[node.getNr()];
                for (int i = 0; i < patternCount; i++) {
                    int code = data.getPattern(taxonIndex, i);
                    int[] statesForCode = data.getDataType().getStatesForCode(code);
                    if (statesForCode.length == 1) {
                        states[i] = statesForCode[0];
                        nodeTraits.setValue(node.getNr(), states[i]);
                    } else {
                        states[i] = code; // Causes ambiguous states to be ignored.
                        nodeTraits.setValue(node.getNr(), states[i]);
                    }
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
//        if (!m_useAmbiguities.get() && leafTriatsInput.get().size() == 0) {
//            return;
//        }
//        traitDimension = tipStates[0].length;

//        leafNr = new int[leafTriatsInput.get().size()];
//        parameters = new IntegerParameter[leafTriatsInput.get().size()];
//
//        List<String> taxaNames = dataInput.get().getTaxaNames();
//        for (int i = 0; i < leafNr.length; i++) {
//            LeafTrait leafTrait = leafTriatsInput.get().get(i);
//            parameters[i] = leafTrait.parameter.get();
//            // sanity check
//            if (parameters[i].getDimension() != traitDimension) {
//                throw new IllegalArgumentException("Expected parameter dimension to be " + traitDimension + ", not "
//                        + parameters[i].getDimension());
//            }
//            // identify node
//            String taxon = leafTrait.taxonName.get();
//            int k = 0;
//            while (k < taxaNames.size() && !taxaNames.get(k).equals(taxon)) {
//                k++;
//            }
//            leafNr[i] = k;
//            // sanity check
//            if (k == taxaNames.size()) {
//                throw new IllegalArgumentException("Could not find taxon '" + taxon + "' in tree");
//            }
//            // initialise parameter value from states
//            Integer[] values = new Integer[tipStates[k].length];
//            for (int j = 0; j < tipStates[k].length; j++) {
//                values[j] = tipStates[k][j];
//            }
//            IntegerParameter p = new IntegerParameter(values);
//            p.setLower(0);
//            p.setUpper(dataType.getStateCount()-1);
//            parameters[i].assignFromWithoutID(p);
//        }

//        storedTipStates = new int[tipStates.length][traitDimension];
//        for (int i = 0; i < tipStates.length; i++) {
//            System.arraycopy(tipStates[i], 0, storedTipStates[i], 0, traitDimension);
//        }
        // TODO: why are we not getting here?
        System.out.println("HERE2");
    }

    @Override
    public void store() {
        super.store();

        storedAreStatesRedrawn = areStatesRedrawn;
        storedJointLogLikelihood = jointLogLikelihood;

    }

    @Override
    public void restore() {

        super.restore();

        areStatesRedrawn = storedAreStatesRedrawn;
        jointLogLikelihood = storedJointLogLikelihood;

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

        return new int[] {nodeTraits.getValue(node.getNr())};
    }

//    private boolean checkConditioning = true;


    @Override
    public double calculateLogP() {
////
        double marginalLogLikelihood = super.calculateLogP();
        likelihoodKnown = true;
//        if (returnMarginalLogLikelihood) {
//            return logP;
//        }
//        // redraw states and return joint density of drawn states
//        redrawAncestralStates();
        jointLogLikelihood = 0;
        TreeInterface tree = treeInput.get();
        traverseSample(tree, tree.getRoot(), null);
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
        states[0] = nodeTraits.getValue(tipNum);
//        System.arraycopy(nodeTraits.getValue(tipNum), 0, states, 0, states.length);
    }

    public void getPartials(int number, double[] partials) {
        int cumulativeBufferIndex = Beagle.NONE;
        /* No need to rescale partials */
        beagle.getBeagle().getPartials(beagle.getPartialBufferHelper().getOffsetIndex(number), cumulativeBufferIndex, partials);
    }

    public void getTransitionMatrix(int matrixNum, double[] probabilities) {
        beagle.getBeagle().getTransitionMatrix(beagle.getMatrixBufferHelper().getOffsetIndex(matrixNum), probabilities);
    }

    public void traverseSample(TreeInterface tree, Node node, int[] parentState) {
//        System.out.println("in traverseSample");
        int nodeNum = node.getNr();

        Node parent = node.getParent();

        // This function assumes that all partial likelihoods have already been calculated
        // If the node is internal, then sample its state given the state of its parent (pre-order traversal).

        double[] conditionalProbabilities = new double[stateCount];
        int[] state = new int[patternCount];

        if (!node.isLeaf()) {

            if (parent == null) {

                double[] rootPartials = new double[stateCount * patternCount];
                likelihoodCore.getNodePartials(node.getNr(), rootPartials);


                double[] rootFrequencies = substitutionModel.getFrequencies();
                if (rootFrequenciesInput.get() != null) {
                    rootFrequencies = rootFrequenciesInput.get().getFreqs();
                }

                // This is the root node
                for (int j = 0; j < patternCount; j++) {
                    if (beagle != null) {
                        getPartials(node.getNr(), conditionalProbabilities);
                    } else {
                        System.arraycopy(rootPartials, j * stateCount, conditionalProbabilities, 0, stateCount);
                    }

                    for (int i = 0; i < stateCount; i++) {
                        conditionalProbabilities[i] *= rootFrequencies[i];
                    }
                    state[j] = (int) nodeTraits.getArrayValue(node.getNr());

                    jointLogLikelihood += Math.log(rootFrequencies[state[j]]);
                }

            } else {

                // This is an internal node, but not the root
                double[] partialLikelihood = new double[stateCount * patternCount];

                if (beagle != null) {
                    getPartials(node.getNr(), partialLikelihood);
                    getTransitionMatrix(nodeNum, probabilities);
                } else {
                    likelihoodCore.getNodePartials(node.getNr(), partialLikelihood);
                    /*((AbstractLikelihoodCore)*/ likelihoodCore.getNodeMatrix(nodeNum, 0, probabilities);
                }


                for (int j = 0; j < patternCount; j++) {

                    int parentIndex = parentState[j] * stateCount;
                    int childIndex = j * stateCount;

                    for (int i = 0; i < stateCount; i++) {
                        conditionalProbabilities[i] = partialLikelihood[childIndex + i] * probabilities[parentIndex + i];
                    }

                    state[j] = (int) nodeTraits.getArrayValue(nodeNum);
                    double contrib = probabilities[parentIndex + state[j]];
                    //System.out.println("Pr(" + parentState[j] + ", " + state[j] +  ") = " + contrib);
                    jointLogLikelihood += Math.log(contrib);
                }
            }

            // Traverse down the two child nodes
            Node child1 = node.getChild(0);
            traverseSample(tree, child1, state);

            Node child2 = node.getChild(1);
            traverseSample(tree, child2, state);
        } else {
            // This is an external leaf
            double[] partialLikelihood = new double[stateCount * patternCount];

            if (beagle != null) {
                getPartials(node.getNr(), partialLikelihood);
                getTransitionMatrix(nodeNum, probabilities);
            } else {
                likelihoodCore.getNodePartials(node.getNr(), partialLikelihood);
                /*((AbstractLikelihoodCore)*/ likelihoodCore.getNodeMatrix(nodeNum, 0, probabilities);
            }

            for (int j = 0; j < patternCount; j++) {

                int parentIndex = parentState[j] * stateCount;
                int childIndex = j * stateCount;

                for (int i = 0; i < stateCount; i++) {
                    conditionalProbabilities[i] = partialLikelihood[childIndex + i] * probabilities[parentIndex + i];
                }

                state[j] = (int) nodeTraits.getArrayValue(nodeNum);
                double contrib = probabilities[parentIndex + state[j]];
                //System.out.println("Pr(" + parentState[j] + ", " + state[j] +  ") = " + contrib);
                jointLogLikelihood += Math.log(contrib);
            }
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

    private String tag;
    private boolean areStatesRedrawn = false;
    private boolean storedAreStatesRedrawn = false;

    private boolean useMAP = false;
    private boolean returnMarginalLogLikelihood = true;

    private double jointLogLikelihood;
    private double storedJointLogLikelihood;

    boolean likelihoodKnown = false;
}
