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

import beast.base.core.BEASTInterface;
import beast.base.core.Citation;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.inference.Distribution;
import beast.base.inference.State;
import beast.base.spec.type.IntScalar;
import beast.base.spec.type.IntVector;
import beast.base.spec.type.Simplex;
import beast.base.spec.type.Tensor;
import tyche.TycheCitation;

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
@Citation(value = TycheCitation.TEXT, year = TycheCitation.YEAR, firstAuthorSurname = TycheCitation.FIRST_AUTHOR, DOI = TycheCitation.DOI)
public class RootTypePrior extends Distribution {

    public Input<Tensor<?, ?>> argInput = new Input<>("arg", "argument of distribution, recommend root trait (an integer scalar such as tyche.inference.distribution.RootType)");
    public Input<Simplex> typeProbabilitiesInput = new Input<>("typeProbabilities", "probabilities representing the probability of the type represented by the index, i.e. the 0-indexed value of this parameter should be the probability that root = type 0", Input.Validate.REQUIRED);

    protected Tensor<?, ?> arg;
    protected Simplex typeProbabilities;

    @Override
    public void initAndValidate() {
        arg = argInput.get();
        typeProbabilities = typeProbabilitiesInput.get();
        if (arg != null && !(arg instanceof IntScalar) && !(arg instanceof IntVector)) {
            throw new IllegalArgumentException("arg must be an integer scalar or an integer vector, but is " + arg.getClass().getName());
        }
    }

    @Override
    public double calculateLogP() {
        logP = 0.0;
        if (arg instanceof IntScalar<?> scalar) {
            logP = Math.log(typeProbabilities.get(scalar.get()));
        } else if (arg instanceof IntVector<?> vector) {
            double prob = 1.0;
            for (int i = 0; i < vector.size(); i++) {
                prob *= typeProbabilities.get(vector.get(i));
            }
            logP = Math.log(prob);
        }
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
        sampleConditions(state, random);
    }
}
