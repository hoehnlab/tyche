GRTBactrianScaleOperator
========================

.. java:class:: public tyche.evolution.operator.GRTBactrianScaleOperator
   :inheritance: beast.base.evolution.operator.kernel.BactrianScaleOperator


   BactrianScaleOperator that will handle rootOnly scale appropriately if the provided Tree is a GermlineRootTree

   



   .. java:method:: public double doGRTProposal()

      handle rootOnly scale appropriately if the provided Tree is a GermlineRootTree

      


   .. java:method:: public double proposal()

      Change the parameter.

      

      :return: Double.NEGATIVE_INFINITY if proposal should not be accepted 

