package no.nixx.aslan.ui;

import javafx.scene.Scene;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class AslanWindow {
    public AslanWindow() {
        final Background transparentBackground = new Background(new BackgroundFill(Color.TRANSPARENT, null, null));

        final BorderPane borderPane = new BorderPane();
        borderPane.setBackground(transparentBackground);

        final AslanMenuBar aslanMenuBar = new AslanMenuBar();
        borderPane.setTop(aslanMenuBar);

        final TabPane tabPane = new TabPane();
        tabPane.setBackground(transparentBackground);
        tabPane.getTabs().add(new AslanTab("Shell 1"));
        borderPane.setCenter(tabPane);

        final Scene scene = new Scene(borderPane);
        scene.setFill(Color.TRANSPARENT);

        final Stage stage = new Stage();
        stage.setTitle("Aslan");
        stage.initStyle(StageStyle.DECORATED); // TODO: We really want something transparent
        stage.setScene(scene);
        stage.setWidth(800);
        stage.setHeight(600);
        stage.setOnCloseRequest(event -> System.exit(0)); // TODO: Crude
        stage.show();
    }
}