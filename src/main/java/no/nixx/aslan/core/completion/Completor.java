package no.nixx.aslan.core.completion;

import no.nixx.aslan.api.Executable;
import no.nixx.aslan.api.ExecutionContext;
import no.nixx.aslan.core.ExecutableLocator;
import no.nixx.aslan.pipeline.PipelineParser;
import no.nixx.aslan.pipeline.model.Command;
import no.nixx.aslan.pipeline.model.Pipeline;

import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static no.nixx.aslan.core.utils.ListUtils.*;
import static no.nixx.aslan.core.utils.StringUtils.*;

public class Completor {

    public CompletionResult getCompletions(String command, int tabPosition, ExecutableLocator executableLocator, ExecutionContext executionContext) {
        final String commandUpToTab = command.substring(0, tabPosition);
        final Command commandToComplete = parseCommand(commandUpToTab);

        if (commandToComplete == null) {
            return createEmptyCompletionResult(command, tabPosition);
        } else {
            return getCompletionsForCommand(command, tabPosition, commandToComplete, executableLocator, executionContext);
        }
    }

    private CompletionResult getCompletionsForCommand(String command, int tabPosition, Command commandToComplete, ExecutableLocator executableLocator, ExecutionContext executionContext) {
        final String executableName = commandToComplete.getExecutableName();
        final Executable executable = executableLocator.lookupExecutable(executableName);
        final List<String> executableCandidates = executableLocator.findExecutableCandidates(executableName);

        if (executableCandidates.size() == 0) {
            return createEmptyCompletionResult(command, tabPosition);
        } else if (executableCandidates.size() == 1) {
            if (executable == null) {
                return createCompletionResult(command, tabPosition, executableName, executableCandidates, true);
            } else {
                return getCompletionsForExecutable(command, tabPosition, commandToComplete, executionContext, executable);
            }
        } else {
            return createCompletionResult(command, tabPosition, executableName, executableCandidates, true);
        }
    }

    private CompletionResult getCompletionsForExecutable(String command, int tabPosition, Command commandToComplete, ExecutionContext executionContext, Executable executable) {
        final String commandUpToTab = command.substring(0, tabPosition);
        final List<String> arguments = getArguments(commandUpToTab, commandToComplete);
        if (arguments.isEmpty()) {
            return createCompletionResult(command, tabPosition, "", asList(""), true);
        } else {
            if (executable instanceof Completable) {
                final Completable completable = (Completable) executable;
                final CompletionSpecRoot completionSpecRoot = completable.getCompletionSpec(executionContext);
                return getCompletionsForCompletable(command, tabPosition, arguments, completionSpecRoot);
            } else {
                return createEmptyCompletionResult(command, tabPosition);
            }
        }
    }

    private CompletionResult getCompletionsForCompletable(String command, int tabPosition, List<String> arguments, CompletionSpecRoot completionSpecRoot) {
        final TemporaryCompletionResult partialCompletionResult = getPartiallyMatchingCompletions(completionSpecRoot, arguments);
        final List<String> partialCompletions = partialCompletionResult.completions;
        if (partialCompletions.isEmpty()) {
            final TemporaryCompletionResult completeCompletionResult = getCompletelyMatchingCompletions(completionSpecRoot, arguments);
            final List<String> completeCompletions = completeCompletionResult.completions;
            if (completeCompletions.isEmpty()) {
                return createEmptyCompletionResult(command, tabPosition);
            } else {
                return createCompletionResult(command, tabPosition, "", asList(""), completeCompletionResult.doAppendQuoteAndSpaceIfOnlyOneCompletion);
            }
        } else {
            return createCompletionResult(command, tabPosition, lastOf(arguments), partialCompletions, partialCompletionResult.doAppendQuoteAndSpaceIfOnlyOneCompletion);
        }
    }

    private CompletionResult createEmptyCompletionResult(String command, int tabPosition) {
        return new CompletionResult(command, tabPosition, emptyList());
    }

