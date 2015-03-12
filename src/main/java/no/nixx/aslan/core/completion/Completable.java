package no.nixx.aslan.core.completion;

import no.nixx.aslan.api.ExecutionContext;

public interface Completable {
    CompletionSpecRoot getCompletionSpec(ExecutionContext executionContext);
}