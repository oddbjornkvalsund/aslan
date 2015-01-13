package no.nixx.aslan.core.completion;

import no.nixx.aslan.pipeline.PipelineParser;
import no.nixx.aslan.pipeline.model.Command;
import no.nixx.aslan.pipeline.model.Pipeline;

import java.util.ArrayList;
import java.util.List;

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

        final List<CompletionSpec> partialMatches = getPartialMatchingNodes(completionSpecRoot, argumentToComplete);
        final List<CompletionSpec> completeMatches = getCompleteMatchingParents(partialMatches, arguments);
        final List<CompletionSpec> requiredCompleteMatches = getMatchesWhereParentRequiresArgument(completeMatches);
        final List<CompletionSpec> result = requiredCompleteMatches.isEmpty() ? completeMatches : requiredCompleteMatches;

        return result.stream().flatMap(completionSpec -> completionSpec.getCompletions(argumentToComplete).stream()).collect(toList());
    }

    private List<CompletionSpec> getPartialMatchingNodes(CompletionSpec completionSpec, String argumentToComplete) {
        final List<CompletionSpec> matchingNodes = new ArrayList<>();
        if (completionSpec.isPartialMatch(argumentToComplete)) {
            matchingNodes.add(completionSpec);
        }

        for (CompletionSpec childComplectionSpec : completionSpec.getChildren()) {
            matchingNodes.addAll(getPartialMatchingNodes(childComplectionSpec, argumentToComplete));
        }
        return matchingNodes;
    }

    private List<CompletionSpec> getCompleteMatchingParents(List<CompletionSpec> completionSpecs, List<String> arguments) {
        final List<String> parentArguments = arguments.subList(0, arguments.size() - 1);
        return completionSpecs.stream().filter(completionSpec -> isCompleteMatch(completionSpec.getParent(), parentArguments)).collect(toList());
    }

    private List<CompletionSpec> getMatchesWhereParentRequiresArgument(List<CompletionSpec> completionSpecs) {
        return completionSpecs.stream().filter(completionSpec -> completionSpec.hasParent() && completionSpec.getParent().isArgumentRequired()).collect(toList());
    }

    private boolean isCompleteMatch(CompletionSpec completionSpec, List<String> arguments) {
        if (completionSpec instanceof CompletionSpecRoot) {
            return true;
        } else if (arguments.isEmpty()) {
            return false;
        } else {
            final String lastArgument = arguments.get(arguments.size() - 1);
            return completionSpec.isCompleteMatch(lastArgument) && isCompleteMatch(completionSpec.getParent(), arguments.subList(0, arguments.size() - 1));
        }
    }
}