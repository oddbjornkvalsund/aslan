package no.nixx.aslan.ui.components;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableListBase;

import java.util.Arrays;
import java.util.List;

import static java.util.stream.IntStream.range;

public class ObservableCompositeList<T> extends ObservableListBase<T> {

    private final List<ObservableList<T>> lists;

    @SafeVarargs
    public ObservableCompositeList(ObservableList<T>... observableLists) {
        lists = Arrays.asList(observableLists);
        addListChangeListeners();
    }

    @Override
    public T get(int index) {
        for (int i = 0; i < lists.size(); i++) {
            final ObservableList<T> list = lists.get(i);
            final int offset = lists.subList(0, i).stream().mapToInt(List::size).sum();
            if (index >= offset && index < (offset + list.size())) {
                return list.get(index - offset);
            }
        }

        throw new IndexOutOfBoundsException(String.valueOf(index));
    }

    @Override
    public int size() {
        return lists.stream().mapToInt(List::size).sum();
    }

    private void addListChangeListeners() {
        for (int listIndex = 0; listIndex < lists.size(); listIndex++) {
            final ObservableList<T> list = lists.get(listIndex);
            final int offset = lists.subList(0, listIndex).stream().mapToInt(List::size).sum();
            list.addListener((ListChangeListener<T>) c -> {
                beginChange();
                while (c.next()) {
                    if (c.wasPermutated()) {
                        nextPermutation(c.getFrom(), c.getTo(), getNewIndexes(offset, c));
                    } else if (c.wasUpdated()) {
                        for (int pos = c.getFrom(); pos < c.getTo(); pos++) {
                            nextUpdate(offset + pos);
                        }
                    } else {
                        if (c.wasRemoved()) {
                            nextRemove(offset + c.getFrom(), c.getRemoved());
                        }
                        if (c.wasAdded()) {
                            nextAdd(offset + c.getFrom(), offset + c.getTo());
                        }
                    }
                }
                endChange();
            });
        }
    }

    private int[] getNewIndexes(int offset, ListChangeListener.Change<? extends T> c) {
        return range(c.getFrom(), c.getTo()).map(i -> offset + c.getPermutation(i)).toArray();
    }
}