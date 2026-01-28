MultiNodeTypeSwitchOperator
===========================

.. java:class:: abstract public tyche.evolution.operator.MultiNodeTypeSwitchOperator
   :inheritance: tyche.evolution.operator.LeafConsciousTypeTreeOperator


   Tree Operator that operates on types associated with internal nodes and ambiguous tips by proposing type changes for a node and a section around that node.

   


   .. java:field:: int numberOfTypes

   .. java:field:: TreeTraverseMode mode

   .. java:field:: TypeSwitchMode typeSwitchMode

   .. java:field:: int generationLimit

   .. java:field:: int homogenousValue

   .. java:field:: Map currentOriginTypes

   .. java:field:: Map isHomogenous

   .. java:field:: ProposalMode currentProposalType


   .. java:constructor:: MultiNodeTypeSwitchOperator()

      empty constructor to facilitate construction by XML + initAndValidate

      


   .. java:constructor:: MultiNodeTypeSwitchOperator(Tree)


   .. java:method:: public void initAndValidate()

      Initialize and validate the operator.

      


   .. java:method:: public double proposal()

      Change the parameter.

      

      :return: log of Hastings Ratio, or Double.NEGATIVE_INFINITY if proposal should not be accepted 


   .. java:method:: abstract protected void setTraverseMode()


   .. java:method:: abstract protected void setTypeSwitchMode()


   .. java:method:: abstract protected void setGenerationsLimit()


   .. java:method:: abstract protected int getGenerationsForProposal()


   .. java:method:: protected double setNodes(Node node)


   .. java:method:: protected double getHastingsRatio(int changedNodes)


   .. java:method:: protected double getProposalDistribution(int changedNodes, Boolean isHomogenous)


   .. java:method:: protected int getRelatedNodeTypeProposalValue(int nodeNum)


   .. java:method:: protected void updateIsHomogenous(int currentType, int proposedType)

      Helper function to update the Map recording whether the current and proposed states are homogenous

      :param currentType: the value of this node in the current state

      :param proposedType: the proposed value of this node 


   .. java:method:: protected void setRelatedNodeType(int nodeNum, int newValue)


   .. java:method:: protected int setSubtree(Node node, int generations)

      Set all nodeType values in the subtree to a new value.

      :param node: the node that is the root of the subtree we are setting to a new value

      :param generations: how many generations to set, -1 (or any value less than 0) for "all the way to tips" 


   .. java:method:: protected int setUptree(Node node, int generations)

      Set all nodeType values of direct ancestors of this node.

      :param node: the node that is the root of the subtree we are setting to a new value

      :param generations: how many generations to set, -1 (or any value less than 0) for "all the way to tips" 

