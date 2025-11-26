package tyche.evolution.tree;

import beast.base.evolution.tree.Node;

import java.util.Objects;
import java.util.TreeMap;

public class GRTNode extends Node {
    protected Node germline;
    protected boolean hasGermline = false;
    private boolean isGermline = false;

    final static double EPSILON = 0.0000001;

    protected void setGermline(GRTNode germline) {
        this.hasGermline = true;
        this.germline = germline;
    }

    protected void unsetAsGermline() {
        this.isGermline = false;
        if (this.getParent() != null && this.getParent() instanceof GRTNode) {
            ((GRTNode) this.getParent()).unsetGermline();
        }
    }
    protected void unsetGermline() {
        this.hasGermline = false;
        this.germline = null;
    }

    /**
     * Sets the height of this node, but if this node is the germline or the root, sets their heights together
     *
     * @param height the new height of this node
     */
    @Override
    public void setHeight(final double height) {
        startEditing();
        if (this.isRoot() && hasGermline) {
            adjustRootAndGermline(height);
        } else if (this.isGermline() && this.getParent().isRoot()) {
            Node parent = this.getParent();
            if (parent instanceof GRTNode) {
                ((GRTNode) parent).setGermline(this);
            } else {
                throw new RuntimeException("Parent is not correct node type");
            }
            ((GRTNode) parent).adjustRootAndGermline(height+(2*EPSILON));
        } else {
            super.setHeight(height);
        }
    }

    public void setSuperHeight(final double height) {
        super.setHeight(height);
    }

    public int getGermlineNumber() {
        if (hasGermline) {
            return germline.getNr();
        }
        return -1;
    }

    public double getMinimumHeight() {
        double minHeight;
        if (this.isRoot() && hasGermline) {
            if (this.getLeft().getNr() == germline.getNr()) {
                minHeight = this.getRight().getHeight();
            } else if (this.getRight().getNr() == germline.getNr()) {
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

    private void adjustRootAndGermline(double newHeight) {
        if (newHeight > this.getHeight()) {
            // move root first then germline
            super.setHeight(newHeight);
            ((GRTNode) germline).setSuperHeight(newHeight - EPSILON);
        }
        else if (newHeight < this.getHeight()) {
            // move germline first
            ((GRTNode) germline).setSuperHeight(newHeight - EPSILON);
            super.setHeight(newHeight);
        }
    }

    public boolean isGermline() {
        return isGermline;
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
        if (original.getID() != null) {
            if (original.getID().toUpperCase().contains("germline".toUpperCase())) {
                node.isGermline = true;
            }
        }

        for (Node child : original.getChildren()) {
            if (!(child instanceof GRTNode)) {
                child = makeNewFromNode(child);
            }
            node.addChild(child);
        }

        original.removeAllChildren(false);
        return node;
    }

    /**
     * Removes a child from this node.
     * @param child the child to remove
     */
    @Override
    public void removeChild(final Node child) {
        startEditing();
        if (hasGermline) {
            if (Objects.equals(child, germline)) {
                ((GRTNode) child).unsetAsGermline();
            }
        }
        super.removeChild(child);
    }


    /**
     * Removes all children from this node.
     * @param inOperator if true then startEditing() is called. For operator uses, called removeAllChildren(true), otherwise
     *                   use set to false.
     */
    @Override
    public void removeAllChildren(final boolean inOperator) {
        if (inOperator) startEditing();
        if (hasGermline) {
            unsetGermline();
        }
        super.removeAllChildren(inOperator);
    }

    @Override
    public void setID(String ID) {
        if (ID != null && ID.toUpperCase().contains("germline".toUpperCase())) {
            isGermline = true;
            if (this.getParent() instanceof GRTNode) {
                GRTNode parent = (GRTNode) this.getParent();
                parent.setGermline(this);
            }
        } else {
            unsetAsGermline();
        }
        super.setID(ID);
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
        if (child.getID() != null) {
            if (child.getID().toUpperCase().contains("germline".toUpperCase())) {
                this.setGermline((GRTNode) child);
            }
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
                childCopy = makeNewFromNode(child);
            } else {
                childCopy = child.copy();
            }
            node.addChild(childCopy);
        }
        return node;
    } // copy


}
