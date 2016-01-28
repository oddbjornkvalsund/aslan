package no.nixx.aslan.ui.components;

import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.Collections;

import static java.util.stream.Collectors.joining;

public class TextFlowBufferItem extends TextFlow implements BufferItem {

    public TextFlowBufferItem(Text... texts) {
        Collections.addAll(getChildren(), texts);
    }

    @Override
    public String getText() {
        return getChildren().stream().map(node -> ((Text) node).getText()).collect(joining());
    }
}