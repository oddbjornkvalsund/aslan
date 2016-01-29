package no.nixx.aslan.ui.components;

import javafx.scene.paint.Color;

public class Fragment {
    final String text;
    final Color color;

    public Fragment(String text) {
        this(text, Color.BLACK);
    }

    public Fragment(String text, Color color) {
        this.text = text;
        this.color = color;
    }

    public String getText() {
        return text;
    }

    public Color getColor() {
        return color;
    }

    @SuppressWarnings("SimplifiableIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Fragment fragment = (Fragment) o;

        if (text != null ? !text.equals(fragment.text) : fragment.text != null) return false;
        return color != null ? color.equals(fragment.color) : fragment.color == null;

    }

    @Override
    public int hashCode() {
        int result = text != null ? text.hashCode() : 0;
        result = 31 * result + (color != null ? color.hashCode() : 0);
        return result;
    }
}
