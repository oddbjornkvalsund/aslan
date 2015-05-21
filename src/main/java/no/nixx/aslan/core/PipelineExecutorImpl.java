package no.nixx.aslan.core;

import no.nixx.aslan.api.Executable;
import no.nixx.aslan.api.ExecutionContext;
import no.nixx.aslan.api.Program;
import no.nixx.aslan.pipeline.model.Argument;
import no.nixx.aslan.pipeline.model.Command;
import no.nixx.aslan.pipeline.model.CommandSubstitution;
import no.nixx.aslan.pipeline.model.CompositeArgument;
import no.nixx.aslan.pipeline.model.Literal;
import no.nixx.aslan.pipeline.model.Pipeline;
import no.nixx.aslan.pipeline.model.QuotedString;
import no.nixx.aslan.pipeline.model.VariableSubstitution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import static no.nixx.aslan.core.utils.ListUtils.firstOf;
import static no.nixx.aslan.core.utils.ListUtils.lastOf;
import static no.nixx.aslan.core.utils.Preconditions.checkNotNull;
import static no.nixx.aslan.core.utils.StringUtils.removeTrailingNewlines;

public class PipelineExecutorImpl implements PipelineExecutor {

    final Logger logger = LoggerFactory.getLogger(PipelineExecutorImpl.class);

    final ExecutorService threadPool;
    final ExecutableLocator executableLocator;
    final ExecutionContextFactory executionContextFactory;
    final PrintStream defaultErrorStream;
    private final InputStream defaultInputStream;
    private final OutputStream defaultOutputStream;

    public PipelineExecutorImpl(ExecutorService threadPool, ExecutableLocator executableLocator, ExecutionContextFactory executionContextFactory, InputStream defaultInputStream, OutputStream defaultOutputStream, OutputStream defaultErrorStream) {
        this.threadPool = threadPool;
        this.executableLocator = executableLocator;
        this.executionContextFactory = executionContextFactory;
        this.defaultInputStream = defaultInputStream;
        this.defaultOutputStream = defaultOutputStream;
        this.defaultErrorStream = new PrintStream(defaultErrorStream);
    }

    @Override
    public void execute(Pipeline pipeline) {
        final ExecutionContext executionContextForExpansion = executionContextFactory.createExecutionContext(defaultInputStream, defaultOutputStream, defaultErrorStream);
        final Pipeline pipelineWithExpandedArguments = expandArguments(executionContextForExpansion, pipeline);
        executePipeline(pipelineWithExpandedArguments, defaultInputStream, defaultOutputStream);
    }

    private Pipeline expandArguments(ExecutionContext context, Pipeline pipeline) {
        final ArrayList<Command> expandedCommands = new ArrayList<>();
        for (Command command : pipeline.getCommandsUnmodifiable()) {
            final ArrayList<Argument> expandedArguments = new ArrayList<>();
            for (Argument argument : command.getArguments()) {
                final ExpandedArgument expandedArgument;
                if (argument.isRenderable()) {
                    expandedArgument = new ExpandedArgument(argument.getRenderedText());
                } else if (argument.isCompositeArgument()) {
                    expandedArgument = new ExpandedArgument(getString(context, (CompositeArgument) argument));
                } else if (argument.isCommandSubstitution()) {
                    expandedArgument = new ExpandedArgument(getString(context, (CommandSubstitution) argument));
                } else if (argument.isVariableSubstitution()) {
                    expandedArgument = new ExpandedArgument(getString(context, (VariableSubstitution) argument));
                } else if (argument.isQuotedString()) {
                    expandedArgument = new ExpandedArgument(getString(context, (QuotedString) argument));
                } else {
                    throw new IllegalStateException("What's this?");
                }
                expandedArguments.add(expandedArgument);
            }
            expandedCommands.add(new Command(command, expandedArguments));
        }

        return new Pipeline(expandedCommands);
    }

    private String getString(Literal literal) {
        return literal.text;
    }

    private String getString(ExecutionContext context, QuotedString quotedString) {
        final StringBuilder sb = new StringBuilder(quotedString.getText());

        int offset = 0;
        for (QuotedString.Component component : quotedString.getComponents()) {
            final String expandedComponentText;
            if (component.argument.isVariableSubstitution()) {
                final VariableSubstitution vs = (VariableSubstitution) component.argument;
                expandedComponentText = getExpandedVariable(context, vs);
            } else if (component.argument.isCommandSubstitution()) {
                final CommandSubstitution cs = (CommandSubstitution) component.argument;
                final Pipeline expandedPipeline = expandArguments(context, cs.getPipeline());
                expandedComponentText = getExpandedCommand(expandedPipeline);
            } else {
                throw new IllegalStateException("Illegal component type, expected VariableSubstitution or CommandSubstitution: " + component.argument);
            }

            sb.insert(component.position + offset, expandedComponentText);
            offset += expandedComponentText.length();
        }

        return sb.toString();
    }

    private String getString(ExecutionContext context, VariableSubstitution vs) {
        return getExpandedVariable(context, vs);
    }

    private String getString(ExecutionContext context, CommandSubstitution cs) {
        final Pipeline expandedPipeline = expandArguments(context, cs.getPipeline());
        return getExpandedCommand(expandedPipeline);
    }

