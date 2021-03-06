package no.nixx.aslan.ui.component.linefragment;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static no.nixx.aslan.core.utils.ListUtils.lastOf;

public class LineFragmentOutputStream extends ByteArrayOutputStream {

    private final List<Line> list;
    private final Adapter adapter;

    public LineFragmentOutputStream(List<Line> list, Adapter adapter) {
        this.list = list;
        this.adapter = adapter;

        if (list.isEmpty()) {
            addLinesToList(singletonList(createNewLine()));
        }
    }

    @Override
    public void flush() {
        final List<Line> newLines = new ArrayList<>();
        Line currentLine = lastOf(list);

        if (count > 0) {
            final String content = toString();
            final StringBuilder buffer = new StringBuilder();
            for (char c : content.toCharArray()) {
                if (c == '\r') {
                    // Skip for now
                } else if (c == '\n') {
                    final Fragment fragment = createNewFragment(buffer.toString());
                    if (!fragmentIsEmpty(fragment)) {
                        addFragmentToLine(fragment, currentLine);
                    }

                    final Line newLine = createNewLine();
                    newLines.add(newLine);
                    currentLine = newLine;

                    buffer.setLength(0);
                } else {
                    buffer.append(c);
                }
            }

            final Fragment fragment = createNewFragment(buffer.toString());
            if (!fragmentIsEmpty(fragment)) {
                addFragmentToLine(fragment, currentLine);
            }

            addLinesToList(newLines);

            reset();
        }
    }

    @Override
    public void close() {
        removeLastLineIfEmpty();
    }

    private void removeLastLineIfEmpty() {
        adapter.removeLastLineIfEmpty();
    }

    private void addFragmentToLine(Fragment fragment, Line line) {
        adapter.addFragmentToLine(fragment, line);
    }

    private void addLinesToList(List<Line> lines) {
        adapter.addLinesToList(lines);
    }

    private boolean fragmentIsEmpty(Fragment fragment) {
        return adapter.fragmentIsEmpty(fragment);
    }

    private Line createNewLine() {
        return adapter.createLine();
    }

    private Fragment createNewFragment(String text) {
        return adapter.createFragment(text);
    }

    // This adapter allows the list to be manipulated synchronously or asynchronously

    public interface Adapter {
        default Line createLine() {
            return new Line();
        }

        default Fragment createFragment(String text) {
            return new Fragment(text);
        }

        default boolean lineIsEmpty(Line line) {
            return line.isEmpty();
        }

        default boolean fragmentIsEmpty(Fragment fragment) {
            return fragment.getText().isEmpty();
        }

        void addFragmentToLine(Fragment fragment, Line line);

        void addLinesToList(List<Line> lines);

        void removeLastLineIfEmpty();
    }
}