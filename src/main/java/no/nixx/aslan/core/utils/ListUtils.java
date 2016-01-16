package no.nixx.aslan.core.utils;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singleton;

@SuppressWarnings("UnusedDeclaration")
public class ListUtils {
    public static <T> T firstOf(List<T> list) {
        return list.get(0);
    }

    public static <T> boolean isFirstOf(List<T> list, T element) {
        return list.indexOf(element) == 0;
    }

    public static <T> List<T> allButFirstOf(List<T> list) {
        return list.subList(1, list.size());
    }

    public static <T> T lastOf(List<T> list) {
        return list.get(list.size() - 1);
    }

    public static <T> List<T> allButLastOf(List<T> list) {
        return list.subList(0, list.size() - 1);
    }

    public static <T> List<T> upToElement(List<T> list, T element) {
        return list.subList(0, list.indexOf(element));
    }

    public static <T> List<T> addElement(List<T> list, T newElement) {
        final List<T> newList = new ArrayList<>(list);
        newList.add(newElement);
        return newList;
    }

    @SafeVarargs
    public static <T> List<T> concatenate(List<T>... lists) {
        final ArrayList<T> newList = new ArrayList<>();
        for (List<T> list : lists) {
            newList.addAll(list);
        }
        return newList;
    }

    public static <T> List<T> removeElement(List<T> list, T removee) {
        final List<T> newList = new ArrayList<>(list);
        if (newList.removeAll(singleton(removee))) {
            return newList;
        } else {
            throw new IllegalArgumentException("Removee not contained in list!");
        }
    }

    public static <T> List<T> replaceElement(List<T> list, T replacee, T replacement) {
        final ArrayList<T> newList = new ArrayList<>(list);
        if (newList.contains(replacee)) {
            newList.replaceAll(s -> s.equals(replacee) ? replacement : s);
            return newList;
        } else {
            throw new IllegalArgumentException("Replacee not contained in list!");
        }
    }

}