package tyche.evolution.tree;

import beast.base.core.Citation;
import beast.base.core.Description;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;

import java.util.TreeMap;
/**
 * @author Jessie Fielding
 * This class is part of the TyCHE package - https://github.com/hoehnlab/tyche
 */


/**
 * A GermlineRootTree compatible node type, that keeps the height of the germline and the root together.
 */
@Description("A GermlineRootTree compatible node type, that keeps the height of the germline and the root together.")
@Citation(value="Fielding, J. J., Wu, S., Melton, H. J., Fisk, N., du Plessis, L., & Hoehn, K. B. (2025).\n" +
        "TyCHE enables time-resolved lineage tracing of heterogeneously-evolving populations.\n" +
        "bioRxiv https://doi.org/10.1101/2025.10.21.683591 (2025) doi:10.1101/2025.10.21.683591.",
        year = 2025, firstAuthorSurname = "Fielding", DOI="10.1101/2025.10.21.683591")
public class GRTNode extends Node {

    private GRTNode germline = null;

    /**
     * associate a germline node with this node
     */
    protected void addGermline(GRTNode germ) {
        this.germline = germ;
    }

    /**
     * is this node the germline?
     * @return true if germline, otherwise false
     */
    protected boolean isGermline() {
        if (this.ID == null) return false;
        if (this.ID.toUpperCase().contains("germline".toUpperCase())) return true;
        return false;
    }

    final static double EPSILON = 0.0000001;


    /**
     * does this node have the germline as a child?
     * @return true if germline is associated with this node, otherwise false
     */
    public boolean hasGermline() {
        return (germline != null);
    }


    /**
     * Private helper function that sets the height of this node, either as normal or as data augmentation, but keeps
     * the germline and root height together.
     * @param height new node height
     * @param isDA is this data augmentation?
     */
    private void setHeight(final double height, boolean isDA) {
        startEditing();
        if (this.m_tree == null) {
            // we haven't finish initializing the tree, so we don't care about setting the root and germline together yet
            setSuperHeight(height, isDA);
        }
        else if (this.isRoot() && hasGermline()) {
            adjustRootAndGermline(height);
            if (isDA) {
                isDirty |= Tree.IS_DIRTY;
            } else {
                setNormalDirt();
            }
        } else {
            if (!this.isGermline()) {
                setSuperHeight(height, isDA);
            }
        }
    }

    /**
     * Helper function that calls the parent class/super set height function, appropriate for normal nodes (i.e. not
     * part of a germline-root pair).
     * @param height new node height
     * @param isDA is this data augmentation?
     */
    protected void setSuperHeight(final double height, boolean isDA) {
        if (isDA) {super.setHeightDA(height);}
        else {super.setHeight(height);}
    }

    /**
     * Helper function to set dirt normally (non-data-augmentation), i.e. this node and all its internal nodes in its
     * subtree.
     */
    private void setNormalDirt() {
        isDirty |= Tree.IS_DIRTY;
        if (!isLeaf()) {
            ((GRTNode) getLeft()).isDirty |= Tree.IS_DIRTY;
            if (getRight() != null) {
                ((GRTNode) getRight()).isDirty |= Tree.IS_DIRTY;
            }
        }
    }

    /**
     * assign values to a tree in array representation *
     */
    @Override
    public void assignTo(final Node[] nodes) {
        super.assignTo(nodes);
        final GRTNode node = (GRTNode) nodes[getNr()];
        if (germline != null) {
            node.addGermline((GRTNode) nodes[germline.getNr()]);
        }
    }

    /**
     * assign values from a tree in array representation *
     */
    @Override
    public void assignFrom(final Node[] nodes, final Node node) {
        super.assignFrom(nodes, node);
        if (node instanceof GRTNode) {
            if (((GRTNode) node).germline != null) {
                addGermline((GRTNode) nodes[((GRTNode) node).germline.getNr()]);
            }
        }
    }


    /**
     * Sets the height of this node, but if this node is the germline or the root, sets their heights together.
     * @param height the new height of this node
     */
    @Override
    public void setHeight(final double height) {
        setHeight(height, false);
    }

    /**
     * Sets the height of this node in operators for data augmentation likelihood, but if this node is the germline or
     * the root, sets their heights together.
     * It only changes this node to be dirty, not any of child nodes.
     * @param height the new height of this node
     */
    @Override
    public void setHeightDA(final double height) {
        setHeight(height, true);
    }


