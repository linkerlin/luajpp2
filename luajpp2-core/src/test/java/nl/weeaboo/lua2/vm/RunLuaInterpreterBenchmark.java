package nl.weeaboo.lua2.vm;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.weeaboo.lua2.LuaRunState;
import nl.weeaboo.lua2.compiler.LoadState;

final class RunLuaInterpreterBenchmark {

    private static final Logger LOG = LoggerFactory.getLogger(RunLuaInterpreterBenchmark.class);

    private static final String INIT =
                      "object = {}\n"
                    + "function object:a() return 1 end\n"
                    + "function object:b() return 2 end\n";

    private static final int BATCH_SIZE = 1_000_000;

    private static final String RUN =
                      "local sum = 0\n"
                    + "for i=1," + BATCH_SIZE + " do sum = sum + 1 end\n"
                    + "for i=1," + BATCH_SIZE + " do sum = sum + object:a() end\n";

    private LuaRunState luaRunState;

    private RunLuaInterpreterBenchmark() {
        luaRunState = LuaRunState.create();
        luaRunState.setInstructionCountLimit(Integer.MAX_VALUE);
        // luaRunState.setDebugEnabled(false);
    }

    public static void main(String[] args) throws IOException {
        RunLuaInterpreterBenchmark benchmark = new RunLuaInterpreterBenchmark();
        while (true) {
            benchmark.run();
        }
    }

    private void run() throws IOException {
        LoadState.load(INIT, "?", luaRunState.getGlobalEnvironment()).call();
        LuaFunction run = LoadState.load(RUN, "?", luaRunState.getGlobalEnvironment());

        long t0 = System.nanoTime();

        LuaValue result = run.call();

        double nanosPerCall = (System.nanoTime() - t0) / (double)BATCH_SIZE;

        LOG.trace(result.tojstring());
        LOG.info("{}ns per call", nanosPerCall);
    }

}
