package no.nixx.aslan.ui;

import javafx.application.Application;
import javafx.stage.Stage;

public class Aslan extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        new AslanWindow();
    }
}
