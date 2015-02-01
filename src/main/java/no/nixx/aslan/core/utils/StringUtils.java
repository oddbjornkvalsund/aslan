package no.nixx.aslan.core.utils;

import java.util.Iterator;
import java.util.List;

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

        final String sampleString = strings.get(0);
        final MutableInteger length = new MutableInteger();
        while (strings.stream().allMatch(c -> c.startsWith(sampleString.substring(0, length.value)))) {
            length.value++;
        }

        return sampleString.substring(0, (length.value == 0) ? 0 : (length.value - 1));
    }
}

class MutableInteger {
    public int value = 0;
}