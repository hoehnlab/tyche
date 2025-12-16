TycheMixedSwitchClockModel
==========================

.. java:class:: public tyche.evolution.branchratemodel.TycheMixedSwitchClockModel
   :inheritance: ReferenceType(arguments=None, dimensions=[], name=AbstractTycheTypeLinkedClockModel, sub_type=None)


   Defines a type-linked rate for each branch in the beast.tree, where the branch rate is calculated from the expected occupancy in each type on branches with differently typed nodes, and on branches with same-typed nodes is assumed to be entirely in that state.

   



   .. java:method:: public boolean isExpectedOccupancy()

      Returns true as this is an expected occupancy model.

      :return: true 


   .. java:method:: public double getBranchRate(Node node)

      Calculates a type-linked rate for this branch, where the branch rate is calculated from the expected occupancy in each type if the branch has differently typed parent and child nodes, or if the branch has same-typed parent and child nodes is assumed to be entirely in that state

      :param node: the current node (child node of the branch)

      :return: the type-linked rate for this branch 

