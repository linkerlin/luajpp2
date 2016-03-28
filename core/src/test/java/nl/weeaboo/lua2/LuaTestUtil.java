package nl.weeaboo.lua2;

import org.junit.Assert;

import nl.weeaboo.lua2.luajava.CoerceLuaToJava;
import nl.weeaboo.lua2.vm.LuaTable;
import nl.weeaboo.lua2.vm.LuaValue;

public final class LuaTestUtil {

    private LuaTestUtil() {
    }

    public static void assertGlobal(String name, int val) {
        Assert.assertEquals(val, getGlobal(name).optint(0));
    }

    public static void assertGlobal(String name, Object val) {
        assertEquals(val, getGlobal(name));
    }

    public static LuaValue getGlobal(String name) {
        LuaTable globals = LuaRunState.getCurrent().getGlobalEnvironment();
        return globals.get(name);
    }

    public static <T> T getGlobal(String name, Class<T> type) {
        return getGlobal(name).optuserdata(type, null);
    }

    public static void assertEquals(Object expected, LuaValue luaValue) {
        if (expected == null) {
            Assert.assertTrue(luaValue.isnil());
        } else {
            Object javaValue = CoerceLuaToJava.coerceArg(luaValue, expected.getClass());
            Assert.assertEquals(expected, javaValue);
        }
    }

}
