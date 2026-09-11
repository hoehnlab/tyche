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

import beast.base.core.*;
import beast.base.inference.distribution.ParametricDistribution;
import beast.base.inference.distribution.Prior;
import beast.base.inference.distribution.Uniform;
import beastfx.app.inputeditor.*;
import javafx.geometry.Insets;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import tyche.inference.distribution.ElementwiseParametricDistribution;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Jessie Fielding
 * This file is part of the TyCHE package - https://github.com/hoehnlab/tyche
 */

/**
 * BEAUti input editor for ElementwiseParametricDistribution.
 * Shows one collapsible, editable panel per element of the wrapped
 * parameter, each backed by the standard ParametricDistribution editor
 * (type, chart, and sub-inputs). Keeps the element count synced to the
 * dimension of the parameter that owns this distribution, growing with
 * Uniform placeholders or shrinking from the end as needed.
 */
@Description("BEAUti input editor for ElementwiseParametricDistribution.")
@Citation(value="Fielding, J. J., Wu, S., Melton, H. J., Fisk, N., du Plessis, L., & Hoehn, K. B. (2025).\n" +
        "TyCHE enables time-resolved lineage tracing of heterogeneously-evolving populations.\n" +
        "bioRxiv https://doi.org/10.1101/2025.10.21.683591 (2025) doi:10.1101/2025.10.21.683591.",
        year = 2025, firstAuthorSurname = "Fielding", DOI="10.1101/2025.10.21.683591")
public class ElementwisePriorInputEditor extends BEASTObjectInputEditor {

    public ElementwisePriorInputEditor() { super(); }
    public ElementwisePriorInputEditor(BeautiDoc doc) { super(doc); }

    @Override
    public Class<?> type() {
        return ElementwiseParametricDistribution.class;
    }

    private VBox distsContainer;

    /**
     * Builds the panel: one collapsible entry per parameter element.
     * @param input the Input whose value (an ElementwiseParametricDistribution) this editor renders
     * @param beastObject the object that owns the input
     * @param itemNr the list index this editor is for, or -1 if the input is not a list item
     * @param isExpandOption whether this editor should render expanded or collapsed
     * @param addButtons whether add/remove buttons should be shown
     */
    @Override
    public void init(Input<?> input, BEASTInterface beastObject, int itemNr,
                     ExpandOption isExpandOption, boolean addButtons) {

        super.init(input, beastObject, itemNr, ExpandOption.TRUE, addButtons);
        m_input = input;
        m_beastObject = beastObject;
        this.itemNr = itemNr;

        ElementwiseParametricDistribution model = (ElementwiseParametricDistribution) input.get();

        distsContainer = new VBox(6);
        distsContainer.setPadding(new Insets(8));
        buildDistsPanel(model);

        getChildren().clear();
        getChildren().add(distsContainer);
    }

    /**
     * Finds the Prior that currently wraps this ElementwiseParametricDistribution
     * (via its distInput), and from that, the parameter it applies to and its
     * dimension. Returns -1 if the model isn't currently wrapped by any Prior
     * yet (e.g. mid-construction, before BEAUti has fully wired the connection).
     * @param model the ElementwiseParametricDistribution to find the owning Prior for
     * @return the Prior whose distribution is the given model, or null if none is found
     */
    private Prior getOwningPrior(ElementwiseParametricDistribution model) {
        for (BEASTInterface o : doc.pluginmap.values()) {
            if (o instanceof Prior && ((Prior) o).distInput.get() == model) {
                return (Prior) o;
            }
        }
        return null;
    }

    /**
     * Parameter (Prior.m_x) that this distribution applies to, or null if not yet wrapped by a Prior.
     * @param model the ElementwiseParametricDistribution whose target parameter should be found
     * @return the parameter the given model supplies a prior for, or null if it can't be found
     */
    private Function getOwningParameter(ElementwiseParametricDistribution model) {
        Prior prior = getOwningPrior(model);
        return (prior != null) ? prior.m_x.get() : null;
    }

    /**
     * Dimension of the owning parameter, or -1 if this distribution is not yet wrapped by a Prior.
     * @param model the ElementwiseParametricDistribution whose target parameter's dimension is needed
     * @return the dimension of the parameter the given model applies to, or 0 if it can't be determined
     */
    private int getOwningParameterDimension(ElementwiseParametricDistribution model) {
        Function param = getOwningParameter(model);
        return (param != null) ? param.getDimension() : -1;
    }

    /**
     * Records each element panel's expanded/collapsed state, keyed by
     * "<model ID>#<index>". Static: BEAUti discards and reconstructs
     * editor instances on refresh, so instance fields do not survive a
     * dropdown change or panel resync. Keyed by slot index rather than
     * by the distribution object's own ID, since swapping a slot's
     * distribution type (e.g. Uniform to Normal) replaces the object
     * entirely, giving it a new, previously-unseen ID.
     */
    private static final Map<String, Boolean> expandedStates = new HashMap<>();

