package no.nixx.aslan.api;

import java.util.List;

public interface Program extends Executable {

    void run(ExecutionContext executionContext, List<String> args);

}