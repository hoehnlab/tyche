GRTSubtreeSlide
===============

.. java:class:: public tyche.evolution.operator.GRTSubtreeSlide
   :inheritance: beast.base.evolution.operator.SubtreeSlide


   SubtreeSlide Operator that will appropriately handle if the provided Tree is a GermlineRootTree

   



   .. java:method:: protected boolean isGermline(Node node)


   .. java:method:: public double doGRTProposal()

      Do a probabilistic subtree slide move.

      

      :return: log of Hastings Ratio, or Double.NEGATIVE_INFINITY if proposal should not be accepted * 


   .. java:method:: public double proposal()

      Do a probabilistic subtree slide move.

      

      :return: log of Hastings Ratio, or Double.NEGATIVE_INFINITY if proposal should not be accepted * 


   .. java:method:: private double getDelta()


   .. java:method:: private int intersectingEdges(Node node, double height, List directChildren)

