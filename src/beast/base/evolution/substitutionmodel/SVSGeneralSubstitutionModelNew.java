package beast.base.evolution.substitutionmodel;

import beast.base.core.Description;
import beastclassic.evolution.substitutionmodel.SVSGeneralSubstitutionModel;


/**
 * @author Jessie Fielding
 */
@Description("Extends SVSGeneralSubstitutionModel so that the rate matrix is stored and restored after rejected proposals.")
public class SVSGeneralSubstitutionModelNew extends SVSGeneralSubstitutionModel {
    private double[][] storedRateMatrix;

    @Override
    public void initAndValidate() {
        super.initAndValidate();
        storedRateMatrix = new double[nrOfStates][nrOfStates];
    }

    @Override
    public void store() {
        for (int i = 0; i < nrOfStates; i++) {
            System.arraycopy(rateMatrix[i], 0, storedRateMatrix[i], 0, nrOfStates);
        }
        super.store();
    }
    @Override
    public void restore() {
        for (int i = 0; i < nrOfStates; i++) {
            System.arraycopy(storedRateMatrix[i], 0, rateMatrix[i], 0, nrOfStates);
        }
        super.restore();
    }
}
