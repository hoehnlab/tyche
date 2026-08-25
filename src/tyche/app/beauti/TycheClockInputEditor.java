package tyche.app.beauti;

import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.datatype.DataType;
import beast.base.evolution.datatype.UserDataType;
import beast.base.evolution.sitemodel.SiteModel;
import beast.base.evolution.substitutionmodel.SubstitutionModel;
import beast.base.inference.parameter.IntegerParameter;
import beast.base.inference.parameter.RealParameter;
import beastfx.app.inputeditor.*;
import beastfx.app.util.FXUtils;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import tyche.evolution.branchratemodel.TycheExpectedOccupancyClockModel;
import tyche.evolution.likelihood.AncestralTypeLikelihood;

import java.util.List;

import javafx.scene.control.TextFormatter;
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


    @Override
    public void init(Input<?> input, BEASTInterface beastObject, int itemNr,
                     ExpandOption isExpandOption, boolean addButtons) {

        // builds the standard combo box + edit button + validation label,
        // exactly as BEASTObjectInputEditor does for any other model choice
        super.init(input, beastObject, itemNr, isExpandOption, addButtons);
        Node partitionEditor = createTraitPartitionEditor((TycheExpectedOccupancyClockModel) input.get());
        m_input = input;
        m_beastObject = beastObject;
        this.itemNr = itemNr;
//        this.model = (TycheExpectedOccupancyClockModel) beastObject;

        // TODO: add any model-specific sub-editors/controls here, following
        // the SiteModelInputEditor pattern, e.g.:
        //
        // InputEditor rateEditor = doc.getInputEditorFactory()
        //         .createInputEditor(myModel.someInput, myModel, doc);
        // pane.getChildren().add(rateEditor.getComponent());

        // we probably want to add:
        // typeLinkedRates, but we want to auto-set the dimension according to subst model, and whether to estimate

        // initial value for internal nodes
        // we'll add a custom subst model editor for tyche svs and let that exist in the site model tab -- rate indicators go there
        VBox root = new VBox(20);
        root.setPadding(new Insets(8));
        traitRatesPane = buildTraitPanel();
        internalNodeInitPane = buildInternalNodeInitPanel();
        root.getChildren().addAll(
                partitionEditor,
                traitRatesPane,
                internalNodeInitPane
        );

//        getChildren().clear();
        String[] colors = new String[]{"red", "green", "blue", "purple"};
        HBox subpanel = null;
        for (int i = 0; i < getChildren().size(); i++) {
            Node child = getChildren().get(i);
            child.setStyle("-fx-border-color: " + colors[i] + "; -fx-border-width: 2px;");
            System.out.println("This is a child of the pane i think? " + child + " is " + colors[i]);
            if (child instanceof Pane) {
                Pane current = (Pane) child;
                for (int j = 0; j < current.getChildren().size(); j++) {
                    Node subchild = current.getChildren().get(j);
                    subchild.setStyle("-fx-border-color: " + colors[i+j+1] + "; -fx-border-width: 2px;");
                    System.out.println("This is a child of the child of the pane i think? " + subchild + " is " + colors[i+j+1]);
                    if (subchild instanceof Pane) {
                        Pane subcurrent = (Pane) subchild;
                        for (int k = 0; k < subcurrent.getChildren().size(); k++) {
                            Node subsubchild = subcurrent.getChildren().get(k);
                            subsubchild.setStyle("-fx-border-color: " + colors[i+j+k+2] + "; -fx-border-width: 2px;");
                            System.out.println("This is a child of the child of the pane i think? " + subsubchild + " is " + colors[i+j+k+2]);
//                    if (child instanceof VBox) {
////                    ((VBox) child).getChildren().clear();
//                        ((VBox) child).getChildren().add(root);
//                    }
                        }
                    }
//                    if (child instanceof VBox) {
////                    ((VBox) child).getChildren().clear();
//                        ((VBox) child).getChildren().add(root);
//                    }
                }
            }
            if (child instanceof HBox) {
                subpanel = (HBox) child;
            }
        }
        if (subpanel != null) {
            for (int i = 0; i < subpanel.getChildren().size(); i++) {
                Node child = subpanel.getChildren().get(i);
//                child.setStyle("-fx-border-color: " + colors[i] + "; -fx-border-width: 2px;");
//                System.out.println("This is a child of the pane i think? " + child + " is " + colors[i]);
                if (child instanceof VBox) {
//                    ((VBox) child).getChildren().clear();
                    ((VBox) child).getChildren().add(root);
                }
            }
////            subpanel.getChildren().add(root);
        }
//        getChildren().clear();
//        getChildren().add(root);
//        getChildren().add(root);
//        getChildren().add(partitionEditor);
//        getChildren().add(ratesEditor);

        // TODO: don't yet know what we'll do to add operators and priors, will revisit that next

    }

    /**
     * NEW: "Initialization value for internal nodes" dropdown.
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
        return new TitledPane("Type Linked Rates", buildTraitPanelContent());
    }


    private TitledPane buildInternalNodeInitPanel() {
        return new TitledPane("Internal Node Initialization", buildInternalNodeInitContent());
    }

    // NEW: call this after updateModelsFromPartition() on partition switch
    private void refreshTraitDependentPanels() {
        traitRatesPane.setContent(buildTraitPanelContent());
        internalNodeInitPane.setContent(buildInternalNodeInitContent());
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
        DataType.Base dataType = (DataType.Base) getATL(partitionToUseForTrait).dataInput.get().getDataType();
        String codeMap = ((UserDataType) dataType).codeMapInput.get();
        int stateCount = getStateCount(codeMap);
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
        }

        return g;
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