    private String getString(ExecutionContext context, CompositeArgument compositeArgument) {
        final StringBuilder sb = new StringBuilder();
        for (Argument arg : compositeArgument) {
            if (arg.isLiteral()) {
                sb.append(getString((Literal) arg));
            } else if (arg.isCommandSubstitution()) {
                sb.append(getString(context, (CommandSubstitution) arg));
            } else if (arg.isVariableSubstitution()) {
                sb.append(getString(context, (VariableSubstitution) arg));
            } else if (arg.isQuotedString()) {
                sb.append(getString(context, (QuotedString) arg));
            } else if (arg.isCompositeArgument()) {
                throw new IllegalStateException("Directly nested composite arguments should not be possible, this is a bug.");
            }
        }
        return sb.toString();
    }

    private String getExpandedVariable(ExecutionContext context, VariableSubstitution vs) {
        return context.getVariable(vs.variableName);
    }

    private String getExpandedCommand(Pipeline pipeline) {
        final InputStream in = new ByteArrayInputStream(new byte[0]);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        executePipeline(pipeline, in, out);

        return removeTrailingNewlines(out.toString());
    }

    private void executePipeline(Pipeline pipeline, InputStream outerInputStream, OutputStream outerOutputStream) {
        final List<ExecutableWithExecutionContextAndArgs> executables = new ArrayList<>();

        final List<Command> commands = pipeline.getCommandsUnmodifiable();
        if (commands.isEmpty()) {
            return;
        }

        final Command first = firstOf(commands);
        final Command last = lastOf(commands);

        Pipe pipe = new Pipe();
        for (Command command : commands) {
            final InputStream in;
            final OutputStream out;
            if (command == first && command == last) {
                in = outerInputStream;
                out = outerOutputStream;
            } else if (command == first) {
                in = outerInputStream;
                out = pipe.getSink();
            } else if (command == last) {
                in = pipe.getSource();
                out = outerOutputStream;
            } else {
                in = pipe.getSource();
                pipe = new Pipe();
                out = pipe.getSink();
            }

            final Executable executable = executableLocator.lookupExecutable(command.getExecutableName());
            if (executable == null) {
                throw new IllegalArgumentException(command.getExecutableName() + ": command not found");
            }
            if (executable instanceof ShellUtil) {
                final ShellUtilExecutionContext context = executionContextFactory.createShellUtilExecutionContext(in, out, defaultErrorStream);
                executables.add(new ExecutableWithExecutionContextAndArgs(executable, context, command.getRenderedArguments()));
            } else {
                final ExecutionContext context = executionContextFactory.createExecutionContext(in, out, defaultErrorStream);
                executables.add(new ExecutableWithExecutionContextAndArgs(executable, context, command.getRenderedArguments()));
            }
        }

        final CountDownLatch latch = new CountDownLatch(executables.size());
        for (final ExecutableWithExecutionContextAndArgs executableWithExecutionContextAndArgs : executables) {
            threadPool.execute(() -> {
                final Executable executable = executableWithExecutionContextAndArgs.executable;
                final ExecutionContext executionContext = executableWithExecutionContextAndArgs.executionContext;
                final String executableName = getExecutableName(executable);
                try {
                    if (executable instanceof Program) {
                        final Program program = (Program) executable;
                        program.run(executionContext, executableWithExecutionContextAndArgs.args);
                    } else if (executable instanceof ShellUtil) {
                        final ShellUtil shellUtil = (ShellUtil) executable;
                        shellUtil.run((ShellUtilExecutionContext) executionContext, executableWithExecutionContextAndArgs.args);
                    } else {
                        throw new IllegalStateException("Unknown executable type: " + executable);
                    }
                } catch (Throwable t) {
                    // TODO: Direct logging output to logfile
                    logger.error(t.getMessage(), t);
                    defaultErrorStream.println(executableName + ": " + t.getMessage());
                } finally {
                    try {
                        if (executionContext.input() != System.in) {
                            executionContext.input().close();
                        }

                        executionContext.output().flush();
                        if (executionContext.output() != System.out) {
                            executionContext.output().close();
                        }

                        defaultErrorStream.flush();
                        if (defaultErrorStream != System.out) {
                            defaultErrorStream.close();
                        }
                    } catch (IOException e) {
                        System.err.println("Unable to close streams: " + e.getMessage());
                    }

                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // TODO: Consider setting the EXITSTATUS and PIPESTATUS variables here
        // (ref. http://unix.stackexchange.com/questions/14270/get-exit-status-of-process-thats-piped-to-another)
    }

    private String getExecutableName(Executable executable) {
        final ExecutableMetadata metadata = executable.getClass().getAnnotation(ExecutableMetadata.class);
        return metadata.name();
    }

    private class ExecutableWithExecutionContextAndArgs {
        public final Executable executable;
        public final ExecutionContext executionContext;
        private final List<String> args;

        ExecutableWithExecutionContextAndArgs(Executable executable, ExecutionContext executionContext, List<String> args) {
            this.executable = executable;
            this.executionContext = executionContext;
            this.args = args;
        }
    }

    private class ExpandedArgument extends Argument {

        private final String text;

        public ExpandedArgument(String text) {
            this.text = checkNotNull(text);
        }

        @Override
        public boolean isRenderable() {
            return true;
        }

        @Override
        public String getRenderedText() {
            return text;
        }

        @Override
        public int getStartIndex() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getStopIndex() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getUnprocessedArgument() {
            throw new UnsupportedOperationException();
        }
    }
}