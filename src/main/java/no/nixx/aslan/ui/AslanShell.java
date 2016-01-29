package no.nixx.aslan.ui;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import no.nixx.aslan.core.ExecutableLocatorImpl;
import no.nixx.aslan.core.PipelineExecutorImpl;
import no.nixx.aslan.core.WorkingDirectoryImpl;
import no.nixx.aslan.core.completion.CompletionResult;
import no.nixx.aslan.core.completion.Completor;
import no.nixx.aslan.pipeline.ParseException;
import no.nixx.aslan.pipeline.PipelineParser;
import no.nixx.aslan.pipeline.model.Pipeline;
import no.nixx.aslan.ui.components.BufferItem;
import no.nixx.aslan.ui.components.Fragment;
import no.nixx.aslan.ui.components.Line;
import no.nixx.aslan.ui.components.LineFragmentAdapter;
import no.nixx.aslan.ui.components.LineFragmentOutputStream;
import no.nixx.aslan.ui.components.ObservableCompositeList;
import org.fxmisc.flowless.Cell;
import org.fxmisc.flowless.VirtualFlow;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.System.currentTimeMillis;
import static java.util.stream.Collectors.toList;
import static javafx.application.Platform.runLater;
import static javafx.collections.FXCollections.observableArrayList;
import static javafx.scene.input.KeyCode.L;
import static javafx.scene.input.KeyCode.TAB;
import static javafx.scene.layout.Priority.ALWAYS;
import static javafx.scene.paint.Color.BLACK;
import static javafx.scene.paint.Color.RED;
import static no.nixx.aslan.core.utils.StringUtils.join;
import static org.fxmisc.flowless.VirtualFlow.createVertical;

public class AslanShell extends VBox {

    private final Background transparentBackground = new Background(new BackgroundFill(Color.TRANSPARENT, null, null));
    private final Border transparentBorder = new Border(new BorderStroke(Color.TRANSPARENT, BorderStrokeStyle.NONE, CornerRadii.EMPTY, BorderWidths.EMPTY));
    private final InputBox inputBox;
    private final Label prompt;
    private final TextField input;
    private final VirtualFlow<BufferItem, Cell<BufferItem, Node>> buffer;
    private final ObservableList<Line> bufferItems;
    private final ObservableList<BufferItem> bufferItemsWithInput;
    private final ExecutorService threadPool = Executors.newFixedThreadPool(8);
    private final PipelineParser parser = new PipelineParser();
    private final ObservableExecutionContextFactory executionContextFactory = new ObservableExecutionContextFactory(new WorkingDirectoryImpl(System.getProperty("user.dir")));
    private Cell<BufferItem, Node> inputBoxCell;
    private long previousKeyTimestamp = Long.MIN_VALUE;
    private KeyCode previousKeyCode = KeyCode.UNDEFINED;

