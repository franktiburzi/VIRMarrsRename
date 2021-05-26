package rename;

import com.univocity.parsers.common.processor.RowListProcessor;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import com.univocity.parsers.csv.CsvWriter;
import com.univocity.parsers.csv.CsvWriterSettings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;

import java.io.*;
import java.util.*;

public class RenamePane extends BorderPane {

    /** Main VBox to hold UI elements */
    private VBox headerVBox = new VBox();

    /** Gridpane to hold the files to rename */
    private GridPane fileGridPane = new GridPane();

    /** path to CSVs to read from */
    private List<String> pathToCSVs = new ArrayList<>();

    /** Map of the csv name to the run group number */
    private Map<String, StringProperty> runGroups = new HashMap<>();

    /** Hold the race id to rename the files with */
    private StringProperty raceId = new SimpleStringProperty();

    /** Directory to write the new files too */
    private String directoryPath;

    /** Display string for title of the tool */
    private static String RENAME_TOOL_TITLE = "MARRS VIR File Rename Utility";

    /** Display string for race ID */
    private static String RACE_ID = "Race ID: ";

    /** Display string for choose directory button */
    private static String CHOOSE_DIRECTORY_BTN = "Choose Directory";

    /** Display string for rename file button */
    private static String RENAME_FILES_BTN = "Rename the Files";

    /** Display String for the current directory identifier */
    private static String CURR_DIRECTORY = "Current Directory: ";

    /** Display string for the file column */
    private static String FILE_COLUMN = "Filename";

    /** Display string for the group number column */
    private static String GROUP_COLUMN = "Group Number";

    /** Property determining if the user can rename the files. cannot rename until all fields are filled */
    private BooleanProperty renameDisabled = new SimpleBooleanProperty(true);

    /**
     * Constructor
     */
    public RenamePane() {
        // No-op... currently
    }

    /**
     * Build the UI the user interacts with
     */
    public void BuildUI() {
        // Setup the UI elements at the top of the pane and place in a VBox
        Text title = new Text(RENAME_TOOL_TITLE);
        title.getStyleClass().add("title");
        Text raceIdLabel = new Text(RACE_ID);
        TextField raceIdField = new TextField();
        // Bind the raceID to access it later
        raceId.bind(raceIdField.textProperty());

        HBox raceIdPane = new HBox(raceIdLabel, raceIdField);
        raceIdPane.setAlignment(Pos.CENTER);

        Text currDirectory = new Text();
        currDirectory.getStyleClass().add("text-id");

        Button chooseDirectoryBtn = new Button(CHOOSE_DIRECTORY_BTN);

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setInitialDirectory(new File("C:\\Users\\Frank\\Documents\\MARRS_Points\\renameTool"));
        chooseDirectoryBtn.setOnAction(e -> {
            File csvDirectory = directoryChooser.showDialog(getScene().getWindow());
            currDirectory.setText(CURR_DIRECTORY + csvDirectory.getPath());
            populateFiles(csvDirectory);
        });

        headerVBox.getChildren().addAll(title, raceIdPane, currDirectory, chooseDirectoryBtn);
        headerVBox.setAlignment(Pos.CENTER);


        VBox renameVbox = new VBox();
        renameVbox.setAlignment(Pos.CENTER);
        renameVbox.prefHeightProperty().bind(heightProperty().multiply(.20));
        Button renameBtn = new Button(RENAME_FILES_BTN);
        renameBtn.disableProperty().bind(renameDisabled);
        renameBtn.setOnAction(e -> processCSVs());

        renameVbox.getChildren().addAll(renameBtn);

        setTop(headerVBox);
        setCenter(fileGridPane);
        setBottom(renameVbox);
    }

    /**
     * Read the files from the users local directory and build the gridpane to display them
     *
     * @param csvDirectory directory containing the CSVs
     */
    private void populateFiles(File csvDirectory) {
        directoryPath = csvDirectory.getPath();
        // Build the gridpane to display the files and the rename input fields
        fileGridPane.setHgap(10);
        fileGridPane.setAlignment(Pos.CENTER);
        Label fileHeader = new Label(FILE_COLUMN);
        Label groupHeader = new Label(GROUP_COLUMN);

        fileGridPane.add(fileHeader, 0, 0);
        fileGridPane.add(groupHeader, 1, 0);

        int index = 1;
        for (File csv : csvDirectory.listFiles()) {
            Text currFile = new Text (csv.getName());
            currFile.getStyleClass().add("filename");
            pathToCSVs.add(csv.getPath());
            TextField runGroup = new TextField();
            runGroup.textProperty().addListener((obs, oldV, newV) -> {
                if (!newV.isEmpty() && validateTextFields()) {
                    renameDisabled.setValue(false);
                } else {
                    renameDisabled.setValue(true);
                }
            });
            // Bind the run group input field to a map of csv names so we don't have to search the scene graph later
            runGroups.put(csv.getPath(), new SimpleStringProperty());
            runGroups.get(csv.getPath()).bind(runGroup.textProperty());

            fileGridPane.add(currFile, 0, index);
            fileGridPane.add(runGroup, 1, index);
            index++;
        }
    }

    /**
     * Remove the SARCC drivers and remove the lowercase "m" from MARRS classes
     */
    private void processCSVs() {
        CsvParserSettings settings = new CsvParserSettings();
        settings.setLineSeparatorDetectionEnabled(true);
        settings.setNullValue("");
        settings.setEmptyValue("");
        CsvParser parser = new CsvParser(settings);
        try {
            for (String currentCSV : pathToCSVs) {
                List<String[]> output = new ArrayList<>();
                // Search the header row for the class column
                int classIndex = 0;
                List<String[]> allRows = parser.parseAll(new FileReader(currentCSV));
                for (int i = 0; i < allRows.get(0).length; i++) {
                    if (allRows.get(0)[i].toLowerCase().equals("class")) {
                        classIndex = i;
                        break;
                    }
                }
                output.add(allRows.get(0));

                // find the MARRS drivers as designated by the "m" in front of the class name
                for (String[] currDriver : allRows.subList(0, allRows.size())) {
                    if (currDriver[classIndex].startsWith("m")) {
                        currDriver[classIndex] = currDriver[classIndex].substring(1);
                        output.add(currDriver);
                    }
                }

                // Delete the old file and create a new file with the correct name
                File csvToDelete = new File(currentCSV);
                csvToDelete.delete();

                StringBuilder newFileName = new StringBuilder(directoryPath);
                newFileName.append("\\Race-");
                newFileName.append(raceId.get());
                newFileName.append("-Group-");
                newFileName.append(runGroups.get(currentCSV).get());
                newFileName.append("-results.csv");

                File outputCSV = new File(newFileName.toString());

                // Write the new csv
                Writer outputWriter =
                        new FileWriter(outputCSV);

                CsvWriterSettings writerSettings = new CsvWriterSettings();
                writerSettings.setQuoteAllFields(true);
                CsvWriter writer = new CsvWriter(outputWriter, writerSettings);
                writer.writeStringRowsAndClose(output);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Check all the text fields to see if they are filled and the user can rename the files */
    private boolean validateTextFields() {
        return raceId.isNotEmpty().get()
                && runGroups.entrySet().stream().allMatch(k -> k.getValue().isNotEmpty().get());
    }
}
