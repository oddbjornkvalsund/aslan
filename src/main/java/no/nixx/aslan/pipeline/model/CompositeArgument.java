package no.nixx.aslan.pipeline.model;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableList;
import static no.nixx.aslan.core.utils.Preconditions.checkNotNull;

public class CompositeArgument extends Argument implements Iterable<Argument> {

    private final List<Argument> arguments;

    public CompositeArgument(List<Argument> arguments, ArgumentProperties properties) {
        super(properties);
        this.arguments = unmodifiableList(checkNotNull(arguments));
    }

    public List<Argument> getArguments() {
        return arguments;
    }

    @Override
    public boolean isRenderable() {
        return arguments.stream().allMatch(Argument::isRenderable);
    }

    @Override
    public String getRenderedText() {
        return arguments.stream().map(Argument::getRenderedText).collect(Collectors.joining());
    }

    @Override
    public boolean isCompositeArgument() {
        return true;
    }

    @Override
    public Iterator<Argument> iterator() {
        return arguments.iterator();
    }

    public Argument get(int idx) {
        return arguments.get(idx);
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