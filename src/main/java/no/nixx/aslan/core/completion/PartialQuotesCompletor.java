package no.nixx.aslan.core.completion;

public class PartialQuotesCompletor {

    // TODO: Make unit test
    // TODO: Make non static

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
}