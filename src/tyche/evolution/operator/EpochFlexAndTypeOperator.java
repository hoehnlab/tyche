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
import beast.base.core.Input;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.inference.operator.kernel.KernelDistribution;
import beast.base.util.Randomizer;

import java.text.DecimalFormat;

/**
 * @author Jessie Fielding
 * This class is part of the TyCHE package - https://github.com/hoehnlab/tyche
 */

/**
 * Scale operator that scales random epoch in a tree, and proposes type switches for the nodes in that epoch.
 */
@Description("Scale operator that scales random epoch in a tree, and proposes type switches for the nodes in that epoch.")
@Citation(value="Fielding, J. J., Wu, S., Melton, H. J., Fisk, N., du Plessis, L., & Hoehn, K. B. (2025).\n" +
        "TyCHE enables time-resolved lineage tracing of heterogeneously-evolving populations.\n" +
        "bioRxiv https://doi.org/10.1101/2025.10.21.683591 (2025) doi:10.1101/2025.10.21.683591.",
        year = 2025, firstAuthorSurname = "Fielding", DOI="10.1101/2025.10.21.683591")
public class EpochFlexAndTypeOperator extends LeafConsciousTypeTreeOperator {
    final public Input<KernelDistribution> kernelDistributionInput = new Input<>("kernelDistribution", "provides sample distribution for proposals",
            KernelDistribution.newDefaultKernelDistribution());
    final public Input<Boolean> optimiseInput = new Input<>("optimise", "flag to indicate that the scale factor is automatically changed in order to achieve a good acceptance rate (default true)", true);
    final public Input<Double> scaleFactorInput = new Input<>("scaleFactor", "scaling factor -- positive number that determines size of the jump: higher means bigger jumps.", 0.05);

    final public Input<Boolean> fromOldestTipOnlyInput = new Input<>("fromOldestTipOnly", "only scale parts between root and oldest tip. If false, use any epoch between youngest tip and root.", true);

    final public Input<Boolean> typeFlipInput = new Input<>("typeFlip", "flip the types, if true, never propose the current type at a node, if false, propose a random type at each node which can be the same as current value, default:false", false);


    protected KernelDistribution kernelDistribution;
    protected double scaleFactor;
    private boolean fromOldestTipOnly;
    protected boolean typeFlip;

    @Override
    public void initAndValidate() {
        super.initAndValidate();
        typeFlip = typeFlipInput.get();
        kernelDistribution = kernelDistributionInput.get();
        scaleFactor = scaleFactorInput.get();
        fromOldestTipOnly = fromOldestTipOnlyInput.get();
    }

    public EpochFlexAndTypeOperator(){}

    public EpochFlexAndTypeOperator(Tree tree, double weight) {
        initByName("tree", tree, "weight", weight);
    }


    private int getFlippedType(int nodeNum) {
        int newType;
        do {
            newType = getRandomType();
        } while (newType == nodeTypes.getValue(nodeNum));
        return newType;
    }
    protected void changeType(Node node) {
        if (typeFlip) {
            setNodeType(node.getNr(), getFlippedType(node.getNr()));
        }
        setNodeType(node.getNr(), getRandomType());

    }

    private int getRandomType() {
        return Randomizer.nextInt(upperInt - lowerInt + 1) + lowerInt; // from 0 to n-1, n must > 0
    }


    /**
     * Change the parameter.
     *
     * @return Double.NEGATIVE_INFINITY if proposal should not be accepted
     */
    @Override
    public double proposal() {
        Tree tree = treeInput.get();
        double oldHeight = tree.getRoot().getHeight();

        double upper = tree.getRoot().getHeight();
        double lower0 = 0;
        Node[] nodes = tree.getNodesAsArray();

        if (fromOldestTipOnly) {
            for (int i = 0; i < nodes.length/2+1; i++) {
                lower0 = Math.max(nodes[i].getHeight(), lower0);
            }
        }

        double intervalLow;
        double intervalHi = 0;


        int x = Randomizer.nextInt(tree.getInternalNodeCount());
        intervalLow = tree.getNode(tree.getLeafNodeCount() + x).getHeight();
        int y = x;
        while (x == y) {
            y = Randomizer.nextInt(tree.getInternalNodeCount());
            intervalHi  = tree.getNode(tree.getLeafNodeCount() + y).getHeight();
        }

        if (intervalHi < intervalLow) {
            // make sure intervalLow < intervalHi
            double tmp = intervalHi; intervalHi = intervalLow; intervalLow = tmp;
        }

        double scale = kernelDistribution.getScaler(1, scaleFactor);
        double to = intervalLow + scale * (intervalHi - intervalLow);
        double delta = to-intervalHi;

        int scaled = 0;
        for (int i = nodes.length/2+1; i < nodes.length; i++) {
            Node node = nodes[i];
            if (!node.isFake()) {
                // only change "real" internal nodes, not ancestral ones
                double h = node.getHeight();
                if (h > intervalLow && h <= intervalHi) {
                    h = intervalLow + scale * (h-intervalLow);
                    node.setHeight(h);
                    changeType(node); // only change the type for nodes in interval
                    scaled++;
                } else if (h > intervalHi) {
                    h += delta;
                    node.setHeight(h);
                }
            }
        }

        if (scaled < 2) {
            // see BEAST2's EpochFlexOperator for proof
            // let L = intervalLow, U = intervalHi, s = scale
            // with 2 nodes between L and U and one node above U, L, U and s are uniquely determined

            // with 1 node between L and U,
            // there are multiple L,U and s values resulting in the same proposal
            // and we cannot guarantee HR is correct
            return Double.NEGATIVE_INFINITY;
        }

        for (Node node0 : nodes) {
            if (node0.getLength() < 0) {
                return Double.NEGATIVE_INFINITY;
            }
        }

        return scaled * Math.log(scale);
    }



    /**
     * Optimize the operator by changing the scale factor.
     *
     */
    @Override
    public void optimize(double logAlpha) {
        // must be overridden by operator implementation to have an effect
        if (optimiseInput.get()) {
            double delta = calcDelta(logAlpha);
            double scaleFactor = getCoercableParameterValue();
            delta += Math.log(scaleFactor);
            scaleFactor = Math.exp(delta);
            setCoercableParameterValue(scaleFactor);
        }
    }

    @Override
    public double getTargetAcceptanceProbability() {
        return 0.4;
    }

    @Override
    public double getCoercableParameterValue() {
        return scaleFactor;
    }

    @Override
    public void setCoercableParameterValue(final double value) {
        scaleFactor = value; // Math.max(Math.min(value, upper), lower);
    }


    @Override
    public String getPerformanceSuggestion() {
        double prob = m_nNrAccepted / (m_nNrAccepted + m_nNrRejected + 0.0);
        double targetProb = getTargetAcceptanceProbability();

        double ratio = prob / targetProb;
        if (ratio > 2.0) ratio = 2.0;
        if (ratio < 0.5) ratio = 0.5;

        // new scale factor
        double newWindowSize = getCoercableParameterValue() * ratio;

        DecimalFormat formatter = new DecimalFormat("#.###");
        if (prob < 0.10 || prob > 0.40) {
            return "Try setting scale factor to about " + formatter.format(newWindowSize);
        } else return "";
    }
}

