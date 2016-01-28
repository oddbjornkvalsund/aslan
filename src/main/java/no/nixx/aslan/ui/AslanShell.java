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
import no.nixx.aslan.core.ExecutableLocatorImpl;
import no.nixx.aslan.core.PipelineExecutorImpl;
import no.nixx.aslan.core.WorkingDirectoryImpl;
import no.nixx.aslan.core.completion.CompletionResult;
import no.nixx.aslan.core.completion.Completor;
import no.nixx.aslan.pipeline.ParseException;
import no.nixx.aslan.pipeline.PipelineParser;
import no.nixx.aslan.pipeline.model.Pipeline;
import no.nixx.aslan.ui.components.BufferItem;
import no.nixx.aslan.ui.components.LineFragmentOutputStream;
import no.nixx.aslan.ui.components.ObservableCompositeList;
import no.nixx.aslan.ui.components.TextFlowBufferItem;
import org.fxmisc.flowless.Cell;
import org.fxmisc.flowless.VirtualFlow;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.System.currentTimeMillis;
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

    // TODO: Remove before commiting
    private static int scrollToInputInvocationCount = 0;
    private final Background transparentBackground = new Background(new BackgroundFill(Color.TRANSPARENT, null, null));
    private final Border transparentBorder = new Border(new BorderStroke(Color.TRANSPARENT, BorderStrokeStyle.NONE, CornerRadii.EMPTY, BorderWidths.EMPTY));
    private final InputBox inputBox;
    private final Label prompt;
    private final TextField input;
    private final VirtualFlow<BufferItem, Cell<BufferItem, Node>> buffer;
    private final ObservableList<TextFlowBufferItem> bufferItems;
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
                    node = inputBox;
                    inputBoxCell = this;
                } else if (item instanceof TextFlowBufferItem) {
                    // Keeping all the items in the list as TextFlow+Text seems to be heavy on memory
                    // Consider storing the items as List<String>+String and reusing TextFlow+Text from a object pool
                    node = (TextFlowBufferItem) item;
                } else {
                    throw new RuntimeException("WTF?");
                }
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
                    System.out.println("Redirecting to TextField: " + keyEvent);
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
        final OutputStream out = new LineFragmentOutputStream<>(bufferItems, new TextFlowLineFragmentAdapter(bufferItems, BLACK));
        final OutputStream err = new LineFragmentOutputStream<>(bufferItems, new TextFlowLineFragmentAdapter(bufferItems, RED));

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
        final Text commandText = new Text(prompt.getText() + command);
        commandText.setFill(BLACK);
        bufferItems.add(new TextFlowBufferItem(commandText));
        bufferItems.add(new TextFlowBufferItem());
    }

    private void tabComplete() {
        final String command = input.getText();
        final int tabPosition = input.getCaretPosition();

        final Completor completor = new Completor();
        final CompletionResult result = completor.getCompletions(command, tabPosition, new ExecutableLocatorImpl(), executionContextFactory.createExecutionContext());

        if (result.hasCompletionCandidates() && isDoubleTab()) {
            bufferItems.add(new TextFlowBufferItem(new Text(join(result.completionCandidates, " ")))); // TODO: Manglar farge
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
        runLater(() -> {
            buffer.showAsLast(bufferItemsWithInput.indexOf(inputBox));
            System.out.println("Scrolled: " + ++scrollToInputInvocationCount);
        });
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

    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    private final Label prompt;
    private final TextField input;

    public InputBox(Label prompt, TextField input) {
        this.prompt = prompt;
        this.input = input;
        HBox.setHgrow(prompt, ALWAYS);
        HBox.setHgrow(input, ALWAYS);
        getChildren().addAll(prompt, input);
    }

    @Override
    public String getText() {
        return input.getText();
    }
}

// TODO: Extract to standalone class
class TextFlowLineFragmentAdapter implements LineFragmentOutputStream.Adapter<TextFlowBufferItem, Text> {
    private final List<TextFlowBufferItem> list;
    private final Color color;

    public TextFlowLineFragmentAdapter(List<TextFlowBufferItem> list, Color color) {
        this.list = list;
        this.color = color;
    }

    @Override
    public TextFlowBufferItem createLine() {
        return new TextFlowBufferItem();
    }

    @Override
    public Text createFragment(String content) {
        final Text text = new Text(content);
        text.setFill(color);
        return text;
    }

    @Override
    public boolean lineIsEmpty(TextFlowBufferItem textFlow) {
        return textFlow.getChildren().isEmpty();
    }

    @Override
    public boolean fragmentIsEmpty(Text text) {
        return text.getText().isEmpty();
    }

    @Override
    public void addFragmentToLine(Text text, TextFlowBufferItem textFlow) {
        runLater(() -> textFlow.getChildren().add(text));
    }

    @Override
    public void addLinesToList(List<TextFlowBufferItem> textFlows) {
        runLater(() -> list.addAll(textFlows));
    }

    @Override
    public void removeLineFromList(TextFlowBufferItem textFlow) {
        runLater(() -> list.remove(textFlow));
    }
}