package nl.weeaboo.lua2;

import org.junit.After;
import org.junit.Before;

import nl.weeaboo.lua2.lib.BaseLib;
import nl.weeaboo.lua2.link.LuaLink;
import nl.weeaboo.lua2.vm.LuaConstants;
import nl.weeaboo.lua2.vm.LuaError;
import nl.weeaboo.lua2.vm.Varargs;

public abstract class AbstractLuaTest {

    protected LuaRunState luaRunState;

    @Before
    public void initLuaRunState() {
        luaRunState = new LuaRunState();
    }

    @After
    public void deinitLuaRunState() {
        luaRunState.destroy();
    }

    protected LuaLink loadScript(String filename) {
        Varargs loadResult = BaseLib.loadFile(filename);
        if (loadResult.isnil(1)) {
            throw new LuaError(loadResult.tojstring(2));
        }
        return luaRunState.newThread(loadResult.checkclosure(1), LuaConstants.NONE);
    }

    public void runToCompletion() throws LuaException {
        for (int n = 0; n < 1000; n++) {
            luaRunState.update();

            if (luaRunState.isFinished()) {
                break;
            }
        }
    }

}
