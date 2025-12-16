package tyche.evolution.operator;


/**
 * @author Jessie Fielding
 * This class is part of the TyCHE package - https://github.com/hoehnlab/tyche
 */

import beast.base.core.Citation;

/**
 * Interface to implement when a class agrees to handle rootOnly scale proposals in such a way that the minimum possible
 * height of the root ignores the height of the germline. Compatible with GermlineRootTree tree classes.
 */
@Citation(value="Fielding, J. J., Wu, S., Melton, H. J., Fisk, N., du Plessis, L., & Hoehn, K. B. (2025).\n" +
        "TyCHE enables time-resolved lineage tracing of heterogeneously-evolving populations.\n" +
        "bioRxiv https://doi.org/10.1101/2025.10.21.683591 (2025) doi:10.1101/2025.10.21.683591.",
        year = 2025, firstAuthorSurname = "Fielding", DOI="10.1101/2025.10.21.683591")
public interface GRTCompatibleOperator {
    abstract double doRootOnlyProposal();
}
