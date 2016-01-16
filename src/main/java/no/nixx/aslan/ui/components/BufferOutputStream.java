package no.nixx.aslan.ui.components;

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import javafx.collections.ObservableList;
import no.nixx.aslan.core.utils.ListUtils;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import static javafx.application.Platform.runLater;
import static no.nixx.aslan.core.utils.ListUtils.*;

public class BufferOutputStream extends ByteArrayOutputStream {
    private final ObservableList<BufferItem> list;
    private final BufferItemFactory factory;

    private BufferItem currentItem;

    public BufferOutputStream(ObservableList<BufferItem> list, BufferItemFactory factory) {
        this.list = list;
        this.factory = factory;
        this.currentItem = factory.createItem("");
        this.list.add(this.currentItem);
    }

    @Override
    public void flush() {
        if (count > 0) {
            addBufferItemsToList(toString());
            reset();
        }
    }

    @Override
    public void close() {
        runLater(() -> {
            if (currentItemIsEmpty()) {
                list.remove(currentItem);
            }
        });
    }

    private void addBufferItemsToList(String content) {
        final BufferItem initialCurrentItem = currentItem;
        final ArrayList<BufferItem> tmpList = new ArrayList<>();
        final StringBuilder buffer = new StringBuilder();
        for (char c : content.toCharArray()) {
            //noinspection StatementWithEmptyBody
            if (c == '\r') {
                // Ignore for now
            } else if (c == '\n') {
                appendToCurrentItem(tmpList, buffer);
                currentItem = factory.createItem("");
                tmpList.add(currentItem);
                buffer.setLength(0);
            } else {
                buffer.append(c);
            }
        }

        appendToCurrentItem(tmpList, buffer);

        final List<BufferItem> newList = concatenate(removeElement(list, initialCurrentItem), tmpList);
        runLater(() -> list.setAll(newList));
    }

    private void appendToCurrentItem(ArrayList<BufferItem> tmpList, StringBuilder buffer) {
        final String newContent = buffer.toString();
        final BufferItem newCurrentItem = factory.createItem(currentItem.getText() + newContent);
        if (tmpList.isEmpty()) {
            tmpList.add(newCurrentItem);
        } else {
            tmpList.set(tmpList.size() - 1, newCurrentItem); // To trigger changelisteners on the list
        }
        currentItem = newCurrentItem;
    }

    private boolean currentItemIsEmpty() {
        return currentItem.getText().isEmpty();
    }
}