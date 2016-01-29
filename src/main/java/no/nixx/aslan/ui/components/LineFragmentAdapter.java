package no.nixx.aslan.ui.components;

import javafx.scene.paint.Color;

import java.util.List;

import static javafx.application.Platform.runLater;

public class LineFragmentAdapter implements LineFragmentOutputStream.Adapter<Line, Fragment> {
    protected final List<Line> list;
    protected final Color color;

    public LineFragmentAdapter(List<Line> list, Color color) {
        this.list = list;
        this.color = color;
    }

    @Override
    public Line createLine() {
        return new Line();
    }

    @Override
    public Fragment createFragment(String text) {
        return new Fragment(text, color);
    }

    @Override
    public boolean lineIsEmpty(Line line) {
        return line.isEmpty();
    }

    @Override
    public boolean fragmentIsEmpty(Fragment fragment) {
        return fragment.getText().isEmpty();
    }

    @Override
    public void addFragmentToLine(Fragment fragment, Line line) {
        runLater(() -> line.add(fragment));
    }

    @Override
    public void addLinesToList(List<Line> lines) {
        runLater(() -> list.addAll(lines));
    }

    @Override
    public void removeLineFromList(Line line) {
        runLater(() -> list.remove(line));
    }
}