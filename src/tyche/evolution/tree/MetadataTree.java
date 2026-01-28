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

import beast.base.core.Citation;
import beast.base.core.Description;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TraitSet;

import java.util.*;

/**
 * @author Jessie Fielding
 * This class is part of the TyCHE package - https://github.com/hoehnlab/tyche
 */

/**
 * A Tree that can store extra metadata for its tips.
 */
@Description("A Tree that can store extra metadata for its tips.")
@Citation(value="Fielding, J. J., Wu, S., Melton, H. J., Fisk, N., du Plessis, L., & Hoehn, K. B. (2025).\n" +
        "TyCHE enables time-resolved lineage tracing of heterogeneously-evolving populations.\n" +
        "bioRxiv https://doi.org/10.1101/2025.10.21.683591 (2025) doi:10.1101/2025.10.21.683591.",
        year = 2025, firstAuthorSurname = "Fielding", DOI="10.1101/2025.10.21.683591")
public class MetadataTree extends Tree {

    /**
     * HashMap of metadata hash maps
     * i.e. tipMetaData.get("traitName") will return a hashmap that maps each tip to its traitName value.
     */
    protected Map<String, Map<String, Object>> tipMetaData = new HashMap<>();

    /**
     * Process trait sets.
     * @param traitList List of trait sets.
     */
    @Override
    protected void processTraits(List<TraitSet> traitList) {
        for (TraitSet traitSet : traitList) {
            HashMap<String, Object> currentTrait = new HashMap<>();
            for (Node node : getExternalNodes()) {
                String id = node.getID();
                if (id != null) {
                    currentTrait.put(id, traitSet.getStringValue(id));
                }
            }
            tipMetaData.put(traitSet.getTraitName(), currentTrait);
        }
        super.processTraits(traitList);
        traitsProcessed = true;
    }

    /**
     * Get the metadata value associated with a tip by its trait name
     * @param pattern  a String representing the name of the trait
     * @param tipID    a String representing the taxon/ID of the tip to get metadata for
     * @return Object representing the metadata value of trait name 'pattern' associated with tip 'tipID'
     */
    public Object getTipMetaData(String pattern, String tipID) {
        if (!tipMetaData.containsKey(pattern)) {
            return null;
        }
        return tipMetaData.get(pattern).get(tipID);
    }

    /**
     * Get the names of the metadata traits associated with this tree.
     * @return Set of Strings containing all the metadata/trait names associated with this tree.
     */
    public Set<String> getTipMetaDataNames() {
        return tipMetaData.keySet();
    }
}
