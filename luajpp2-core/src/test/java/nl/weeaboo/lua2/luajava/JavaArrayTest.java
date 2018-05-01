package nl.weeaboo.lua2.luajava;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import nl.weeaboo.lua2.AbstractLuaTest;
import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.LuaTestUtil;
import nl.weeaboo.lua2.vm.LuaTable;

public class JavaArrayTest extends AbstractLuaTest {

    // static type is Object[], dynamic type is Number[]
    private Object[] array;
    private Object[] originalArray;

    @Before
    public void before() {
        array = new Number[2];
        array[0] = Integer.valueOf(11);
        array[1] = new AtomicInteger(22);

        originalArray = array.clone();

        LuaTable globals = luaRunState.getGlobalEnvironment();
        globals.rawset("instance", CoerceJavaToLua.coerce(array));
    }

    /**
     * Access values from a covariant Java array.
     */
    @Test
    public void accessArray() throws LuaException {
        loadScript("luajava/array.lua");
        runToCompletion();

        LuaTable props = LuaTestUtil.getGlobal("properties").checktable();

        LuaTable props0 = props.rawget(1).checktable();
        Assert.assertEquals(CoerceJavaToLua.coerce(originalArray[0], Number.class), props0.rawget("original"));
        Assert.assertEquals(false, props0.get("hasCAS").toboolean());

        LuaTable props1 = props.rawget(2).checktable();
        Assert.assertEquals(CoerceJavaToLua.coerce(originalArray[1], Number.class), props1.rawget("original"));
        // Because the array is a Number[], the AtomicInteger-specific interface methods are inaccessible
        Assert.assertEquals(false, props1.get("hasCAS").toboolean());
    }

}