    public AslanShell() {
        // Prompt
        prompt = new Label();
        // TODO: Messy, re-think this whole concept
        runLater(() -> setLabelFromPath(prompt, executionContextFactory.workingDirectoryProperty().get().asPath()));
        executionContextFactory.workingDirectoryProperty().addListener((observable, oldValue, newValue) -> {
            runLater(() -> setLabelFromPath(prompt, newValue.asPath()));
        });

        // Input
        input = new TextField();
        undecorate(input);
        input.setOnKeyPressed(this::handleKeyPressed);
        runLater(input::requestFocus);
        inputBox = new InputBox(prompt, input);

        // Buffer
        bufferItems = observableArrayList();
        bufferItemsWithInput = new ObservableCompositeList<>((ObservableList) bufferItems, FXCollections.<BufferItem>observableArrayList(inputBox));
        buffer = createVertical(bufferItemsWithInput, bufferItem -> new Cell<BufferItem, Node>() {

            private Node node;
            private TextFlow textFlow = new TextFlow();

            {
                updateItem(bufferItem);
            }

            @Override
            public Node getNode() {
                return node;
            }

            @Override
            public boolean isReusable() {
                return true;
            }

            @Override
            public void updateItem(BufferItem item) {
                if (item == inputBox) {
                    textFlow.getChildren().clear();
                    node = inputBox;
                    inputBoxCell = this;
                } else if (item instanceof Line) {
                    final Line line = (Line) item;
                    textFlow.getChildren().setAll(line.stream().map(this::createText).collect(toList()));
                    node = textFlow;
                }
            }

            private Text createText(Fragment fragment) {
                final Text text = new Text(fragment.getText());
                text.setFill(fragment.getColor());
                return text;
            }

            @Override
            public void reset() {
                node = null;
            }
        });

        final EventHandler<KeyEvent> sceneKeyEventEventHandler = (keyEvent) -> {
            final EventTarget target = keyEvent.getTarget();
            final Scene scene = AslanShell.this.getScene();
            if (isAncestorOrScene(this, target)) {
                final boolean isKeyPressed = KeyEvent.KEY_PRESSED.equals(keyEvent.getEventType());
                switch (keyEvent.getCode()) {
                    case PAGE_UP:
                    case PAGE_DOWN:
                    case HOME:
                    case END:
                        if (isKeyPressed) {
                            handlePageUpDownHomeEnd(keyEvent);
                        }
                    case CONTROL:
                    case SHIFT:
                    case ALT:
                    case ALT_GRAPH:
                    case META:
                        keyEvent.consume();
                        break;
                }

                if (!keyEvent.isConsumed()) {
                    scrollToInput();
                    focusTextFieldAndFireKeyEvent(input, scene, keyEvent);
                }
            }
        };

        sceneProperty().addListener((scenePropery, previousScene, newScene) -> {
            if (newScene == null) {
                previousScene.removeEventFilter(KeyEvent.ANY, sceneKeyEventEventHandler);
            } else {
                newScene.addEventFilter(KeyEvent.ANY, sceneKeyEventEventHandler);
            }
        });

        buffer.visibleCells().addListener((ListChangeListener<Cell<BufferItem, Node>>) change -> {
            while (change.next()) {
                if (change.wasAdded() && change.getAddedSubList().contains(inputBoxCell)) {
                    runLater(input::requestFocus);
                }
            }
        });

        bufferItems.addListener((ListChangeListener<? super BufferItem>) c -> scrollToInput());
        input.addEventHandler(KeyEvent.KEY_PRESSED, this::handlePageUpDownHomeEnd);
        input.setOnAction(this::executeCommand);

        VBox.setVgrow(buffer, ALWAYS);
        getChildren().add(buffer);
    }

    private void setLabelFromPath(Label label, Path path) {
        if (path.getFileName() == null) {
            label.setText(path + "> ");
        } else {
            label.setText(path.getFileName() + "> ");
        }
    }

