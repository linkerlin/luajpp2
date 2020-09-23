package nl.weeaboo.lua2.stdlib;

import static nl.weeaboo.lua2.vm.LuaConstants.NONE;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import nl.weeaboo.lua2.AbstractLuaTest;
import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.vm.LuaThread;

public class DebugTraceTest extends AbstractLuaTest {

    /** Test stacktrace functions */
    @Test
    public void callEvalStackTrace() throws LuaException {
        // Load script with some test functions
        LuaThread thread = loadScript("/stdlib/stacktrace.lua");
        thread.resume(NONE);

        // Main thread is paused in the yield call inside z
        assertStackTrace(thread,
                "z (/stdlib/stacktrace.lua:13)",
                "y (/stdlib/stacktrace.lua:9)",
                "x (/stdlib/stacktrace.lua:5)",
                "? (/stdlib/stacktrace.lua:16)");

        thread.resume(NONE);

        // Main thread is paused in the yield call inside tailz
        // A tail call overwrites the current function in the call stack when called
        assertStackTrace(thread,
                "tailz (/stdlib/stacktrace.lua:29)",
                "? (/stdlib/stacktrace.lua:32)");
    }

    private void assertStackTrace(LuaThread thread, String... expected) {
        Assert.assertEquals(Arrays.asList(expected), DebugTrace.stackTrace(thread).stream()
                .map(e -> e.toString())
                .collect(Collectors.toList()));
    }

}
