package no.nixx.aslan.ui.components;

import javafx.collections.ObservableList;
import javafx.scene.paint.Color;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static javafx.collections.FXCollections.observableArrayList;
import static org.assertj.core.api.Assertions.assertThat;

public class LineFragmentOutputStreamTest {

    @Test
    public void testInitial() throws IOException {
        final ObservableList<Line> list = observableArrayList();
        final LineFragmentOutputStream<Line, Fragment> os = createTestOutputStream(list);
        assertThat(list).isEqualTo(singletonList(new Line()));
        os.flush();
        assertThat(list).isEqualTo(singletonList(new Line()));
        os.close();
        assertThat(list).isEmpty();
    }

    @Test
    public void testSimpleWrite() throws IOException {
        final ObservableList<Line> list = observableArrayList();
        final LineFragmentOutputStream<Line, Fragment> os = createTestOutputStream(list);

        write(os, "Hello");
        assertThat(list).isEqualTo(singletonList(new Line()));
        os.flush();
        assertThat(list).isEqualTo(singletonList(new Line(new Fragment("Hello"))));
        os.close();
        assertThat(list).isEqualTo(singletonList(new Line(new Fragment("Hello"))));
    }

    @Test
    public void testManyWritesToSameLine() throws IOException {
        final ObservableList<Line> list = observableArrayList();
        final LineFragmentOutputStream<Line, Fragment> os = createTestOutputStream(list);

        write(os, "Hello");
        assertThat(list).isEqualTo(singletonList(new Line()));
        write(os, "Hello");
        assertThat(list).isEqualTo(singletonList(new Line()));
        os.flush();
        assertThat(list).isEqualTo(singletonList(new Line(new Fragment("HelloHello"))));
        os.close();
        assertThat(list).isEqualTo(singletonList(new Line(new Fragment("HelloHello"))));
    }

    @Test
    public void testNewline() throws IOException {
        final ObservableList<Line> list = observableArrayList();
        final LineFragmentOutputStream<Line, Fragment> os = createTestOutputStream(list);

        write(os, "Hello");
        assertThat(list).isEqualTo(singletonList(new Line()));
        write(os, "\n");
        assertThat(list).isEqualTo(singletonList(new Line()));
        os.flush();
        assertThat(list).isEqualTo(asList(new Line(new Fragment("Hello")), new Line()));
    }

    @Test
    public void testCarriageReturn() throws IOException {
        final ObservableList<Line> list = observableArrayList();
        final LineFragmentOutputStream<Line, Fragment> os = createTestOutputStream(list);


        write(os, "Hello\r\n");
        os.flush();
        assertThat(list).isEqualTo(asList(new Line(new Fragment("Hello")), new Line()));
    }

    @Test
    public void testWriteRemovesEmptyLastLine() throws IOException {
        final ObservableList<Line> list = observableArrayList();
        final LineFragmentOutputStream<Line, Fragment> os = createTestOutputStream(list);

        write(os, "Hello\n");
        assertThat(list).isEqualTo(singletonList(new Line()));
        os.flush();
        assertThat(list).isEqualTo(asList(new Line(new Fragment("Hello")), new Line()));
        os.close();
        assertThat(list).isEqualTo(singletonList(new Line(new Fragment("Hello"))));
    }

    @Test
    public void testManyStreamsLinkedToOneList() throws IOException {
        final ObservableList<Line> list = observableArrayList();
        final LineFragmentOutputStream<Line, Fragment> os1 = createTestOutputStream(list);
        final LineFragmentOutputStream<Line, Fragment> os2 = createTestOutputStream(list);

        write(os1, "Foo");
        assertThat(list).isEqualTo(singletonList(new Line()));
        write(os2, "Bar");
        assertThat(list).isEqualTo(singletonList(new Line()));
        os1.flush();
        assertThat(list).isEqualTo(singletonList(new Line(new Fragment("Foo"))));
        os2.flush();
        assertThat(list).isEqualTo(singletonList(new Line(new Fragment("Foo"), new Fragment("Bar"))));
        os1.close();
        assertThat(list).isEqualTo(singletonList(new Line(new Fragment("Foo"), new Fragment("Bar"))));
        os2.close();
        assertThat(list).isEqualTo(singletonList(new Line(new Fragment("Foo"), new Fragment("Bar"))));
    }

    private LineFragmentOutputStream<Line, Fragment> createTestOutputStream(ObservableList<Line> list) {
        return new LineFragmentOutputStream<>(list, new TestAdapter(list));
    }

    private void write(LineFragmentOutputStream<Line, Fragment> os, String string) {
        try {
            os.write(string.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class TestAdapter extends LineFragmentAdapter {

        public TestAdapter(List<Line> list) {
            super(list, Color.BLACK);
        }

        @Override
        public void addFragmentToLine(Fragment fragment, Line line) {
            line.add(fragment);
        }

        @Override
        public void addLinesToList(List<Line> lines) {
            list.addAll(lines);
        }

        @Override
        public void removeLineFromList(Line line) {
            list.remove(line);
        }

    }
}