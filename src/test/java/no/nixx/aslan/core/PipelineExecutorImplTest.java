package no.nixx.aslan.core;

import no.nixx.aslan.pipeline.PipelineParser;
import no.nixx.aslan.pipeline.model.Pipeline;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;

public class PipelineExecutorImplTest {

    final static PipelineParser parser = new PipelineParser();
    final static ExecutableLocatorImpl executableLocator = new ExecutableLocatorImpl();
    final static ExecutionContextFactoryImpl executionContextFactory = new ExecutionContextFactoryImpl(new WorkingDirectoryImpl("."));

    static ExecutorService threadPool;

    @BeforeClass
    public static void startupThreadPool() throws Exception {
        threadPool = Executors.newFixedThreadPool(4);
    }

    @AfterClass
    public static void shutdownThreadPool() throws Exception {
        threadPool.shutdown();
        threadPool.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    public void testEmptyPipeline() {
        final InputStream in = getEmptyInputStream();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final PipelineExecutor executor = new PipelineExecutorImpl(threadPool, executableLocator, executionContextFactory, in, out, System.err);
        executor.execute(new Pipeline());

        assertEquals(format(""), out.toString());
    }

    @Test
    public void testEcho() {
        final InputStream in = getEmptyInputStream();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final PipelineExecutor executor = new PipelineExecutorImpl(threadPool, executableLocator, executionContextFactory, in, out, System.err);
        executor.execute(parser.parseCommand("echo foo"));

        assertEquals(format("foo%n"), out.toString());
    }

    @Test
    public void testPipeline() {
        final InputStream in = getEmptyInputStream();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final PipelineExecutor executor = new PipelineExecutorImpl(threadPool, executableLocator, executionContextFactory, in, out, System.err);
        executor.execute(parser.parseCommand("echo foo | grep foo"));

        assertEquals(format("foo%n"), out.toString());
    }

    @Test
    public void testVariableSubstitution() {
        final InputStream in = getEmptyInputStream();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final PipelineExecutor executor = new PipelineExecutorImpl(threadPool, executableLocator, executionContextFactory, in, out, System.err);
        executionContextFactory.setVariable("HOME", "MyHome");
        final Pipeline pipeline = parser.parseCommand("echo ${HOME}");
        executor.execute(pipeline);

        assertEquals(format("MyHome%n"), out.toString());
    }

    @Test
    public void testVariableSubstitutionInQuotedString() {
        final InputStream in = getEmptyInputStream();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final PipelineExecutor executor = new PipelineExecutorImpl(threadPool, executableLocator, executionContextFactory, in, out, System.err);
        executionContextFactory.setVariable("A", "aaa");
        executionContextFactory.setVariable("B", "bbb");
        executionContextFactory.setVariable("C", "ccc");
        executor.execute(parser.parseCommand("echo \"A: ${A}, B: ${B}, C: ${C}\""));
        assertEquals(format("A: aaa, B: bbb, C: ccc%n"), out.toString());
    }

    @Test
    public void testCommandSubstitution() {
        final InputStream in = getEmptyInputStream();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final PipelineExecutor executor = new PipelineExecutorImpl(threadPool, executableLocator, executionContextFactory, in, out, System.err);
        executor.execute(parser.parseCommand("echo $(echo foo | grep o | grep o)"));

        assertEquals(format("foo%n"), out.toString());
    }

    @Test
    public void testCommandSubstitutionInQuotedString() {
        final InputStream in = getEmptyInputStream();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final PipelineExecutor executor = new PipelineExecutorImpl(threadPool, executableLocator, executionContextFactory, in, out, System.err);
        executor.execute(parser.parseCommand("echo \"echo foo outputs: $(echo foo).\""));

        assertEquals(format("echo foo outputs: foo.%n"), out.toString());
    }

    @Test
    public void testComplexQuotedString() {
        final InputStream in = getEmptyInputStream();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final PipelineExecutor executor = new PipelineExecutorImpl(threadPool, executableLocator, executionContextFactory, in, out, System.err);
        executionContextFactory.setVariable("O", "o");
        executor.execute(parser.parseCommand("echo \"echo foo outputs: $(echo $(echo foo | grep $(echo ${O})))\""));

        assertEquals(format("echo foo outputs: foo%n"), out.toString());
    }

    @Test
    public void testCommandSubstitutionWithErrors() {
        final InputStream in = getEmptyInputStream();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final PipelineExecutor executor = new PipelineExecutorImpl(threadPool, executableLocator, executionContextFactory, in, out, System.err);
        executor.execute(parser.parseCommand("echo $(echo fooooooo | failwhenrun | grep o)"));

        // The command substitution prints 4 characters before failing. The newline is added by the first "echo"
        assertEquals(format("fooo%n"), out.toString());
    }

    @Test
    public void testCompositeArguments() {
        final InputStream in = getEmptyInputStream();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final PipelineExecutor executor = new PipelineExecutorImpl(threadPool, executableLocator, executionContextFactory, in, out, System.err);
        executor.execute(parser.parseCommand("echo foo-$(echo bar-$(echo zomg))\" some spaces \""));
        assertEquals(format("foo-bar-zomg some spaces %n"), out.toString());
    }

    private ByteArrayInputStream getEmptyInputStream() {
        return new ByteArrayInputStream(new byte[0]);
    }
}