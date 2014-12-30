package no.nixx.wing.pipeline;

import no.nixx.wing.antlr.PipelineLexer;
import no.nixx.wing.antlr.PipelineParser;
import no.nixx.wing.antlr.PipelineParserBaseListener;
import no.nixx.wing.core.Executable;
import no.nixx.wing.core.ExecutableLocator;
import no.nixx.wing.core.ExecutableLocatorImpl;
import no.nixx.wing.core.Pipe;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.misc.NotNull;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ParserTest {
    final Map<String, String> context = new HashMap<String, String>();
    final ExecutorService threadPool = Executors.newFixedThreadPool(10); // TODO: Hard coded pool size
    final ExecutableLocator executableLocator = new ExecutableLocatorImpl();

    public static void main(String[] args) {
        new ParserTest().run();
    }

    public void run() {
        context.putAll(System.getenv());
        context.put("CWD", System.getProperty("user.dir"));// TODO: HACK

        final String cmd = "ls | grep t | grep o";
        final PipelineLexer lexer = new PipelineLexer(new ANTLRInputStream(cmd));
        final PipelineParser parser = new PipelineParser(new BufferedTokenStream(lexer));

        final PipelineListener pipelineListener = new PipelineListener();
        parser.addParseListener(pipelineListener);
        parser.pipeline();

        final Pipeline pipeline = pipelineListener.getPipeline();
        substituteVariables(pipeline); // TODO: Handle "string" with vs and cs
        substituteCommands(pipeline); // TODO: Not implemented

        execute(pipeline);

        try {
            threadPool.shutdown();
            threadPool.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void substituteVariables(Pipeline pipeline) {
        for (Command command : pipeline.getCommandsUnmodifiable()) {
            for (Argument argument : command.getArgumentsUnmodifiable()) {
                if (argument.isVariableSubstitution()) {
                    command.replaceArgument(argument, getExpandedVariable((VariableSubstitution) argument));
                }
            }
        }
    }

    private Argument getExpandedVariable(VariableSubstitution vs) {
        if (context.containsKey(vs.variableName)) {
            return new Literal(context.get(vs.variableName));
        } else {
            throw new IllegalArgumentException("No such variable: " + vs.variableName);
        }
    }

    private void substituteCommands(@SuppressWarnings("UnusedParameters") Pipeline pipeline) {

    }

    private void execute(Pipeline pipeline) {
        final List<Executable> executables = new ArrayList<Executable>();

        // TODO: Pipeline sanity checks

        // TODO: References to System.in, System.out and System.err should perhaps be removed from this method

        final List<Command> commands = pipeline.getCommandsUnmodifiable();
        final Command first = commands.get(0);
        final Command last = commands.get(commands.size() - 1);

        Pipe pipe = new Pipe();
        for (Command command : commands) {
            final InputStream in;
            final OutputStream out;
            if (command == first && command == last) {
                in = System.in;
                out = System.out;
            } else if (command == first) {
                in = System.in;
                out = pipe.getSink();
            } else if (command == last) {
                in = pipe.getSource();
                out = System.out;
            } else {
                in = pipe.getSource();
                pipe = new Pipe();
                out = pipe.getSink();
            }

            final Executable executable = executableLocator.lookupExecutable(command.getExecutableName());
            executable.init(in, out, System.err, context, command.getArgumentsAsStrings()); // TODO: What do we do here if this fails?
            executables.add(executable);
        }

        final CountDownLatch latch = new CountDownLatch(executables.size());
        for (final Executable executable : executables) {
            threadPool.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        executable.run();
                    } catch (Throwable t) {
                        throw new RuntimeException(t); // TODO: Where does this go?
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}

class PipelineListener extends PipelineParserBaseListener {

    final Pipeline pipeline = new Pipeline();

    private Command currentCommand; // TODO: This must be a Stack<Command>

    public Pipeline getPipeline() {
        return pipeline;
    }

    @Override
    public void enterCmd(@NotNull PipelineParser.CmdContext ctx) {
        currentCommand = new Command();
    }

    @Override
    public void exitCmd(@NotNull PipelineParser.CmdContext ctx) {
        pipeline.addCommand(currentCommand);
    }

    @Override
    public void exitArg(@NotNull PipelineParser.ArgContext ctx) {
        if (ctx.ARG() != null) {
            currentCommand.addArgument(new Literal(ctx.ARG().getText()));
        }
    }

    @Override
    public void exitLiteral(@NotNull PipelineParser.LiteralContext ctx) {
        currentCommand.addArgument(new Literal(ctx.LT_TEXT().getText()));
    }

    @Override
    public void exitVs(@NotNull PipelineParser.VsContext ctx) {
        currentCommand.addArgument(new VariableSubstitution(ctx.VS_VARIABLE().getText()));
    }

    @Override
    public void enterCs(@NotNull PipelineParser.CsContext ctx) {
        // TODO
    }

    @Override
    public void exitCs(@NotNull PipelineParser.CsContext ctx) {
        // TODO
    }

    @Override
    public void enterText(@NotNull PipelineParser.TextContext ctx) {
        // TODO
    }

    @Override
    public void exitText(@NotNull PipelineParser.TextContext ctx) {
        // TODO
    }
}