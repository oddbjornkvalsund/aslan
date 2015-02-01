package no.nixx.aslan.core.completion;

import no.nixx.aslan.pipeline.PipelineParser;
import no.nixx.aslan.pipeline.model.Command;
import no.nixx.aslan.pipeline.model.Pipeline;

import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

public class Completor {

    public List<String> getCompletions(String command, int tabPosition, CompletionSpecRoot completionSpecRoot) {
        return getCompletions(completionSpecRoot, getArguments(command, tabPosition));
    }

    private List<String> getArguments(String command, int tabPosition) {
        final String commandUpToTab = command.substring(0, tabPosition);
        final Pipeline pipeline;
        try {
            // Snipping up to tabPosition means we always want the last command in the pipeline
            final PipelineParser parser = new PipelineParser();
            pipeline = parser.parseCommand(commandUpToTab);
        } catch (Exception e) {
            return emptyList();
        }

        final List<Command> commands = pipeline.getCommandsUnmodifiable();
        final Command commandToComplete = commands.get(commands.size() - 1);
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
        final Map<Integer, List<String>> completionSpecsByDepth = new TreeMap<>();
        for (CompletionSpec completionSpec : complectionSpecs) {
            final int level = getLevel(completionSpec);

            if (!completionSpecsByDepth.containsKey(level)) {
                completionSpecsByDepth.put(level, new ArrayList<>());
            }

            completionSpecsByDepth.get(level).addAll(completionSpec.getCompletions(argumentToComplete));
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

    private int getLevel(CompletionSpec completionSpec) {
        if (completionSpec instanceof CompletionSpecRoot) {
            return 0;
        } else {
            return getLevel(completionSpec.getParent()) + 1;
        }
    }
}