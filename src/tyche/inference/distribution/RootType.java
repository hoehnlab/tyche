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

import beast.base.core.Citation;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Loggable;
import beast.base.evolution.tree.Tree;
import beast.base.inference.CalculationNode;
import beast.base.spec.domain.NonNegativeInt;
import beast.base.spec.inference.parameter.IntVectorParam;
import beast.base.spec.type.IntScalar;
import tyche.TycheCitation;

import java.io.PrintStream;
/**
 * @author Jessie Fielding
 * This class is part of the TyCHE package - https://github.com/hoehnlab/tyche
 */

/**
 * The type at the root of a tree, as a spec integer scalar.
 */
@Description("Integer scalar representing the type at the root.")
@Citation(value = TycheCitation.TEXT, year = TycheCitation.YEAR, firstAuthorSurname = TycheCitation.FIRST_AUTHOR, DOI = TycheCitation.DOI)
public class RootType extends CalculationNode implements IntScalar<NonNegativeInt>, Loggable {

    final public Input<Tree> treeInput = new Input<>("tree", "beast.tree on which this operation is performed", Input.Validate.REQUIRED);
    final public Input<IntVectorParam<?>> nodeTypesInput = new Input<>("nodeTypes", "parameter representing the types", Input.Validate.REQUIRED);

    protected Tree tree;
    protected IntVectorParam<?> nodeTypes;

    @Override
    public void initAndValidate() {
        tree = treeInput.get();
        nodeTypes = nodeTypesInput.get();
    }

    @Override
    public int get() {
        return nodeTypes.get(tree.getRoot().getNr());
    }

    @Override
    public NonNegativeInt getDomain() {
        return NonNegativeInt.INSTANCE;
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
        out.append((double) get() + "\t");
    }

    @Override
    public void close(PrintStream out) {
        // nothing to close
    }
}
