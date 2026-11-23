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

package tyche.evolution.tree;


import beast.base.core.BEASTInterface;
import beast.base.core.Citation;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Log;
import beast.base.evolution.operator.Exchange;
import beast.base.evolution.operator.kernel.BactrianSubtreeSlide;
import beast.base.evolution.operator.WilsonBalding;
import beast.base.evolution.tree.Node;
import beast.base.spec.evolution.tree.coalescent.RandomTree;
import beast.base.inference.Operator;
import beast.base.inference.StateNode;
import beast.pkgmgmt.BEASTClassLoader;
import tyche.evolution.operator.*;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import tyche.TycheCitation;

/**
 * @author Jessie Fielding
 * This class is part of the TyCHE package - https://github.com/hoehnlab/tyche
 */


/**
 * A MetadataTree that treats the Germline and root as one unit, using GRTNodes to set their heights together.
 */
@Description("A MetadataTree that treats the Germline and root as one unit, using GRTNodes to set their heights together.")
@Citation(value = TycheCitation.TEXT, year = TycheCitation.YEAR, firstAuthorSurname = TycheCitation.FIRST_AUTHOR, DOI = TycheCitation.DOI)
public class GermlineRootTree extends MetadataTree {

    protected int germlineNum = -1;

    protected String nodeType;

    // Operators are matched by class name so that this class never refers to a class BEAST 3 has deprecated.
    private static final String LEGACY_SCALE = "beast.base.evolution.operator.ScaleOperator";
    private static final String LEGACY_BACTRIAN_SCALE = "beast.base.evolution.operator.kernel.BactrianScaleOperator";
    private static final String LEGACY_SUBTREE_SLIDE = "beast.base.evolution.operator.SubtreeSlide";
    private static final String SPEC_SCALE_TREE = "beast.base.spec.evolution.operator.ScaleTreeOperator";
    private static final String LEGACY_TREE_STRETCH = "beast.base.evolution.operator.TreeStretchOperator";
    private static final Map<String, String> GRTCompatibleOperatorsSuggestions = new HashMap<>(){
        {
            put(WilsonBalding.class.getName(), GRTWilsonBalding.class.getName());
            put(Exchange.class.getName(), GRTExchange.class.getName());
            put(SPEC_SCALE_TREE, GRTBactrianScaleOperator.class.getName());
            put(LEGACY_BACTRIAN_SCALE, GRTBactrianScaleOperator.class.getName());
            put(LEGACY_SCALE, GRTBactrianScaleOperator.class.getName());
            put(LEGACY_SUBTREE_SLIDE, GRTBactrianSubtreeSlide.class.getName());
            put(BactrianSubtreeSlide.class.getName(), GRTBactrianSubtreeSlide.class.getName());
        }
    };

