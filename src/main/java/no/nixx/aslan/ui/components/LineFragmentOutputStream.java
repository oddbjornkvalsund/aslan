package no.nixx.aslan.ui.components;

import javafx.collections.ObservableList;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.Collections.singletonList;
import static no.nixx.aslan.core.utils.ListUtils.lastOf;

public class LineFragmentOutputStream<Line, Fragment> extends ByteArrayOutputStream {

    private final ObservableList<Line> list;
    private final Supplier<Line> lineFactory;
    private final Function<String, Fragment> fragmentFactory;
    private final Predicate<Line> lineIsEmptyPredicate;
    private final Predicate<Fragment> fragmentIsEmptyPredicate;
    private final BiPredicate<Line, Fragment> addFragmentToLinePredicate;
    private final Predicate<List<Line>> addLinesToListFunction;
    private final Predicate<Line> removeLineFromListFunction;

    public LineFragmentOutputStream(ObservableList<Line> list, Supplier<Line> lineFactory, Function<String, Fragment> fragmentFactory, Predicate<Line> lineIsEmptyPredicate, Predicate<Fragment> fragmentIsEmptyPredicate, BiPredicate<Line, Fragment> addFragmentToLinePredicate, Predicate<Line> removeLineFromListFunction, Predicate<List<Line>> addLinesToListFunction) {
        this.list = list;
        this.lineFactory = lineFactory;
        this.fragmentFactory = fragmentFactory;
        this.lineIsEmptyPredicate = lineIsEmptyPredicate;
        this.fragmentIsEmptyPredicate = fragmentIsEmptyPredicate;
        this.addFragmentToLinePredicate = addFragmentToLinePredicate;
        this.addLinesToListFunction = addLinesToListFunction;
        this.removeLineFromListFunction = removeLineFromListFunction;

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
                if (c == '\n') {
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

    private boolean addFragmentToLine(Fragment fragment, Line currentLine) {
        return addFragmentToLinePredicate.test(currentLine, fragment);
    }

    private boolean addLinesToList(List<Line> newLines) {
        return addLinesToListFunction.test(newLines);
    }

    private boolean removeLineFromList(Line lastLine) {
        return removeLineFromListFunction.test(lastLine);
    }

    private boolean lineIsEmpty(Line lastLine) {
        return lineIsEmptyPredicate.test(lastLine);
    }

    private boolean fragmentIsEmpty(Fragment fragment) {
        return fragmentIsEmptyPredicate.test(fragment);
    }

    private Line createNewLine() {
        return lineFactory.get();
    }

    private Fragment createNewFragment(String text) {
        return fragmentFactory.apply(text);
    }
}