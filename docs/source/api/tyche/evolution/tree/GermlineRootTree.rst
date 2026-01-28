GermlineRootTree
================

.. java:class:: public tyche.evolution.tree.GermlineRootTree
   :inheritance: tyche.evolution.tree.MetadataTree


   A MetadataTree that treats the Germline and root as one unit, using GRTNodes to set their heights together.

   


   .. java:field:: int germlineNum

   .. java:field:: String nodeType

   .. java:field:: Map GRTCompatibleOperatorsSuggestions


   .. java:method:: public void initAndValidate()

      Initialize and validate the tree.

      


   .. java:method:: protected void findGermline()

      checks all external nodes to see if any contain "germline" in their name

      


   .. java:method:: public int getGermlineNum()

      Get the node number of the germline

      :return: integer representing the node number of the node that is the germline 


   .. java:method:: protected void lookForRandomTree()

      checks all beast "outputs" associated with this tree to see if any are a RandomTree tree initializer, and if so,

      ensures that the tree initializer is using a compatible node type.

      


   .. java:method:: protected boolean isIncompatibleOperator(BEASTInterface o)

      checks if a beast "outputs" associated with this tree is a problematic operator type that's not a GRTCompatibleOperator

      :param o: BEASTInterface object to check

      :return: true if not compatible, false if compatible 


   .. java:method:: protected void lookAtOperators()

      checks all beast "outputs" associated with this tree to identify scale operators that are not GRTCompatibleOperators

      if any exist, check to see if they are tree scalers with rootOnly set to true, because this is the only case where

      core BEAST operators break.

      


   .. java:method:: protected GRTNode newNode()

      Makes a new node of type GRTNode or node type specified in XML.

      :return: a new GRTNode object 

