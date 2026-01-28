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

package tyche.evolution.operator;

import beast.base.core.Citation;
import beast.base.core.Description;
import beast.base.core.Log;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.inference.util.InputUtil;
import beast.base.util.Randomizer;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Jessie Fielding
 * This class is part of the TyCHE package - https://github.com/hoehnlab/tyche
 */


/**
 * Tree Operator that operates on types associated with internal nodes and ambiguous tips by proposing type changes for a node and a section around that node.
 */
@Description("Tree Operator that operates on types associated with internal nodes and ambiguous tips by proposing type changes for a node and a section around that node.")
@Citation(value="Fielding, J. J., Wu, S., Melton, H. J., Fisk, N., du Plessis, L., & Hoehn, K. B. (2025).\n" +
        "TyCHE enables time-resolved lineage tracing of heterogeneously-evolving populations.\n" +
        "bioRxiv https://doi.org/10.1101/2025.10.21.683591 (2025) doi:10.1101/2025.10.21.683591.",
        year = 2025, firstAuthorSurname = "Fielding", DOI="10.1101/2025.10.21.683591")
public abstract class MultiNodeTypeSwitchOperator extends LeafConsciousTypeTreeOperator {

    protected int numberOfTypes;

    protected TreeTraverseMode mode;

    protected TypeSwitchMode typeSwitchMode;
    protected int generationLimit;

    protected int homogenousValue;

    protected enum States {
        CURRENT,
        PROPOSAL
    }

    protected enum ProposalMode {
        HETEROGENOUS,
        HOMOGENOUS
    }

    protected enum TypeSwitchMode {
        HALF_HOMOGENOUS,
        RANDOM,
        TYPE_FLIP
    }

    protected enum TreeTraverseMode {
        SUBTREE,
        UPTREE,
        BOTH
    }

    protected Map<States, Integer> currentOriginTypes;
    protected Map<States, Boolean> isHomogenous;


    /**
     * empty constructor to facilitate construction by XML + initAndValidate
     */
    public MultiNodeTypeSwitchOperator() {
    }

    public MultiNodeTypeSwitchOperator(Tree tree) {
        try {
            initByName(treeInput.getName(), tree);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            throw new RuntimeException("Failed to construct Trait Operator.");
        }
    }


    /**
     * Initialize and validate the operator.
     */
    @Override
    public void initAndValidate() {
        super.initAndValidate();
        numberOfTypes = (upperInt - lowerInt) + 1;
        setTraverseMode();
        setTypeSwitchMode();
        setGenerationsLimit();
        switch (typeSwitchMode) {
            case HALF_HOMOGENOUS:
                isHomogenous = new HashMap<>();
                break;
            case RANDOM:
                currentProposalType = ProposalMode.HETEROGENOUS;
                break;
            case TYPE_FLIP:
                break;
            default:
//                TODO: make this error better
                throw new RuntimeException("Must set type switch mode.");
        }
    }

