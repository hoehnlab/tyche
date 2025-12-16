package tyche.evolution.tree;


import beast.base.core.BEASTInterface;
import beast.base.core.Citation;
import beast.base.core.Description;
import beast.base.core.Log;
import beast.base.evolution.operator.ScaleOperator;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.coalescent.RandomTree;
import beast.base.inference.Operator;
import beast.base.inference.StateNode;
import beast.pkgmgmt.BEASTClassLoader;
import tyche.evolution.operator.GRTBactrianScaleOperator;
import tyche.evolution.operator.GRTCompatibleOperator;
import tyche.evolution.operator.GRTScaleOperator;

import java.util.List;

/**
 * @author Jessie Fielding
 * This class is part of the TyCHE package - https://github.com/hoehnlab/tyche
 */


/**
 * A MetadataTree that treats the Germline and root as one unit, using GRTNodes to set their heights together.
 */
@Description("A MetadataTree that treats the Germline and root as one unit, using GRTNodes to set their heights together.")
@Citation(value="Fielding, J. J., Wu, S., Melton, H. J., Fisk, N., du Plessis, L., & Hoehn, K. B. (2025).\n" +
        "TyCHE enables time-resolved lineage tracing of heterogeneously-evolving populations.\n" +
        "bioRxiv https://doi.org/10.1101/2025.10.21.683591 (2025) doi:10.1101/2025.10.21.683591.",
        year = 2025, firstAuthorSurname = "Fielding", DOI="10.1101/2025.10.21.683591")
public class GermlineRootTree extends MetadataTree {

    protected int germlineNum = -1;

    protected String nodeType;


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
            if (o instanceof ScaleOperator && !(o instanceof GRTCompatibleOperator)) {
                // not explicitly compatible scale operator, check if it's a case that breaks (i.e. rootOnly tree scaler)
                ScaleOperator so = (ScaleOperator) o;
                if (so.rootOnlyInput.get() && so.treeInput.get() != null) {
                    // not explicitly compatible and it is a case that breaks, let's double check that it actually applies
                    // to this tree
                    List<StateNode> stateNodes = so.listStateNodes();
                    if (stateNodes.contains(this)) {
                        // now we're sure it applies to this tree, and it's a case that will break things, so throw an error
                        throw new IllegalArgumentException(this.getID() + ": " + this.getClass() + " has found a germline, and is therefore incompatible with rootOnly scale operator " + o.getID() + " of class " + o.getClass() + ".\nUse one of these compatible scale operators instead: " + GRTScaleOperator.class.getName() + ", " + GRTBactrianScaleOperator.class.getName() + "\nor a class that implements " + GRTCompatibleOperator.class.getName());
                    }
                }
            }
        }

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
