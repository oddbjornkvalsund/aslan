package no.nixx.aslan.core.completion;

import java.util.Stack;

public class PartialPipelineSupplementor {

    public String supplementPipeline(String partialPipeline) {
        if (partialPipeline == null) {
            throw new IllegalArgumentException("Pipeline cannot be null!");
        } else if (partialPipeline.isEmpty()) {
            return partialPipeline;
        }

        final CommandStateStack stateStack = new CommandStateStack();

        char p = 0;
        for (char c : partialPipeline.toCharArray()) {
            if (c == '\'') {
                if (stateStack.inSingleQuotes()) {
                    stateStack.pop();
                } else {
                    stateStack.push(CommandState.IN_SINGLE_QUOTES);
                }
            } else if (c == '"') {
                if (stateStack.inDoubleQuotes()) {
                    stateStack.pop();
                } else {
                    stateStack.push(CommandState.IN_DOUBLE_QUOTES);
                }
            } else if (p == '$' && c == '(') {
                if (!stateStack.inSingleQuotes()) {
                    stateStack.push(CommandState.IN_CS);
                }
            } else if (c == ')') {
                if (stateStack.inCommandSubstitution()) {
                    stateStack.pop();
                }
            } else if (p == '$' && c == '{') {
                if (!stateStack.inSingleQuotes()) {
                    stateStack.push(CommandState.IN_VS);
                }
            } else if (c == '}') {
                if (stateStack.inVariableSubstitution()) {
                    stateStack.pop();
                }
            }
            p = c;
        }

        final StringBuilder sb = new StringBuilder(partialPipeline);
        while (!stateStack.isEmpty()) {
            sb.append(stateStack.pop().supplement);
        }

        return sb.toString();
    }

    private enum CommandState {
        BALANCED(""),
        IN_SINGLE_QUOTES("'"),
        IN_DOUBLE_QUOTES("\""),
        IN_CS(")"),
        IN_VS("}");

        final String supplement;

        CommandState(String supplement) {
            this.supplement = supplement;
        }
    }

    private class CommandStateStack extends Stack<CommandState> {

        private static final long serialVersionUID = 2884722987827868503L;

        public boolean inSingleQuotes() {
            return currentState() == CommandState.IN_SINGLE_QUOTES;
        }

        public boolean inDoubleQuotes() {
            return currentState() == CommandState.IN_DOUBLE_QUOTES;
        }

        public boolean inCommandSubstitution() {
            return currentState() == CommandState.IN_CS;
        }

        public boolean inVariableSubstitution() {
            return currentState() == CommandState.IN_VS;
        }

        private CommandState currentState() {
            if (isEmpty()) {
                return CommandState.BALANCED;
            } else {
                return peek();
            }
        }
    }
}