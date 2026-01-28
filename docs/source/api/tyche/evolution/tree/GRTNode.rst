GRTNode
=======

.. java:class:: public tyche.evolution.tree.GRTNode
   :inheritance: beast.base.evolution.tree.Node


   A GermlineRootTree compatible node type, that keeps the height of the germline and the root together.

   


   .. java:field:: GRTNode germline

   .. java:field:: double EPSILON

      branch length between root and germline since branches shouldn't be zero

      


   .. java:method:: protected void addGermline(GRTNode germ)

      associate a germline node with this node

      :param germ: GRTNode representing the germline to associate with this node 


   .. java:method:: protected boolean isGermline()

      is this node the germline?

      :return: true if germline, otherwise false 


   .. java:method:: public boolean hasGermline()

      does this node have the germline as a child?

      :return: true if germline is associated with this node, otherwise false 


   .. java:method:: private void setHeight(double height, boolean isDA)

      Private helper function that sets the height of this node, either as normal or as data augmentation, but keeps

      the germline and root height together.

      :param height: new node height

      :param isDA: is this data augmentation? 


   .. java:method:: protected void setSuperHeight(double height, boolean isDA)

      Helper function that calls the parent class/super set height function, appropriate for normal nodes (i.e. not

      part of a germline-root pair).

      :param height: new node height

      :param isDA: is this data augmentation? 


   .. java:method:: private void setNormalDirt()

      Helper function to set dirt normally (non-data-augmentation), i.e. this node and all its internal nodes in its

      subtree.

      


   .. java:method:: public void assignTo(Node nodes)

      assign values to a tree in array representation

      


   .. java:method:: public void assignFrom(Node nodes, Node node)

      assign values from a tree in array representation

      


   .. java:method:: public void setHeight(double height)

      Sets the height of this node, but if this node is the germline or the root, sets their heights together.

      :param height: the new height of this node 


   .. java:method:: public void setHeightDA(double height)

      Sets the height of this node in operators for data augmentation likelihood, but if this node is the germline or

      the root, sets their heights together.

      It only changes this node to be dirty, not any of child nodes.

      :param height: the new height of this node 


   .. java:method:: public double getMinimumHeight()

      Get the minimum height this node can be set to.

      If this node is the root and has the germline associated with it, the minimum height is just the height of

      its non-germline child.

      In all other cases, this is the maximum of its children's heights.

      :return: a double representing the minimum height this node can be set to 


   .. java:method:: private void adjustRootAndGermline(double newHeight)

      Helper function to set the root and the germline height together.

      :param newHeight: new root height 


   .. java:method:: static public GRTNode makeNewFromNode(Node original)

      Makes a new GRTNode from a regular node

      :param original: the node to recreate as a GRTNode

      :return: a new GRTNode 


   .. java:method:: public double getHeight()

      get the height of this node

      :return: double representing the height of this node 


   .. java:method:: public double getDate()

      get the date of this node

      :return: double representing the date of this node 


   .. java:method:: public void addChild(Node child)

      Adds a child to this node.

      :param child: the child to add 


   .. java:method:: public void removeChild(Node child)

      Removes a child from this node, if it is the germline, unassociates germline from this node.

      :param child: the child to remove 


   .. java:method:: public Node copy()

      Makes a deepy copy of a node.

      :return: (deep) copy of node 


   .. java:method:: public int scale(double scale)

      scale height of this node and all its internal descendants, but if this node is the root and has a germline

      child, set the germline height with the root height.

      :param scale: scale factor

      :return: degrees of freedom scaled (used for HR calculations) 

