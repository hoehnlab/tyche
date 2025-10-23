package tyche.evolution.branchratemodel;

import beast.base.core.Citation;
import beast.base.core.Description;
import beast.base.evolution.tree.Node;

@Description("Defines a type-linked rate for each branch in the beast.tree, where the branch rate is calculated from the expected occupancy in each type on branches with differently typed nodes, and on branches with same-typed nodes is assumed to be entirely in that state.")
@Citation(value="Fielding, J. J., Wu, S., Melton, H. J., Fisk, N., du Plessis, L., & Hoehn, K. B. (2025).\n" +
        "TyCHE enables time-resolved lineage tracing of heterogeneously-evolving populations.\n" +
        "bioRxiv https://doi.org/10.1101/2025.10.21.683591 (2025) doi:10.1101/2025.10.21.683591.",
        year = 2025, firstAuthorSurname = "Fielding", DOI="10.1101/2025.10.21.683591")
public class TycheMixedSwitchClockModel extends AbstractTycheTypeLinkedClockModel {
    @Override
    public boolean isExpectedOccupancy() {
        return true;
    }

    @Override
    public double getBranchRate(final Node node) {

        if (node.isRoot()) {
            return 1.0;
        }

        int type = (int) nodeTypes.getArrayValue(node.getNr());
        int parentType = (int) nodeTypes.getArrayValue(node.getParent().getNr());
        if (type == parentType) {
            return getTypeLinkedRate(type);
        }
        double typeTime = node.getLength() * typeSwitchClockRate.getArrayValue();
        double[] occupancy = getOccupancy(parentType, type, typeTime, node.getNr());
        return (getTypeLinkedRate(0)*occupancy[0] + getTypeLinkedRate(1)*occupancy[1]);
    }

}
