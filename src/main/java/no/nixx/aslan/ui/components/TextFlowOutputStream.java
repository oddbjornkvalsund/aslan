package no.nixx.aslan.ui.components;

import javafx.collections.ObservableList;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static no.nixx.aslan.core.utils.ListUtils.lastOf;

public class TextFlowOutputStream<Line, Fragment> extends ByteArrayOutputStream {

    private final ObservableList<Line> list;
    private final Supplier<Line> lineFactory;
    private final Function<String, Fragment> fragmentFactory;
    private final Predicate<Line> lineIsEmptyPredicate;
    private final Predicate<Fragment> fragmentIsEmptyPredicate;
    private final BiPredicate<Line, Fragment> lineContainsFragmentPredicate;
    private final BiPredicate<Line, Fragment> addFragmentToLinePredicate;
    private final Function<Line, Boolean> addLineToListFunction;
    private final Function<Line, Boolean> removeLineFromListFunction;

    public TextFlowOutputStream(ObservableList<Line> list, Supplier<Line> lineFactory, Function<String, Fragment> fragmentFactory, Predicate<Line> lineIsEmptyPredicate, Predicate<Fragment> fragmentIsEmptyPredicate, BiPredicate<Line, Fragment> lineContainsFragmentPredicate, BiPredicate<Line, Fragment> addFragmentToLinePredicate, Function<Line, Boolean> addLineToListFunction, Function<Line, Boolean> removeLineFromListFunction) {
        this.list = list;
        this.lineFactory = lineFactory;
        this.fragmentFactory = fragmentFactory;
        this.lineIsEmptyPredicate = lineIsEmptyPredicate;
        this.fragmentIsEmptyPredicate = fragmentIsEmptyPredicate;
        this.lineContainsFragmentPredicate = lineContainsFragmentPredicate;
        this.addFragmentToLinePredicate = addFragmentToLinePredicate;
        this.addLineToListFunction = addLineToListFunction;
        this.removeLineFromListFunction = removeLineFromListFunction;

        if (list.isEmpty()) {
            appendLineToList(createNewLine());
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
        // TODO: This method should only cause one list change to the ObservableList

        if (count > 0) {
            final String content = toString();
            final StringBuilder buffer = new StringBuilder();
            for (char c : content.toCharArray()) {
                if (c == '\n') {
                    final Fragment fragment = createNewFragment(buffer.toString());
                    if (!fragmentIsEmpty(fragment)) {
                        appendFragmentToLastLineOfList(fragment);
                    }
                    appendLineToList(createNewLine());
                    buffer.setLength(0);
                } else {
                    buffer.append(c);
                }
            }

            final Fragment fragment = createNewFragment(buffer.toString());
            if (!fragmentIsEmpty(fragment)) {
                appendFragmentToLastLineOfList(fragment);
            }

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

    private Boolean appendLineToList(Line line) {
        return addLineToListFunction.apply(line);
    }

    private boolean appendFragmentToLastLineOfList(Fragment fragment) {
        return addFragmentToLinePredicate.test(lastOf(list), fragment);
    }

    private boolean appendFragmentToLastLineOfList(Fragment fragment, List<Line> list) {
        return addFragmentToLinePredicate.test(lastOf(list), fragment);
    }

    private Boolean removeLineFromList(Line lastLine) {
        return removeLineFromListFunction.apply(lastLine);
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