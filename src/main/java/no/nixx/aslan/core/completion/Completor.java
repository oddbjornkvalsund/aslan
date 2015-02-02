package no.nixx.aslan.core.completion;

import no.nixx.aslan.core.Executable;
import no.nixx.aslan.core.ExecutableLocator;
import no.nixx.aslan.pipeline.PipelineParser;
import no.nixx.aslan.pipeline.model.Command;
import no.nixx.aslan.pipeline.model.Pipeline;

import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static no.nixx.aslan.core.utils.StringUtils.getCommonStartOfStrings;

public class Completor {

    public CompletionResult getCompletions(String command, int tabPosition, ExecutableLocator executableLocator) {
        final String commandUpToTab = command.substring(0, tabPosition);
        final CompletionResult emptyCompletionResult = new CompletionResult(tabPosition, command, emptyList());

        final Command commandToComplete = getCommandToComplete(commandUpToTab);
        if (commandToComplete == null) {
            return emptyCompletionResult;
        }

        final String executableName = commandToComplete.getExecutableName();
        final Executable executable = executableLocator.lookupExecutable(executableName);
        if (executable == null) {
            final List<String> executableCandidates = executableLocator.findExecutableCandidates(executableName);
            if (executableCandidates.isEmpty()) {
                return emptyCompletionResult;
            } else {
                return createCompletionResult(command, tabPosition, executableName, executableCandidates);
            }
        }

        if (executable instanceof Completable) {
            final Completable completable = (Completable) executable;
            final CompletionSpecRoot completionSpecRoot = completable.getCompletionSpec();

            final List<String> arguments = getArguments(commandUpToTab, commandToComplete);
            if (arguments.isEmpty()) {
                return new CompletionResult(executableName.length() + 1, executableName + " ", emptyList());
            }

            // TODO: Add quotation marks to completions containing spaces
            final List<String> completions = getCompletions(completionSpecRoot, arguments);
            if (completions.isEmpty()) {
                if (isCompleteMatchWithCompleteAncestry(completionSpecRoot, arguments)) {
                    return new CompletionResult(tabPosition + 1, command + " ", emptyList());
                } else {
                    return emptyCompletionResult;
                }
            } else {
                return createCompletionResult(command, tabPosition, arguments.get(arguments.size() - 1), completions);
            }
        }

        return emptyCompletionResult;
    }

    private boolean isCompleteMatchWithCompleteAncestry(CompletionSpecRoot completionSpecRoot, List<String> arguments) {
        if (arguments.isEmpty()) {
            return false;
        } else {
            final List<CompletionSpec> completeMatchingNodes = findCompleteMatchingNodes(completionSpecRoot, arguments.get(arguments.size() - 1));
            final List<CompletionSpec> completeMatchWithCompleteAncestry = findNodesWithCompleteAncestry(completeMatchingNodes, arguments);
            return completeMatchWithCompleteAncestry.size() > 0;
        }
    }

    private CompletionResult createCompletionResult(String command, int tabPosition, String argumentToComplete, List<String> completions) {
        final boolean onlyOneCompletion = (completions.size() == 1);
        final int idx = command.substring(0, tabPosition).lastIndexOf(argumentToComplete);
        final String completion = onlyOneCompletion ? (completions.get(0) + " ") : getCommonStartOfStrings(completions);
        final String newText = command.substring(0, idx) + completion + command.substring(idx + argumentToComplete.length());
        final int newTabPosition = tabPosition + (completion.length() - argumentToComplete.length());
        return new CompletionResult(newTabPosition, newText, onlyOneCompletion ? emptyList() : completions);
    }

    private Command getCommandToComplete(String commandUpToTab) {
        final Pipeline pipeline;
        try {
            final PipelineParser parser = new PipelineParser();
            pipeline = parser.parseCommand(commandUpToTab);
        } catch (Exception e) {
            return null;
        }

        final List<Command> commands = pipeline.getCommandsUnmodifiable();
        return commands.get(commands.size() - 1);
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
            final String lastParsedArgument = arguments.get(arguments.size() - 1);
            return !command.endsWith(lastParsedArgument);
        }
    }

    private List<String> getCompletions(CompletionSpecRoot completionSpecRoot, List<String> arguments) {
        if (arguments.isEmpty()) {
            return emptyList();
        }

        final String argumentToComplete = arguments.get(arguments.size() - 1);
        final List<String> preceedingArguments = arguments.subList(0, arguments.size() - 1);

        final List<CompletionSpec> partialMatchingNodes = findPartialMatchingNodes(completionSpecRoot, argumentToComplete);
        final List<CompletionSpec> nodesWithCompleteAncestry = findNodesWithCompleteAncestry(partialMatchingNodes, arguments);
        final List<CompletionSpec> nodesWithCorrectOccurenceCount = findNodesWithCorrectOccurenceCount(nodesWithCompleteAncestry, preceedingArguments);

        return findMostDeeplyNestedCompletions(argumentToComplete, nodesWithCorrectOccurenceCount);
    }

    private List<String> findMostDeeplyNestedCompletions(String argumentToComplete, List<CompletionSpec> complectionSpecs) {
        if (complectionSpecs.isEmpty()) {
            return emptyList();
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
        return completionSpecsByDepth.get(maxDepth);
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
            final String argumentToMatch = arguments.get(arguments.size() - 1);
            final List<String> preceedingArguments = arguments.subList(0, arguments.size() - 1);
            if (completionSpec.isCompleteMatch(argumentToMatch)) {
                return hasCompleteAncestry(completionSpec.getParent(), preceedingArguments);
            } else {
                return hasCompleteAncestry(completionSpec, preceedingArguments);
            }
        }
    }

    private List<CompletionSpec> findPartialMatchingNodes(CompletionSpec completionSpec, String argument) {
        final ArrayList<CompletionSpec> matches = new ArrayList<>();

        if (completionSpec.isPartialMatch(argument)) {
            matches.add(completionSpec);
        }

        for (CompletionSpec child : completionSpec.getChildren()) {
            matches.addAll(findPartialMatchingNodes(child, argument));
        }

        return matches;
    }

    private List<CompletionSpec> findCompleteMatchingNodes(CompletionSpec completionSpec, String argument) {
        final ArrayList<CompletionSpec> matches = new ArrayList<>();

        if (completionSpec.isCompleteMatch(argument)) {
            matches.add(completionSpec);
        }

        for (CompletionSpec child : completionSpec.getChildren()) {
            matches.addAll(findCompleteMatchingNodes(child, argument));
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
}