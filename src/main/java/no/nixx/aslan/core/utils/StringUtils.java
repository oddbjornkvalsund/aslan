package no.nixx.aslan.core.utils;

import java.util.Iterator;
import java.util.List;

import static no.nixx.aslan.core.utils.ListUtils.firstOf;

public class StringUtils {
    public static String removeTrailingNewlines(String string) {
        return string.replaceAll("[\r\n]+$", "");
    }

    public static String join(Iterable<?> elements, String separator) {
        final StringBuilder sb = new StringBuilder();
        final Iterator<?> iterator = elements.iterator();
        while (iterator.hasNext()) {
            sb.append(iterator.next());
            if (iterator.hasNext()) {
                sb.append(separator);
            }
        }

        return sb.toString();
    }

    public static String getCommonStartOfStrings(List<String> strings) {
        if (strings.isEmpty()) {
            return "";
        }

        final String sampleString = firstOf(strings);
        final MutableInteger length = new MutableInteger();
        while (strings.stream().allMatch(c -> c.length() >= length.value && c.startsWith(sampleString.substring(0, length.value)))) {
            length.value++;
        }

        return sampleString.substring(0, (length.value == 0) ? 0 : (length.value - 1));
    }

    public static String completeOpenQuotes(String s) {
        if (inSingleQuotes(s)) {
            return s + "'";
        } else if (inDoubleQuotes(s)) {
            return s + "\"";
        } else {
            return s;
        }
    }

    public static boolean inSingleQuotes(String s) {
        boolean inSingleQuotes = false;
        boolean inDoubleQuotes = false;

        for (char c : s.toCharArray()) {
            if (c == '\'') {
                if (!inDoubleQuotes) {
                    inSingleQuotes = !inSingleQuotes;
                }
            } else if (c == '\"') {
                if (!inSingleQuotes) {
                    inDoubleQuotes = !inDoubleQuotes;
                }
            }
        }

        return inSingleQuotes;
    }

    public static boolean inDoubleQuotes(String s) {
        boolean inSingleQuotes = false;
        boolean inDoubleQuotes = false;

        for (char c : s.toCharArray()) {
            if (c == '\'') {
                if (!inDoubleQuotes) {
                    inSingleQuotes = !inSingleQuotes;
                }
            } else if (c == '\"') {
                if (!inSingleQuotes) {
                    inDoubleQuotes = !inDoubleQuotes;
                }
            }
        }

        return inDoubleQuotes;
    }

    public static boolean containsWhiteSpace(String s) {
        for (char c : s.toCharArray()) {
            if (Character.isWhitespace(c) || Character.isSpaceChar(c)) {
                return true;
            }
        }

        return false;
    }

    public static boolean anyContainsWhiteSpace(List<String> strings) {
        return strings.stream().anyMatch(StringUtils::containsWhiteSpace);
    }
}

class MutableInteger {
    public int value = 0;
}