package tyche.app.beauti;

import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.datatype.DataType;
import beast.base.evolution.datatype.UserDataType;
import beast.base.evolution.operator.ScaleOperator;
import beast.base.evolution.sitemodel.SiteModel;
import beast.base.evolution.substitutionmodel.SubstitutionModel;
import beast.base.inference.parameter.BooleanParameter;
import beast.base.inference.parameter.IntegerParameter;
import beast.base.inference.parameter.RealParameter;
import beastfx.app.inputeditor.*;
import beastfx.app.util.FXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.Node;
import tyche.evolution.branchratemodel.TycheExpectedOccupancyClockModel;
import tyche.evolution.likelihood.AncestralTypeLikelihood;

import java.util.List;

import javafx.scene.control.TextFormatter;
import tyche.evolution.substitutionmodel.TycheSVSGeneralSubstitutionModel;

import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TycheClockInputEditor extends BEASTObjectInputEditor {

    public TycheClockInputEditor() {
        super();
    }

    public TycheClockInputEditor(BeautiDoc doc) {
        super(doc);
    }

    private ComboBox<String> traitCombo;

    @Override
    public Class<?> type() {
        return TycheExpectedOccupancyClockModel.class;
    }


    private Alignment partitionToUseForTrait;

    private VBox perCategoryBox;

    private TitledPane traitRatesPane;
    private TitledPane internalNodeInitPane;
    private TitledPane allowedTransitionsPane;



    @Override
    public void init(Input<?> input, BEASTInterface beastObject, int itemNr,
                     ExpandOption isExpandOption, boolean addButtons) {

        // builds the standard combo box + edit button + validation label
        super.init(input, beastObject, itemNr, isExpandOption, addButtons);
        Node partitionEditor = createTraitPartitionEditor((TycheExpectedOccupancyClockModel) input.get());
        m_input = input;
        m_beastObject = beastObject;
        this.itemNr = itemNr;

        // Model-specific sub-editors/controls here, following
        // the SiteModelInputEditor pattern, e.g.:
        // InputEditor rateEditor = doc.getInputEditorFactory()
        //         .createInputEditor(myModel.someInput, myModel, doc);
        // pane.getChildren().add(rateEditor.getComponent());

        VBox root = new VBox(20);
        root.setPadding(new Insets(8));
        traitRatesPane = buildTraitPanel();
        internalNodeInitPane = buildInternalNodeInitPanel();
        allowedTransitionsPane = buildAllowedTransitionsPanel();
        root.getChildren().addAll(
                partitionEditor,
                traitRatesPane,
                internalNodeInitPane,
                allowedTransitionsPane
        );

        HBox subpanel = null;
        for (int i = 0; i < getChildren().size(); i++) {
            Node child = getChildren().get(i);
            if (child instanceof HBox) {
                subpanel = (HBox) child;
            }
        }
        if (subpanel != null) {
            for (int i = 0; i < subpanel.getChildren().size(); i++) {
                Node child = subpanel.getChildren().get(i);
                if (child instanceof VBox) {
                    ((VBox) child).getChildren().add(root);
                }
            }
        }

    }

    /**
     * Initialization value for internal nodes dropdown.
     * Reuses the same codeMap-derived trait names as buildTraitPanel(),
     * and on selection writes the chosen type's integer code into every
     * internal-node entry of the shared nodeTypes IntegerParameter.
     */
    private GridPane buildInternalNodeInitContent() {
        GridPane g = new GridPane();
        g.setHgap(8);
        g.setVgap(4);
        g.setPadding(new Insets(4));

        AncestralTypeLikelihood atl = getATL(partitionToUseForTrait);
        if (atl == null) {
            g.add(new Label("Select a trait partition above to configure internal node initialization."), 0, 0);
            return g;
        }
        String codeMap = ((UserDataType) ((DataType.Base)
                atl.dataInput.get().getDataType())).codeMapInput.get();
        int stateCount = getStateCount(codeMap);

        ComboBox<String> internalNodeInitCombo = new ComboBox<>();
        for (int i = 0; i < stateCount; i++) {
            internalNodeInitCombo.getItems().add(getTraitFromInt(i, codeMap));
        }
        if (!internalNodeInitCombo.getItems().isEmpty()) {
            internalNodeInitCombo.getSelectionModel().selectFirst();
        }

        internalNodeInitCombo.setOnAction(e -> {
            String trait = internalNodeInitCombo.getValue();
            int typeCode = getIntFromTrait(trait, codeMap);
            if (typeCode == -1) return;
            applyInternalNodeInit(atl, typeCode);
        });

        g.add(new Label("Initialization value for internal nodes:"), 0, 0);
        g.add(internalNodeInitCombo, 1, 0);
        return g;
    }

    /**
     * Writes typeCode as the value of the shared
     * nodeTypes parameter (the same IntegerParameter object pointed to
     * by both the model and the AncestralTypeLikelihood — see
     * updateModelsFromPartition).
     */
    private void applyInternalNodeInit(AncestralTypeLikelihood atl, int typeCode) {
        if (atl == null) return;
        IntegerParameter nodeTypes = atl.nodeTypesInput.get();
        if (nodeTypes == null) return;

        nodeTypes.valuesInput.set(String.valueOf(typeCode));
        nodeTypes.initAndValidate();
    }


//    private TitledPane buildPerCategoryPanel() {
//        perCategoryBox = new VBox(4);
//        perCategoryBox.setPadding(new Insets(4));
//        TitledPane p = new TitledPane("Per-Type Evolutionary Rates", perCategoryBox);
//        p.setExpanded(true);
//        return p;
//    }

//    public InputEditor createMutationRateEditor() {
//        DataType dataType = partitionToUseForTrait.getDataType();
//        int nStates = dataType.getStateCount();
//
//        dataType.getCharacter(code);
//        if (partitionToUseForTrait instanceof AlignmentFromTrait)
//        final Input<?> input = sitemodel.muParameterInput;
//        ParameterInputEditor mutationRateEditor = new ParameterInputEditor(doc);
//        mutationRateEditor.init(input, sitemodel, -1, ExpandOption.FALSE, true);
//        mutationRateEditor.getEntry().setDisable(doc.autoUpdateFixMeanSubstRate);
//        mutationRateEditor.m_isEstimatedBox.setOnAction(e -> {
//            mutationRateEditor.toggleEstimate();
//            setUpOperator();
//        });
//
//        return mutationRateEditor;
//    }

//    private Node createTypeLinkedRatesEditor(TycheExpectedOccupancyClockModel model) {
//        InputEditor editor;
//        System.out.println("are we even here? " + model.typeLinkedRatesInput + " " + model.typeLinkedRatesInput.get());
//        try {
//            editor = doc.getInputEditorFactory()
//                    .createInputEditor(model.typeLinkedRatesInput, model, doc);
//        } catch (Exception ex) {
////            Alert.showMessageDialog(getComponent(),
////                    "Could not set estimate flag: " + ex.getMessage());
//            System.out.println("Error making typelinkedrateseditor: " + ex);
//            return null;
//        }
//
//        return editor.getComponent();
//    }

    private void updateModelsFromPartition(TycheExpectedOccupancyClockModel model, Alignment partitionToUseForTrait) {
        AncestralTypeLikelihood atl = null;
        if (partitionToUseForTrait == null) return;
        for (BEASTInterface o : doc.pluginmap.values()) {
            if (o instanceof AncestralTypeLikelihood) {
                Alignment atlPartition = doc.getPartition(o);
                if (atlPartition != null && atlPartition.getID().equals(partitionToUseForTrait.getID())) {
                    atl = (AncestralTypeLikelihood) o;
                    break;
                }
            }
        }
        if (atl == null) return;
//        model.nodeTypesInput.set(atl.nodeTypesInput.get());
        System.out.println("atl is " + atl.getID() + " atl partition is " + doc.getPartition(atl) + " subst model is " + ((SubstitutionModel.Base)((SiteModel.Base) atl.siteModelInput.get()).substModelInput.get()).getID());
        model.svsInput.set(((SiteModel.Base) atl.siteModelInput.get()).substModelInput.get());
        model.nodeTypesInput.set(atl.nodeTypesInput.get());
        model.typeSwitchClockRateInput.set(atl.branchRateModelInput.get().meanRateInput.get());
    }

    private AncestralTypeLikelihood getATL(Alignment partitionToUseForTrait) {
        AncestralTypeLikelihood atl = null;
        if (partitionToUseForTrait == null) return null;
        for (BEASTInterface o : doc.pluginmap.values()) {
            if (o instanceof AncestralTypeLikelihood) {
                Alignment atlPartition = doc.getPartition(o);
                if (atlPartition != null && atlPartition.getID().equals(partitionToUseForTrait.getID())) {
                    atl = (AncestralTypeLikelihood) o;
                    break;
                }
            }
        }
        if (atl == null) return null;
//        model.nodeTypesInput.set(atl.nodeTypesInput.get());

        return atl;
    }

    private TitledPane buildTraitPanel() {
        RealParameter typeLinkedRates = ((TycheExpectedOccupancyClockModel) m_input.get()).typeLinkedRatesInput.get();

        Label titleLabel = new Label("Type Linked Rates");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox titleBox = new HBox(titleLabel, spacer, createTypeLinkedRatesBoundsButton(typeLinkedRates));
        titleBox.setAlignment(Pos.CENTER_LEFT);

        TitledPane pane = new TitledPane("", buildTraitPanelContent());   // empty title -- titleBox replaces it
        pane.setGraphic(titleBox);
        titleBox.prefWidthProperty().bind(pane.widthProperty().subtract(40));   // 40px ~ room for the expand arrow
        return pane;
    }


    private TitledPane buildInternalNodeInitPanel() {
        return new TitledPane("Internal Node Initialization", buildInternalNodeInitContent());
    }

    // NEW: call this after updateModelsFromPartition() on partition switch
    private void refreshTraitDependentPanels() {
        traitRatesPane.setContent(buildTraitPanelContent());
        internalNodeInitPane.setContent(buildInternalNodeInitContent());
        allowedTransitionsPane.setContent(buildAllowedTransitionsContent());
    }


    private Node createTraitPartitionEditor(TycheExpectedOccupancyClockModel model) {
        HBox box = FXUtils.newHBox();
        box.getChildren().add(new Label("Trait partition"));
        SmallLabel traitPartitionValidateLabel;

        ComboBox<Alignment> traitPartitionCombo = new ComboBox<>();
        traitPartitionCombo.getItems().add(null);

        Alignment ownPartition = doc.getPartition(model);
        for (Alignment data : doc.alignments) {
            if (ownPartition == null || !data.getID().equals(ownPartition.getID())) {
                if (isPartitionTycheTrait(data)) traitPartitionCombo.getItems().add(data);
            }
        }
        partitionToUseForTrait = doc.getPartition(model.svsInput.get());
        if (partitionToUseForTrait == null && traitPartitionCombo.getItems().size() > 1) {
            partitionToUseForTrait = traitPartitionCombo.getItems().get(1);   // index 0 is always null; first real entry
            updateModelsFromPartition(model, partitionToUseForTrait);        // actually wire the model, not just the combo display
        }
        if (partitionToUseForTrait == null) traitPartitionCombo.setValue(null);
        else traitPartitionCombo.setValue(partitionToUseForTrait);
        traitPartitionCombo.setId("traitPartition");

        traitPartitionValidateLabel = new SmallLabel("x", "red");
        box.getChildren().add(traitPartitionCombo);
        box.getChildren().add(traitPartitionValidateLabel);

//        traitPartitionCombo.setButtonCell();
        traitPartitionCombo.setButtonCell(new ListCell<Alignment>() {
            @Override
            protected void updateItem(Alignment item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    setText(item.getID());
                }
                else {
                    setText(null);
                }
            }
        });

        traitPartitionCombo.setOnAction(e -> {
//            System.out.println("HERE!!!!!!!!!!");
//            System.out.println(((ComboBox<BEASTObject>) e.getSource()).getValue());
            Object selected = (e.getSource() instanceof ComboBox) ? ((ComboBox<?>) e.getSource()).getValue() : null;
//            String selectedID = (e.getSource() instanceof ComboBox) ? ((ComboBox<String>) e.getSource()).getValue() : null;
            partitionToUseForTrait = (selected instanceof Alignment) ? ((Alignment) selected) : null;
//            partitionToUseForTrait = (selected instanceof Alignment) ? ((Alignment) selected) : null;
            String newID = (selected instanceof Alignment) ? ((Alignment) selected).getID() : null;
            System.out.println("selected partition is " + partitionToUseForTrait + " and ID is " + newID);
            updateModelsFromPartition(model, partitionToUseForTrait);
            validateTraitPartition(selected, traitPartitionValidateLabel);
            refreshTraitDependentPanels();
            sync();
        });

        validateTraitPartition(partitionToUseForTrait, traitPartitionValidateLabel); // initial state
        return box;
    }

    private String getTraitFromInt(int code, String codeMap) {
        String traitWithCode = String.valueOf(codeMap.split(",")[code]);
        if (traitWithCode.split("=")[1].equals(String.valueOf(code))) {
            return traitWithCode.split("=")[0];
        }
        return "unknown";
    }

    private void setTraitRateByName(String trait, double rate, String codeMap) {
        int traitCode = getIntFromTrait(trait, codeMap);
        System.out.println("in set trait rate by name, trait was " + trait + " traitcode is " + traitCode);
        if (traitCode == -1) return;
        RealParameter typeLinkedRates = ((TycheExpectedOccupancyClockModel) m_input.get()).typeLinkedRatesInput.get();
        List<Double> values = typeLinkedRates.valuesInput.get();
        System.out.println("current values are " + values);
        values.set(traitCode, rate);                 // valuesInput's List: what gets saved
        typeLinkedRates.setValue(traitCode, rate);   // values[]: what runtime/printing reads
        System.out.println("updated values are " + values);
//        typeLinkedRates.valuesInput.setValue(null, typeLinkedRates);
//        typeLinkedRates.valuesInput.setValue(values, typeLinkedRates);
        System.out.println("updated typelinkedrates is " + ((TycheExpectedOccupancyClockModel) m_input.get()).typeLinkedRatesInput.get().valuesInput.get());
//        typeLinkedRates.setValue(traitCode, rate);
//        ((TycheExpectedOccupancyClockModel) m_input.get()).typeLinkedRatesInput.setValue(typeLinkedRates,((TycheExpectedOccupancyClockModel) m_input.get()));
    }

    private int getIntFromTrait(String trait, String codeMap) {
        String[] codes = codeMap.split(",");
        for (String code : codes) {
            if (code.split("=")[0].equals(trait)) {
                return Integer.parseInt(code.split("=")[1]);
            }
        }
        return -1;
    }

    private int getStateCount(String codeMap) {
        String[] codes = codeMap.split(",");
        int count = 0;
        for (String code : codes) {
            if (code.split("=")[0].contains("?")) {
                continue;
            }
            count++;
        }
        return count;
    }

    private GridPane buildTraitPanelContent() {
        GridPane g = new GridPane();
        g.setHgap(8);
        g.setVgap(4);
        g.setPadding(new Insets(4));
        RealParameter typeLinkedRates = ((TycheExpectedOccupancyClockModel) m_input.get()).typeLinkedRatesInput.get();
        AncestralTypeLikelihood atl = getATL(partitionToUseForTrait);
        if (atl == null) {
            g.add(new Label("Select a trait partition above to configure trait-linked rates."), 0, 0);
            return g;
        }
        DataType.Base dataType = (DataType.Base) atl.dataInput.get().getDataType();
        String codeMap = ((UserDataType) dataType).codeMapInput.get();
        int stateCount = getStateCount(codeMap);
        BooleanParameter indicator = getTypeLinkedRatesIndicator();
        if (typeLinkedRates.getDimension() != stateCount) {
            typeLinkedRates.setDimension(stateCount);
            final int oldDim = typeLinkedRates.getDimension();
            String valueString = IntStream.range(0, stateCount)
                    .mapToObj(i -> i < oldDim ? typeLinkedRates.getValue(i) : 1.0) // default for new states
                    .map(String::valueOf)
                    .collect(Collectors.joining(" "));

            typeLinkedRates.valuesInput.setValue(valueString, typeLinkedRates); // clears + rebuilds the List at correct length
            typeLinkedRates.initAndValidate();                                   // re-derives values[] from that List
        }
        for (int i = 0; i < stateCount; i++) {
            String trait = getTraitFromInt(i, codeMap);
            g.add(new Label(getTraitFromInt(i, codeMap)), 0, i);

            TextField numberField = new TextField();
            UnaryOperator<TextFormatter.Change> filter = change -> {
                String newText = change.getControlNewText();
                if (newText.matches("-?\\d*(\\.\\d*)?([eE][-+]?\\d*)?")) {
                    return change; // allow
                }
                return null; // reject the keystroke
            };
            numberField.setTextFormatter(new TextFormatter<>(filter));
            numberField.setText(String.valueOf(typeLinkedRates.getValue(i)));
            numberField.setOnAction( e -> {
                setTraitRateByName(trait, Double.parseDouble(numberField.getText()), codeMap);
                System.out.println("numberField set action " + trait + " to " + Double.parseDouble(numberField.getText()));
            });
            numberField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                if (!isFocused) {
                    setTraitRateByName(trait, Double.parseDouble(numberField.getText()), codeMap);
                    System.out.println("numberField set action " + trait + " to " + Double.parseDouble(numberField.getText()));                                      // clicked elsewhere / tabbed away
                }
            });
            g.add(numberField, 1, i);
            if (indicator != null) {
                CheckBox estimateBox = new CheckBox("estimate");
                estimateBox.setSelected(indicator.getValue(i));
                final int idx = i;
                estimateBox.setOnAction(e -> {
                    List<Boolean> indicatorValues = indicator.valuesInput.get();
                    indicatorValues.set(idx, estimateBox.isSelected());
                    indicator.setValue(idx, estimateBox.isSelected());
                    updateTypeLinkedRatesEstimateFlag(typeLinkedRates, indicator);
                });
                g.add(estimateBox, 2, i);
            }
        }

        return g;
    }

    private Button createTypeLinkedRatesBoundsButton(RealParameter typeLinkedRates) {
        Button b = new Button("e");
        b.setStyle("-fx-background-radius: 10; -fx-min-width: 15px; -fx-min-height: 15px; " +
                "-fx-max-width: 15px; -fx-max-height: 15px; -fx-font-size: 5pt");
        b.setTooltip(new Tooltip("Edit type-linked rates bounds"));
        b.setOnAction(e -> {
            BEASTObjectDialog dlg = new BEASTObjectDialog(typeLinkedRates, RealParameter.class, doc);
            if (dlg.showDialog()) {
                try {
                    dlg.accept(typeLinkedRates, doc);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        return b;
    }

    private void updateTypeLinkedRatesEstimateFlag(RealParameter typeLinkedRates, BooleanParameter indicator) {
        boolean anyEstimated = false;
        for (int i = 0; i < indicator.getDimension(); i++) {
            if (indicator.getValue(i)) { anyEstimated = true; break; }
        }
        typeLinkedRates.isEstimatedInput.setValue(anyEstimated, typeLinkedRates);
    }

    private BooleanParameter getTypeLinkedRatesIndicator() {
        String context = BeautiDoc.parsePartition(((TycheExpectedOccupancyClockModel) m_input.get()).getID());
        BEASTInterface scaler = doc.pluginmap.get("typeLinkedRatesScaler.c:" + context);
        if (!(scaler instanceof ScaleOperator)) return null;
        return ((ScaleOperator) scaler).indicatorInput.get();
    }


    private boolean isPartitionTycheTrait(Alignment partition) {
        if (partition == null) return false;
        for (BEASTInterface o : doc.pluginmap.values()) {
            if (o instanceof AncestralTypeLikelihood) {
                Alignment atlPartition = doc.getPartition(o);
                if (atlPartition != null && atlPartition.getID().equals(partition.getID())) {
                    return true;
                }
            }
        }
        return false;
    }

//    private GridPane buildAllowedTransitionsContent() {
//        GridPane g = new GridPane();
//        g.setHgap(8);
//        g.setVgap(4);
//        g.setPadding(new Insets(4));
//
//        AncestralTypeLikelihood atl = getATL(partitionToUseForTrait);
//        if (atl == null) {
//            g.add(new Label("Select a trait partition above to configure allowed transitions."), 0, 0);
//            return g;
//        }
//
//        DataType.Base dataType = (DataType.Base) atl.dataInput.get().getDataType();
//        String codeMap = ((UserDataType) dataType).codeMapInput.get();
//        int stateCount = getStateCount(codeMap);
//
//        TycheSVSGeneralSubstitutionModel substModel = (TycheSVSGeneralSubstitutionModel)
//                ((SiteModel.Base) atl.siteModelInput.get()).substModelInput.get();
//        BooleanParameter rateIndicator = substModel.indicator.get();
//        RealParameter relativeGeoRates = (RealParameter) substModel.ratesInput.get();
//        boolean isSymmetric = substModel.isSymmetricInput.get();
//
//        int nRates = isSymmetric ? stateCount * (stateCount - 1) / 2 : stateCount * (stateCount - 1);
//        if (rateIndicator.getDimension() != nRates || relativeGeoRates.getDimension() != nRates) {
//            g.add(new Label("Rate dimensions do not match trait state count -- please reopen the trait editor."), 0, 0);
//            return g;
//        }
//
//        CheckBox[][] boxes = new CheckBox[stateCount][stateCount];
//        TextField[][] rateFields = new TextField[stateCount][stateCount];
//
//        int row = 0;
//        for (int i = 0; i < stateCount; i++) {
//            for (int j = 0; j < stateCount; j++) {
//                if (i == j) continue;
//                g.add(new Label(getTraitFromInt(i, codeMap) + " -> " + getTraitFromInt(j, codeMap)), 0, row);
//
//                int idx = rateIndicatorIndex(i, j, stateCount, isSymmetric);
//
//                TextField rateField = new TextField(String.valueOf(relativeGeoRates.getValue(idx)));
//                UnaryOperator<TextFormatter.Change> filter = change -> {
//                    String newText = change.getControlNewText();
//                    return newText.matches("-?\\d*(\\.\\d*)?([eE][-+]?\\d*)?") ? change : null;
//                };
//                rateField.setTextFormatter(new TextFormatter<>(filter));
//                rateFields[i][j] = rateField;
//
//                int finalJ = j;
//                int finalI = i;
//                Runnable commitRate = () -> {
//                    double value = Double.parseDouble(rateField.getText());
//                    List<Double> rateValues = relativeGeoRates.valuesInput.get();
//                    rateValues.set(idx, value);
//                    relativeGeoRates.setValue(idx, value);
//                    if (isSymmetric) {
//                        TextField pairedField = rateFields[finalJ][finalI];
//                        if (pairedField != null && !pairedField.getText().equals(rateField.getText())) {
//                            pairedField.setText(rateField.getText());
//                        }
//                    }
//                };
//                rateField.setOnAction(e -> commitRate.run());
//                rateField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
//                    if (!isFocused) commitRate.run();
//                });
//                g.add(rateField, 1, row);
//
//                CheckBox allowedBox = new CheckBox("Allowed");
//                allowedBox.setSelected(rateIndicator.getValue(idx));
//                boxes[i][j] = allowedBox;
//
//                rateField.disableProperty().bind(allowedBox.selectedProperty().not());
//
//                int finalJ1 = j;
//                int finalI1 = i;
//                allowedBox.setOnAction(e -> {
//                    boolean selected = allowedBox.isSelected();
//                    List<Boolean> indicatorValues = rateIndicator.valuesInput.get();
//                    indicatorValues.set(idx, selected);
//                    rateIndicator.setValue(idx, selected);
//                    if (isSymmetric) {
//                        CheckBox pairedBox = boxes[finalJ1][finalI1];
//                        if (pairedBox != null && pairedBox.isSelected() != selected) {
//                            pairedBox.setSelected(selected);
//                        }
//                    }
//                });
//                g.add(allowedBox, 2, row);
//                row++;
//            }
//        }
//        return g;
//    }
    private TitledPane buildAllowedTransitionsPanel() {
        Label titleLabel = new Label("Relative State Transition Rates");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox titleBox = new HBox(titleLabel, spacer, createAllowedTransitionsEditButton());
        titleBox.setAlignment(Pos.CENTER_LEFT);

        TitledPane pane = new TitledPane("", buildAllowedTransitionsContent());
        pane.setGraphic(titleBox);
        titleBox.prefWidthProperty().bind(pane.widthProperty().subtract(40));
        return pane;
    }

    /** Looks up the current relativeGeoRates fresh on every click -- unlike typeLinkedRates,
     *  this parameter belongs to the linked trait's svs model, which swaps identity whenever
     *  the trait-partition combo changes. Capturing it once at build time would go stale. */
    private Button createAllowedTransitionsEditButton() {
        Button b = new Button("e");
        b.setStyle("-fx-background-radius: 10; -fx-min-width: 15px; -fx-min-height: 15px; " +
                "-fx-max-width: 15px; -fx-max-height: 15px; -fx-font-size: 5pt");
        b.setTooltip(new Tooltip("Edit relative rates"));
        b.setOnAction(e -> {
            AncestralTypeLikelihood atl = getATL(partitionToUseForTrait);
            if (atl == null) return;
            TycheSVSGeneralSubstitutionModel substModel = (TycheSVSGeneralSubstitutionModel)
                    ((SiteModel.Base) atl.siteModelInput.get()).substModelInput.get();
            RealParameter rates = (RealParameter) substModel.ratesInput.get();
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
    private GridPane buildAllowedTransitionsContent() {
        AncestralTypeLikelihood atl = getATL(partitionToUseForTrait);
        if (atl == null) {
            GridPane g = new GridPane();
            g.add(new Label("Select a trait partition above to configure allowed transitions."), 0, 0);
            return g;
        }
        DataType.Base dataType = (DataType.Base) atl.dataInput.get().getDataType();
        String codeMap = ((UserDataType) dataType).codeMapInput.get();
        TycheSVSGeneralSubstitutionModel substModel = (TycheSVSGeneralSubstitutionModel)
                ((SiteModel.Base) atl.siteModelInput.get()).substModelInput.get();
        return TraitTransitionRatesPanel.buildContent(substModel, codeMap);
    }

//    private TitledPane buildAllowedTransitionsPanel() {
//        return new TitledPane("Allowed Transitions", buildAllowedTransitionsContent());
//    }

    private int rateIndicatorIndex(int i, int j, int stateCount, boolean isSymmetric) {
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
            return i * (stateCount - 1) + (j < i ? j : j - 1);   // matches GeneralSubstitutionModel exactly
        }
    }

    private void validateTraitPartition(Object selectedTrait, SmallLabel traitPartitionValidateLabel) {
        String partitionID = (selectedTrait instanceof Alignment) ? ((Alignment) selectedTrait).getID() : null;

        if (partitionID == null) {
            traitPartitionValidateLabel.setVisible(true);
            traitPartitionValidateLabel.setColor("red");
            traitPartitionValidateLabel.setTooltip(new Tooltip(
                    "Select a partition containing a TyCHE trait."));
            return;
        }

        boolean hasAncestralTypeLikelihood = false;
        for (BEASTInterface o : doc.pluginmap.values()) {
            if (o instanceof AncestralTypeLikelihood) {
                Alignment atlPartition = doc.getPartition(o);
                if (atlPartition != null && atlPartition.getID().equals(partitionID)) {
                    hasAncestralTypeLikelihood = true;
                    break;
                }
            }
        }

        if (hasAncestralTypeLikelihood) {
            traitPartitionValidateLabel.setVisible(false);
        } else {
            traitPartitionValidateLabel.setVisible(true);
            traitPartitionValidateLabel.setColor("red");
            traitPartitionValidateLabel.setTooltip(
                    "Partition '" + partitionID + "' does not have a TyCHE trait.");
            return;
        }

        // NEW: warn (don't block) when the trait has more than two states
        String codeMap = ((UserDataType) getATL(partitionToUseForTrait).dataInput.get().getDataType()).codeMapInput.get();
        int stateCount = getStateCount(codeMap);
        if (stateCount > 2) {
            traitPartitionValidateLabel.setVisible(true);
            traitPartitionValidateLabel.setColor("orange");   // warning, not error
            traitPartitionValidateLabel.setTooltip(
                    "Partition '" + partitionID + "' has " + stateCount +
                            " trait states. The expected occupancy clock model is designed for two-state traits.");
            return;
        }
        traitPartitionValidateLabel.setVisible(false);
    }


}