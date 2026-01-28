CombinedRootOperator
====================

.. java:class:: public tyche.evolution.operator.CombinedRootOperator
   :inheritance: tyche.evolution.operator.LeafConsciousTypeTreeOperator


   Tree Operator that operates on the root's height and type together.

   



   .. java:constructor:: CombinedRootOperator()

      empty constructor to facilitate construction by XML + initAndValidate

      


   .. java:constructor:: CombinedRootOperator(Tree)


   .. java:method:: public void initAndValidate()


   .. java:method:: private double getNewHeight(double heightRoot, double heightMRCA)


   .. java:method:: private int getRandomType()


   .. java:method:: private double adjustRoot(Node root, double newHeight, double heightMRCA)


   .. java:method:: public double proposal()

      Change the parameter.

      

      :return: Double.NEGATIVE_INFINITY if proposal should not be accepted 

