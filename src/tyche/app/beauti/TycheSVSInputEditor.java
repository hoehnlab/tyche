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

import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.evolution.datatype.DataType;
import beast.base.evolution.datatype.UserDataType;
import beast.base.evolution.sitemodel.SiteModel;
import beast.base.inference.parameter.RealParameter;
import beastfx.app.inputeditor.BEASTObjectDialog;
import beastfx.app.inputeditor.BEASTObjectInputEditor;
import beastfx.app.inputeditor.BeautiDoc;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import tyche.evolution.likelihood.AncestralTypeLikelihood;
import tyche.evolution.substitutionmodel.TycheSVSGeneralSubstitutionModel;

/**
 * @author Jessie Fielding
 * This file is part of the TyCHE package - https://github.com/hoehnlab/tyche
 */

/**
 * Editor for TycheSVSGeneralSubstitutionModel. Shows the same Rate
 * Transitions panel as the clock model's editor (shared via
 * TraitTransitionRatesPanel), with a title-bar edit button that opens the
 * native detail dialog for the rates parameter specifically -- not the
 * indicator, which the checkboxes already fully cover.
 */
public class TycheSVSInputEditor extends BEASTObjectInputEditor {

    public TycheSVSInputEditor() { super(); }
    public TycheSVSInputEditor(BeautiDoc doc) { super(doc); }

    @Override
    public Class<?> type() {
        return TycheSVSGeneralSubstitutionModel.class;
    }

    @Override
    public void init(Input<?> input, BEASTInterface beastObject, int itemNr,
                     ExpandOption isExpandOption, boolean addButtons) {
        super.init(input, beastObject, itemNr, isExpandOption, addButtons);
        m_input = input;
        m_beastObject = beastObject;
        this.itemNr = itemNr;

        TycheSVSGeneralSubstitutionModel substModel = (TycheSVSGeneralSubstitutionModel) input.get();

        Label titleLabel = new Label("Relative Type-Transition Rates");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox titleBox = new HBox(titleLabel, spacer, createRatesEditButton(substModel));
        titleBox.setAlignment(Pos.CENTER_LEFT);

        GridPane content;
        String codeMap = findCodeMap(substModel);
        if (codeMap == null) {
            content = new GridPane();
            content.add(new Label("Could not find the linked trait for this substitution model."), 0, 0);
        } else {
            content = TraitTransitionRatesPanel.buildContent(substModel, codeMap);
        }

        TitledPane pane = new TitledPane("", content);
        pane.setGraphic(titleBox);
        titleBox.prefWidthProperty().bind(pane.widthProperty().subtract(40));

        getChildren().clear();
        getChildren().add(pane);
    }

    /**
     * Finds the codeMap of whichever AncestralTypeLikelihood is currently linked to this substModel.
     * @param substModel the substitution model to find the linked trait's code map for
     * @return the code map string of whichever trait partition currently uses this substitution model, or null if none is found
     */
    private String findCodeMap(TycheSVSGeneralSubstitutionModel substModel) {
        for (BEASTInterface o : doc.pluginmap.values()) {
            if (o instanceof AncestralTypeLikelihood) {
                AncestralTypeLikelihood atl = (AncestralTypeLikelihood) o;
                Object linkedSubstModel = ((SiteModel.Base) atl.siteModelInput.get()).substModelInput.get();
                if (linkedSubstModel == substModel) {
                    DataType.Base dataType = (DataType.Base) atl.dataInput.get().getDataType();
                    return ((UserDataType) dataType).codeMapInput.get();
                }
            }
        }
        return null;
    }

    private Button createRatesEditButton(TycheSVSGeneralSubstitutionModel substModel) {
        RealParameter rates = (RealParameter) substModel.ratesInput.get();
        Button b = new Button("e");
        b.setStyle("-fx-background-radius: 10; -fx-min-width: 15px; -fx-min-height: 15px; " +
                "-fx-max-width: 15px; -fx-max-height: 15px; -fx-font-size: 5pt");
        b.setTooltip(new Tooltip("Edit relative rates"));
        b.setOnAction(e -> {
            BEASTObjectDialog dlg = new BEASTObjectDialog(rates, RealParameter.class, doc);
            if (dlg.showDialog()) {
                try {
                    dlg.accept(rates, doc);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        return b;
    }
}
