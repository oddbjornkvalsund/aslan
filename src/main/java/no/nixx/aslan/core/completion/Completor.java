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

            // TODO: Add quotation marks to completions containing spaces
            final TemporaryCompletionResult temporaryCompletionResult = getCompletions(completionSpecRoot, arguments);
            final List<String> completions = temporaryCompletionResult.completions;
            if (completions.isEmpty()) {
                return emptyCompletionResult;
            } else {
                return createCompletionResult(command, tabPosition, lastOf(arguments), completions, temporaryCompletionResult.doAppendSpaceIfOnlyOneCompletion);
            }
        }

        return emptyCompletionResult;
    }

    private CompletionResult createCompletionResult(String command, int tabPosition, String argumentToComplete, List<String> completions, boolean doAppendSpaceIfOnlyOneCompletion) {
        final boolean onlyOneCompletion = (completions.size() == 1);
        final int idx = command.substring(0, tabPosition).lastIndexOf(argumentToComplete);
        final String completion = onlyOneCompletion ? (firstOf(completions) + (doAppendSpaceIfOnlyOneCompletion ? " " : "")) : getCommonStartOfStrings(completions);
        final String newText = command.substring(0, idx) + completion + command.substring(idx + argumentToComplete.length());
        final int newTabPosition = tabPosition + (completion.length() - argumentToComplete.length());
        return new CompletionResult(newTabPosition, newText, onlyOneCompletion ? emptyList() : completions);
    }

    private Command getCommandToComplete(String commandUpToTab) {
        final PartialCommandExtractor partialCommandExtractor = new PartialCommandExtractor();
        final String partialCommand = partialCommandExtractor.getLastCommand(commandUpToTab);

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
        if (lastArgumentIsBlank(commandUpToTab, commandToComplete)) {
            arguments.add("");
        }

        return arguments;
    }

    private boolean lastArgumentIsBlank(String command, Command commandToComplete) {
        final List<String> arguments = commandToComplete.getArgumentsAsStrings();
        if (arguments.isEmpty()) {
            final String executableName = commandToComplete.getExecutableName();
            return !command.endsWith(executableName);
        } else {
            final String lastParsedArgument = lastOf(arguments);
            return !command.endsWith(lastParsedArgument);
        }
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
            doAppendSpaceIfOnlyOneCompletion = firstOf(complectionSpecs).appendSpaceIfOnlyOneCompletion();
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
        public final boolean doAppendSpaceIfOnlyOneCompletion;
        public final List<String> completions;

        TemporaryCompletionResult(boolean doAppendSpaceIfOnlyOneCompletion, List<String> completions) {
            this.doAppendSpaceIfOnlyOneCompletion = doAppendSpaceIfOnlyOneCompletion;
            this.completions = completions;
        }
    }
}