package tyche.evolution.tree;

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
public class MetadataTree extends Tree {

    /**
     * HashMap of metadata hash maps
     * i.e. tipMetaData.get("traitName") will return a hashmap that maps each tip to its traitName value.
     */
    protected Map<String, Map<String, Object>> tipMetaData = new HashMap<>();

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
     *
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
     *
     * @return Set of Strings containing all the metadata/trait names associated with this tree.
     */
    public Set<String> getTipMetaDataNames() {
        return tipMetaData.keySet();
    }
}
