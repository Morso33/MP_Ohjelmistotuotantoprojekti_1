package com.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.NodeOrientation;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ResourceBundle;

/**
 * JavaFX application of the localized Temperature Converter. Loads the window from
 * main-view.fxml with the texts of the chosen language and reloads it when the user
 * picks another language from the drop-down.
 */
public class Main extends Application {

    static final String VIEW = "main-view.fxml";

    private final DBConnection database;
    private Stage stage;
    private MainController controller;

    /** Used by JavaFX: connects to the database given by DB_URL, DB_USER and DB_PASSWORD. */
    public Main() {
        this(DBConnection.fromEnvironment());
    }

    Main(DBConnection database) {
        this.database = database;
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws IOException {
        this.stage = stage;
        stage.setScene(new Scene(loadView(Language.ENGLISH), 820, 640));
        stage.show();
    }

    /** Shows the window in another language, keeping what the user had typed and chosen. */
    void switchLanguage(Language language) {
        MainController.State state = controller.getState();
        try {
            stage.getScene().setRoot(loadView(language));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        controller.restoreState(state);
    }

    /**
     * Loads main-view.fxml with the resource bundle of the language. The FXMLLoader fills in
     * every "%key" of the FXML file from the bundle. Arabic windows are mirrored right to left.
     */
    Parent loadView(Language language) throws IOException {
        ResourceBundle bundle = language.getBundle();
        FXMLLoader loader = new FXMLLoader(Main.class.getResource(VIEW), bundle);
        loader.setControllerFactory(type -> new MainController(database, language, this::switchLanguage));
        Parent root = loader.load();
        root.setNodeOrientation(language.isRightToLeft()
                ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);
        controller = loader.getController();
        stage.setTitle(bundle.getString("app.title"));
        return root;
    }

    /** The controller of the window currently shown. */
    MainController getController() {
        return controller;
    }
}
