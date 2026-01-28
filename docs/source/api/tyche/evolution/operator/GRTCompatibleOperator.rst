GRTCompatibleOperator
=====================

.. java:class:: public tyche.evolution.operator.GRTCompatibleOperator
   Interface to implement when a class agrees to handle rootOnly scale proposals in such a way that the minimum possible

   height of the root ignores the height of the germline. Compatible with GermlineRootTree tree classes.

   



   .. java:method:: abstract double doGRTProposal()

      handle proposal appropriately if the provided Tree is a GermlineRootTree

      

