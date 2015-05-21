package no.nixx.aslan.core.completion;

import no.nixx.aslan.api.Executable;
import no.nixx.aslan.api.ExecutionContext;
import no.nixx.aslan.core.ExecutableLocator;
import no.nixx.aslan.pipeline.PipelineParser;
import no.nixx.aslan.pipeline.model.Argument;
import no.nixx.aslan.pipeline.model.Command;
import no.nixx.aslan.pipeline.model.Literal;
import no.nixx.aslan.pipeline.model.Pipeline;

import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static no.nixx.aslan.core.utils.ListUtils.*;
import static no.nixx.aslan.core.utils.StringUtils.*;

public class Completor {

    public CompletionResult getCompletions(String command, int tabPosition, ExecutableLocator executableLocator, ExecutionContext executionContext) {
        final String commandUpToTab = command.substring(0, tabPosition);
        final Pipeline pipelineToComplete = parseCommand(commandUpToTab);
        final boolean isUnableToParsePipeline = pipelineToComplete == null;

        if (isUnableToParsePipeline) {
            return createEmptyCompletionResult(command, tabPosition);
        } else {
            return getCompletionsForPipeline(command, tabPosition, pipelineToComplete, executableLocator, executionContext);
        }
    }

    private CompletionResult getCompletionsForPipeline(String command, int tabPosition, Pipeline pipelineToComplete, ExecutableLocator executableLocator, ExecutionContext executionContext) {
        final Command commandToComplete = pipelineToComplete.getCommandAtPosition(tabPosition - 1);
        final String executableName = commandToComplete.getExecutableName();
        final Executable executable = executableLocator.lookupExecutable(executableName);
        final List<String> executableCandidates = executableLocator.findExecutableCandidates(executableName);

        if (executableCandidates.isEmpty()) {
            return createEmptyCompletionResult(command, tabPosition);
        } else if (executableCandidates.size() == 1) {
            if (executable == null) {
                return createCompletionResult(command, commandToComplete, executableCandidates, true, true);
            } else {
                return getCompletionsForExecutable(command, tabPosition, commandToComplete, executionContext, executable);
            }
        } else {
            return createCompletionResult(command, commandToComplete, executableCandidates, true, true);
        }
    }

    private CompletionResult getCompletionsForExecutable(String command, int tabPosition, Command commandToComplete, ExecutionContext executionContext, Executable executable) {
        if (commandToComplete.getRenderedArguments().isEmpty()) {
            return new CompletionResult(command + " ", tabPosition + 1, emptyList());
        } else {
            if (executable instanceof Completable) {
                final Completable completable = (Completable) executable;
                return getCompletionsForCompletable(command, tabPosition, commandToComplete, completable.getCompletionSpec(executionContext));
            } else {
                return createEmptyCompletionResult(command, tabPosition);
            }
        }
    }

    private CompletionResult getCompletionsForCompletable(String command, int tabPosition, Command commandToComplete, CompletionSpecRoot completionSpecRoot) {
        final List<String> arguments = commandToComplete.getRenderedArguments();
        final TemporaryCompletionResult partialCompletionResult = getPartiallyMatchingCompletions(completionSpecRoot, arguments);
        final List<String> partialCompletions = partialCompletionResult.completions;
        if (partialCompletions.isEmpty()) {
            final TemporaryCompletionResult completeCompletionResult = getCompletelyMatchingCompletions(completionSpecRoot, arguments);
            final List<String> completeCompletions = completeCompletionResult.completions;
            if (completeCompletions.isEmpty()) {
                return createEmptyCompletionResult(command, tabPosition);
            } else {
                return createCompletionResult(command, commandToComplete, completeCompletions, completeCompletionResult.doAppendSpaceIfOnlyOneCompletion, completeCompletionResult.doAppendQuoteIfOnlyOneCompletion);
            }
        } else {
            return createCompletionResult(command, commandToComplete, partialCompletions, partialCompletionResult.doAppendSpaceIfOnlyOneCompletion, partialCompletionResult.doAppendQuoteIfOnlyOneCompletion);
        }
    }

    private CompletionResult createEmptyCompletionResult(String command, int tabPosition) {
        return new CompletionResult(command, tabPosition, emptyList());
    }

    private CompletionResult createCompletionResult(String command, Command commandToComplete, List<String> completions, boolean doAppendSpaceIfOnlyOneCompletion, boolean doAppendQuoteIfOnlyOneCompletion) {
        final boolean onlyOneCompletion = (completions.size() == 1);
        final String completion;
        if (onlyOneCompletion) {
            final String onlyCompletion = firstOf(completions);
            if (containsWhiteSpace(onlyCompletion)) {
                completion = String.format("\"%s%s%s", onlyCompletion, doAppendQuoteIfOnlyOneCompletion ? "\"" : "", doAppendSpaceIfOnlyOneCompletion ? " " : "");
            } else {
                completion = String.format("%s%s", onlyCompletion, doAppendSpaceIfOnlyOneCompletion ? " " : "");
            }
        } else {
            if (anyContainsWhiteSpace(completions)) {
                completion = "\"" + getCommonStartOfStrings(completions);
                completions = completions.stream().map(s -> "\"" + s + "\"").collect(toList());
            } else {
                completion = getCommonStartOfStrings(completions);
            }
        }

        final Argument argumentToReplace = lastOf(commandToComplete.getArgumentsUnmodifiable());
        final String commandUpToCompletion = command.substring(0, argumentToReplace.getStartIndex());
        final String commandAfterCompletion = (command.length() < argumentToReplace.getStopIndex()) ? "" : command.substring(argumentToReplace.getStopIndex());
        final String completedCommand = commandUpToCompletion + completion + commandAfterCompletion;
        final int newTabPosition = commandUpToCompletion.length() + completion.length();
        return new CompletionResult(completedCommand, newTabPosition, onlyOneCompletion ? emptyList() : completions);
    }

    private Pipeline parseCommand(String commandUpToTab) {
        final PartialPipelineSupplementor partialPipelineSupplementor = new PartialPipelineSupplementor();
        final String partialCommand = partialPipelineSupplementor.supplementPipeline(commandUpToTab);

        try {
            final PipelineParser parser = new PipelineParser();
            final Pipeline pipeline = parser.parseCommand(partialCommand);

            if (lastArgumentIsComplete(commandUpToTab)) {
                final Literal emptyLiteral = new Literal("", commandUpToTab.length(), commandUpToTab.length(), "");
                final List<Command> commands = pipeline.getCommandsUnmodifiable();
                final Command commandWithoutEmptyLiteral = lastOf(commands);
                final Command commandWithEmptyLiteral = commandWithoutEmptyLiteral.addArgument(emptyLiteral);
                return new Pipeline(replaceElement(commands, commandWithoutEmptyLiteral, commandWithEmptyLiteral));
            }

            return pipeline;
        } catch (Exception e) {
            return null;
        }
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
            return new TemporaryCompletionResult(true, true, emptyList());
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
        public final boolean doAppendSpaceIfOnlyOneCompletion;
        public final boolean doAppendQuoteIfOnlyOneCompletion;
        public final List<String> completions;

        TemporaryCompletionResult(boolean doAppendSpaceIfOnlyOneCompletion, boolean doAppendQuoteIfOnlyOneCompletion, List<String> completions) {
            this.doAppendSpaceIfOnlyOneCompletion = doAppendSpaceIfOnlyOneCompletion;
            this.doAppendQuoteIfOnlyOneCompletion = doAppendQuoteIfOnlyOneCompletion;
            this.completions = completions;
        }
    }
}