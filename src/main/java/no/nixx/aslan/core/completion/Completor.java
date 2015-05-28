package no.nixx.aslan.core.completion;

import no.nixx.aslan.api.Executable;
import no.nixx.aslan.api.ExecutionContext;
import no.nixx.aslan.core.ExecutableLocator;
import no.nixx.aslan.core.utils.Preconditions;
import no.nixx.aslan.pipeline.PipelineParser;
import no.nixx.aslan.pipeline.model.Argument;
import no.nixx.aslan.pipeline.model.Command;
import no.nixx.aslan.pipeline.model.Pipeline;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static java.lang.Math.min;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static no.nixx.aslan.core.utils.ListUtils.allButLastOf;
import static no.nixx.aslan.core.utils.ListUtils.firstOf;
import static no.nixx.aslan.core.utils.ListUtils.lastOf;
import static no.nixx.aslan.core.utils.StringUtils.anyContainsWhiteSpace;
import static no.nixx.aslan.core.utils.StringUtils.containsWhiteSpace;
import static no.nixx.aslan.core.utils.StringUtils.getCommonStartOfStrings;

public class Completor {

    public CompletionResult getCompletions(String command, int tabPosition, ExecutableLocator executableLocator, ExecutionContext executionContext) {
        Preconditions.checkNotNull(command);
        Preconditions.checkArgument(tabPosition <= command.length());
        Preconditions.checkNotNull(executableLocator);
        Preconditions.checkNotNull(executionContext);

        final CompletionResult emptyCompletionResult = createEmptyCompletionResult(command, tabPosition);
        final Pipeline pipelineToComplete = parseCommand(command);
        final boolean isUnableToParsePipeline = pipelineToComplete == null;

        if (isUnableToParsePipeline) {
            return emptyCompletionResult;
        } else {
            final Command commandToComplete = pipelineToComplete.getCommandAtPosition(tabPosition);
            final Argument argumentToComplete = commandToComplete.getArgumentAtPosition(tabPosition);

            final String renderedArgumentToComplete = argumentToComplete.substring(tabPosition);
            final List<String> renderedPrecedingArguments = commandToComplete.getPrecedingArguments(argumentToComplete).stream().map(Argument::getRenderedText).collect(toList());

            if (commandToComplete.isFirstArgument(argumentToComplete)) {
                return createCompletionResult(command, argumentToComplete, executableLocator.findExecutableCandidates(renderedArgumentToComplete), true, true);
            }

            final Executable executable = executableLocator.lookupExecutable(commandToComplete.getExecutableName());
            if (!(executable instanceof Completable)) {
                return emptyCompletionResult;
            }

            final Completable completable = (Completable) executable;
            final CompletionSpecRoot completionSpecRoot = completable.getCompletionSpec(executionContext);

            final TemporaryCompletionResult partialCompletionResult = getPartiallyMatchingCompletions(completionSpecRoot, renderedArgumentToComplete, renderedPrecedingArguments);
            if (partialCompletionResult.completions.isEmpty()) {
                final TemporaryCompletionResult completeCompletionResult = getCompletelyMatchingCompletions(completionSpecRoot, renderedArgumentToComplete, renderedPrecedingArguments);
                if (completeCompletionResult.completions.isEmpty()) {
                    return emptyCompletionResult;
                } else {
                    return createCompletionResult(command, argumentToComplete, completeCompletionResult.completions, completeCompletionResult.appendSpace, completeCompletionResult.appendQuote);
                }
            } else {
                return createCompletionResult(command, argumentToComplete, partialCompletionResult.completions, partialCompletionResult.appendSpace, partialCompletionResult.appendQuote);
            }
        }
    }

    private CompletionResult createEmptyCompletionResult(String command, int tabPosition) {
        return new CompletionResult(command, tabPosition, emptyList());
    }

