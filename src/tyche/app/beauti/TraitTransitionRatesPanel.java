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

import beast.base.inference.parameter.BooleanParameter;
import beast.base.inference.parameter.RealParameter;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.GridPane;
import tyche.evolution.substitutionmodel.TycheSVSGeneralSubstitutionModel;

import java.util.List;
import java.util.function.UnaryOperator;

/**
 * @author Jessie Fielding
 * This file is part of the TyCHE package - https://github.com/hoehnlab/tyche
 */

/**
 * Shared GridPane- and row-building logic for the "State Transitions" panel, used by
 * both TycheClockInputEditor and TycheSVSInputEditor so the two editors
 * don't duplicate the pair-index/rate/indicator wiring.
 */
class TraitTransitionRatesPanel {

    /** Matches SVSGeneralSubstitutionModel.setupRateMatrix()'s indexing exactly, for both modes. */
    static int rateIndicatorIndex(int i, int j, int stateCount, boolean isSymmetric) {
        if (isSymmetric) {
            int a = Math.min(i, j), b = Math.max(i, j);
            int count = 0;
            for (int x = 0; x < stateCount; x++) {
                for (int y = x + 1; y < stateCount; y++) {
                    if (x == a && y == b) return count;
                    count++;
                }
            }
            return -1;
        } else {
            return i * (stateCount - 1) + (j < i ? j : j - 1);
        }
    }

    static String getTraitFromInt(int code, String codeMap) {
        String traitWithCode = String.valueOf(codeMap.split(",")[code]);
        if (traitWithCode.split("=")[1].equals(String.valueOf(code))) {
            return traitWithCode.split("=")[0];
        }
        return "unknown";
    }

    static int getStateCount(String codeMap) {
        String[] codes = codeMap.split(",");
        int count = 0;
        for (String code : codes) {
            if (code.split("=")[0].contains("?")) continue;
            count++;
        }
        return count;
    }

    static GridPane buildContent(TycheSVSGeneralSubstitutionModel substModel, String codeMap) {
        GridPane g = new GridPane();
        g.setHgap(8);
        g.setVgap(4);
        g.setPadding(new Insets(4));

        int stateCount = getStateCount(codeMap);
        BooleanParameter rateIndicator = substModel.indicator.get();
        RealParameter relativeGeoRates = (RealParameter) substModel.ratesInput.get();
        boolean isSymmetric = substModel.isSymmetricInput.get();

        int nRates = isSymmetric ? stateCount * (stateCount - 1) / 2 : stateCount * (stateCount - 1);
        if (rateIndicator.getDimension() != nRates || relativeGeoRates.getDimension() != nRates) {
            g.add(new Label("Rate dimensions do not match trait state count -- please reopen the trait editor."), 0, 0);
            return g;
        }

        CheckBox[][] boxes = new CheckBox[stateCount][stateCount];
        TextField[][] rateFields = new TextField[stateCount][stateCount];

        int row = 0;
        for (int i = 0; i < stateCount; i++) {
            for (int j = 0; j < stateCount; j++) {
                if (i == j) continue;
                g.add(new Label(getTraitFromInt(i, codeMap) + " -> " + getTraitFromInt(j, codeMap)), 0, row);

                int idx = rateIndicatorIndex(i, j, stateCount, isSymmetric);

                TextField rateField = new TextField(String.valueOf(relativeGeoRates.getValue(idx)));
                UnaryOperator<TextFormatter.Change> filter = change -> {
                    String newText = change.getControlNewText();
                    return newText.matches("-?\\d*(\\.\\d*)?([eE][-+]?\\d*)?") ? change : null;
                };
                rateField.setTextFormatter(new TextFormatter<>(filter));
                rateFields[i][j] = rateField;

                int finalJ = j;
                int finalI = i;
                Runnable commitRate = () -> {
                    double value = Double.parseDouble(rateField.getText());
                    List<Double> rateValues = relativeGeoRates.valuesInput.get();
                    rateValues.set(idx, value);
                    relativeGeoRates.setValue(idx, value);
                    if (isSymmetric) {
                        TextField pairedField = rateFields[finalJ][finalI];
                        if (pairedField != null && !pairedField.getText().equals(rateField.getText())) {
                            pairedField.setText(rateField.getText());
                        }
                    }
                };
                rateField.setOnAction(e -> commitRate.run());
                rateField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                    if (!isFocused) commitRate.run();
                });
                g.add(rateField, 1, row);

                CheckBox allowedBox = new CheckBox("transition allowed");
                allowedBox.setSelected(rateIndicator.getValue(idx));
                boxes[i][j] = allowedBox;

                rateField.disableProperty().bind(allowedBox.selectedProperty().not());

                int finalJ1 = j;
                int finalI1 = i;
                allowedBox.setOnAction(e -> {
                    boolean selected = allowedBox.isSelected();
                    List<Boolean> indicatorValues = rateIndicator.valuesInput.get();
                    indicatorValues.set(idx, selected);
                    rateIndicator.setValue(idx, selected);
                    if (isSymmetric) {
                        CheckBox pairedBox = boxes[finalJ1][finalI1];
                        if (pairedBox != null && pairedBox.isSelected() != selected) {
                            pairedBox.setSelected(selected);
                        }
                    }
                });
                g.add(allowedBox, 2, row);
                row++;
            }
        }
        return g;
    }
}
