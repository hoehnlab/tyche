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

package tyche.evolution;

import beast.base.core.Citation;
import beast.base.core.Description;
import beast.base.spec.inference.parameter.IntVectorParam;
import tyche.TycheCitation;


/**
 * @author Jessie Fielding
 * This class is part of the TyCHE package - https://github.com/hoehnlab/tyche
 */

/**
 * Works out the range of node types a node types parameter allows.
 */
@Description("Works out the range of node types a node types parameter allows.")
@Citation(value = TycheCitation.TEXT, year = TycheCitation.YEAR, firstAuthorSurname = TycheCitation.FIRST_AUTHOR, DOI = TycheCitation.DOI)
public final class NodeTypeBounds {
    /**
     * BEAST 3 builds differ in whether an explicit upper bound written 
     * on an integer vector parameter takes effect (and the
     * attribute is deprecated), so the number of types is not read from that 
     * attribute. It is taken from the parameter's own bound when it has one, 
     * and otherwise has to be given explicitly (the operators' "numberOfTypes"
     * input; the clock models use the dimension of typeLinkedRates).
    */

    private static final int UNBOUNDED = Integer.MAX_VALUE - 1;

    private NodeTypeBounds() {
    }

    /** the smallest allowed type */
    public static int lower(final IntVectorParam<?> nodeTypes) {
        return Math.max(nodeTypes.getLower(), nodeTypes.getDomain().getLower());
    }

    /** whether the parameter itself has an upper bound */
    public static boolean hasUpper(final IntVectorParam<?> nodeTypes) {
        return Math.min(nodeTypes.getUpper(), nodeTypes.getDomain().getUpper()) < UNBOUNDED;
    }

    /**
     * the largest allowed type
     * @param nodeTypes     the node types parameter
     * @param numberOfTypes the number of types given explicitly, or null if none was given
     * @throws IllegalArgumentException if the number of types is unknown, or the explicit number contradicts the parameter's bound
     */
    public static int upper(final IntVectorParam<?> nodeTypes, final Integer numberOfTypes) {
        if (numberOfTypes != null) {
            if (numberOfTypes < 1) {
                throw new IllegalArgumentException("numberOfTypes must be at least 1, but is " + numberOfTypes);
            }
            final int upper = lower(nodeTypes) + numberOfTypes - 1;
            if (hasUpper(nodeTypes) && Math.min(nodeTypes.getUpper(), nodeTypes.getDomain().getUpper()) != upper) {
                throw new IllegalArgumentException("numberOfTypes (" + numberOfTypes + ") does not match the upper bound of nodeTypes (" + nodeTypes.getID() + ")");
            }
            return upper;
        }
        if (!hasUpper(nodeTypes)) {
            throw new IllegalArgumentException("The number of types is unknown: nodeTypes (" + nodeTypes.getID() + ") has no upper bound. "
                    + "Set numberOfTypes on this element, e.g. numberOfTypes=\"2\" for two types.");
        }
        return Math.min(nodeTypes.getUpper(), nodeTypes.getDomain().getUpper());
    }
}
