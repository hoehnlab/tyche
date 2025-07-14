/*
 *  Copyright (C) 2025 Hoehn Lab, Dartmouth College
 *
 * This file is part of TyCHE.
 *
 * TyCHE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * TyCHE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with TyCHE.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package tyche.evolution.likelihood;


import beagle.Beagle;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.datatype.DataType;
import beast.base.evolution.datatype.UserDataType;
import beast.base.evolution.likelihood.TreeLikelihood;
import beast.base.evolution.sitemodel.SiteModel;
import beast.base.evolution.substitutionmodel.GeneralSubstitutionModel;
import beast.base.evolution.substitutionmodel.SubstitutionModel;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.TreeInterface;
import beastclassic.evolution.tree.TreeTrait;
import beastclassic.evolution.tree.TreeTraitProvider;
import beast.base.inference.parameter.IntegerParameter;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author Jessie Fielding
 * This class is part of the TyCHE package - https://github.com/hoehnlab/tyche
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

        // get the state for each leaf
        Alignment data = dataInput.get();
        for (Node node : treeInput.get().getExternalNodes()) {
            // need to look each leaf up in the data by taxon/ID to get its index in the data
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
                // set the nodeTypes parameter to the tipStates data for all known (not ambiguous) leaves
                if (!dataType.isAmbiguousCode(tipStates[node.getNr()][0])) {
                    nodeTypes.setValue(node.getNr(), tipStates[node.getNr()][0]);
                }
            } else {
                int [] states = tipStates[node.getNr()];
                int code = data.getPattern(taxonIndex, 0);
                int[] statesForCode = dataType.getStatesForCode(code);
                if (statesForCode.length == 1) {
                    states[0] = statesForCode[0];
                } else {
                    states[0] = code; // Causes ambiguous states to be ignored.
                }
                nodeTypes.setValue(node.getNr(), states[0]);
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
                return getFormattedState(getStatesForNode(tree,node), dataType);
            }
        });

    }

    @Override
    protected boolean requiresRecalculation() {

        boolean isDirty = super.requiresRecalculation();

        isDirty |= nodeTypes.isDirty(nodeTypes.getLastDirty());

        return isDirty;

    }


    @Override
    public double calculateLogP() {

        jointLogLikelihood = 0;
        TreeInterface tree = treeInput.get();
        traverseTypeTree(tree.getRoot(), -1);
        logP = jointLogLikelihood;
        return logP;
    }

    public void traverseTypeTree(Node node, int parentState) {
        int nodeNum = node.getNr();

        double conditionalProbability;
        final int thisState = nodeTypes.getValue(nodeNum);
        int parentIndex = parentState * stateCount; // not used if root

        if (!node.isLeaf()) {
            if (node.getParent() == null) {
                // This is the root node, so use the root frequencies
                double[] rootFrequencies = substitutionModel.getFrequencies();
                if (rootFrequenciesInput.get() != null) {
                    rootFrequencies = rootFrequenciesInput.get().getFreqs();
                }

                conditionalProbability = rootFrequencies[thisState];
            } else {
                // This is an internal node, but not the root
                // use the probability from transition matrix, different from root
                getTransitionMatrix(nodeNum, probabilities);
                conditionalProbability = probabilities[parentIndex + thisState];
            }

            // Traverse down the two child nodes
            Node child1 = node.getChild(0);
            traverseTypeTree(child1, thisState);

            Node child2 = node.getChild(1);
            traverseTypeTree(child2, thisState);
        } else {
            // This is an external leaf, so just use the probability from transition matrix
            getTransitionMatrix(nodeNum, probabilities);

            // theoretically should work without this line unless we operate on leaf states via the AlignmentFromTrait traitset
            likelihoodCore.getNodeStates(nodeNum, tipStates[nodeNum]);

            // Check for ambiguity codes
            if (dataType.isAmbiguousCode(tipStates[nodeNum][0])) {
                boolean [] stateSet = dataType.getStateSet(tipStates[nodeNum][0]);
                // ensure that thiState is an allowed type of this ambiguous code
                conditionalProbability = stateSet[thisState] ? probabilities[parentIndex + thisState] : 0;
            } else {
                conditionalProbability = probabilities[parentIndex + thisState];
            }
        }
        jointLogLikelihood += Math.log(conditionalProbability); // update the jointLogLikelihood
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

    /**
     *  Helper methods, wrappers for beagle/likelihoodCore calls
     */

    public void getTransitionMatrix(int nodeNum, double[] probabilities) {
        if (beagle != null) {
            beagle.getBeagle().getTransitionMatrix(beagle.getMatrixBufferHelper().getOffsetIndex(nodeNum), probabilities);
        } else {
            /*((AbstractLikelihoodCore)*/ likelihoodCore.getNodeMatrix(nodeNum, 0, probabilities);
        }
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


    private static String getFormattedState(int[] state, DataType dataType) {
        String delimiter = (dataType instanceof UserDataType) ? " " : "";
        return "\"" + Arrays.stream(state).mapToObj(dataType::getCharacter).collect( Collectors.joining(delimiter)) + "\"";
    }


    protected DataType dataType;
    private String tag;

    private double jointLogLikelihood;
    private double storedJointLogLikelihood;
}