SubtreeTypeSwitchOperator
=========================

.. java:class:: public tyche.evolution.operator.SubtreeTypeSwitchOperator
   :inheritance: beast.base.evolution.operator.TreeOperator


   Tree Operator that operates on types associated with internal nodes and ambiguous tips by switching a node and its subtree to the new type.

   


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

      


   .. java:constructor:: SubtreeTypeSwitchOperator()

      empty constructor to facilitate construction by XML + initAndValidate

      


   .. java:constructor:: SubtreeTypeSwitchOperator(Tree)
      :no-index:


   .. java:method:: public void initAndValidate()

      Initialize and validate the operator.

      


   .. java:method:: public double proposal()

      Change the parameter and return the hastings ratio.

      

      :return: log of Hastings Ratio, or Double.NEGATIVE_INFINITY if proposal should not be accepted 


   .. java:method:: private void setSubtree(Node node, int newValue)

      Set all nodeType values in the subtree to a new value.

      :param node: the node that is the root of the subtree we are setting to a new value

      :param newValue: the new value we are setting every member of the subtree equal to 

