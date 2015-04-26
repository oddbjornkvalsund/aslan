package no.nixx.aslan.core.completion;

import java.util.List;

import static no.nixx.aslan.core.utils.Preconditions.checkNotNull;

public class CompletionResult {
    public final String text;
    public final int tabPosition;
    public final List<String> completionCandidates;

    public CompletionResult(String text, int tabPosition, List<String> completionCandidates) {
        this.text = checkNotNull(text);
        this.tabPosition = tabPosition;
        this.completionCandidates = checkNotNull(completionCandidates);
    }

    public boolean hasCompletionCandidates() {
        return completionCandidates.size() > 0;
    }

    @Override
    public String toString() {
        return "CompletionResult{" +
                "text='" + text + '\'' +
                ", tabPosition=" + tabPosition +
                ", completionCandidates=" + completionCandidates +
                '}';
    }


    @Override
    public int hashCode() {
        return text.hashCode() + new Integer(tabPosition).hashCode() + completionCandidates.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CompletionResult) {
            final CompletionResult that = (CompletionResult) obj;
            return text.equals(that.text) && tabPosition == that.tabPosition && completionCandidates.equals(that.completionCandidates);
        }

        return false;
    }
}