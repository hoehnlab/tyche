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

package tyche.app.beauti;

import java.util.ArrayList;
import java.util.List;

import beastfx.app.beauti.PriorProvider;
import beastfx.app.inputeditor.BeautiDoc;
import tyche.inference.distribution.RootTypePrior;
import tyche.inference.distribution.RootType;
import beast.base.inference.Distribution;
import beast.base.inference.parameter.RealParameter;

/**
 * PriorProvider for RootTypePrior
 * Enables users to add RootTypePrior via the "Add Prior" button in Beauti
 * 
 * @author Jessie Fielding
 */
public class RootTypePriorProvider implements PriorProvider {
    
    @Override
    public List<Distribution> createDistribution(BeautiDoc doc) {
        List<Distribution> priors = new ArrayList<>();
        
        RootTypePrior prior = new RootTypePrior();
        prior.setID("rootTypePrior");
        
        RootType rootType = new RootType();
        rootType.setID("rootType");
        prior.argInput.setValue(rootType, prior);
        
        RealParameter typeProbabilities = new RealParameter("0.5 0.5");
        typeProbabilities.setID("typeProbabilities");
        prior.typeProbabilitiesInput.setValue(typeProbabilities, prior);
        
        try {
            prior.initAndValidate();
        } catch (Exception e) {
            return null;
        }
        
        priors.add(prior);
        return priors;
    }
    
    @Override
    public String getDescription() {
        return "Root Type Prior";
    }
    
    @Override
    public boolean canProvidePrior(BeautiDoc doc) {
        // Check if the document contains a tree that could use this prior
        return doc != null;
    }
}
