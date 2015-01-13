package no.nixx.aslan.core.completion;

import java.util.List;

public interface CustomCompletable {
    List<String> getCompletions(String command, int tabPosition);
}