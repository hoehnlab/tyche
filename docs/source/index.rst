.. TyCHE documentation master file, created by
   sphinx-quickstart on Tue Oct 14 09:20:34 2025.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.

TyCHE documentation
===================

.. raw:: html

   <div class="admonition-announcement attention">
      <p class="admonition-title">Under construction</p>
      This site is still under construction. Be aware that some information may be missing or incorrect.
      <br><br>
   </div>


Welcome to TyCHE!
====================


.. raw:: html

   <p>
   <img src="_static/tyche_logo_transparent.png" alt="TyCHE logo" 
   style="float:left; width:168px; height:168px; margin-right: 15px;margin-bottom:10px;"><span style="vertical-align:top"><br/>TyCHE is a Bayesian phylogenetics BEAST2 package that infers time trees of populations with distinct evolutionary rates. Mutation rates often vary dramatically by cell type, and TyCHE improves accuracy of trees for these heterogeneously evolving populations by simultaneously reconstructing ancestral cell types and inferring the time tree by linking those cell types to mutation rates for each branch.<br/></span>
   </p>


TyCHE stands for Type-linked Clocks for Heterogenous Evolution, and is named in honor of the Greek goddess of chance in recognition of the stochastic nature of Bayesian analysis.


.. include:: contact.rst

.. include:: authors.rst


Getting Started
----------------
We recommend starting with :ref:`dowser-vignette`. 

A BEAUti template for TyCHE is under development and will be made available soon.

.. toctree::
   :maxdepth: 1
   :caption: Contents:

   About TyCHE <self>
   build-tyche-trees-with-dowser.rst
   api/packages_index.rst

.. include:: citing.rst

.. include:: license.rst

