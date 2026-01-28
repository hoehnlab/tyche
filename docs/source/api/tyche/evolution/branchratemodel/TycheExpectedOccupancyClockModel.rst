TycheExpectedOccupancyClockModel
================================

.. java:class:: public tyche.evolution.branchratemodel.TycheExpectedOccupancyClockModel
   :inheritance: tyche.evolution.branchratemodel.AbstractTycheTypeLinkedClockModel


   Defines a type-linked rate for each branch in the beast.tree, where the branch rate is calculated from the expected occupancy in each type.

   



   .. java:method:: public boolean isExpectedOccupancy()

      Returns true as this is an expected occupancy model.

      :return: true 


   .. java:method:: public double getBranchRate(Node node)

      Calculates a type-linked rate for this branch, where the branch rate is calculated from the expected occupancy in each type.

      :param node: the current node (child node of the branch)

      :return: the type-linked rate for this branch 