    /** whether the class, or any class it extends, has this name */
    private static boolean hasAncestorNamed(Class<?> c, String name) {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            if (k.getName().equals(name)) return true;
        }
        return false;
    }

    /**
     * Initialize and validate the tree.
     */
    @Override
    public void initAndValidate() {
        // set the node type to something compatible, add warning if we're changing it
        try {
            if (GRTNode.class.isAssignableFrom(BEASTClassLoader.forName(nodeTypeInput.get()))) {
                nodeType = nodeTypeInput.get();
            }
            else {
                Log.warning("GermlineRootTree must use compatible nodes. Using TyCHE GRTNode.");
                nodeType = GRTNode.class.getName();
            }
        } catch (Exception e) {
            // throw error if the input node type can't be found by BEASTClassLoader
            throw new IllegalArgumentException("Cannot find type of nodeTypeInput " + nodeTypeInput.get() + ": " + e.getMessage());
        }
        // initialize the tree as any normal tree would be initialized
        super.initAndValidate();

        // get the germline number and validate there's only one germline
        findGermline();

        // ensure the random tree also uses compatible nodes
        lookForRandomTree();

        // validate that the operators are compatible with this type of tree
        lookAtOperators();
    }

    /**
     * checks all external nodes to see if any contain "germline" in their name
     */
    protected void findGermline() {
        for (Node node : getExternalNodes()) {
            if (node.getID().toUpperCase().contains("germline".toUpperCase())) {
                if (germlineNum != -1) {
                    throw new IllegalArgumentException("GermlineRootTree: Multiple germlines found. Please provide only one sequence with a string containing 'germline' as its taxon label, or use a different type of tree.");
                }
                this.germlineNum = node.getNr();
            }
        }
    }

    /**
     * Get the node number of the germline
     * @return integer representing the node number of the node that is the germline
     */
    public int getGermlineNum() {
        return germlineNum;
    }

    /**
     * checks all beast "outputs" associated with this tree to see if any are a RandomTree tree initializer, and if so,
     * ensures that the tree initializer is using a compatible node type.
     */
    protected void lookForRandomTree() {

        for (BEASTInterface o : getOutputs()) {
            if (o instanceof RandomTree) {
                RandomTree treeInit = (RandomTree) o;
                try {
                    if (!GRTNode.class.isAssignableFrom(BEASTClassLoader.forName(treeInit.nodeTypeInput.get()))) {
                        Log.warning("GermlineRootTree must use compatible nodes. Using TyCHE GRTNode.");
                        treeInit.nodeTypeInput.set(nodeType);
                    }
                } catch (Exception e) {
                    throw new IllegalArgumentException(treeInit.getID() + ": Cannot find type of nodeTypeInput " + treeInit.nodeTypeInput.get() + ": " + e.getMessage());
                }
            }
        }

    }

    /**
     * checks if a beast "outputs" associated with this tree is a problematic operator type that's not a GRTCompatibleOperator
     * @param o BEASTInterface object to check
     * @return true if not compatible, false if compatible
     */
    protected boolean isIncompatibleOperator(BEASTInterface o) {
        boolean isPotentiallyIncompatible = false;
        Class<?> c = o.getClass();
        if (hasAncestorNamed(c, SPEC_SCALE_TREE) || hasAncestorNamed(c, LEGACY_SCALE) || hasAncestorNamed(c, LEGACY_BACTRIAN_SCALE)) {
            // if it's not an explicitly compatible scale operator, check if it's a case that breaks (i.e. rootOnly tree scaler)
            Input<?> rootOnly = o.getInput("rootOnly");
            Input<?> tree = o.getInput("tree");
            if (rootOnly != null && Boolean.TRUE.equals(rootOnly.get()) && tree != null && tree.get() != null) {
                isPotentiallyIncompatible = true;
            }
        } else if (o instanceof Exchange || o instanceof BactrianSubtreeSlide || o instanceof WilsonBalding || hasAncestorNamed(c, LEGACY_SUBTREE_SLIDE) || hasAncestorNamed(c, LEGACY_TREE_STRETCH)) {
            isPotentiallyIncompatible = true;
        }
        return isPotentiallyIncompatible && !(o instanceof GRTCompatibleOperator);
    }

    /**
     * checks all beast "outputs" associated with this tree to identify scale operators that are not GRTCompatibleOperators
     * if any exist, check to see if they are tree scalers with rootOnly set to true, because this is the only case where
     * core BEAST operators break.
     */
    protected void lookAtOperators() {
        if (germlineNum == -1) {
            // if there isn't a germline, we don't care which operators are used
            return;
        }
        for (BEASTInterface o : getOutputs()) {
            if (isIncompatibleOperator(o)) {
                if (o instanceof Operator) { // we know it will be an operator this is just a check for java
                    Operator op = (Operator) o;
                    List<StateNode> stateNodes = op.listStateNodes();
                    if (stateNodes.contains(this)) {
                        String suggestedClass = GRTCompatibleOperatorsSuggestions.getOrDefault(o.getClass().getName(), "");
                        String suggestion = (Objects.equals(suggestedClass, "")) ? "" : suggestedClass + " or";
                        // now we're sure it applies to this tree, and it's a case that will break things, so throw an error
                        if (hasAncestorNamed(o.getClass(), LEGACY_TREE_STRETCH)) {
                            // TreeStretchOperator treats the germline as a fixed tip, so its root move is not reversible
                            throw new IllegalArgumentException(this.getID() + ": " + this.getClass() + " has found a germline, and is therefore incompatible with TreeStretchOperator " + o.getID() + " of class " + o.getClass() + ".\nRemove it, or use a class that implements " + GRTCompatibleOperator.class.getName());
                        }
                        throw new IllegalArgumentException(this.getID() + ": " + this.getClass() + " has found a germline, and is therefore incompatible with rootOnly scale operator " + o.getID() + " of class " + o.getClass() + ".\nTry using " + suggestion + " a class that implements " + GRTCompatibleOperator.class.getName());
                    }
                }
            }
        }

    }

    @Override
    public void store() {
        if (root instanceof GRTNode) {
            ((GRTNode) root).resetGermline();
        }
        super.store();
//        System.out.println("Here?");
    }

    @Override
    public void restore() {
        super.restore();
        if (root instanceof GRTNode) {
            ((GRTNode) root).resetGermline();
        }
    }

    /**
     * BEAST's {@code Tree.scale}, followed by a check that no node ended up above its parent (or at NaN/infinity).
     * {@link GRTNode#intervalScale} already refuses a zero, negative or non-finite factor before changing anything; this
     * second check catches any other way a scale could leave a negative branch length, and names the node. It throws
     * {@link IllegalArgumentException}, which is the rejection signal the {@code Scalable} contract uses: a scale
     * operator turns it into a rejected proposal instead of evaluating an invalid tree (which crashed MRCAPrior).
     */
    @Override
    public double scale(final double scale) {
        final double logJacobian = super.scale(scale);
        for (final Node node : getNodesAsArray()) {
            if (node.isRoot()) continue;
            final double h = node.getHeight();
            final double parentHeight = node.getParent().getHeight();
            if (!(Double.isFinite(h) && Double.isFinite(parentHeight) && parentHeight >= h)) {
                throw new IllegalArgumentException("Scale gives negative branch length: node " + node.getNr()
                        + " (height " + h + ") is above its parent " + node.getParent().getNr()
                        + " (height " + parentHeight + ") after scaling by " + scale);
            }
        }
        return logJacobian;
    }

    /**
     * The tree's position on its dilation axis (BEAST 3's {@code Scalable} contract): the sum of the margins (height
     * above the tallest child) of all internal nodes. It is exactly {@code s}-equivariant under {@link #scale(double)},
     * which scales the same margins.
     * <p>
     * BEAST's version measures the root's margin from its tallest child, which here is the germline, sitting
     * {@code GRTNode.EPSILON} below the root. That would leave the real root branch out of the sum and put a constant
     * 1e-7 in its place. Here the root's margin is measured from its non-germline child (see
     * {@link GRTNode#getMinimumHeight()}), so the root branch is included and the germline offset is not.
     * @return the sum of margins, in the tree's time units
     */
    @Override
    public double getScalableValue() {
        return sumMargins(getRoot());
    }

    private double sumMargins(final Node node) {
        if (node.isLeaf()) {
            return 0.0;
        }
        if (node.isFake()) {
            // sampled-ancestor fake node: skip it and follow the child that is not the direct ancestor (as BEAST does)
            return sumMargins(node.getLeft().isDirectAncestor() ? node.getRight() : node.getLeft());
        }
        final double minHeight;
        if (node instanceof GRTNode) {
            minHeight = ((GRTNode) node).getMinimumHeight();
        } else {
            double m = 0.0;
            for (final Node child : node.getChildren()) {
                m = Math.max(m, child.getHeight());
            }
            minHeight = m;
        }
        double sum = node.getHeight() - minHeight;
        for (final Node child : node.getChildren()) {
            sum += sumMargins(child);
        }
        return sum;
    }

    /**
     * Makes a new node of type GRTNode or node type specified in XML.
     * @return a new GRTNode object
     */
    @Override
    protected GRTNode newNode() {
        try {
            return (GRTNode) BEASTClassLoader.forName(nodeType).newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Cannot create node of type "
                    + nodeTypeInput.get() + ": " + e.getMessage());
        }
    }

}
