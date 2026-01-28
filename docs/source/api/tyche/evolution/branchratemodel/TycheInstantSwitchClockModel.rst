TycheInstantSwitchClockModel
============================

.. java:class:: public tyche.evolution.branchratemodel.TycheInstantSwitchClockModel
   :inheritance: tyche.evolution.branchratemodel.AbstractTycheTypeLinkedClockModel


   Defines a type-linked rate for each branch in the beast.tree, where the branch is assumed to be entirely in the child state.

   



   .. java:method:: public double getBranchRate(Node node)

      Calculates a type-linked rate for this branch, where the branch is assumed to be entirely in the child state.

      :param node: the current node (child node of the branch)

      :return: the type-linked rate for this branch 

