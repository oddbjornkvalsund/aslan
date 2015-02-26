package no.nixx.aslan.core;

import no.nixx.aslan.api.Executable;

import java.util.List;

public interface ShellUtil extends Executable {

    public void run(ShellUtilExecutionContext context, List<String> args);

}