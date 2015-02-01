package no.nixx.aslan.ui;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import no.nixx.aslan.core.ExecutableLocatorImpl;
import no.nixx.aslan.core.PipelineExecutorImpl;
import no.nixx.aslan.core.completion.CompletionResult;
import no.nixx.aslan.core.completion.Completor;
import no.nixx.aslan.pipeline.ParseException;
import no.nixx.aslan.pipeline.PipelineParser;
import no.nixx.aslan.pipeline.model.Pipeline;
import no.nixx.aslan.ui.components.LabelOutputStream;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static javafx.application.Platform.runLater;
import static javafx.scene.input.KeyCode.L;
import static javafx.scene.input.KeyCode.TAB;
import static javafx.scene.layout.HBox.setHgrow;
import static javafx.scene.layout.Priority.ALWAYS;
import static no.nixx.aslan.core.utils.StringUtils.join;

public class AslanShell extends VBox {

    private final ExecutorService threadPool = Executors.newFixedThreadPool(8);

    // TODO: Create a proper type for the history
    private final List<String> history = new ArrayList<>();

    private final PipelineParser parser = new PipelineParser();
    private final ObservableExecutionContext executionContext = new ObservableExecutionContext();

    private final Background transparentBackground = new Background(new BackgroundFill(Color.TRANSPARENT, null, null));
    private final Border transparentBorder = new Border(new BorderStroke(Color.TRANSPARENT, BorderStrokeStyle.NONE, CornerRadii.EMPTY, BorderWidths.EMPTY));

    private final TextField inputTextField; // TODO: This needs to be a separate class
    private final VBox console;
    private final ScrollPane consoleScrollPane;
    private final Label inputPrompt;

    public AslanShell() {
        console = createConsole();
        consoleScrollPane = createConsoleScrollPane(console);
        inputPrompt = createInputPrompt();
        inputTextField = createInputTextField();

        getChildren().add(new VBox(consoleScrollPane, new HBox(inputPrompt, inputTextField)));

        executionContext.setCurrentWorkingDirectory(System.getProperty("user.dir"));
        runLater(inputTextField::requestFocus);

        // DEBUG:
//        this.setBackground(new Background(new BackgroundFill(Color.RED, null, null)));
    }

    private Label createInputPrompt() {
        final Label label = new Label("> ");
        undecorate(label);

        executionContext.currentWorkingDirectoryProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                // TODO: Do this as a more elegant binding
                final File newCwd = new File(newValue);
                runLater(() -> label.setText(newCwd.getName() + "> "));
            }
        });
        return label;
    }

    private VBox createConsole() {
        final VBox vBox = new VBox();
        undecorate(vBox);
        return vBox;
    }

    private ScrollPane createConsoleScrollPane(VBox console) {
        final ScrollPane scrollPane = new ScrollPane(console);
        undecorate(scrollPane);

        // Hide consoleScrollPane when console is empty and scroll consoleScrollPane to bottom when console changes size
        scrollPane.visibleProperty().bind(Bindings.isNotEmpty(console.getChildren()));
        scrollPane.managedProperty().bind(Bindings.isNotEmpty(console.getChildren()));
        console.heightProperty().addListener((observable, oldValue, newValue) -> scrollPane.setVvalue(1));

        return scrollPane;
    }

    private TextField createInputTextField() {
        final TextField textField = new TextField();
        undecorate(textField);
        setHgrow(textField, ALWAYS);
        textField.setOnAction(action -> executeCommand()); // TODO: This should not run on the JavaFX event thread
        textField.setOnKeyPressed((event) -> handleKeyPressed(event));

        return textField;
    }

    private void undecorate(Region region) {
        region.setBackground(transparentBackground);
        region.setBorder(transparentBorder);
        region.setPadding(Insets.EMPTY);

        // Ref: https://community.oracle.com/thread/3538169
        if (region instanceof ScrollPane) {
            final URL stylesheetURL = AslanShell.class.getResource("/style.css");
            region.getStylesheets().add(stylesheetURL.toExternalForm());
        }
    }

    private void handleKeyPressed(KeyEvent event) {
        if (event.isControlDown() && event.getCode().equals(L)) {
            clearConsole();
        } else if (event.getCode().equals(TAB)) {
            tabComplete();
            event.consume();
        }
    }

    private void tabComplete() {
        final String command = inputTextField.getText();
        final int tabPosition = inputTextField.getCaretPosition();

        final Completor completor = new Completor();
        final CompletionResult result = completor.getCompletions(command, tabPosition, new ExecutableLocatorImpl());

        if (!result.completionCandidates.isEmpty()) {
            output(join(result.completionCandidates, " "));
        }

        inputTextField.setText(result.text);
        inputTextField.positionCaret(result.tabPosition);
    }

    private void clearConsole() {
        console.getChildren().clear();
    }

    private void executeCommand() {
        final String command = getCommand();

        final Pipeline pipeline;
        try {
            pipeline = parser.parseCommand(command);
        } catch (ParseException parseException) {
            error(parseException.getMessage());
            return;
        }

        final InputStream in = new ByteArrayInputStream(new byte[0]);
        final OutputStream out = new LabelOutputStream(console, Color.BLACK, transparentBackground);
        final OutputStream err = new LabelOutputStream(console, Color.RED, new Background(new BackgroundFill(Color.TRANSPARENT, null, null)));

        final PipelineExecutorImpl pipelineExecutor = new PipelineExecutorImpl(threadPool, new ExecutableLocatorImpl(), in, out, err);

        try {
            pipelineExecutor.execute(executionContext, pipeline);
            history.add(command);
        } catch (Exception exception) {
            error(exception.getMessage()); // Should not happen very often. Never?
        }
    }

    private String getCommand() {
        final String command = inputTextField.getText();
        output("> " + command);
        inputTextField.clear();
        return command;
    }

    private void output(String string) {
        addToConsole(string, Color.BLACK);
    }

    private void error(String string) {
        addToConsole(string, Color.RED);
    }

    private void addToConsole(String string, Color color) {
        final Label label = new Label(string);
        label.setWrapText(true);
        label.setTextFill(color);
        runLater(() -> console.getChildren().add(label));
    }
}