package rename;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        RenamePane renamePane= new RenamePane();
        renamePane.BuildUI();
        primaryStage.setTitle("VIR MARRS Rename Tool");
        primaryStage.setScene(new Scene(renamePane, 1200, 800));
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