    private CompletionResult createCompletionResult(String command, Argument argumentToReplace, List<String> completions, boolean doAppendSpace, boolean doAppendQuote) {
        final boolean onlyOneCompletion = (completions.size() == 1);
        final String completion;
        if (onlyOneCompletion) {
            final String onlyCompletion = firstOf(completions);
            if (containsWhiteSpace(onlyCompletion)) {
                completion = String.format("\"%s%s%s", onlyCompletion, doAppendQuote ? "\"" : "", doAppendSpace ? " " : "");
            } else {
                completion = String.format("%s%s", onlyCompletion, doAppendSpace ? " " : "");
            }
        } else {
            if (anyContainsWhiteSpace(completions)) {
                completion = "\"" + getCommonStartOfStrings(completions);
                completions = completions.stream().map(s -> "\"" + s + "\"").collect(toList());
            } else {
                completion = getCommonStartOfStrings(completions);
            }
        }

        final String commandUpToCompletion = command.substring(0, argumentToReplace.getStartIndex());
        final String commandAfterCompletion = command.substring(min(command.length(), argumentToReplace.getStopIndex()));
        final String completedCommand = commandUpToCompletion + completion + commandAfterCompletion;
        final int newTabPosition = commandUpToCompletion.length() + completion.length();
        return new CompletionResult(completedCommand, newTabPosition, onlyOneCompletion ? emptyList() : completions);
    }

    private Pipeline parseCommand(String command) {
        final PartialPipelineSupplementor partialPipelineSupplementor = new PartialPipelineSupplementor();
        final String supplementedCommand = partialPipelineSupplementor.supplementPipeline(command);

        try {
            final PipelineParser parser = new PipelineParser();
            return parser.parseCommand(supplementedCommand);
        } catch (Exception e) {
            return null;
        }
    }

    private TemporaryCompletionResult getPartiallyMatchingCompletions(CompletionSpecRoot completionSpecRoot, String argumentToComplete, List<String> precedingArguments) {
        return getMatchingCompletions(completionSpecRoot, false, argumentToComplete, precedingArguments);
    }

    private TemporaryCompletionResult getCompletelyMatchingCompletions(CompletionSpecRoot completionSpecRoot, String argumentToComplete, List<String> precedingArguments) {
        return getMatchingCompletions(completionSpecRoot, true, argumentToComplete, precedingArguments);
    }

    private TemporaryCompletionResult getMatchingCompletions(CompletionSpecRoot completionSpecRoot, boolean findCompleteMatches, String argumentToComplete, List<String> precedingArguments) {
        final List<CompletionSpec> matchingNodes = (findCompleteMatches) ? findCompleteMatchingNodes(completionSpecRoot, argumentToComplete) : findPartialMatchingNodes(completionSpecRoot, argumentToComplete);
        final List<CompletionSpec> nodesWithCompleteAncestry = findNodesWithCompleteAncestry(matchingNodes, precedingArguments);
        final List<CompletionSpec> nodesWithCorrectOccurenceCount = findNodesWithCorrectOccurenceCount(nodesWithCompleteAncestry, precedingArguments);

        return findMostDeeplyNestedCompletions(argumentToComplete, nodesWithCorrectOccurenceCount);
    }

