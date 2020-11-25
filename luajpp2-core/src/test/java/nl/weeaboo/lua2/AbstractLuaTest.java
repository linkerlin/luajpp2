package nl.weeaboo.lua2;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import nl.weeaboo.lua2.compiler.ScriptLoader;
import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.stdlib.DebugTrace;
import nl.weeaboo.lua2.vm.LuaConstants;
import nl.weeaboo.lua2.vm.LuaThread;
import nl.weeaboo.lua2.vm.Varargs;

public abstract class AbstractLuaTest {

    static {
        System.setProperty("sun.io.serialization.extendedDebugInfo", "true");
    }

    protected LuaRunState luaRunState;

    @Before
    public void initLuaRunState() throws LuaException {
        luaRunState = LuaRunState.create();
        luaRunState.setExceptionHandler(new FailLuaExceptionHandler());
    }

    @After
    public void deinitLuaRunState() {
        luaRunState.destroy();
    }

    protected LuaThread loadScript(String filename) {
        Varargs loadResult = ScriptLoader.loadFile(filename);
        if (loadResult.isnil(1)) {
            throw new LuaException(loadResult.tojstring(2));
        }

        LuaThread mainThread = luaRunState.getMainThread();
        mainThread.pushPending(loadResult.checkclosure(1), LuaConstants.NONE);
        return mainThread;
    }

    /**
     * Runs Lua code until all threads are finished.
     */
    public void runToCompletion() {
        for (int n = 0; n < 1000; n++) {
            luaRunState.update();

            if (luaRunState.isFinished()) {
                break;
            }
        }
        Assert.assertTrue("Current stack trace: " + DebugTrace.stackTrace(luaRunState.getMainThread()),
                luaRunState.isFinished());
    }

    @LuaSerializable
    private static final class FailLuaExceptionHandler implements ILuaExceptionHandler {

        private static final long serialVersionUID = 1L;

        @Override
        public void onScriptException(LuaThread thread, Exception exception) {
            String message = "Uncaught exception in thread: " + thread;
            if (thread.isMainThread()) {
                if (exception instanceof RuntimeException) {
                    throw (RuntimeException)exception;
                } else {
                    throw new RuntimeException(message, exception);
                }
            }
            throw new AssertionError(message, exception);
        }

    }

}
