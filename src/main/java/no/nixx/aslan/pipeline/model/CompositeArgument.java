package no.nixx.aslan.pipeline.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static no.nixx.aslan.core.utils.ListUtils.firstOf;
import static no.nixx.aslan.core.utils.ListUtils.lastOf;

public class CompositeArgument extends Argument implements Iterable<Argument> {

    private final List<Argument> arguments = new ArrayList<>();

    @Override
    public boolean isRenderableTextAvailableWithoutCommmandExecution() {
        return arguments.stream().allMatch(Argument::isRenderableTextAvailableWithoutCommmandExecution);
    }

    @Override
    public String getRenderableText() {
        if (!isRenderableTextAvailableWithoutCommmandExecution()) {
            throw new IllegalStateException("Renderable text is not available without commmand execution: " + this);
        }

        final StringBuilder sb = new StringBuilder();
        arguments.stream().forEach(s -> sb.append(s.getRenderableText()));
        return sb.toString();
    }

    @Override
    public boolean isCompositeArgument() {
        return true;
    }

    @Override
    public Iterator<Argument> iterator() {
        return arguments.iterator();
    }

    public void addArgument(Argument argument) {
        arguments.add(argument);
    }

    public Argument get(int idx) {
        return arguments.get(idx);
    }

    public Argument firstArgument() {
        return firstOf(arguments);
    }

    public Argument lastArgument() {
        return lastOf(arguments);
    }

    public int size() {
        return arguments.size();
    }

    public boolean isEmpty() {
        return arguments.isEmpty();
    }

    @Override
    public String toString() {
        return "CompositeArgument{" +
                "arguments=" + arguments +
                '}';
    }
}