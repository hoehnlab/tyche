ElementwisePrior
================

.. java:class:: public tyche.inference.distribution.ElementwisePrior
   :inheritance: beast.base.inference.Distribution


   ElementwisePrior applies a different prior distribution to each value of a RealParameter with dimension > 1.

   


   .. java:field:: Input parameterInput

   .. java:field:: Input distsInput

   .. java:field:: RealParameter parameter

   .. java:field:: List dists


   .. java:method:: public void initAndValidate()


   .. java:method:: public double calculateLogP()


   .. java:method:: public String getParameterName()

      return name of the parameter this prior is applied to *

      


   .. java:method:: public List getArguments()


   .. java:method:: public List getConditions()


   .. java:method:: public void sample(State state, Random random)

