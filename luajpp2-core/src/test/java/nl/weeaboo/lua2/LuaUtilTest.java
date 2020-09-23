package nl.weeaboo.lua2;

import static nl.weeaboo.lua2.vm.LuaConstants.NONE;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import nl.weeaboo.lua2.vm.LuaClosure;
import nl.weeaboo.lua2.vm.LuaInteger;
import nl.weeaboo.lua2.vm.LuaThread;
import nl.weeaboo.lua2.vm.LuaValue;

public class LuaUtilTest extends AbstractLuaTest {

    /** Test for eval and stacktrace functions. */
    @SuppressWarnings("deprecation") // Unit test of deprecated method
    @Test
    public void callEvalStackTrace() throws LuaException {
        // Load script with some test functions
        LuaThread thread = loadScript("util/calleval.lua");
        thread.resume(NONE);

        // Call test function
        LuaClosure x = LuaUtil.getEntryForPath(thread, "x").checkclosure();
        thread.callFunctionInThread(x, LuaValue.varargsOf(LuaInteger.valueOf(1), LuaInteger.valueOf(2),
                LuaInteger.valueOf(3)));

        // Check that parameters were passed and the function was executed
        LuaTestUtil.assertGlobal("test", 1 + 2 + 3);

        // Main thread is paused in the yield call on line 13
        Assert.assertEquals(Arrays.asList("/util/calleval.lua:13"), LuaUtil.getLuaStack(thread));

        // Try to run some arbitrary code
        LuaUtil.eval(thread, "test = 42");
        LuaTestUtil.assertGlobal("test", 42);
        // After eval completes, the thread is still stuck in the same position as before
        Assert.assertEquals(Arrays.asList("/util/calleval.lua:13"), LuaUtil.getLuaStack(thread));
    }

}
