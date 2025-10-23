LeafConsciousTypeTreeOperator
=============================

.. java:class:: public tyche.evolution.operator.LeafConsciousTypeTreeOperator
   :inheritance: beast.base.evolution.operator.TreeOperator


   Tree Operator that operates on types associated with internal nodes and ambiguous tips but does not operate on known leaf types.

   


   .. java:field:: Input nodeTypesInput

      input object for the node types parameter to operate on

      

   .. java:field:: Input dataInput

      input object for type alignment data for the tips

      

   .. java:field:: IntegerParameter nodeTypes

      the node types parameter to operate on

      

   .. java:field:: int lowerInt

   .. java:field:: int upperInt

   .. java:field:: boolean isAmbiguous

      an array to keep track of which nodes are ambiguous, especially important for ambiguous tips

      


   .. java:constructor:: LeafConsciousTypeTreeOperator()

      empty constructor to facilitate construction by XML + initAndValidate

      


   .. java:constructor:: LeafConsciousTypeTreeOperator(Tree)
      :no-index:


   .. java:method:: public void initAndValidate()

      Initialize and validate the operator.

      


   .. java:method:: public double proposal()

      Change the parameter and return the hastings ratio.

      

      :return: log of Hastings Ratio, or Double.NEGATIVE_INFINITY if proposal should not be accepted 

