package nl.weeaboo.lua2.lib;

import org.junit.Before;
import org.junit.Test;

import nl.weeaboo.lua2.AbstractLuaTest;
import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.LuaRunState;

public class IoLibTest extends AbstractLuaTest {

    @Before
    public void before() {
        new LuaRunState().registerOnThread();
    }

    @Test
    public void runDebugger() throws LuaException {
        loadScript("lib/io/stdfile.lua");

        runToCompletion();
    }

}
