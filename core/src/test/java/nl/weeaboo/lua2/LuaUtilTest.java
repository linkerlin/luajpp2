package nl.weeaboo.lua2;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import nl.weeaboo.lua2.link.LuaLink;

public class LuaUtilTest extends AbstractLuaTest {

    /** Test for eval and stacktrace functions. */
    @Test
    public void callEvalStackTrace() throws LuaException {
        // Load script with some test functions
        LuaLink thread = loadScript("util/calleval.lua");
        thread.update();

        // Call test function
        thread.call("x", 1, 2, 3);

        // Check that parameters were passed and the function was executed
        LuaTestUtil.assertGlobal("test", 1 + 2 + 3);

        // Main thread is paused in the yield call on line 13
        Assert.assertEquals(Arrays.asList("/util/calleval.lua:13"), LuaUtil.getLuaStack(thread.getThread()));

        // Run some arbitrary code
        LuaUtil.eval(thread, "test = 42"); // TODO: Thread.sleep is still > 0, so call fails now...
        LuaTestUtil.assertGlobal("test", 42);
        // After eval completes, the thread is still stuck in the same position as before
        Assert.assertEquals(Arrays.asList("/util/calleval.lua:13"), LuaUtil.getLuaStack(thread.getThread()));
    }

}
