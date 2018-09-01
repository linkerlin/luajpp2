package nl.weeaboo.lua2.luajava;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.weeaboo.lua2.LuaRunState;
import nl.weeaboo.lua2.vm.LuaConstants;
import nl.weeaboo.lua2.vm.LuaString;
import nl.weeaboo.lua2.vm.LuaValue;
import nl.weeaboo.lua2.vm.Varargs;

final class RunLuaMethodBenchmark {

    private static final Logger LOG = LoggerFactory.getLogger(RunLuaMethodBenchmark.class);
    private static final LuaString METHOD_A = LuaString.valueOf("methodA");

    private final Potato potato;
    private final LuaValue luaPotato;

    private RunLuaMethodBenchmark() {
        LuaRunState.create();

        potato = new Potato();
        luaPotato = CoerceJavaToLua.coerce(potato);
    }

    public static void main(String[] args) {
        RunLuaMethodBenchmark benchmark = new RunLuaMethodBenchmark();
        while (true) {
            benchmark.run();
        }
    }

    private void run() {
        long t0 = System.nanoTime();
        final int runs = 100_000_000;

        Varargs result = LuaConstants.NONE;
        for (int run = 0; run < runs; run++) {
            result = luaPotato.invokemethod(METHOD_A);
        }

        double nanosPerCall = (System.nanoTime() - t0) / (double)runs;

        LOG.trace(result.tojstring());
        LOG.info("{}ns per call", nanosPerCall);
    }

    @SuppressWarnings("unused")
    private static final class Potato {

        public int methodA() {
            return 1;
        }

        public int methodB() {
            return 2;
        }

        public int methodC() {
            return 3;
        }

        public int methodD() {
            return 4;
        }

        public int methodE() {
            return 5;
        }

    }

}
