package no.nixx.aslan.core.completion;

import java.util.ArrayList;

public class PartialCommandExtractor {

    public String getLastCommand(String cmd) {
        final CommandStack cs = new CommandStack();

        if(cmd.isEmpty()) {
            return cmd;
        }

        char p = 0;
        int nestedLevels = 0;
        boolean inSingleQuotedString = false;

        for (char c : cmd.toCharArray()) {
            if (cs.depth() == 0) {
                cs.push();
            }

            cs.append(c);

            if (c == '\'') {
                inSingleQuotedString = !inSingleQuotedString;
            } else {
                if (!inSingleQuotedString) {
                    if (c == '|') {
                        cs.pop();
                        cs.push();
                    } else if (p == '$' && c == '(') {
                        cs.push();
                        nestedLevels++;
                    } else if (nestedLevels > 0 && c == ')') {
                        cs.pop();
                        nestedLevels--;
                    }
                }
            }

            p = c;
        }

        return cs.getCurrentCommand();
    }

    private class CommandStack {

        private ArrayList<StringBuilder> commands = new ArrayList<>();

        public void append(char c) {
            if(commands.isEmpty()) {
                throw new IllegalStateException("No current command to append to: " + c);
            }

            for (StringBuilder sb : commands) {
                sb.append(c);
            }
        }

        public void push() {
            commands.add(new StringBuilder());
        }

        public void pop() {
            if(commands.isEmpty()) {
                throw new IllegalStateException("No current command to pop!");
            }

            commands.remove(commands.size() - 1);
        }

        public String getCurrentCommand() {
            if(commands.isEmpty()) {
                throw new IllegalStateException("No current command!");
            }

            return commands.get(commands.size() - 1).toString();
        }

        public int depth() {
            return commands.size();
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            for (StringBuilder level : commands) {
                sb.append(String.format("Command %d: %s\n", commands.indexOf(level), level));
            }
            return sb.toString();
        }
    }
}