package no.nixx.aslan.ui.components;

import javafx.collections.ObservableList;

import java.io.ByteArrayOutputStream;

public class BufferOutputStream extends ByteArrayOutputStream {
    private final ObservableList<BufferItem> list;
    private final BufferItemFactory factory;

    private BufferItem currentLine;

    public BufferOutputStream(ObservableList<BufferItem> list, BufferItemFactory factory) {
        this.list = list;
        this.factory = factory;
        this.currentLine = factory.createItem("");
        this.list.add(this.currentLine);
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
        if (currentLineIsEmpty()) {
            list.remove(currentLine);
        }
    }

    private void addBufferItemsToList(String content) {
        // Possible improvement: Don't do list.add(...), but collect to temporary list and do list.addAll(tmpList)
        final StringBuilder buffer = new StringBuilder();
        for (char c : content.toCharArray()) {
            //noinspection StatementWithEmptyBody
            if (c == '\r') {
                // Ignore for now
            } else if (c == '\n') {
                appendToCurrentLine(buffer);
                currentLine = factory.createItem("");
                list.add(currentLine);
                buffer.setLength(0);
            } else {
                buffer.append(c);
            }
        }

        appendToCurrentLine(buffer);
    }

    private void appendToCurrentLine(StringBuilder buffer) {
        final String newContent = buffer.toString();
        if (!newContent.isEmpty()) {
            final BufferItem newCurrentLine = factory.createItem(currentLine.getText() + newContent);
            list.set(list.indexOf(currentLine), newCurrentLine); // To trigger changelisteners on the list
            currentLine = newCurrentLine;
        }
    }

    private boolean currentLineIsEmpty() {
        return currentLine.getText().isEmpty();
    }
}