    private void executeCommand(ActionEvent actionEvent) {
        final String command = input.getText();

        final Pipeline pipeline;
        try {
            pipeline = parser.parseCommand(command);
        } catch (ParseException parseException) {
            throw new RuntimeException(parseException);
        }

        addPromptAndCommandToBuffer(command);
        input.setText("");

        final InputStream in = new ByteArrayInputStream(new byte[0]);
        final OutputStream out = new LineFragmentOutputStream<>(bufferItems, new LineFragmentAdapter(bufferItems, BLACK));
        final OutputStream err = new LineFragmentOutputStream<>(bufferItems, new LineFragmentAdapter(bufferItems, RED));

        final PipelineExecutorImpl pipelineExecutor = new PipelineExecutorImpl(threadPool, new ExecutableLocatorImpl(), executionContextFactory, in, out, err);

        try {
            pipelineExecutor.execute(pipeline);
//            history.add(command);
        } catch (Exception exception) {
            final PrintWriter errPrintWriter = new PrintWriter(err);
            errPrintWriter.println(exception.getMessage());
            errPrintWriter.flush();
            throw new RuntimeException(exception);
        } finally {
            try {
                out.flush();
                out.close();
                err.flush();
                err.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void addPromptAndCommandToBuffer(String command) {
        bufferItems.addAll(
                new Line(new Fragment(prompt.getText() + command, BLACK)),
                new Line()
        );
    }

    private void tabComplete() {
        final String command = input.getText();
        final int tabPosition = input.getCaretPosition();

        final Completor completor = new Completor();
        final CompletionResult result = completor.getCompletions(command, tabPosition, new ExecutableLocatorImpl(), executionContextFactory.createExecutionContext());

        if (result.hasCompletionCandidates() && isDoubleTab()) {
            bufferItems.add(new Line(new Fragment(join(result.completionCandidates, " "), BLACK)));
        }

        input.setText(result.text);
        input.positionCaret(result.tabPosition);
    }

    private boolean isDoubleTab() {
        return previousKeyCode == KeyCode.TAB && millisSinceLastKeyPress() < 1000;
    }

    private long millisSinceLastKeyPress() {
        return currentTimeMillis() - previousKeyTimestamp;
    }

    private void handlePageUpDownHomeEnd(KeyEvent keyEvent) {
        switch (keyEvent.getCode()) {
            case HOME:
                if (keyEvent.isControlDown()) {
                    buffer.scrollY(-Double.MAX_VALUE);
                }
                break;
            case END:
                if (keyEvent.isControlDown()) {
                    buffer.scrollY(Double.MAX_VALUE);
                }
                break;
            case PAGE_UP:
                buffer.scrollY(-buffer.getHeight());
                break;
            case PAGE_DOWN:
                buffer.scrollY(buffer.getHeight());
                break;
        }
    }

    private void handleKeyPressed(KeyEvent event) {
        final KeyCode keyCode = event.getCode();
        final boolean controlIsDown = event.isControlDown();
        if (controlIsDown && keyCode.equals(L)) {
            bufferItems.clear();
            event.consume();
        } else if (controlIsDown && keyCode.equals(KeyCode.C)) {
            input.clear();
            event.consume();
        } else if (keyCode.equals(TAB)) {
            tabComplete();
            event.consume();
        }

        previousKeyTimestamp = currentTimeMillis();
        previousKeyCode = keyCode;
    }

    private void scrollToInput() {
        runLater(() -> buffer.showAsLast(bufferItemsWithInput.indexOf(inputBox)));
    }

    private void focusTextFieldAndFireKeyEvent(final TextField textField, final Scene scene, final KeyEvent keyEvent) {
        // Wait for the TextField to be focused before firing the KeyEvent, as it will not be handled if the TextField is not focused
        textField.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean wasFocused, Boolean isFocused) {
                if (isFocused) {
                    runLater(() -> {
                        textField.end();
                        textField.fireEvent(keyEvent.copyFor(scene, textField));
                    });
                    observable.removeListener(this);
                }
            }
        });

        textField.requestFocus();
    }

    private void undecorate(Region region) {
        region.setBackground(transparentBackground);
        region.setBorder(transparentBorder);
        region.setPadding(Insets.EMPTY);
    }

    @SuppressWarnings("SimplifiableIfStatement")
    private boolean isAncestorOrScene(Node node, EventTarget target) {
        if (node == null || node.equals(node.getParent())) {
            return false;
        } else if (target.equals(node.getScene())) {
            return true;
        } else if (target.equals(node.getParent())) {
            return true;
        } else {
            return isAncestorOrScene(node.getParent(), target);
        }
    }
}

class InputBox extends HBox implements BufferItem {

    public InputBox(Label prompt, TextField input) {
        HBox.setHgrow(prompt, ALWAYS);
        HBox.setHgrow(input, ALWAYS);
        getChildren().addAll(prompt, input);
    }
}