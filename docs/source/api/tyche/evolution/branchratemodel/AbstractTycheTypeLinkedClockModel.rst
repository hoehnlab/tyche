AbstractTycheTypeLinkedClockModel
=================================

.. java:class:: public abstract tyche.evolution.branchratemodel.AbstractTycheTypeLinkedClockModel
   :inheritance: beast.base.evolution.branchratemodel.BranchRateModel.Base


   Abstract branch rate model for type-linked mutation rates

   


   .. java:field:: Input typeLinkedRatesInput

      input object for the mutation rate for each type

      

   .. java:field:: Input nodeTypesInput

      input object for the type for each node

      

   .. java:field:: Input typeSwitchClockRateInput

      input object for the clock rate for the Ancestral Reconstruction Tree Likelihood

      

   .. java:field:: Input svsInput

      input object for the substitution model describing type substitutions

      

   .. java:field:: Input branchRatesInput

      input object for a real parameter to log branch rates

      

   .. java:field:: Input occupanciesInput

      input object for a real parameter to log expected occupancy

      

   .. java:field:: Function typeSwitchClockRate

      the clock rate for the Ancestral Reconstruction Tree Likelihood

      

   .. java:field:: TycheSVSGeneralSubstitutionModel svs

      the substitution model describing type substitutions

      

   .. java:field:: double qMatrix

      the Q matrix describing type transitions

      

   .. java:field:: RealParameter typeLinkedRates

      the mutation rate for each type

      

   .. java:field:: RealParameter branchRates

      a real parameter to log branch rates

      

   .. java:field:: RealParameter occupancies

      a real parameter to log expected occupancy

      

   .. java:field:: IntegerParameter nodeTypes

      the type for each node

      

   .. java:field:: Function muParameter


   .. java:method:: public void initAndValidate()

      Initialize and validate inputs that are required for all TyCHE branch models

      


   .. java:method:: public double getTypeLinkedRate(int type)

      Get the rate that corresponds to the type's integer position in the rate list

      :param type: an integer representing the type whose clock rate should be returned

      :return: the clock rate at the position in the rate list corresponding to the integer representing the type 


   .. java:method:: public double getOccupancy(int parentType, int currentType, Double time, int nodeNum)

      Get the occupancies in each of two states for a branch

      :param parentType: an integer representing the type of the parent of this branch

      :param currentType: an integer representing the type of the child of this branch (current node)

      :param time: a Double representing the timespan of the branch

      :param nodeNum: an integer representing the node number of the child of this branch (current node)

      :return: a double array listing the expected occupancy for each type 


   .. java:method:: public abstract double getBranchRate(Node node)

      Get the rate for this branch

      :param node: the current node (child of this branch)

      :return: the rate to be used for this branch 


   .. java:method:: public double getRateForBranch(Node node)

      Get the rate for this branch by calling helper getBranchRate method, handling extra logging

      :param node: the current node (child of this branch)

      :return: the rate to be used for this branch 


   .. java:method:: public boolean isExpectedOccupancy()

      Return whether this model is an expected occupancy model or not

      :return: true if this model is an expected occupancy model, otherwise false 


   .. java:method:: public boolean requiresRecalculation()

      Return whether this model requires recalculation

      :return: true, so that this model is always recalculated 

