/*
 *  Copyright (C) 2025 Hoehn Lab, Dartmouth College
 *
 * This file is part of TyCHE.
 *
 * TyCHE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * TyCHE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with TyCHE.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package tyche.inference.distribution;

import beast.base.core.*;
import beast.base.inference.Distribution;
import beast.base.inference.State;
import beast.base.inference.distribution.ParametricDistribution;
import beast.base.inference.parameter.RealParameter;
import org.apache.commons.math.MathException;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author Jessie Fielding
 * This class is part of the TyCHE package - https://github.com/hoehnlab/tyche
 */

/**
 * A distribution to set a prior on the type of given argument, recommend root trait using tyche.inference.distribution.RootType
 */
@Description("A distribution to set a prior on the type of given argument, recommend root trait using tyche.inference.distribution.RootType")
@Citation(value="Fielding, J. J., Wu, S., Melton, H. J., Fisk, N., du Plessis, L., & Hoehn, K. B. (2025).\n" +
        "TyCHE enables time-resolved lineage tracing of heterogeneously-evolving populations.\n" +
        "bioRxiv https://doi.org/10.1101/2025.10.21.683591 (2025) doi:10.1101/2025.10.21.683591.",
        year = 2025, firstAuthorSurname = "Fielding", DOI="10.1101/2025.10.21.683591")
public class RootTypePrior extends Distribution {

    public Input<Function> argInput = new Input<Function>("arg", "argument of distribution, recommend root trait");

    public Input<RealParameter> typeProbabilitiesInput = new Input<>("typeProbabilities", "probabilities representing the probability of the type represented by the index, i.e. the 0-indexed value of this parameter should be the probability that root = type 0", Input.Validate.REQUIRED);

    protected Function arg;
    protected RealParameter typeProbabilities;

    @Override
    public void initAndValidate() {
        arg = argInput.get();
        typeProbabilities = typeProbabilitiesInput.get();
        // TODO: check type probabilities sum to 1 (or close enough)

//            throw new IllegalArgumentException("Number of prior distributions must match parameter dimension");

    }

    @Override
    public double calculateLogP() {
        logP = 0.0;
        double prob = 1.0;
        int [] x = new int[arg.getDimension()];
        for (int i = 0; i < x.length; i++) {
            x[i] = (int) arg.getArrayValue(i);
            prob *= typeProbabilities.getArrayValue(x[i]);
        }
        logP = Math.log(prob);
        return logP;
    }


    @Override
    public List<String> getArguments() {
        List<String> arguments = new ArrayList<>();

        if (arg != null) {
            arguments.add(((BEASTInterface) arg).getID());
        }

        return arguments;
    }

    @Override
    public List<String> getConditions() {
        List<String> conditions = new ArrayList<>();

        if (typeProbabilities != null) {
            conditions.add(((BEASTInterface) typeProbabilities).getID());
        }

        return conditions;
    }

    @Override
    public void sample(State state, Random random) {
        if (sampledFlag)
            return;

        sampledFlag = true;

        // Cause conditional parameters to be sampled
        sampleConditions(state, random);
        Double[] newx = new Double[arg.getDimension()];

        // wait if we can't set an arg, does it even matter?

    }
}
