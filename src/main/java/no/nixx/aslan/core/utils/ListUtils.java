package no.nixx.aslan.core.utils;

import java.util.List;

public class ListUtils {
    public static <T> T firstOf(List<T> list) {
        return list.get(0);
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
}