    private CompletionResult createCompletionResult(String command, int tabPosition, String argumentToComplete, List<String> completions, boolean doAppendQuoteAndSpaceIfOnlyOneCompletion) {
        final boolean onlyOneCompletion = (completions.size() == 1);
        final String commandUpToTab = command.substring(0, tabPosition);
        final boolean inSingleQuotes = inSingleQuotes(commandUpToTab);
        final boolean inDoubleQuotes = inDoubleQuotes(commandUpToTab);
        final String completion;
        if (inSingleQuotes || inDoubleQuotes) {
            final String quote = (inSingleQuotes) ? "\'" : "\"";
            if (onlyOneCompletion) {
                completion = firstOf(completions) + ((doAppendQuoteAndSpaceIfOnlyOneCompletion) ? (quote + " ") : "");
            } else {
                completion = getCommonStartOfStrings(completions);
                completions = completions.stream().map(s -> quote + s + quote).collect(toList());
            }
        } else {
            final String quote = "\"";
            if (onlyOneCompletion) {
                if (containsWhiteSpace(firstOf(completions))) {
                    completion = quote + firstOf(completions) + ((doAppendQuoteAndSpaceIfOnlyOneCompletion) ? (quote + " ") : "");
                } else {
                    completion = firstOf(completions) + ((doAppendQuoteAndSpaceIfOnlyOneCompletion) ? " " : "");
                }
            } else {
                if (anyContainsWhiteSpace(completions)) {
                    completion = quote + getCommonStartOfStrings(completions);
                    completions = completions.stream().map(s -> quote + s + quote).collect(toList());
                } else {
                    completion = getCommonStartOfStrings(completions);
                }
            }
        }

        final int idx = commandUpToTab.lastIndexOf(argumentToComplete);
        final String commandUpToCompletion = command.substring(0, idx);
        final String commandAfterCompletion = command.substring(idx + argumentToComplete.length());
        final String completedCommand = commandUpToCompletion + completion + commandAfterCompletion;
        final int newTabPosition = tabPosition + (completion.length() - argumentToComplete.length());
        return new CompletionResult(completedCommand, newTabPosition, onlyOneCompletion ? emptyList() : completions);
    }

    private Command parseCommand(String commandUpToTab) {
        final PartialCommandExtractor partialCommandExtractor = new PartialCommandExtractor();
        final String partialCommand = completeOpenQuotes(partialCommandExtractor.getLastCommand(commandUpToTab));

        final Pipeline pipeline;
        try {
            final PipelineParser parser = new PipelineParser();
            pipeline = parser.parseCommand(partialCommand);
        } catch (Exception e) {
            return null;
        }

        return lastOf(pipeline.getCommandsUnmodifiable());
    }

    private List<String> getArguments(String commandUpToTab, Command commandToComplete) {
        final List<String> arguments = commandToComplete.getArgumentsAsStrings();
        if (lastArgumentIsComplete(commandUpToTab)) {
            arguments.add("");
        }

        return arguments;
    }

    private boolean lastArgumentIsComplete(String command) {
        final boolean notInQuotes = !(inSingleQuotes(command) || inDoubleQuotes(command));
        return notInQuotes && command.endsWith(" ");
    }

    private TemporaryCompletionResult getPartiallyMatchingCompletions(CompletionSpecRoot completionSpecRoot, List<String> arguments) {
        return getMatchingCompletions(completionSpecRoot, arguments, false);
    }

    private TemporaryCompletionResult getCompletelyMatchingCompletions(CompletionSpecRoot completionSpecRoot, List<String> arguments) {
        return getMatchingCompletions(completionSpecRoot, arguments, true);
    }

    private TemporaryCompletionResult getMatchingCompletions(CompletionSpecRoot completionSpecRoot, List<String> arguments, boolean findCompleteMatches) {
        if (arguments.isEmpty()) {
            return new TemporaryCompletionResult(true, emptyList());
        }
        final String argumentToComplete = lastOf(arguments);
        final List<String> preceedingArguments = allButLastOf(arguments);

        final List<CompletionSpec> matchingNodes = (findCompleteMatches) ? findCompleteMatchingNodes(completionSpecRoot, argumentToComplete) : findPartialMatchingNodes(completionSpecRoot, argumentToComplete);
        final List<CompletionSpec> nodesWithCompleteAncestry = findNodesWithCompleteAncestry(matchingNodes, arguments);
        final List<CompletionSpec> nodesWithCorrectOccurenceCount = findNodesWithCorrectOccurenceCount(nodesWithCompleteAncestry, preceedingArguments);

        return findMostDeeplyNestedCompletions(argumentToComplete, nodesWithCorrectOccurenceCount);
    }

    @SuppressWarnings("SimplifiableIfStatement")
    private TemporaryCompletionResult findMostDeeplyNestedCompletions(String argumentToComplete, List<CompletionSpec> complectionSpecs) {
        if (complectionSpecs.isEmpty()) {
            return new TemporaryCompletionResult(false, emptyList());
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
        if (allCompletions.size() == 1 && complectionSpecs.size() == 1) {
            doAppendSpaceIfOnlyOneCompletion = firstOf(complectionSpecs).appendQuoteAndSpaceIfOnlyOneCompletion();
        } else {
            doAppendSpaceIfOnlyOneCompletion = true;
        }

        return new TemporaryCompletionResult(doAppendSpaceIfOnlyOneCompletion, allCompletions);
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
        public final boolean doAppendQuoteAndSpaceIfOnlyOneCompletion;
        public final List<String> completions;

        TemporaryCompletionResult(boolean doAppendQuoteAndSpaceIfOnlyOneCompletion, List<String> completions) {
            this.doAppendQuoteAndSpaceIfOnlyOneCompletion = doAppendQuoteAndSpaceIfOnlyOneCompletion;
            this.completions = completions;
        }
    }
}