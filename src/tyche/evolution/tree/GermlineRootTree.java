package tyche.evolution.tree;


import beast.base.core.Description;
import beast.base.evolution.tree.Node;
import beast.pkgmgmt.BEASTClassLoader;


@Description("A MetadataTree that treats the Germline and root as one unit, using GRTNodes to set their heights together.")
public class GermlineRootTree extends MetadataTree {

    @Override
    protected GRTNode newNode() {
        try {
            Node temp = (Node) BEASTClassLoader.forName(nodeTypeInput.get()).newInstance();
            return GRTNode.makeNewFromNode(temp);
        } catch (Exception e) {
            throw new RuntimeException("Cannot create node of type "
                    + nodeTypeInput.get() + ": " + e.getMessage());
        }
    }

    @Override
    public GRTNode getRoot() {
        if (root instanceof GRTNode) {
            return (GRTNode) root;
        } else {
            throw new RuntimeException("Root node in GermlineRootTree is not GRTNode.");
        }
    }

}
