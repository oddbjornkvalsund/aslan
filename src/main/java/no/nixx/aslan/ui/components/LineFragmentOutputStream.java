package no.nixx.aslan.ui.components;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static no.nixx.aslan.core.utils.ListUtils.lastOf;

public class LineFragmentOutputStream<Line, Fragment> extends ByteArrayOutputStream {

    private final List<Line> list;
    private final Adapter<Line, Fragment> adapter;

    public LineFragmentOutputStream(List<Line> list, Adapter<Line, Fragment> adapter) {
        this.list = list;
        this.adapter = adapter;

        if (list.isEmpty()) {
            addLinesToList(singletonList(createNewLine()));
        }
    }

    // Only for test
    public void write(String string) {
        try {
            write(string.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void flush() throws IOException {
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
        final Line lastLine = lastOf(list);
        if (lineIsEmpty(lastLine)) {
            removeLineFromList(lastLine);
        }
    }

    private void addFragmentToLine(Fragment fragment, Line line) {
        adapter.addFragmentToLine(fragment, line);
    }

    private void addLinesToList(List<Line> lines) {
        adapter.addLinesToList(lines);
    }

    private void removeLineFromList(Line line) {
        adapter.removeLineFromList(line);
    }

    private boolean lineIsEmpty(Line line) {
        return adapter.lineIsEmpty(line);
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

    public interface Adapter<Line, Fragment> {
        Line createLine();

        Fragment createFragment(String text);

        boolean lineIsEmpty(Line line);

        boolean fragmentIsEmpty(Fragment fragment);

        void addFragmentToLine(Fragment fragment, Line line);

        void addLinesToList(List<Line> lines);

        void removeLineFromList(Line line);
    }
}