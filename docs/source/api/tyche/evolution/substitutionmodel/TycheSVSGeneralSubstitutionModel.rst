TycheSVSGeneralSubstitutionModel
================================

.. java:class:: public tyche.evolution.substitutionmodel.TycheSVSGeneralSubstitutionModel
   :inheritance: beastclassic.evolution.substitutionmodel.SVSGeneralSubstitutionModel


   Extends SVSGeneralSubstitutionModel so that the rate matrix is stored and restored after rejected proposals.

   


   .. java:field:: double storedRateMatrix

   .. java:field:: BooleanParameter rateIndicator


   .. java:method:: public void initAndValidate()

      Initialize and validate using SVSGeneralSubstitutionModel initAndValidate, and then set up storedRateMatrix.

      


   .. java:method:: public void getTransitionProbabilities(Node node, double startTime, double endTime, double rate, double matrix)

      Get the transition probabilities, using SVSGeneralSubstitutionModel getTransitionProbabilities, and use rate indicator to address machine accuracy errors.

      :param node: the current node

      :param startTime: the start time of the branch

      :param endTime: the end time of the branch

      :param rate: the rate of the branch

      :param matrix: the double array to copy the transition probability matrix into  


   .. java:method:: public void store()

      Store the rate matrix so that it can be restored after rejected proposals.

      


   .. java:method:: public void restore()

      Restore the rate matrix that was stored before the proposal.

      

