package tyche.evolution.operator;

import beast.base.core.Citation;
import beast.base.core.Description;
import beast.base.evolution.operator.kernel.BactrianSubtreeSlide;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.inference.util.InputUtil;
import beast.base.util.Randomizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import tyche.TycheCitation;

@Description("BactrianSubtreeSlide operator that will appropriately handle a GermlineRootTree: the step is drawn from a Bactrian kernel " +
        "like BactrianSubtreeSlide, but the root is never moved and no new root is created, so the germline stays attached to the root. " +
        "On a tree that is not a GermlineRootTree it behaves exactly like BactrianSubtreeSlide.")
@Citation(value = TycheCitation.TEXT, year = TycheCitation.YEAR, firstAuthorSurname = TycheCitation.FIRST_AUTHOR, DOI = TycheCitation.DOI)
public class GRTBactrianSubtreeSlide extends BactrianSubtreeSlide implements GRTCompatibleOperator {

    /**
     * The BactrianSubtreeSlide proposal, restricted so that a GermlineRootTree stays valid. Differences from the core proposal:
     * (1) a slide whose parent node p is the root is rejected. The root's two children are the germline and the MRCA; a move up
     *     would always be accepted but the reverse move (down) never, because it would go below the germline, so the operator
     *     would not be reversible and would push root height upward. Root height is moved by the dedicated root operators.
     * (2) no new root is created (moving past the root is rejected).
     * The step comes from the Bactrian kernel (getDelta() of the parent), which is symmetric, so the Hastings ratio is the same as for SubtreeSlide.
     */
    @Override
    public double doGRTProposal() {
        final Tree tree = (Tree) InputUtil.get(treeInput, this);
        double logq;
        Node i;

        final boolean markClades = markCladesInput.get();
        final int nodeCount = tree.getNodeCount();
        if (nodeCount == 1) {
            return Double.NEGATIVE_INFINITY;
        }

        do {
            i = tree.getNode(Randomizer.nextInt(nodeCount));
        } while (i.isRoot());

        final Node p = i.getParent();
        if (p.isRoot()) {
            return Double.NEGATIVE_INFINITY;
        }
        final Node CiP = getOtherChild(p, i);
        final Node PiP = p.getParent();

        final double delta = getDelta();
        final double oldHeight = p.getHeight();
        final double newHeight = oldHeight + delta;

        if (delta > 0) {
            if (PiP != null && PiP.getHeight() < newHeight) {
                Node newParent = PiP;
                Node newChild = p;
                while (newParent.getHeight() < newHeight) {
                    newChild = newParent;
                    if (markClades) newParent.makeDirty(Tree.IS_FILTHY);
                    newParent = newParent.getParent();
                    if (newParent == null) break;
                }
                if (newChild.isRoot()) {
                    // would create a new root above the germline's root
                    return Double.NEGATIVE_INFINITY;
                }
                if (!Objects.equals(p, newChild)) {
                    replace(p, CiP, newChild);
                    replace(PiP, p, CiP);
                    replace(newParent, newChild, p);
                }
                p.setHeight(newHeight);
                final int possibleSources = intersectingEdges(newChild, oldHeight, null);
                logq = -Math.log(possibleSources);
            } else {
                p.setHeight(newHeight);
                logq = 0.0;
            }
        } else {
            if (i.getHeight() > newHeight) {
                return Double.NEGATIVE_INFINITY;
            }
            if (CiP.getHeight() > newHeight) {
                final List<Node> newChildren = new ArrayList<>();
                final int possibleDestinations = intersectingEdges(CiP, newHeight, newChildren);
                if (newChildren.size() == 0) {
                    return Double.NEGATIVE_INFINITY;
                }
                final int childIndex = Randomizer.nextInt(newChildren.size());
                final Node newChild = newChildren.get(childIndex);
                final Node newParent = newChild.getParent();
                replace(p, CiP, newChild);
                replace(PiP, p, CiP);
                replace(newParent, newChild, p);
                p.setHeight(newHeight);
                if (markClades) {
                    Node n = p;
                    while (n != CiP) {
                        n.makeDirty(Tree.IS_FILTHY);
                        n = n.getParent();
                    }
                }
                logq = Math.log(possibleDestinations);
            } else {
                p.setHeight(newHeight);
                logq = 0.0;
            }
        }
        return logq;
    }

    @Override
    public double proposal() {
        try {
            if (isStructureGRT(treeInput)) {
                double toReturn = doGRTProposal();
                if (!isStructureGRT(treeInput)) {
                    return Double.NEGATIVE_INFINITY;
                }
                return toReturn;
            } else {
                return super.proposal();
            }
        } catch (Exception e) {
            return Double.NEGATIVE_INFINITY;
        }
    }

    /** Same as the parent's, but returns 0 instead of failing when the node has no parent (the root of a GermlineRootTree). */
    @Override
    protected int intersectingEdges(Node node, double height, List<Node> directChildren) {
        final Node parent = node.getParent();
        if (parent == null) {
            return 0;
        }
        if (parent.getHeight() < height) return 0;
        if (node.getHeight() < height) {
            if (directChildren != null) directChildren.add(node);
            return 1;
        }
        if (node.isLeaf()) {
            return 0;
        } else {
            return intersectingEdges(node.getLeft(), height, directChildren) +
                    intersectingEdges(node.getRight(), height, directChildren);
        }
    }
}
