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

package tyche.evolution.branchratemodel;

import beast.base.core.BEASTObject;
import beast.base.core.Citation;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.spec.domain.NonNegativeReal;
import beast.base.spec.type.RealVector;
import tyche.TycheCitation;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jessie Fielding
 * This class is part of the TyCHE package - https://github.com/hoehnlab/tyche
 */

/**
 * A read-only, per-node view of a value that a type-linked clock model works out for every branch. 
 * 
 * Replaces the output-only "holder" parameters that earlier versions nested inside the clock model. 
 * 
 * The values are calculated on demand from the current tree and node types, 
 * so they are always consistent with the state being logged.
 * 
 * Use it as "metadata" of a tree logger. The id of this element is the name the value gets in the logged tree.
 */
@Description("Per-node view of the expected occupancy (in type 0) or the branch rate of a type-linked clock model, for logging with a tree logger")
@Citation(value = TycheCitation.TEXT, year = TycheCitation.YEAR, firstAuthorSurname = TycheCitation.FIRST_AUTHOR, DOI = TycheCitation.DOI)
public class TycheBranchValueView extends BEASTObject implements RealVector<NonNegativeReal> {

    /** the value to show for each node */
    public enum Value {
        /** proportion of the branch above the node spent in type 0 (0.0 for the root) */
        expectedOccupancy,
        /** the rate the clock model assigns to the branch above the node (1.0 for the root) */
        branchRate
    }

    public final Input<AbstractTycheTypeLinkedClockModel> clockModelInput = new Input<>("clockModel", "the type-linked clock model", Input.Validate.REQUIRED);
    public final Input<Tree> treeInput = new Input<>("tree", "the tree the clock model is used with", Input.Validate.REQUIRED);
    public final Input<Value> valueInput = new Input<>("value", "which value to show for each node: expectedOccupancy or branchRate", Value.expectedOccupancy, Value.values());

    protected AbstractTycheTypeLinkedClockModel clockModel;
    protected Tree tree;
    protected Value value;

    @Override
    public void initAndValidate() {
        clockModel = clockModelInput.get();
        tree = treeInput.get();
        value = valueInput.get();
    }

    @Override
    public double get(final int i) {
        final Node node = tree.getNode(i);
        return value == Value.branchRate ? clockModel.getBranchRate(node) : clockModel.getNodeOccupancy(node);
    }

    @Override
    public int size() {
        return tree.getNodeCount();
    }

    @Override
    public List<Double> getElements() {
        final List<Double> elements = new ArrayList<>(size());
        for (int i = 0; i < size(); i++) {
            elements.add(get(i));
        }
        return elements;
    }

    @Override
    public NonNegativeReal getDomain() {
        return NonNegativeReal.INSTANCE;
    }
}
