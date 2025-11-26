package tyche.evolution.tree;

import beast.base.core.Description;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TraitSet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Description("A Tree that can store extra metadata for its tips.")
public class MetadataTree extends Tree {

    protected Map<String, Map<String, Object>> tipMetaData;

    @Override
    public void initAndValidate() {
        this.tipMetaData = new HashMap<>();
        super.initAndValidate();
    }

    @Override
    protected void processTraits(List<TraitSet> traitList) {
        for (TraitSet traitSet : traitList) {
            if (traitSet instanceof StringTraitSet) {
                HashMap<String, Object> currentTrait = new HashMap<>();
                for (Node node : getExternalNodes()) {
                    String id = node.getID();
                    if (id != null) {
                        node.setMetaData(traitSet.getTraitName(), traitSet.getStringValue(id));
                        currentTrait.put(id, traitSet.getStringValue(id));
                    }
                }
                tipMetaData.put(traitSet.getTraitName(), currentTrait);
            }
        }
        traitList.removeIf(traitSet -> (traitSet instanceof StringTraitSet));
        super.processTraits(traitList);
        traitsProcessed = true;
    }

    public Object getTipMetaData(String pattern, String tipID) {
        if (!tipMetaData.containsKey(pattern)) {
            return null;
        }
        return tipMetaData.get(pattern).get(tipID);
    }

    public Set<String> getTipMetaDataNames() {
        return tipMetaData.keySet();
    }
}
