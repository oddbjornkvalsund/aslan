package no.nixx.aslan.ui.components;

import javafx.collections.ObservableList;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static javafx.collections.FXCollections.observableArrayList;
import static org.assertj.core.api.Assertions.assertThat;

public class LineFragmentOutputStreamTest {

    @Test
    public void testInitial() throws IOException {
        final ObservableList<Line> list = observableArrayList();
        final LineFragmentOutputStream<Line, LineFragment> os = createTestOutputStream(list);
        assertThat(list.size()).isEqualTo(1);
        os.flush();
        assertThat(list.size()).isEqualTo(1);
        os.close();
        assertThat(list.size()).isEqualTo(0);
    }

    @Test
    public void testSimpleWrite() throws IOException {
        final ObservableList<Line> list = observableArrayList();
        final LineFragmentOutputStream<Line, LineFragment> os = createTestOutputStream(list);

        os.write("Hello");
        assertThat(list).isEqualTo(singletonList(new Line()));
        os.flush();
        assertThat(list).isEqualTo(singletonList(new Line(new LineFragment("Hello"))));
        os.close();
        assertThat(list).isEqualTo(singletonList(new Line(new LineFragment("Hello"))));
    }

    @Test
    public void testManyWritesToSameLine() throws IOException {
        final ObservableList<Line> list = observableArrayList();
        final LineFragmentOutputStream<Line, LineFragment> os = createTestOutputStream(list);

        os.write("Hello");
        assertThat(list).isEqualTo(singletonList(new Line()));
        os.write("Hello");
        assertThat(list).isEqualTo(singletonList(new Line()));
        os.flush();
        assertThat(list).isEqualTo(singletonList(new Line(new LineFragment("HelloHello"))));
        os.close();
        assertThat(list).isEqualTo(singletonList(new Line(new LineFragment("HelloHello"))));
    }

    @Test
    public void testNewline() throws IOException {
        final ObservableList<Line> list = observableArrayList();
        final LineFragmentOutputStream<Line, LineFragment> os = createTestOutputStream(list);

        os.write("Hello");
        assertThat(list).isEqualTo(singletonList(new Line()));
        os.write("\n");
        assertThat(list).isEqualTo(singletonList(new Line()));
        os.flush();
        assertThat(list).isEqualTo(asList(new Line(new LineFragment("Hello")), new Line()));
    }

    @Test
    public void testWriteRemovesEmptyLastLine() throws IOException {
        final ObservableList<Line> list = observableArrayList();
        final LineFragmentOutputStream<Line, LineFragment> os = createTestOutputStream(list);

        os.write("Hello\n");
        assertThat(list).isEqualTo(singletonList(new Line()));
        os.flush();
        assertThat(list).isEqualTo(asList(new Line(new LineFragment("Hello")), new Line()));
        os.close();
        assertThat(list).isEqualTo(singletonList(new Line(new LineFragment("Hello"))));
    }

    @Test
    public void testManyStreamsLinkedToOneList() throws IOException {
        final ObservableList<Line> list = observableArrayList();
        final LineFragmentOutputStream<Line, LineFragment> os1 = createTestOutputStream(list);
        final LineFragmentOutputStream<Line, LineFragment> os2 = createTestOutputStream(list);

        os1.write("Foo");
        assertThat(list).isEqualTo(singletonList(new Line()));
        os2.write("Bar");
        assertThat(list).isEqualTo(singletonList(new Line()));
        os1.flush();
        assertThat(list).isEqualTo(singletonList(new Line(new LineFragment("Foo"))));
        os2.flush();
        assertThat(list).isEqualTo(singletonList(new Line(new LineFragment("Foo"), new LineFragment("Bar"))));
        os1.close();
        assertThat(list).isEqualTo(singletonList(new Line(new LineFragment("Foo"), new LineFragment("Bar"))));
        os2.close();
        assertThat(list).isEqualTo(singletonList(new Line(new LineFragment("Foo"), new LineFragment("Bar"))));
    }

    private static class Line extends ArrayList<LineFragment> {

        public Line() {
        }

        public Line(LineFragment... lineFragments) {
            Collections.addAll(this, lineFragments);
        }
    }

    private static class LineFragment {

        private String text;

        public LineFragment() {
            this.text = "";
        }

        public LineFragment(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public boolean isEmpty() {
            return text.isEmpty();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            LineFragment that = (LineFragment) o;

            return text != null ? text.equals(that.text) : that.text == null;

        }

        @Override
        public int hashCode() {
            return text != null ? text.hashCode() : 0;
        }
    }

    private LineFragmentOutputStream<Line, LineFragment> createTestOutputStream(ObservableList<Line> list) {
        return new LineFragmentOutputStream<>(list, Line::new, LineFragment::new, Line::isEmpty, LineFragment::isEmpty, Line::add, list::remove, list::addAll);
    }
}