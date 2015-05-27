package no.nixx.aslan.pipeline.model;

import static no.nixx.aslan.core.utils.Preconditions.checkArgument;
import static no.nixx.aslan.core.utils.Preconditions.checkNotNull;

public abstract class Argument {

    private final ArgumentProperties properties;

    // This constructor should only be used by special subclasses such as ExpandedArgument in PipelineExecutor
    public Argument() {
        this.properties = ArgumentProperties.undefined();
    }

    public Argument(ArgumentProperties properties) {
        this.properties = checkNotNull(properties);
    }

    public abstract boolean isRenderable();

    public abstract String getRenderedText();

    public ArgumentProperties getProperties() {
        return properties;
    }

    public boolean spansPosition(int position) {
        return properties.startIndex <= position && properties.stopIndex >= position;
    }

    public boolean precedes(Argument that) {
        return this.getStartIndex() < that.getStartIndex() && this.getStopIndex() < that.getStopIndex();
    }

    public boolean isLiteral() {
        return false;
    }

    public boolean isQuotedString() {
        return false;
    }

    public boolean isCommandSubstitution() {
        return false;
    }

    public boolean isVariableSubstitution() {
        return false;
    }

    public boolean isCompositeArgument() {
        return false;
    }

    public int getStartIndex() {
        return properties.startIndex;
    }

    public int getStopIndex() {
        return properties.stopIndex;
    }

    public String getUnprocessedArgument() {
        return properties.unprocessedArgument;
    }

    /**
     * Returns the rendered version of this Argument up to position, with position absolute with respect to startIndex and stopIndex.
     */
    public String substring(int position) {
        checkArgument(isRenderable());
        checkArgument(position >= properties.startIndex);
        checkArgument(position <= properties.stopIndex);

        boolean inSingleQuotes = false;
        boolean inDoubleQuotes = false;
        int correctedPosition = position;

        // Correct the position argument to compensate for the fact that unprocessedArgument may contain quotation
        // marks not present in the rendered text. I.e. the unprocessedArgument ""''""'' would render as the empty string.
        // See exhaustive tests in class ArgumentTest.
        for (char c : properties.unprocessedArgument.substring(0, position - properties.startIndex).toCharArray()) {
            if (c == '\'') {
                if (!inDoubleQuotes) {
                    inSingleQuotes = !inSingleQuotes;
                    correctedPosition--;
                }
            } else if (c == '"') {
                if (!inSingleQuotes) {
                    inDoubleQuotes = !inDoubleQuotes;
                    correctedPosition--;
                }
            }
        }

        return getRenderedText().substring(0, correctedPosition - properties.startIndex);
    }
}
