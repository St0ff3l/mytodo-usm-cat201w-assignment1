package com.mytodo;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import java.io.IOException;
import java.net.URL;

public class AboutDialogController {

    @FXML private DialogPane aboutPane;
    @FXML private Label headerLabel;
    @FXML private Label contentLabel;
    @FXML private HBox separatorBox;
    @FXML private Button okButton;

    // New static method for MainController to call
    public static void showAboutDialog(Window owner) {
        try {
            FXMLLoader loader = new FXMLLoader(AboutDialogController.class.getResource("/com/mytodo/AboutDialogView.fxml"));
            DialogPane aboutPane = loader.load();

            AboutDialogController controller = loader.getController();
            controller.setMessage(
                    "CAT201 Integrated Software Development Workshop Assignment I",
                    "Version: v1.0.1 (JavaFX)\nFeatures: Task Management, Search & Filter, JSON I/O\nTeam: \nCHEN ZEKAI 23101653\nZHANG JUN 23101903\nZHANG YIFEI 23101912"
            );

            Stage aboutStage = new Stage();
            aboutStage.setTitle("About mytodo-usm-cat201w-assignment1");
            aboutStage.setScene(new Scene(aboutPane));
            aboutStage.initOwner(owner);
            aboutStage.initModality(Modality.APPLICATION_MODAL);
            aboutStage.setResizable(false);
            aboutStage.showAndWait();

        } catch (IOException e) {
            System.err.println("Error loading About Dialog: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void initialize() {
        try {
            URL cssUrl = getClass().getResource("/com/mytodo/Main.css");
            if (cssUrl != null) {
                aboutPane.getStylesheets().add(cssUrl.toExternalForm());
            } else {
                System.err.println("FATAL (AboutDialog): Main.css not found!");
            }
        } catch (Exception e) {
            System.err.println("Error loading CSS in AboutDialogController: " + e.getMessage());
            e.printStackTrace();
        }

        okButton.setOnAction(event -> closeDialog());
    }

    private void closeDialog() {
        Stage stage = (Stage) aboutPane.getScene().getWindow();
        stage.close();
    }

    private void setMessage(String header, String content) {
        if (header != null && !header.isEmpty()) {
            headerLabel.setText(header);
            headerLabel.setVisible(true);
            separatorBox.setVisible(true);
        } else {
            headerLabel.setVisible(false);
            separatorBox.setVisible(false);
        }
        contentLabel.setText(content);
    }
}
