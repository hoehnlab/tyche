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
import beast.base.evolution.tree.Tree;
import beast.base.inference.CalculationNode;
import beast.base.inference.parameter.IntegerParameter;

import java.io.PrintStream;

/**
 * @author Jessie Fielding
 * This class is part of the TyCHE package - https://github.com/hoehnlab/tyche
 */

/**
 * Function to represent the type at the root.
 */
@Description("Function to represent the type at the root.")
@Citation(value="Fielding, J. J., Wu, S., Melton, H. J., Fisk, N., du Plessis, L., & Hoehn, K. B. (2025).\n" +
        "TyCHE enables time-resolved lineage tracing of heterogeneously-evolving populations.\n" +
        "bioRxiv https://doi.org/10.1101/2025.10.21.683591 (2025) doi:10.1101/2025.10.21.683591.",
        year = 2025, firstAuthorSurname = "Fielding", DOI="10.1101/2025.10.21.683591")
public class RootType extends CalculationNode implements Function, Loggable {
    final public Input<Tree> treeInput = new Input<>("tree", "beast.tree on which this operation is performed", Input.Validate.REQUIRED);

    final public Input<IntegerParameter> nodeTypesInput = new Input<>("nodeTypes", "parameter representing the types", Input.Validate.REQUIRED);

    protected Tree tree;
    protected IntegerParameter nodeTypes;

    @Override
    public void initAndValidate() {
        tree = treeInput.get();
        nodeTypes = nodeTypesInput.get();

    }
    @Override
    public int getDimension() {
        return 1;
    }

    @Override
    public double getArrayValue() {
        return getArrayValue(0);
    }

    @Override
    public double getArrayValue(int iDim) {
        int root = tree.getRoot().getNr();
        double value = nodeTypes.getValue(root);
        return value;
    }

    @Override
    public void init(PrintStream out) {
        String id = getID();
        if (id == null) {
            id = "RootType";
        }
        out.append(id + "\t");
    }

    @Override
    public void log(long nSample, PrintStream out) {
        out.append(getArrayValue() + "\t");
    }

    @Override
    public void close(PrintStream out) {
        // nothing to do
    };
}
