package nl.weeaboo.lua2.lib;

import static nl.weeaboo.lua2.vm.LuaConstants.NONE;

import org.junit.Assert;
import org.junit.Test;

import nl.weeaboo.lua2.AbstractLuaTest;
import nl.weeaboo.lua2.LuaTestUtil;
import nl.weeaboo.lua2.vm.LuaThread;

public class ThreadLibTest extends AbstractLuaTest {

    @Test
    public void testYield() {
        LuaThread thread = loadScript("lib/thread/yield.lua");

        thread.resume(NONE);
        // After yielding, still 10-1=9 yield frames left
        Assert.assertEquals(9, thread.getSleep());

        runToCompletion();

        Assert.assertEquals(0, thread.getSleep());
    }

    @Test
    public void testNewThread() {
        loadScript("lib/thread/newthread.lua");

        runToCompletion();

        LuaTestUtil.assertGlobal("result", 1);
    }

    @Test
    public void testJump() {
        loadScript("lib/thread/jump1.lua");

        runToCompletion();

        LuaTestUtil.assertGlobal("jump1Finished", false);
        LuaTestUtil.assertGlobal("jump2Finished", true);
    }

    @Test
    public void invalidJump() {
        loadScript("lib/thread/jumpinvalid.lua");

        runToCompletion();

        LuaThread mainThread = luaRunState.getMainThread();
        Assert.assertEquals(false, mainThread.isRunnable());
    }

    @Test
    public void testEndCall() {
        loadScript("lib/thread/endcall.lua");

        runToCompletion();

        LuaTestUtil.assertGlobal("testEnd", false);
        LuaTestUtil.assertGlobal("scriptEnd", true);
    }

}