    /**
     * Get the minimum height this node can be set to.
     * If this node is the root and has the germline associated with it, the minimum height is just the height of
     * its non-germline child.
     * In all other cases, this is the maximum of its children's heights.
     * @return a double representing the minimum height this node can be set to
     */
    public double getMinimumHeight() {
        double minHeight;
        if (m_tree != null && isRoot() && hasGermline()) {
            int germlineNum = germline.getNr();
            if (this.getLeft().getNr() == germlineNum) {
                minHeight = this.getRight().getHeight();
            } else if (this.getRight().getNr() == germlineNum) {
                minHeight = this.getLeft().getHeight();
            } else {
                throw new RuntimeException("GRTNode has germline associated but neither left nor right child matches germline.");
            }
        }
        else {
            minHeight = Math.max(this.getRight().getHeight(), this.getLeft().getHeight());
        }
        return minHeight;
    }


    /**
     * Helper function to set the root and the germline height together.
     * @param newHeight new root height
     */
    private void adjustRootAndGermline(double newHeight) {
        germline.height = newHeight - EPSILON;
        height = newHeight;
    }


    /**
     * Makes a new GRTNode from a regular node
     * @param original the node to recreate as a GRTNode
     * @return a new GRTNode
     */
    public static GRTNode makeNewFromNode(Node original) {
        final GRTNode node = new GRTNode();
        node.height = original.getHeight();
        node.labelNr = original.getNr();

        for (String key : original.getMetaDataNames()) {
            node.setMetaData(key, original.getMetaData(key));
        }
        node.parent = null;
        node.setID(original.getID());

        for (Node child : original.getChildren()) {
            if (!(child instanceof GRTNode)) {
                child = makeNewFromNode(child);
            }
            node.addChild(child);
        }
        // remove children and parent from original so that nothing points to it and garbage collection can clean it up
        original.removeAllChildren(false);
        original.setParent(null,false);
        return node;
    }


    /**
     * get the height of this node
     * @return double representing the height of this node
     */
    @Override
    public double getHeight() {
        // TODO: theoretically, this should always be correct anyway, so check if we can remove this
        if (isGermline() && parent != null && parent.isRoot()) {
            height = parent.getHeight() - EPSILON;
        }
        return height;
    }

    /**
     * get the date of this node
     * @return double representing the date of this node
     */
    @Override
    public double getDate() {
        // TODO: theoretically, this should always be correct anyway, so check if we can remove this
        return m_tree.getDate(getHeight());
    }


    /**
     * Adds a child to this node.
     * @param child the child to add
     */
    @Override
    public void addChild(Node child) {
        if (!(child instanceof GRTNode)) {
            child = makeNewFromNode(child);
        }
        if (((GRTNode) child).isGermline()) {
            addGermline((GRTNode) child);
        }
        super.addChild(child);
    }



    /**
     * @return (deep) copy of node
     */
    @Override
    public Node copy() {
        final GRTNode node = new GRTNode();
        node.height = height;
        node.labelNr = labelNr;
        node.metaDataString = metaDataString;
        node.lengthMetaDataString = lengthMetaDataString;
        node.metaData = new TreeMap<>(metaData);
        node.lengthMetaData = new TreeMap<>(lengthMetaData);
        node.parent = null;
        node.setID(getID());

        for (final Node child : getChildren()) {
            Node childCopy;

            if (!(child instanceof GRTNode)) {
                childCopy = makeNewFromNode(child).copy(); // necessary for it to be deep copy
            } else {
                childCopy = child.copy();
            }
            node.addChild(childCopy); // this should handle setting the germline correctly
        }
        return node;
    } // copy

    /**
     * scale height of this node and all its internal descendants, but if this node is the root and has a germline
     * child, set the germline height with the root height.
     * @param scale scale factor
     * @return degrees of freedom scaled (used for HR calculations)
     */
    @Override
    public int scale(final double scale) {
        startEditing();

        int dof = 0;

        isDirty |= Tree.IS_DIRTY;
        if (!isLeaf() && !isFake()) {
            if (isRoot() && hasGermline()) {
                adjustRootAndGermline(height*scale);
            } else {
                height *= scale;
            }

            if (isRoot() || parent.getHeight() != getHeight())
                dof += 1;
        }
        if (!isLeaf()) {
            dof += getLeft().scale(scale);
            if (getRight() != null) {
                dof += getRight().scale(scale);
            }
            if (height < getLeft().getHeight() || height < getRight().getHeight()) {
                throw new IllegalArgumentException("Scale gives negative branch length");
            }
        }

        return dof;
    }
}
