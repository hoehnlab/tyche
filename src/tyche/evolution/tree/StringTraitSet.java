package tyche.evolution.tree;

import beast.base.core.Description;
import beast.base.evolution.tree.TraitSet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Description("A TraitSet that can handle string traits.")
public class StringTraitSet extends TraitSet {

    public Map<String, String> stringMap;
    @Override
    public void initAndValidate() {
        super.initAndValidate();
        if (traitsInput.get().matches("^\\s*$")) {
            return;
        }

        // first, determine taxon numbers associated with traits
        // The Taxon number is the index in the alignment, and
        // used as node number in a tree.
        stringMap = new HashMap<>();

        String[] traits = traitsInput.get().split(",");
        List<String> labels = taxaInput.get().asStringList();
        for (String trait : traits) {
            trait = trait.strip();
            String[] strs = trait.split("=");
            if (strs.length != 2) {
                throw new IllegalArgumentException("could not parse trait: " + trait);
            }
            String taxonID = normalize(strs[0].strip());
            int taxonNr = labels.indexOf(taxonID);
            if (taxonNr < 0) {
                throw new IllegalArgumentException("Trait (" + taxonID + ") is not a known taxon. Spelling error perhaps?");
            }
            stringMap.put(taxonID, strs[1].strip());
        }

    }

    @Override
    public String getStringValue(String taxonName) {
        //Log.trace.println("Trait " + taxonName + " => " + values[map.get(taxonName)]);
        return stringMap.get(taxonName);
    }

}
