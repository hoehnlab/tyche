open module tyche {
    requires beast.pkgmgmt;
    requires beast.base;
    requires beast.classic;
    // AncestralTypeLikelihood uses beagle.Beagle (via the inherited TreeLikelihood.beagle field)
    requires beagle;
    // ParametricDistribution (extended by ElementwiseParametricDistribution) implements this module's ContinuousDistribution
    requires org.apache.commons.statistics.distribution;

    exports tyche.evolution.branchratemodel;
    exports tyche.evolution.likelihood;
    exports tyche.evolution.operator;
    exports tyche.evolution.substitutionmodel;
    exports tyche.evolution.tree;
    exports tyche.inference.distribution;

    provides beast.base.core.BEASTInterface with
        tyche.evolution.branchratemodel.TycheInstantSwitchClockModel,
        tyche.evolution.branchratemodel.TycheExpectedOccupancyClockModel,
        tyche.evolution.branchratemodel.TycheMixedSwitchClockModel,
        tyche.evolution.branchratemodel.TycheBranchValueView,
        tyche.evolution.likelihood.AncestralTypeLikelihood,
        tyche.evolution.likelihood.RootFreqsTreeLikelihood,
        tyche.evolution.substitutionmodel.TycheSVSGeneralSubstitutionModel,
        tyche.evolution.operator.LeafConsciousTypeTreeOperator,
        tyche.evolution.operator.CombinedRootOperator,
        tyche.evolution.operator.GRTScaleOperator,
        tyche.evolution.operator.GRTBactrianScaleOperator,
        tyche.evolution.operator.GRTBactrianSubtreeSlide,
        tyche.evolution.operator.SubtreeTypeSwitchOperator,
        tyche.evolution.operator.GRTSubtreeSlide,
        tyche.evolution.operator.GRTExchange,
        tyche.evolution.operator.GRTWilsonBalding,
        tyche.evolution.operator.RelativeParameterOperator,
        tyche.evolution.operator.UniformNodeTypeHeightOperator,
        tyche.evolution.operator.EpochFlexAndTypeOperator,
        tyche.evolution.tree.GRTNode,
        tyche.evolution.tree.GermlineRootTree,
        tyche.evolution.tree.MetadataTree,
        tyche.evolution.tree.GRTIntervals,
        tyche.evolution.tree.GRTRootlessLogger,
        tyche.evolution.tree.GRTBayesianSkyline,
        tyche.inference.distribution.ElementwisePrior,
        tyche.inference.distribution.RootType,
        tyche.inference.distribution.RootTypePrior,
        tyche.inference.distribution.ElementwiseParametricDistribution;
}
