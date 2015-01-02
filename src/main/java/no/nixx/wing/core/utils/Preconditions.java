package no.nixx.wing.core.utils;

public class Preconditions {
    public static <T> T notNull(T t) {
        if (t == null) {
            throw new NullPointerException();
        } else {
            return t;
        }
    }
}