CombinedRootOperator
====================

.. java:class:: public tyche.evolution.operator.CombinedRootOperator
   :inheritance: beast.base.evolution.operator.TreeOperator


   Tree Operator

   


   .. java:field:: Input nodeTypesInput

      input object for the node types parameter to operate on

      

   .. java:field:: IntegerParameter nodeTypes

      the node types parameter to operate on

      

   .. java:field:: int lowerInt

   .. java:field:: int upperInt

   .. java:field:: int germlineNum


   .. java:constructor:: CombinedRootOperator()

      empty constructor to facilitate construction by XML + initAndValidate

      


   .. java:constructor:: CombinedRootOperator(Tree)
      :no-index:


   .. java:method:: public void initAndValidate()

      Initialize and validate the operator.

      


   .. java:method:: private double getRandomScale(double heightRoot, double heightMRCA)


   .. java:method:: private double getNewHeight(double heightRoot, double heightMRCA)


   .. java:method:: private int getRandomType()


   .. java:method:: private double adjustRoot(Node root, double newHeight)


   .. java:method:: public double proposal()

      Change the parameter and return the hastings ratio.

      

      :return: Double.NEGATIVE_INFINITY if proposal should not be accepted 

