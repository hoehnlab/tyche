package tyche.evolution.operator;


/**
 * @author Jessie Fielding
 * This class is part of the TyCHE package - https://github.com/hoehnlab/tyche
 */

/**
 * Interface to implement when a class agrees to handle rootOnly scale proposals in such a way that the minimum possible
 * height of the root ignores the height of the germline. Compatible with GermlineRootTree tree classes.
 */
public interface GRTCompatibleOperator {
    abstract double doRootOnlyProposal();
}
