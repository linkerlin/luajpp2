package nl.weeaboo.lua2.luajava;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import nl.weeaboo.lua2.AbstractLuaTest;
import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.LuaRunState;
import nl.weeaboo.lua2.LuaTestUtil;
import nl.weeaboo.lua2.vm.LuaTable;
import nl.weeaboo.lua2.vm.LuaValue;

public class LuaMethodTest extends AbstractLuaTest {

    private TestB instance;

    @Before
    public void before() {
        instance = new TestB(1);

        LuaTable globals = luaRunState.getGlobalEnvironment();
        globals.rawset("instance", LuajavaLib.toUserdata(instance, instance.getClass()));
    }

    /**
     * Call an method that's overridden in a subclass.
     */
    @Test
    public void callOverridden() throws LuaException {
        loadScript("luajava/override.lua");
        runToCompletion();

        // Check that the overridden version of the method is called
        LuaValue luaResult = LuaTestUtil.getGlobal("result");
        // Returned object is of the overridden type
        TestB javaResult = luaResult.checkuserdata(TestB.class);
        Assert.assertEquals(2, javaResult.getId());
    }

    /**
     * Serialize a userdata object
     */
    @Test
    public void serialize() throws IOException, LuaException {
        instance.value = 3;
        Assert.assertEquals(3, instance.value);
        loadScript("luajava/serialize.lua");
        runToCompletion();

        LuaRunState lrs = LuaTestUtil.serialize(luaRunState);
        lrs.registerOnThread();

        instance.value = 4; // Modify original instance

        LuaValue luaInstance = LuaTestUtil.getGlobal("instance");
        // Check that the deserialized instance is an independent object, and that 'value' was stored
        Assert.assertEquals(3, luaInstance.get("value").toint());

        // After deserialization, the script should still run
        loadScript("luajava/serialize.lua");
        runToCompletion();
    }

}
