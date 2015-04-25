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
import static no.nixx.aslan.core.completion.PartialQuotesCompletor.inDoubleQuotes;
import static no.nixx.aslan.core.completion.PartialQuotesCompletor.inSingleQuotes;
import static no.nixx.aslan.core.utils.ListUtils.*;
import static no.nixx.aslan.core.utils.StringUtils.getCommonStartOfStrings;

public class Completor {

    public CompletionResult getCompletions(String command, int tabPosition, ExecutableLocator executableLocator, ExecutionContext executionContext) {
        final String commandUpToTab = command.substring(0, tabPosition);
        final CompletionResult emptyCompletionResult = new CompletionResult(tabPosition, command, emptyList());

        final Command commandToComplete = getCommandToComplete(commandUpToTab);
        if (commandToComplete == null) {
            return emptyCompletionResult;
        }

        final String executableName = commandToComplete.getExecutableName();
        final Executable executable = executableLocator.lookupExecutable(executableName);
        final List<String> executableCandidates = executableLocator.findExecutableCandidates(executableName);
        if (executableCandidates.isEmpty()) {
            return emptyCompletionResult;
        } else if (executable == null && executableCandidates.size() == 1) {
            return createCompletionResult(command, tabPosition, executableName, executableCandidates, true);
        } else if (executable != null && executableCandidates.size() > 1) {
            return createCompletionResult(command, tabPosition, executableName, executableCandidates, true);
        }

        if (executable instanceof Completable) {
            final Completable completable = (Completable) executable;
            final CompletionSpecRoot completionSpecRoot = completable.getCompletionSpec(executionContext);

            final List<String> arguments = getArguments(commandUpToTab, commandToComplete);
            if (arguments.isEmpty()) {
                return createCompletionResult(command, tabPosition, "", asList(""), true);
            }

            final TemporaryCompletionResult partialCompletionResult = getCompletions(completionSpecRoot, arguments);
            final List<String> partialCompletions = partialCompletionResult.completions;
            if (partialCompletions.isEmpty()) {
                final TemporaryCompletionResult completeCompletionResult = getCompleteCompletions(completionSpecRoot, arguments);
                final List<String> completeCompletions = completeCompletionResult.completions;
                if (completeCompletions.isEmpty()) {
                    return emptyCompletionResult;
                } else {
                    return createCompletionResult(command, tabPosition, "", asList(""), completeCompletionResult.doAppendQuoteAndSpaceIfOnlyOneCompletion);
                }
            } else {
                return createCompletionResult(command, tabPosition, lastOf(arguments), partialCompletions, partialCompletionResult.doAppendQuoteAndSpaceIfOnlyOneCompletion);
            }
        }

        return emptyCompletionResult;
    }

    private CompletionResult createCompletionResult(String command, int tabPosition, String argumentToComplete, List<String> completions, boolean doAppendQuoteAndSpaceIfOnlyOneCompletion) {
        final boolean onlyOneCompletion = (completions.size() == 1);
        final String commmandUpToTab = command.substring(0, tabPosition);
        final int idx = commmandUpToTab.lastIndexOf(argumentToComplete);
        final boolean inSingleQuotes = inSingleQuotes(commmandUpToTab);
        final boolean inDoubleQuotes = inDoubleQuotes(commmandUpToTab);
        final String completion;
        if (inDoubleQuotes) {
            if (onlyOneCompletion) {
                completion = firstOf(completions) + ((doAppendQuoteAndSpaceIfOnlyOneCompletion) ? "\" " : "");
            } else {
                completion = getCommonStartOfStrings(completions);
                for (int i = 0; i < completions.size(); i++) {
                    completions.set(i, "\"" + completions.get(i) + "\"");
                }
            }
        } else if (inSingleQuotes) {
            if (onlyOneCompletion) {
                completion = firstOf(completions) + ((doAppendQuoteAndSpaceIfOnlyOneCompletion) ? "\' " : "");
            } else {
                completion = getCommonStartOfStrings(completions);
                for (int i = 0; i < completions.size(); i++) {
                    completions.set(i, "\'" + completions.get(i) + "\'");
                }
            }
        } else {
            if (onlyOneCompletion) {
                if (firstOf(completions).contains(" ")) {
                    completion = "\"" + firstOf(completions) + ((doAppendQuoteAndSpaceIfOnlyOneCompletion) ? "\" " : "");
                } else {
                    completion = firstOf(completions) + ((doAppendQuoteAndSpaceIfOnlyOneCompletion) ? " " : "");
                }
            } else {
                if (completions.stream().anyMatch(s -> s.contains(" "))) {
                    completion = "\"" + getCommonStartOfStrings(completions);
                    for (int i = 0; i < completions.size(); i++) {
                        completions.set(i, "\"" + completions.get(i) + "\"");
                    }
                } else {
                    completion = getCommonStartOfStrings(completions);
                }
            }
        }

        final String newText = command.substring(0, idx) + completion + command.substring(idx + argumentToComplete.length());
        final int newTabPosition = tabPosition + (completion.length() - argumentToComplete.length());
        return new CompletionResult(newTabPosition, newText, onlyOneCompletion ? emptyList() : completions);
    }

    private Command getCommandToComplete(String commandUpToTab) {
        final PartialCommandExtractor partialCommandExtractor = new PartialCommandExtractor();
        final String partialCommand = PartialQuotesCompletor.completeOpenQuotes(partialCommandExtractor.getLastCommand(commandUpToTab));

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
        if (lastArgumentIsComplete(commandUpToTab, commandToComplete)) {
            arguments.add("");
        }

        return arguments;
    }

    // TODO: Horrible method name. This method determines if the last argument of the command was completed or not
    private boolean lastArgumentIsComplete(String command, Command commandToComplete) {
        final List<String> arguments = commandToComplete.getArgumentsAsStrings();
        if (arguments.isEmpty()) {
            return command.endsWith(" ");
        } else
            return !(inSingleQuotes(command) || inDoubleQuotes(command)) && command.endsWith(" ");
    }

    private TemporaryCompletionResult getCompletions(CompletionSpecRoot completionSpecRoot, List<String> arguments) {
        if (arguments.isEmpty()) {
            return new TemporaryCompletionResult(true, emptyList());
        }

        final String argumentToComplete = lastOf(arguments);
        final List<String> preceedingArguments = allButLastOf(arguments);

        final List<CompletionSpec> partialMatchingNodes = findPartialMatchingNodes(completionSpecRoot, argumentToComplete);
        final List<CompletionSpec> nodesWithCompleteAncestry = findNodesWithCompleteAncestry(partialMatchingNodes, arguments);
        final List<CompletionSpec> nodesWithCorrectOccurenceCount = findNodesWithCorrectOccurenceCount(nodesWithCompleteAncestry, preceedingArguments);

        return findMostDeeplyNestedCompletions(argumentToComplete, nodesWithCorrectOccurenceCount);
    }

    // TODO: Find better name and combine with method above!
    private TemporaryCompletionResult getCompleteCompletions(CompletionSpecRoot completionSpecRoot, List<String> arguments) {
        if (arguments.isEmpty()) {
            return new TemporaryCompletionResult(true, emptyList());
        }
        final String argumentToComplete = lastOf(arguments);
        final List<String> preceedingArguments = allButLastOf(arguments);

        final List<CompletionSpec> completeMatchingNodes = findCompleteMatchingNodes(completionSpecRoot, argumentToComplete);
        final List<CompletionSpec> nodesWithCompleteAncestry = findNodesWithCompleteAncestry(completeMatchingNodes, arguments);
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