    /**
     * Change the parameter.
     *
     * @return log of Hastings Ratio, or Double.NEGATIVE_INFINITY if proposal should not be accepted
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
            final int nodeNr = Randomizer.nextInt(nodeCount);
            node = tree.getNode(nodeNr);
        } while (node.isLeaf()); // operator shouldn't pick a tree section that starts at the leaves

        double logHastingsRatio = setNodes(node);

        if (markCladesInput.get()) {
            node.makeAllDirty(Tree.IS_DIRTY);
        }

        return logHastingsRatio;
    }

    protected abstract void setTraverseMode();

    protected abstract void setTypeSwitchMode();

    protected abstract void setGenerationsLimit();

    protected abstract int getGenerationsForProposal();
    protected double setNodes(Node node) {
        // get how many generations from this node to go
        int generations = getGenerationsForProposal();

        switch (typeSwitchMode) {
            case HALF_HOMOGENOUS:
                // 50% of the time, make homogenous
                boolean makeHomogenous = Randomizer.nextDouble() > 0.5;
                currentProposalType = (makeHomogenous) ? ProposalMode.HOMOGENOUS : ProposalMode.HETEROGENOUS;
                homogenousValue = Randomizer.nextInt(upperInt - lowerInt + 1) + lowerInt; // from 0 to n-1, n must > 0,

                // make the isHomogenous map start at true until we find a difference
                for (States state : States.values()) {
                    isHomogenous.put(state, true);
                }

                // make the map for this origin node's type in current and in the proposal
                currentOriginTypes = new HashMap<>();
                break;
            case RANDOM:
                break;
            case TYPE_FLIP:
                break;
        }

        int changedNodes = 0;
        switch (mode) {
            case SUBTREE:
                changedNodes += setSubtree(node, generations);
                break;
            case UPTREE:
                changedNodes += setUptree(node, generations);
                break;
            case BOTH:
                changedNodes += setSubtree(node, generations);
                if (node.getParent() != null) {
                    changedNodes += setUptree(node.getParent(), generations-1);
                }
                break;
            default:
//                TODO: make this error message better
                throw new RuntimeException("MultiNodeTypeSwitchOperator cannot handle this mode.");
        }

        return getHastingsRatio(changedNodes);

    }



    protected double getHastingsRatio(int changedNodes) {
        if (typeSwitchMode == TypeSwitchMode.TYPE_FLIP || typeSwitchMode == TypeSwitchMode.RANDOM) {
            return 0.0;
        }
        double gCurrentGivenProposal = getProposalDistribution(changedNodes, isHomogenous.get(States.CURRENT));
        double gProposalGivenCurrent = getProposalDistribution(changedNodes, isHomogenous.get(States.PROPOSAL));
        return Math.log(gCurrentGivenProposal/gProposalGivenCurrent);
    }

    protected double getProposalDistribution(int changedNodes, Boolean isHomogenous) {
        double homogenousTerm = isHomogenous ? 1.0/(2*numberOfTypes) : 0.0;
        double heterogenousTerm = 1/(2*Math.pow(numberOfTypes, changedNodes));
        return homogenousTerm + heterogenousTerm;
    }

    protected int getRelatedNodeTypeProposalValue(int nodeNum) {
        // by default, just choose a random new type
        int newValue = Randomizer.nextInt(upperInt - lowerInt + 1) + lowerInt;;
        switch (typeSwitchMode) {
            case TYPE_FLIP:
                // if mode is type flip, make sure it's not the old value (i.e. we're always flipping). works best in 2 state case
                int oldValue = nodeTypes.getValue(nodeNum);
                while (newValue == oldValue) {
                    newValue = Randomizer.nextInt(upperInt - lowerInt + 1) + lowerInt; // from 0 to n-1, n must > 0
                }
                break;
            case HALF_HOMOGENOUS:
                // if mode is half-homogenous, random type is all we need for heterogenous proposal, but if it's a
                // homogenous proposal, we want to just return the homogenous value.
                if (currentProposalType == ProposalMode.HOMOGENOUS) {
                    newValue = homogenousValue;
                }
                break;
        }
        return newValue;
    }

    /**
     * Helper function to update the Map recording whether the current and proposed states are homogenous
     * @param currentType the value of this node in the current state
     * @param proposedType the proposed value of this node
     */
    protected void updateIsHomogenous(int currentType, int proposedType) {
        if (typeSwitchMode != TypeSwitchMode.HALF_HOMOGENOUS) {
            return;
        }
        for (States state : States.values()) {
            int comparisonType = state == States.CURRENT ? currentType : proposedType;
            if (!currentOriginTypes.containsKey(state)) {
                currentOriginTypes.put(state, comparisonType);
            }
            else if (isHomogenous.get(state)) {
                if (currentOriginTypes.get(state) != comparisonType) isHomogenous.put(state, false);
            }
        }
    }

    protected void setRelatedNodeType(int nodeNum, int newValue) {
        updateIsHomogenous(nodeTypes.getValue(nodeNum), newValue);
        nodeTypes.setValue(nodeNum, newValue);
    }

    /**
     * Set all nodeType values in the subtree to a new value.
     * @param node the node that is the root of the subtree we are setting to a new value
     * @param generations how many generations to set, -1 (or any value less than 0) for "all the way to tips"
     */
    protected int setSubtree(Node node, int generations) {
        int changedNodes = 0;
        if (generations == 0) {
            return changedNodes;
        }
        int nodeNum = node.getNr();
        if (node.isLeaf() && !isAmbiguous[nodeNum]) {
            return changedNodes;
        }

        int newValue = getRelatedNodeTypeProposalValue(nodeNum);
        setRelatedNodeType(nodeNum, newValue);
        changedNodes += 1;

        for (Node childNode : node.getChildren()) {
            changedNodes += setSubtree(childNode, generations - 1);
        }
        return changedNodes;
    }

    /**
     * Set all nodeType values of direct ancestors of this node.
     * @param node the node that is the root of the subtree we are setting to a new value
     * @param generations how many generations to set, -1 (or any value less than 0) for "all the way to tips"
     */
    protected int setUptree(Node node, int generations) {
        int changedNodes = 0;
        if (generations == 0) {
            return changedNodes;
        }
        int nodeNum = node.getNr();
        if (node.isLeaf() && !isAmbiguous[nodeNum]) {
            // theoretically upTree should never encounter a tip/leaf, but just in case
            return changedNodes;
        }

        int newValue = getRelatedNodeTypeProposalValue(nodeNum);

        setRelatedNodeType(nodeNum, newValue);
        changedNodes += 1;

        if (node.isRoot() && isGermlineRoot && germlineNum > 0) {
            setRelatedNodeType(germlineNum, newValue);
            changedNodes += 1;
        }

        if (node.getParent() != null) {
            changedNodes += setUptree(node.getParent(), generations - 1);
        }

        return changedNodes;
    }

    protected ProposalMode currentProposalType;

}