    /**
     * Rebuilds distsContainer with one TitledPane per element of distsInput.
     * Reconciles the element count against the owning parameter's current
     * dimension first, then builds each element's editor via the standard
     * factory path (see wrapElement), restoring
     * each pane's expanded/collapsed state from expandedStates.
     * @param model the ElementwiseParametricDistribution to build the per-element editor panel for
     */
    private void buildDistsPanel(ElementwiseParametricDistribution model) {
        distsContainer.getChildren().clear();
        List<ParametricDistribution> dists = model.distsInput.get();
        if (dists.size() != getOwningParameterDimension(model)) {
            reconcileDists(dists, getOwningParameterDimension(model), model.getID());
        }

        for (int i = 0; i < dists.size(); i++) {
            InputEditor editor;
            try {
                editor = doc.getInputEditorFactory().createInputEditor(
                        wrapElement(dists, i), model,
                        true, ExpandOption.TRUE, ButtonStatus.NONE, null, doc);
            } catch (Exception ex) {
                System.out.println("Could not build editor for dist " + i + ": " + ex);
                continue;
            }

            String key = model.getID() + "#" + i;
            TitledPane p = new TitledPane("Element " + i, editor.getComponent());
            p.setExpanded(expandedStates.getOrDefault(key, false));

            p.expandedProperty().addListener((obs, wasExpanded, isNowExpanded) ->
                    expandedStates.put(key, isNowExpanded));

            distsContainer.getChildren().add(p);
        }
    }

    /**
     * Wraps a single element of distsInput as its own scalar Input, so the
     * standard factory path sees a plain ParametricDistribution, not a List.
     * Needed because BEASTObjectInputEditor.expandedInit() casts input.get()
     * to BEASTInterface unconditionally, without checking itemNr, and would
     * fail against the raw list-typed Input otherwise. get()/setValue() are
     * overridden to read from and write back to the real list at index i, so
     * changes made through the returned editor persist in distsInput itself.
     * @param dists the list of per-element distributions
     * @param i the index of the element to wrap
     * @return a synthetic, scalar Input wrapping the chosen element, suitable for the standard ParametricDistribution editor
     */
    private Input<ParametricDistribution> wrapElement(List<ParametricDistribution> dists, int i) {
        return new Input<ParametricDistribution>("distribution", "single element", dists.get(i), ParametricDistribution.class) {
            @Override
            public ParametricDistribution get() {
                return dists.get(i);
            }
            @Override
            public void setValue(Object value, BEASTInterface beastObject) {
                dists.set(i, (ParametricDistribution) value);
            }
        };
    }

    /**
     * Grows or shrinks dists to match targetDim. Growth appends Uniform
     * placeholders, named "<class>.<ownerLabel>.<index>" so BEAUti's combo
     * preselection (which matches on the class name recovered before the
     * first '.') resolves to Uniform, not to ownerLabel's own class. Shrink
     * removes from the end and logs each removed distribution's type and
     * parameter values, so the user can reconstruct it if needed.
     * @param dists the list of per-element distributions to resize
     * @param targetDim the dimension the list should end up at
     * @param ownerLabel a label identifying the owning parameter, used to build unique placeholder IDs
     */
    static void reconcileDists(List<ParametricDistribution> dists, int targetDim, String ownerLabel) {
        int currentDim = dists.size();
        if (currentDim == targetDim) return;

        if (currentDim < targetDim) {
            for (int i = currentDim; i < targetDim; i++) {
                ParametricDistribution d = new Uniform();
                d.setID(d.getClass().getSimpleName() + "." + ownerLabel + "." + i);
                d.initAndValidate();
                dists.add(d);
            }
            System.out.println(ownerLabel + ": dimension increased to " + targetDim +
                    " -- added " + (targetDim - currentDim) + " new Uniform placeholder(s). Please review.");
        } else {
            StringBuilder removedInfo = new StringBuilder();
            int nRemoved = currentDim - targetDim;
            while (dists.size() > targetDim) {
                ParametricDistribution removed = dists.remove(dists.size() - 1);
                if (removedInfo.length() > 0) removedInfo.append("; ");
                removedInfo.append(describeDistribution(removed));
            }
            System.out.println(ownerLabel + ": dimension decreased -- removed " +
                    nRemoved + " distribution(s) from the end: " + removedInfo);
        }
    }

    /**
     * Type name and every populated input's value, for a removed distribution's removal log entry.
     * @param d the distribution to describe
     * @return a short, human-readable summary of the distribution's type and parameters
     */
    static String describeDistribution(ParametricDistribution d) {
        StringBuilder sb = new StringBuilder(d.getClass().getSimpleName()).append("(");
        boolean first = true;
        for (Input<?> input : d.listInputs()) {
            if (input.get() == null) continue;
            if (!first) sb.append(", ");
            sb.append(input.getName()).append("=").append(input.get());
            first = false;
        }
        return sb.append(")").toString();
    }

}