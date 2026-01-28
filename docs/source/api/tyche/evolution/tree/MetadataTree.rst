MetadataTree
============

.. java:class:: public tyche.evolution.tree.MetadataTree
   :inheritance: beast.base.evolution.tree.Tree


   A Tree that can store extra metadata for its tips.

   


   .. java:field:: Map tipMetaData

      HashMap of metadata hash maps

      i.e. tipMetaData.get("traitName") will return a hashmap that maps each tip to its traitName value.

      


   .. java:method:: protected void processTraits(List traitList)

      Process trait sets.

      :param traitList: List of trait sets. 


   .. java:method:: public Object getTipMetaData(String pattern, String tipID)

      Get the metadata value associated with a tip by its trait name

      :param pattern: a String representing the name of the trait

      :param tipID: a String representing the taxon/ID of the tip to get metadata for

      :return: Object representing the metadata value of trait name 'pattern' associated with tip 'tipID' 


   .. java:method:: public Set getTipMetaDataNames()

      Get the names of the metadata traits associated with this tree.

      :return: Set of Strings containing all the metadata/trait names associated with this tree. 

