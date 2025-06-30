package beast.base.inference.distribution;

import beast.base.core.BEASTInterface;
import beast.base.core.BEASTObject;
import beast.base.core.Input;
import beast.base.inference.Distribution;
import beast.base.inference.State;
import beast.base.inference.parameter.RealParameter;
import org.apache.commons.math.MathException;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ElementwisePrior extends Distribution {
    public Input<RealParameter> parameterInput = new Input<>("parameter", "parameter", Input.Validate.REQUIRED);
    final public Input<List<ParametricDistribution>> distsInput = new Input<>("distribution", "distributions used to calculate prior, e.g. normal, beta, gamma.", new ArrayList<>());

    private RealParameter parameter;
    private List<ParametricDistribution> dists;

    @Override
    public void initAndValidate() {
        parameter = parameterInput.get();
        dists = distsInput.get();

        if (parameter.getDimension() != dists.size()) {
            throw new IllegalArgumentException("Number of prior distributions must match parameter dimension");
        }
    }

    @Override
    public double calculateLogP() {
        logP = 0.0;

        for (int i = 0; i < parameter.getDimension(); i++) {
            RealParameter x = new RealParameter(String.valueOf(parameter.getValue(i)));
            ParametricDistribution dist = dists.get(i);
            logP += dist.calcLogP(x);
        }

        return logP;
    }

    /**
     * return name of the parameter this prior is applied to *
     */
    public String getParameterName() {
        if (parameter != null) {
            return ((BEASTObject) parameter).getID();
        }
        return parameter + "";
    }

    @Override
    public List<String> getArguments() {
        List<String> arguments = new ArrayList<>();

        if (parameter != null) {
            arguments.add(((BEASTInterface) parameter).getID());
        }

        return arguments;
    }

    @Override
    public List<String> getConditions() {
        List<String> conditions = new ArrayList<>();
        for (ParametricDistribution dist : dists) {
            conditions.add(dist.getID());
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

        Double[] newx = new Double[parameter.getDimension()];
        for (int i = 0; i < parameter.getDimension(); i++) {
            ParametricDistribution dist = dists.get(i);
            try {
                newx[i] = dist.sample(1)[0][0];
                while (parameter.getLower() > newx[i] || parameter.getUpper() < newx[i]) {
                    newx[i] = dist.sample(1)[0][0];
                }
            } catch (MathException e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to sample!");
            }
        }
    }

}