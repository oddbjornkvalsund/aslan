package no.nixx.aslan.ui.components;

import javafx.scene.paint.Color;

public class TextBufferItem implements BufferItem {

    public final String text;
    public final Color textColor;

    public TextBufferItem(String text, Color textColor) {
        this.text = text;
        this.textColor = textColor;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return "TextBufferItem{" +
                "text='" + text + '\'' +
                ", textColor=" + textColor +
                '}';
    }
}