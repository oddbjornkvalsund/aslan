package no.nixx.aslan.core.utils;

import java.util.stream.Stream;

public class Preconditions {
    public static <T> T checkNotNull(T t) {
        if (t == null) {
            throw new NullPointerException();
        } else {
            return t;
        }
    }

    public static <T> T[] checkNoNulls(T[] ts) {
        Stream.of(checkNotNull(ts)).forEach(Preconditions::checkNotNull);
        return ts;
    }

    public static void checkArgument(boolean condition) {
        if (!condition) {
            throw new IllegalArgumentException();
        }
    }
}