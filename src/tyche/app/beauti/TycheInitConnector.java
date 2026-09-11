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

import java.util.List;
import beast.base.core.BEASTInterface;
import beast.base.inference.MCMC;
import beast.base.inference.StateNodeInitialiser;
import beastfx.app.inputeditor.BeautiDoc;
import tyche.evolution.likelihood.AncestralTypeLikelihood;

/**
 * @author Jessie Fielding
 * This file is part of the TyCHE package - https://github.com/hoehnlab/tyche
 */

/**
 * beastfx.app.beauti.StateNodeInitialiserListInputEditor.customConnector
 * (wired in BeastFX's Standard.xml) strips any StateNodeInitialiser from
 * mcmc's "init" list whose getInitialisedStateNodes() doesn't include a
 * Tree -- it assumes every initialiser exists to seed a Tree. It runs on
 * every sync, so AncestralTypeLikelihood gets scrubbed the moment anything
 * (including save()) resyncs the model. This puts it back, running
 * immediately after that scrub (mainid='mcmc' -> fires on every context,
 * same as the connector above).
 */
public class TycheInitConnector {
    public static boolean reconnectInits(BeautiDoc doc) {
        MCMC mcmc = (MCMC) doc.mcmc.get();
        List<StateNodeInitialiser> inits = mcmc.initialisersInput.get();

        for (BEASTInterface o : doc.pluginmap.values()) {
            if (o instanceof AncestralTypeLikelihood) {
                AncestralTypeLikelihood atl = (AncestralTypeLikelihood) o;
                // only resurrect it if it's still actually part of the live
                // model -- don't reconnect one left behind by a removed trait
                if (doc.posteriorPredecessors != null
                        && doc.posteriorPredecessors.contains(atl)
                        && !inits.contains(atl)) {
                    inits.add(atl);
                }
            }
        }
        return true;
    }
}