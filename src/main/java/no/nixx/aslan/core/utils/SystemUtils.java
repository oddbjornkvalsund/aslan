package no.nixx.aslan.core.utils;

public class SystemUtils {
    public static boolean isWindows() {
        return System.getProperty("os.name").startsWith("Windows");
    }
}
