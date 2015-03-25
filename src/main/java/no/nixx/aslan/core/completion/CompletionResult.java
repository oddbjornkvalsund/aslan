package no.nixx.aslan.core.completion;

import java.util.List;

import static no.nixx.aslan.core.utils.Preconditions.checkNotNull;

public class CompletionResult {
    public final int tabPosition;
    public final String text;
    public final List<String> completionCandidates;

    public CompletionResult(int tabPosition, String text, List<String> completionCandidates) {
        this.tabPosition = tabPosition;
        this.text = checkNotNull(text);
        this.completionCandidates = checkNotNull(completionCandidates);
    }

    public boolean hasCompletionCandidates() {
        return completionCandidates.size() > 0;
    }

    @Override
    public String toString() {
        return "CompletionResult{" +
                "tabPosition=" + tabPosition +
                ", text='" + text + '\'' +
                ", completionCandidates=" + completionCandidates +
                '}';
    }


    @Override
    public int hashCode() {
        return new Integer(tabPosition).hashCode() + text.hashCode() + completionCandidates.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CompletionResult) {
            final CompletionResult that = (CompletionResult) obj;
            return tabPosition == that.tabPosition && text.equals(that.text) && completionCandidates.equals(that.completionCandidates);
        }

        return false;
    }
}