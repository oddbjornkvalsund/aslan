package no.nixx.aslan.ui.component.linefragment;

import javafx.scene.paint.Color;

import java.util.List;

import static javafx.application.Platform.runLater;
import static no.nixx.aslan.core.utils.ListUtils.lastOf;

public class LineFragmentAdapter implements LineFragmentOutputStream.Adapter {
    protected final List<Line> list;
    protected final Color color;

    public LineFragmentAdapter(List<Line> list, Color color) {
        this.list = list;
        this.color = color;
    }

    @Override
    public Fragment createFragment(String text) {
        return new Fragment(text, color);
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
    public void removeLastLineIfEmpty() {
        runLater(() -> {
            final Line lastLine = lastOf(list);
            if (lineIsEmpty(lastLine)) {
                list.remove(lastLine);
            }
        });
    }
}