    @SuppressWarnings("SimplifiableIfStatement")
    private TemporaryCompletionResult findMostDeeplyNestedCompletions(String argumentToComplete, List<CompletionSpec> complectionSpecs) {
        if (complectionSpecs.isEmpty()) {
            return new TemporaryCompletionResult(false, false, emptyList());
        }

        final Map<Integer, List<String>> completionSpecsByDepth = new TreeMap<>();
        for (CompletionSpec completionSpec : complectionSpecs) {
            final int depth = getDepth(completionSpec);

            if (!completionSpecsByDepth.containsKey(depth)) {
                completionSpecsByDepth.put(depth, new ArrayList<>());
            }

            completionSpecsByDepth.get(depth).addAll(completionSpec.getCompletions(argumentToComplete));
        }

        final int maxDepth = completionSpecsByDepth.keySet().stream().max(Comparator.<Integer>naturalOrder()).get();
        final List<String> allCompletions = completionSpecsByDepth.get(maxDepth);

        final boolean doAppendSpaceIfOnlyOneCompletion;
        final boolean doAppendQuoteIfOnlyOneCompletion;
        if (allCompletions.size() == 1 && complectionSpecs.size() == 1) {
            doAppendSpaceIfOnlyOneCompletion = firstOf(complectionSpecs).appendSpaceIfOnlyOneCompletion();
            doAppendQuoteIfOnlyOneCompletion = firstOf(complectionSpecs).appendQuoteIfOnlyOneCompletion();
        } else {
            doAppendSpaceIfOnlyOneCompletion = true;
            doAppendQuoteIfOnlyOneCompletion = true;
        }

        return new TemporaryCompletionResult(doAppendSpaceIfOnlyOneCompletion, doAppendQuoteIfOnlyOneCompletion, allCompletions);
    }

    private List<CompletionSpec> findNodesWithCorrectOccurenceCount(List<CompletionSpec> completionSpecs, List<String> arguments) {
        final List<CompletionSpec> nodesWithCorrectOccurenceCount = new ArrayList<>();
        for (CompletionSpec completionSpec : completionSpecs) {
            if (completionSpec.canOccurOnlyOnce() && hasCompleteAncestry(completionSpec, arguments)) {
                continue;
            }

            nodesWithCorrectOccurenceCount.add(completionSpec);
        }

        return nodesWithCorrectOccurenceCount;
    }

    private List<CompletionSpec> findNodesWithCompleteAncestry(List<CompletionSpec> completionSpecs, List<String> arguments) {
        return completionSpecs.stream().filter(node -> hasCompleteAncestry(node.getParent(), arguments)).collect(toList());
    }

    private boolean hasCompleteAncestry(CompletionSpec completionSpec, List<String> arguments) {
        if (completionSpec instanceof CompletionSpecRoot) {
            return true;
        } else if (arguments.isEmpty()) {
            return false;
        } else {
            final String argumentToMatch = lastOf(arguments);
            final List<String> preceedingArguments = allButLastOf(arguments);
            if (completionSpec.isCompleteMatch(argumentToMatch)) {
                return hasCompleteAncestry(completionSpec.getParent(), preceedingArguments);
            } else {
                return hasCompleteAncestry(completionSpec, preceedingArguments);
            }
        }
    }

    private List<CompletionSpec> findCompleteMatchingNodes(CompletionSpec completionSpec, String argument) {
        return findMatchingNodes(completionSpec, argument, true);
    }

    private List<CompletionSpec> findPartialMatchingNodes(CompletionSpec completionSpec, String argument) {
        return findMatchingNodes(completionSpec, argument, false);
    }

    private List<CompletionSpec> findMatchingNodes(CompletionSpec completionSpec, String argument, boolean findCompleteMatches) {
        final ArrayList<CompletionSpec> matches = new ArrayList<>();

        if (findCompleteMatches ? completionSpec.isCompleteMatch(argument) : completionSpec.isPartialMatch(argument)) {
            matches.add(completionSpec);
        }

        for (CompletionSpec child : completionSpec.getChildren()) {
            matches.addAll(findCompleteMatches ? findCompleteMatchingNodes(child, argument) : findPartialMatchingNodes(child, argument));
        }

        return matches;
    }

    private int getDepth(CompletionSpec completionSpec) {
        if (completionSpec instanceof CompletionSpecRoot) {
            return 0;
        } else {
            return getDepth(completionSpec.getParent()) + 1;
        }
    }

    private class TemporaryCompletionResult {
        public final boolean appendSpace;
        public final boolean appendQuote;
        public final List<String> completions;

        TemporaryCompletionResult(boolean appendSpace, boolean appendQuote, List<String> completions) {
            this.appendSpace = appendSpace;
            this.appendQuote = appendQuote;
            this.completions = completions;
        }
    }
}