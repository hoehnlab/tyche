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

import beast.base.core.BEASTObject;
import beast.base.core.Citation;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.inference.distribution.ScalarDistribution;
import beast.base.spec.inference.distribution.TensorDistribution;
import beast.base.spec.type.RealVector;
import tyche.TycheCitation;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jessie Fielding
 * This class is part of the TyCHE package - https://github.com/hoehnlab/tyche
 */

/**
 * A prior over a vector parameter that has a different distribution for each element. 
 * 
 * Written like BEAST 3's IID distribution except that there is one distribution 
 * per element. The distributions are used without their own "param": 
 * each one is evaluated at the matching element of this distribution's "param".
 * 
 */
@Description("ElementwisePrior applies a different prior distribution to each element of a real vector parameter.")
@Citation(value = TycheCitation.TEXT, year = TycheCitation.YEAR, firstAuthorSurname = TycheCitation.FIRST_AUTHOR, DOI = TycheCitation.DOI)
public class ElementwisePrior extends TensorDistribution<RealVector<?>, Double> {

    final public Input<List<ScalarDistribution<?, Double>>> distsInput = new Input<>("distribution",
            "one distribution for each element of the parameter, in order, e.g. normal, beta, gamma.", new ArrayList<>());

    private List<ScalarDistribution<?, Double>> dists;

    @Override
    public void initAndValidate() {
        super.initAndValidate();
        dists = distsInput.get();
        if (param == null) {
            throw new IllegalArgumentException("ElementwisePrior requires param, but it is null.");
        }
        if (param.size() != dists.size()) {
            throw new IllegalArgumentException("Number of prior distributions (" + dists.size() + ") must match parameter dimension (" + param.size() + ")");
        }
    }

    @Override
    public double calculateLogP() {
        refresh(); // make sure the distributions' own parameters are up to date if they are sampled during MCMC
        logP = 0.0;
        for (int i = 0; i < param.size(); i++) {
            logP += dists.get(i).logDensity(param.get(i)); // log space so we can add
        }
        return logP;
    }

    @Override
    public void refresh() {
        for (ScalarDistribution<?, Double> dist : dists) {
            dist.refresh();
        }
    }

    @Override
    protected double calcLogP(Double... value) {
        refresh();
        double sum = 0.0;
        for (int i = 0; i < value.length; i++) {
            sum += dists.get(i).logDensity(value[i]);
        }
        return sum;
    }

    @Override
    public List<Double> sample() {
        List<Double> newX = new ArrayList<>(dists.size());
        for (ScalarDistribution<?, Double> dist : dists) {
            Double x = dist.sample().getFirst();
            while (!param.isValid(x)) {
                x = dist.sample().getFirst();
            }
            newX.add(x);
        }
        return newX;
    }

    /** the largest lower bound of any element's distribution: a value valid for every element must respect all of them */
    @Override
    public Double getLowerBoundOfParameter() {
        Double lower = null;
        for (ScalarDistribution<?, Double> dist : dists) {
            Double l = dist.getLowerBoundOfParameter();
            if (l != null && (lower == null || l > lower)) lower = l;
        }
        return lower;
    }

    /** the smallest upper bound of any element's distribution */
    @Override
    public Double getUpperBoundOfParameter() {
        Double upper = null;
        for (ScalarDistribution<?, Double> dist : dists) {
            Double u = dist.getUpperBoundOfParameter();
            if (u != null && (upper == null || u < upper)) upper = u;
        }
        return upper;
    }

    @Override
    public Double getLower() {
        return getLowerBoundOfParameter();
    }

    @Override
    public Double getUpper() {
        return getUpperBoundOfParameter();
    }

    public String getParameterName() {
        if (param != null) {
            return ((BEASTObject) param).getID();
        }
        return String.valueOf(param);
    }
}
