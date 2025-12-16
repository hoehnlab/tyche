AncestralTypeLikelihood
=======================

.. java:class:: public tyche.evolution.likelihood.AncestralTypeLikelihood
   :inheritance: beast.base.evolution.likelihood.TreeLikelihood


   AncestralTypeLikelihood to assess likelihood of internal and ambiguous node types.

   


   .. java:field:: String STATES_KEY

   .. java:field:: Input tagInput

      input object, a string label for reconstruction characters in tree log

      

   .. java:field:: Input useJava

      input object, prefer java, even if beagle is available, default: true

      

   .. java:field:: Input nodeTypesInput

      input object, the type associated with each node

      

   .. java:field:: IntegerParameter nodeTypes

   .. java:field:: double qMatrix

   .. java:field:: int patternCount

   .. java:field:: int stateCount

   .. java:field:: int tipStates

   .. java:field:: Helper treeTraits

      Method required for implementing TreeTraitProvider

      for logging with beastclassic.evolution.tree.TreeWithTraitLogger

      

   .. java:field:: DataType dataType

   .. java:field:: String tag

   .. java:field:: double jointLogLikelihood

   .. java:field:: double storedJointLogLikelihood


   .. java:method:: public void initAndValidate()

      Initialize the model and validate inputs

      


   .. java:method:: protected boolean requiresRecalculation()

      Whether the likelihood needs to be recalculated.

      true if any node types have been operated on or if anything has been operated on that would make any normal treelikelihood

      model need to be recalculated

      :return: true if anything this model depends on has changed since the last time it was calculated 


   .. java:method:: public double calculateLogP()

      Calculate the likelihood of the ancestral type reconstruction.

      :return: the likelihood in log space 


   .. java:method:: public void traverseTypeTree(Node node, int parentState)

      Helper to calculate the likelihood of the ancestral type reconstruction by recursively traversing the tree.

      Updates this.jointLogLikelihood

      :param node: the current node

      :param parentState: the state (type) of the parent of the current node 


   .. java:method:: public void store()

      Store the current values of fields that should be restored after a rejected proposal.

      


   .. java:method:: public void restore()

      Restore the stored values of fields that were saved before a proposal.

      


   .. java:method:: public void getTransitionMatrix(int nodeNum, double probabilities)

      Helper method to get the transition matrix, wrapper for beagle/likelihoodCore calls

      :param nodeNum: the number of the current node

      :param probabilities: the array to write transition matrix probabilities to 


   .. java:method:: public DataType getDataType()

      Method required for implementing TreeTraitProvider

      for logging with beastclassic.evolution.tree.TreeWithTraitLogger

      


   .. java:method:: public int getStatesForNode(TreeInterface tree, Node node)

      Method required for implementing TreeTraitProvider

      for logging with beastclassic.evolution.tree.TreeWithTraitLogger

      


   .. java:method:: public TreeTrait getTreeTraits()

      Method required for implementing TreeTraitProvider

      for logging with beastclassic.evolution.tree.TreeWithTraitLogger

      


   .. java:method:: public TreeTrait getTreeTrait(String key)

      Method required for implementing TreeTraitProvider

      for logging with beastclassic.evolution.tree.TreeWithTraitLogger

      


   .. java:method:: private static String getFormattedState(int state, DataType dataType)

      Method required for implementing TreeTraitProvider

      for logging with beastclassic.evolution.tree.TreeWithTraitLogger

      

