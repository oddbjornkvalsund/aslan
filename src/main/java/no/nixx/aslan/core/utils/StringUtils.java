package no.nixx.aslan.core.utils;

public class StringUtils {
    public static String removeTrailingNewlines(String string) {
        return string.replaceAll("[\r\n]+$", "");
    }
}
