package no.nixx.aslan.ui.components;

import javafx.collections.ObservableList;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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

        os.write("Hello");
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

        os.write("Hello");
        assertThat(list).isEqualTo(singletonList(new Line()));
        os.write("Hello");
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

        os.write("Hello");
        assertThat(list).isEqualTo(singletonList(new Line()));
        os.write("\n");
        assertThat(list).isEqualTo(singletonList(new Line()));
        os.flush();
        assertThat(list).isEqualTo(asList(new Line(new Fragment("Hello")), new Line()));
    }

    @Test
    public void testCarriageReturn() throws IOException {
        final ObservableList<Line> list = observableArrayList();
        final LineFragmentOutputStream<Line, Fragment> os = createTestOutputStream(list);

        os.write("Hello\r\n");
        os.flush();
        assertThat(list).isEqualTo(asList(new Line(new Fragment("Hello")), new Line()));
    }

    @Test
    public void testWriteRemovesEmptyLastLine() throws IOException {
        final ObservableList<Line> list = observableArrayList();
        final LineFragmentOutputStream<Line, Fragment> os = createTestOutputStream(list);

        os.write("Hello\n");
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

        os1.write("Foo");
        assertThat(list).isEqualTo(singletonList(new Line()));
        os2.write("Bar");
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

    private static class Line extends ArrayList<Fragment> {

        public Line(Fragment... fragments) {
            Collections.addAll(this, fragments);
        }
    }

    private static class Fragment {

        private String text;

        public Fragment(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Fragment that = (Fragment) o;

            return text != null ? text.equals(that.text) : that.text == null;

        }

        @Override
        public int hashCode() {
            return text != null ? text.hashCode() : 0;
        }
    }

    private static class TestAdapter implements LineFragmentOutputStream.Adapter<Line, Fragment> {

        private final List<Line> list;

        public TestAdapter(List<Line> list) {
            this.list = list;
        }

        @Override
        public Line createLine() {
            return new Line();
        }

        @Override
        public Fragment createFragment(String string) {
            return new Fragment(string);
        }

        @Override
        public boolean lineIsEmpty(Line line) {
            return line.isEmpty();
        }

        @Override
        public boolean fragmentIsEmpty(Fragment fragment) {
            return fragment.getText().isEmpty();
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