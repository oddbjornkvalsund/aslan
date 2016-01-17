package no.nixx.aslan.ui.components;

import javafx.collections.ObservableList;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import static javafx.application.Platform.runLater;
import static no.nixx.aslan.core.utils.ListUtils.concatenate;
import static no.nixx.aslan.core.utils.ListUtils.removeElement;

public class BufferOutputStream extends ByteArrayOutputStream {
    private final ObservableList<BufferItem> list;
    private final BufferItemFactory factory;
    private final boolean modifyListOnJavaFXThread;

    private BufferItem currentItem;

    public BufferOutputStream(ObservableList<BufferItem> list, BufferItemFactory factory) {
        this(list, factory, true);
    }

    public BufferOutputStream(ObservableList<BufferItem> list, BufferItemFactory factory, boolean modifyListOnJavaFXThread) {
        this.list = list;
        this.factory = factory;
        this.modifyListOnJavaFXThread = modifyListOnJavaFXThread;
        this.currentItem = factory.createItem("");
        this.list.add(this.currentItem);
    }

    @Override
    public void flush() {
        if (count > 0) {
            decomposeToBufferItemsAndAddToList(toString());
            reset();
        }
    }

    @Override
    public void close() {
        if (modifyListOnJavaFXThread) {
            runLater(this::removeCurrentItemIfEmpty);
        } else {
            removeCurrentItemIfEmpty();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    private void decomposeToBufferItemsAndAddToList(String content) {
        final BufferItem initialCurrentItem = currentItem;

        final ArrayList<BufferItem> newItems = new ArrayList<>();
        newItems.add(currentItem);

        final StringBuilder buffer = new StringBuilder();
        for (char c : content.toCharArray()) {
            if (c == '\b') {
                final String currentText = currentItem.getText();
                final String newText = currentText.isEmpty() ? "" : currentText.substring(0, currentText.length() - 1);
                replaceCurrentItem(factory.createItem(newText), newItems);
            } else if (c == '\r') {
                // Ignore for now
            } else if (c == '\n') {
                appendToCurrentItem(buffer.toString(), newItems);
                currentItem = factory.createItem("");
                newItems.add(currentItem);
                buffer.setLength(0);
            } else {
                buffer.append(c);
            }
        }

        appendToCurrentItem(buffer.toString(), newItems);

        final List<BufferItem> newList = concatenate(removeElement(list, initialCurrentItem), newItems);
        if (modifyListOnJavaFXThread) {
            runLater(() -> list.setAll(newList));
        } else {
            list.setAll(newList);
        }
    }

    private void appendToCurrentItem(String text, ArrayList<BufferItem> bufferItems) {
        final BufferItem newCurrentItem = factory.createItem(currentItem.getText() + text);
        replaceCurrentItem(newCurrentItem, bufferItems);
    }

    private void replaceCurrentItem(BufferItem newCurrentItem, ArrayList<BufferItem> bufferItems) {
        bufferItems.replaceAll(item -> (item == currentItem) ? newCurrentItem : item);
        currentItem = newCurrentItem;
    }

    private void removeCurrentItemIfEmpty() {
        if (currentItemIsEmpty()) {
            list.remove(currentItem);
        }
    }

    private boolean currentItemIsEmpty() {
        return currentItem.getText().isEmpty();
    }
}