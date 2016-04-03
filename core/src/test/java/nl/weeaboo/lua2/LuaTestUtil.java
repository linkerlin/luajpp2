package nl.weeaboo.lua2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.Assert;

import nl.weeaboo.lua2.io.LuaSerializer;
import nl.weeaboo.lua2.io.ObjectDeserializer;
import nl.weeaboo.lua2.io.ObjectSerializer;
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

    public static LuaRunState serialize(LuaRunState luaRunState) throws IOException {
        LuaSerializer ls = new LuaSerializer();
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectSerializer out = ls.openSerializer(bout);
        try {
            out.writeObject(luaRunState);
        } finally {
            out.close();
        }

        LuaRunState lrs;
        ObjectDeserializer in = ls.openDeserializer(new ByteArrayInputStream(bout.toByteArray()));
        try {
            lrs = (LuaRunState)in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        } finally {
            in.close();
        }
        lrs.registerOnThread();
        return lrs;
    }

}
