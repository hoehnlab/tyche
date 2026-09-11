package tyche.inference.distribution;

import beast.base.core.Citation;
import beast.base.core.Description;
import beast.base.core.Function;
import beast.base.core.Input;
import beast.base.inference.distribution.ParametricDistribution;
import beast.base.inference.parameter.RealParameter;
import beast.base.util.Randomizer;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.Distribution;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jessie Fielding
 * This class is part of the TyCHE package - https://github.com/hoehnlab/tyche
 */

/**
 * ParametricDistribution-shaped counterpart to ElementwisePrior, following the
 * same pattern beast2's Dirichlet uses: a joint distribution over a whole
 * vector, wrapped inside a standard Prior so it appears in BEAUti's regular
 * per-parameter "choose a distribution" dropdown alongside Uniform/Normal/etc.
 * Scalar density/quantile methods are intentionally unimplemented, matching
 * Dirichlet -- this object is never meaningful for a single scalar value.
 */
@Description("Applies a different prior distribution to each element of a vector parameter, " +
        "for use as the distribution inside a standard Prior.")
@Citation(value="Fielding, J. J., Wu, S., Melton, H. J., Fisk, N., du Plessis, L., & Hoehn, K. B. (2025).\n" +
        "TyCHE enables time-resolved lineage tracing of heterogeneously-evolving populations.\n" +
        "bioRxiv https://doi.org/10.1101/2025.10.21.683591 (2025) doi:10.1101/2025.10.21.683591.",
        year = 2025, firstAuthorSurname = "Fielding", DOI="10.1101/2025.10.21.683591")
public class ElementwiseParametricDistribution extends ParametricDistribution {

    final public Input<List<ParametricDistribution>> distsInput = new Input<>(
            "distribution", "distributions used to calculate prior, e.g. normal, beta, gamma.",
            new ArrayList<>());

    @Override
    public void initAndValidate() {
        // dimension is only knowable once wrapped in a Prior with a concrete
        // m_x parameter, so there's nothing to validate against here yet --
        // ElementwiseUtil.calcElementwiseLogP() checks dimensions at call time.
    }

    @Override
    public Distribution getDistribution() {
        return null;   // same as Dirichlet: no single-value shape exists
    }

    @Override
    public double calcLogP(final Function pX) {
        List<ParametricDistribution> dists = distsInput.get();
        if (pX.getDimension() != dists.size()) {
            throw new IllegalArgumentException("Number of prior distributions must match parameter dimension");
        }
        double logP = 0.0;
        for (int i = 0; i < pX.getDimension(); i++) {
            RealParameter x = new RealParameter(String.valueOf(pX.getArrayValue(i)));
            logP += dists.get(i).calcLogP(x);
        }
        return logP;
    }

    @Override
    public Double[][] sample(int size) {
        List<ParametricDistribution> dists = distsInput.get();
        Double[][] samples = new Double[size][];
        for (int s = 0; s < size; s++) {
            Double[] x = new Double[dists.size()];
            for (int i = 0; i < dists.size(); i++) {
                try {
                    x[i] = dists.get(i).sample(1)[0][0];
                } catch (MathException e) {
                    throw new RuntimeException("Failed to sample element " + i, e);
                }
            }
            samples[s] = x;
        }
        return samples;
    }
}