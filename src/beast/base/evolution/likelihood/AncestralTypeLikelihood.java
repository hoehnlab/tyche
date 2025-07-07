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
import beast.base.evolution.tree.TreeInterface;
import beastclassic.evolution.tree.TreeTrait;
import beastclassic.evolution.tree.TreeTraitProvider;
import beast.base.inference.parameter.IntegerParameter;

/**
 * @author Jessie Fielding
 */
@Description("AncestralTypeLikelihood to assess likelihood of internal and ambiguous node types.")
public class AncestralTypeLikelihood extends TreeLikelihood implements TreeTraitProvider {
    public static final String STATES_KEY = "states";

    public Input<String> tagInput = new Input<String>("tag","label used to report trait", Validate.REQUIRED);

    public Input<Boolean> useJava = new Input<Boolean>("useJava", "prefer java, even if beagle is available", true);

    public Input<IntegerParameter> nodeTypesInput = new Input<IntegerParameter>("nodeTypes", "the type associated with each node", Validate.REQUIRED);

    /**
     * AncestralTypeLikelihood
     *
     * @param dataType        - need to provide the data-type, so that corrent data characters can be returned
     * @param tag             - string label for reconstruction characters in tree log
     * @param useJava         - prefer java, even if beagle is available, default: true
     * @param nodeTypes       - the type associated with each node
     */

    IntegerParameter nodeTypes;
    double[][] qMatrix;
    int patternCount;
    int stateCount;

    int[][] tipStates; // used to store tip states

