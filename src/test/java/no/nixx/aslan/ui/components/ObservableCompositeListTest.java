package no.nixx.aslan.ui.components;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ObservableCompositeListTest {

    @Test
    public void testAddAndRemove() {
        final ObservableList<String> first = FXCollections.observableArrayList();
        final ObservableList<String> last = FXCollections.observableArrayList();
        final ObservableCompositeList<String> compositeList = new ObservableCompositeList<>(first, last);

        first.add("a1");
        last.add("z1");
        assertEquals(asList("a1", "z1"), compositeList);

        first.add("a2");
        assertEquals(asList("a1", "a2", "z1"), compositeList);

        last.add("z2");
        assertEquals(asList("a1", "a2", "z1", "z2"), compositeList);

        first.remove(0);
        assertEquals(asList("a2", "z1", "z2"), compositeList);

        last.remove(0);
        assertEquals(asList("a2", "z2"), compositeList);

        first.remove(0);
        assertEquals(singletonList("z2"), compositeList);

        last.remove(0);
        assertEquals(emptyList(), compositeList);
    }

    @Test
    public void testAddSingleItem() {
        final ObservableList<String> first = FXCollections.observableArrayList("ignore");
        final ObservableList<String> last = FXCollections.observableArrayList();
        final ObservableCompositeList<String> list = new ObservableCompositeList<>(first, last);

        final AtomicBoolean oneItemAddObserved = new AtomicBoolean(false);
        list.addListener(new ListChangeListener<String>() {
            @Override
            public void onChanged(Change<? extends String> c) {
                while (c.next()) {
                    assertEquals(1, c.getFrom());
                    assertEquals(1, c.getAddedSize());
                    assertEquals(singletonList("foo"), c.getAddedSubList());
                    oneItemAddObserved.set(true);
                }
                list.removeListener(this);
            }
        });
        last.add("foo");
        assertTrue(oneItemAddObserved.get());
        assertEquals(asList("ignore", "foo"), list);
    }

    @Test
    public void testAddMultipleItems() {
        final ObservableList<String> first = FXCollections.observableArrayList("ignore");
        final ObservableList<String> last = FXCollections.observableArrayList();
        final ObservableCompositeList<String> list = new ObservableCompositeList<>(first, last);

        final AtomicBoolean multipleItemsAddObserved = new AtomicBoolean(false);
        list.addListener(new ListChangeListener<String>() {
            @Override
            public void onChanged(Change<? extends String> c) {
                while (c.next()) {
                    assertEquals(1, c.getFrom());
                    assertEquals(3, c.getAddedSize());
                    assertEquals(asList("foo", "bar", "quux"), c.getAddedSubList());
                    multipleItemsAddObserved.set(true);
                }
                list.removeListener(this);
            }
        });
        last.addAll("foo", "bar", "quux");
        assertTrue(multipleItemsAddObserved.get());
        assertEquals(asList("ignore", "foo", "bar", "quux"), list);
    }

    @Test
    public void testRemoveSingleItem() {
        final ObservableList<String> first = FXCollections.observableArrayList("ignore");
        final ObservableList<String> last = FXCollections.observableArrayList("foo");
        final ObservableCompositeList<String> list = new ObservableCompositeList<>(first, last);

        final AtomicBoolean oneItemRemoveObserved = new AtomicBoolean(false);
        list.addListener(new ListChangeListener<String>() {
            @Override
            public void onChanged(Change<? extends String> c) {
                while (c.next()) {
                    assertEquals(1, c.getFrom());
                    assertEquals(1, c.getRemovedSize());
                    assertEquals(singletonList("foo"), c.getRemoved());
                    oneItemRemoveObserved.set(true);
                }
                list.removeListener(this);
            }
        });
        last.remove(0);
        assertTrue(oneItemRemoveObserved.get());
        assertEquals(singletonList("ignore"), list);
    }

    @Test
    public void testRemoveMultipleItems() {
        final ObservableList<String> first = FXCollections.observableArrayList("ignore");
        final ObservableList<String> last = FXCollections.observableArrayList("foo", "bar");
        final ObservableCompositeList<String> list = new ObservableCompositeList<>(first, last);

        final AtomicBoolean multipleItemsRemoveObserved = new AtomicBoolean(false);
        list.addListener(new ListChangeListener<String>() {
            @Override
            public void onChanged(Change<? extends String> c) {
                while (c.next()) {
                    assertEquals(1, c.getFrom());
                    assertEquals(2, c.getRemovedSize());
                    assertEquals(asList("foo", "bar"), c.getRemoved());
                    multipleItemsRemoveObserved.set(true);
                }
                list.removeListener(this);
            }
        });
        last.remove(0, 2);
        assertTrue(multipleItemsRemoveObserved.get());
        assertEquals(singletonList("ignore"), list);
    }

    @Test
    public void testClear() {
        final ObservableList<String> first = FXCollections.observableArrayList("ignore");
        final ObservableList<String> last = FXCollections.observableArrayList("foo", "bar");
        final ObservableCompositeList<String> list = new ObservableCompositeList<>(first, last);

        final AtomicBoolean multipleItemsRemoveObserved = new AtomicBoolean(false);
        list.addListener(new ListChangeListener<String>() {
            @Override
            public void onChanged(Change<? extends String> c) {
                while (c.next()) {
                    assertEquals(1, c.getFrom());
                    assertEquals(2, c.getRemovedSize());
                    assertEquals(asList("foo", "bar"), c.getRemoved());
                    multipleItemsRemoveObserved.set(true);
                }
                list.removeListener(this);
            }
        });
        last.clear();
        assertTrue(multipleItemsRemoveObserved.get());
        assertEquals(singletonList("ignore"), list);
    }

    @Test
    public void testSet() {
        final ObservableList<String> first = FXCollections.observableArrayList("ignore");
        final ObservableList<String> last = FXCollections.observableArrayList("foo");
        final ObservableCompositeList<String> list = new ObservableCompositeList<>(first, last);

        final AtomicBoolean setObserved = new AtomicBoolean(false);
        list.addListener(new ListChangeListener<String>() {
            @Override
            public void onChanged(Change<? extends String> c) {
                while (c.next()) {
                    assertTrue(c.wasAdded());
                    assertEquals(1, c.getFrom());
                    assertEquals(2, c.getTo());
                    assertEquals(singletonList("bar"), c.getAddedSubList());
                    setObserved.set(true);
                }
                list.removeListener(this);
            }
        });
        last.set(0, "bar");
        assertTrue(setObserved.get());
        assertEquals(asList("ignore", "bar"), list);
    }
}

