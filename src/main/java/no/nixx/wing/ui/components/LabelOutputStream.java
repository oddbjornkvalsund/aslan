package no.nixx.wing.ui.components;

import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import no.nixx.wing.core.utils.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static javafx.application.Platform.runLater;

public class LabelOutputStream extends ByteArrayOutputStream {

    private final Pane parent;
    private final AtomicBoolean labelAddedToParent;
    private final Label label;

    public LabelOutputStream(Pane parent, Color textColor, Background background) {
        this.parent = parent;
        this.labelAddedToParent = new AtomicBoolean(false);
        this.label = new Label();
        this.label.setWrapText(true);
        this.label.setTextFill(textColor);
        this.label.setBackground(background);
    }

    @Override
    public void flush() throws IOException {
        final String string = this.toString();
        if (!string.isEmpty()) {
            if (labelNotAddedToParent()) {
                runLater(() -> parent.getChildren().add(label));
            }
            appendText(string);
        }

        reset();
    }

    @Override
    public void close() throws IOException {
        flush();
        removeTrailingNewlines();
    }

    private boolean labelNotAddedToParent() {
        return labelAddedToParent.compareAndSet(false, true);
    }

    private void removeTrailingNewlines() {
        label.setText(StringUtils.removeTrailingNewlines(label.getText()));
    }

    private void appendText(String string) {
        label.setText(label.getText() + string);
    }
}