    @Override
    public void initAndValidate() {
        if (dataInput.get().getSiteCount() == 0) {
            return;
        }

        // ensure that super class initiates with java only if useJava is true
        String sJavaOnly = null;
        if (useJava.get()) {
            sJavaOnly = System.getProperty("java.only");
            System.setProperty("java.only", "" + true);
        }
        super.initAndValidate();
        // reset system property
        if (useJava.get()) {
            if (sJavaOnly != null) {
                System.setProperty("java.only", sJavaOnly);
            } else {
                System.clearProperty("java.only");
            }
        }

        tag = tagInput.get();
        TreeInterface treeModel = treeInput.get();
        patternCount = dataInput.get().getPatternCount();

        if (patternCount > 1) {
            throw new IllegalArgumentException("AncestralTypeLikelihood is only implemented for traits with a pattern count of 1.");
        }

        dataType = dataInput.get().getDataType();
        stateCount = dataType.getStateCount();

        nodeTypes = nodeTypesInput.get();

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
                if (!dataType.isAmbiguousCode(tipStates[node.getNr()][0])) {
                    nodeTypes.setValue(node.getNr(), tipStates[node.getNr()][0]);
                }
            } else {
                int [] states = tipStates[node.getNr()];
                int code = data.getPattern(taxonIndex, 0);
                int[] statesForCode = data.getDataType().getStatesForCode(code);
                if (statesForCode.length == 1) {
                    states[0] = statesForCode[0];
                    nodeTypes.setValue(node.getNr(), states[0]);
                } else {
                    states[0] = code; // Causes ambiguous states to be ignored.
                    nodeTypes.setValue(node.getNr(), states[0]);
                }
            }
        }

        if (m_siteModel.getCategoryCount() > 1)
            throw new RuntimeException("Reconstruction not implemented for multiple categories yet.");

        if (substitutionModel instanceof GeneralSubstitutionModel) {
            qMatrix = ((GeneralSubstitutionModel) substitutionModel).getRateMatrix();
        } else {
            throw new RuntimeException("Reconstruction not implemented for substitution models which do not inherit from the GeneralSubstitutionModel class.");
        }

        // use this for readable logging
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

    }

    @Override
    protected boolean requiresRecalculation() {

        super.requiresRecalculation();
        return true;

//        if (nodeTypes.isDirty(nodeTypes.getLastDirty())) {
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


    @Override
    public double calculateLogP() {

        super.calculateLogP();
        jointLogLikelihood = 0;
        TreeInterface tree = treeInput.get();
        traverseSample(tree.getRoot(), -1);
        logP = jointLogLikelihood;
        return logP;
    }


//    public void getStates(int tipNum, int[] states)  {
//        // Saved locally to reduce BEAGLE library access
//        System.arraycopy(tipStates[tipNum], 0, states, 0, states.length);
//    }

    public void traverseSample(Node node, int parentState) {
        int nodeNum = node.getNr();

        Node parent = node.getParent();

        // This function assumes that all partial likelihoods have already been calculated
        // If the node is internal, then sample its state given the state of its parent (pre-order traversal).

        double[] conditionalProbabilities = new double[stateCount];
        double conditionalProbability;
        final int thisState = nodeTypes.getValue(nodeNum);

        if (!node.isLeaf()) {

            if (parent == null) {

                double[] rootPartials = new double[stateCount * patternCount];
                likelihoodCore.getNodePartials(nodeNum, rootPartials);


                double[] rootFrequencies = substitutionModel.getFrequencies();
                if (rootFrequenciesInput.get() != null) {
                    rootFrequencies = rootFrequenciesInput.get().getFreqs();
                }

                // This is the root node
                if (beagle != null) {
                    getPartials(node.getNr(), conditionalProbabilities);
                } else {
                    System.arraycopy(rootPartials, 0, conditionalProbabilities, 0, stateCount);
                }

//                    for (int i = 0; i < stateCount; i++) {
//                        conditionalProbabilities[i] *= rootFrequencies[i];
//                    }

                conditionalProbability = conditionalProbabilities[thisState] * rootFrequencies[thisState];

                jointLogLikelihood += Math.log(conditionalProbability);

            } else {

                // This is an internal node, but not the root
                double[] partialLikelihood = new double[stateCount * patternCount];

                // get the partial likelihoods and the probabilities from the transition matrix, different from root
                if (beagle != null) {
                    getPartials(node.getNr(), partialLikelihood);
                    getTransitionMatrix(nodeNum, probabilities);
                } else {
                    likelihoodCore.getNodePartials(node.getNr(), partialLikelihood);
                    /*((AbstractLikelihoodCore)*/ likelihoodCore.getNodeMatrix(nodeNum, 0, probabilities);
                }

                int parentIndex = parentState * stateCount;

//                    for (int i = 0; i < stateCount; i++) {
//                        conditionalProbabilities[i] = partialLikelihood[childIndex + i] * probabilities[parentIndex + i];
//                    }
                conditionalProbability = partialLikelihood[thisState] * probabilities[parentIndex + thisState];

                double contrib = conditionalProbability;
                jointLogLikelihood += Math.log(contrib);
            }

            // Traverse down the two child nodes
            Node child1 = node.getChild(0);
            traverseSample(child1, thisState);

            Node child2 = node.getChild(1);
            traverseSample(child2, thisState);
        } else {

            // This is an external leaf
            // Check for ambiguity codes and sample them
            final int parentIndex = parentState * stateCount;
            if (beagle != null) {
                /*((AbstractLikelihoodCore) */ getTransitionMatrix(nodeNum, probabilities);
            } else {
                /*((AbstractLikelihoodCore) */likelihoodCore.getNodeMatrix(nodeNum, 0, probabilities);
            }
//           TODO(jf): test that this still works without this line
            likelihoodCore.getNodeStates(nodeNum, tipStates[nodeNum]);
            if (dataType.isAmbiguousCode(tipStates[nodeNum][0])) {
                boolean [] stateSet = dataType.getStateSet(thisState);
                conditionalProbability = stateSet[thisState] ? probabilities[parentIndex + thisState] : 0;
//                for (int i = 0; i < stateCount; i++) {
//                    conditionalProbabilities[i] =  stateSet[i] ? probabilities[parentIndex + i] : 0;
//                }
            } else {
//                for (int i = 0; i < stateCount; i++) {
//                    conditionalProbabilities[i] = probabilities[parentIndex + i];
//                }
                conditionalProbability = probabilities[parentIndex + thisState];
            }

            double contrib = conditionalProbability;
            jointLogLikelihood += Math.log(contrib);
        }
    }

    @Override
    public void store() {
        super.store();
        storedJointLogLikelihood = jointLogLikelihood;
    }

    @Override
    public void restore() {
        super.restore();
        jointLogLikelihood = storedJointLogLikelihood;

    }

//    @Override
//    public void log(final long sample, final PrintStream out) {
//        // useful when logging on a fixed tree in an AncestralTreeLikelihood that is logged, but not part of the posterior
//        // TODO (jf): what is the point of this and do we need it?
//        hasDirt = Tree.IS_FILTHY;
//        calculateLogP();
//        out.print(getCurrentLogP() + "\t");
//    }
    /**
     *  Helper methods, wrappers for beagle calls
     */

    public void getPartials(int number, double[] partials) {
        int cumulativeBufferIndex = Beagle.NONE;
        /* No need to rescale partials */
        beagle.getBeagle().getPartials(beagle.getPartialBufferHelper().getOffsetIndex(number), cumulativeBufferIndex, partials);
    }

    public void getTransitionMatrix(int matrixNum, double[] probabilities) {
        beagle.getBeagle().getTransitionMatrix(beagle.getMatrixBufferHelper().getOffsetIndex(matrixNum), probabilities);
    }

    /**
     *  Methods required for implementing TreeTraitProvider
     *  for logging with beastclassic.evolution.tree.TreeWithTraitLogger
     */
    public DataType getDataType() {
        return dataType;
    }

    public int[] getStatesForNode(TreeInterface tree, Node node) {
        if (tree != treeInput.get()) {
            throw new RuntimeException("Can only reconstruct states on treeModel given to constructor");
        }

        return new int[] {nodeTypes.getValue(node.getNr())};
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


    protected DataType dataType;

    private String tag;

    private double jointLogLikelihood;
    private double storedJointLogLikelihood;
}