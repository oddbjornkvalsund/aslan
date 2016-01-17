package no.nixx.aslan.ui.components;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BufferOutputStreamTest {

    @Test
    public void testEmptyLines() throws IOException {
        final ObservableList<BufferItem> list = FXCollections.observableArrayList();
        final BufferOutputStream os = new BufferOutputStream(list, s -> new TextBufferItem(s, Color.BLACK), false);

        assertEquals(1, list.size());

        os.write("".getBytes());
        os.flush();
        assertEquals(1, list.size());

        os.write("\n".getBytes());
        os.flush();
        assertEquals(2, list.size());

        os.write("\n".getBytes());
        os.flush();
        assertEquals(3, list.size());

        os.write("1".getBytes());
        os.flush();
        assertEquals(3, list.size());
    }

    @Test
    public void testNormal() throws IOException {
        final ObservableList<BufferItem> list = FXCollections.observableArrayList();
        final BufferOutputStream os = new BufferOutputStream(list, s -> new TextBufferItem(s, Color.BLACK), false);

        assertEquals(1, list.size());

        os.write("hello".getBytes());
        os.flush();
        assertEquals(list.size(), 1);
        assertEquals("hello", list.get(0).getText());

        os.write("there".getBytes());
        os.flush();
        assertEquals(list.size(), 1);
        assertEquals("hellothere", list.get(0).getText());

        os.write("\n".getBytes());
        os.flush();
        assertEquals(2, list.size());
        assertEquals("hellothere", list.get(0).getText());
        assertEquals("", list.get(1).getText());

        os.write("you".getBytes());
        os.flush();
        assertEquals(2, list.size());
        assertEquals("hellothere", list.get(0).getText());
        assertEquals("you", list.get(1).getText());
    }

    @Test
    public void test() throws IOException {
        final ObservableList<BufferItem> list = FXCollections.observableArrayList();
        final BufferOutputStream os = new BufferOutputStream(list, s -> new TextBufferItem(s, Color.BLACK), false);

        os.write("a\nb\n".getBytes());
        os.flush();
        assertEquals(3, list.size());
        os.close();
        assertEquals(2, list.size());

        assertEquals("a", list.get(0).getText());
        assertEquals("b", list.get(1).getText());
    }

    @Test
    public void testEmpty() throws IOException {
        final ObservableList<BufferItem> list = FXCollections.observableArrayList();
        final BufferOutputStream os = new BufferOutputStream(list, s -> new TextBufferItem(s, Color.BLACK), false);

        os.flush();
        assertEquals(1, list.size());
        os.close();
        assertEquals(0, list.size());
    }
}