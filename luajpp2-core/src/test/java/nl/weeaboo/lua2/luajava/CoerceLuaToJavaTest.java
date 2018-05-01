package nl.weeaboo.lua2.luajava;

import org.junit.Assert;
import org.junit.Test;

import nl.weeaboo.lua2.vm.LuaBoolean;
import nl.weeaboo.lua2.vm.LuaDouble;
import nl.weeaboo.lua2.vm.LuaInteger;
import nl.weeaboo.lua2.vm.LuaString;
import nl.weeaboo.lua2.vm.LuaUserdata;
import nl.weeaboo.lua2.vm.LuaValue;

public class CoerceLuaToJavaTest {

    /** Coercion from a LuaValue to Object.class */
    @Test
    public void coerceToJavaObject() {
        Assert.assertEquals(42, coerce(LuaInteger.valueOf(42), Object.class));
        Assert.assertEquals(8.5, coerce(LuaDouble.valueOf(8.5), Object.class));
        Assert.assertEquals(true, coerce(LuaBoolean.TRUE, Object.class));
        Assert.assertEquals("ABC", coerce(LuaString.valueOf("ABC"), Object.class));

        final Double obj = new Double(12345.6789);
        Assert.assertSame(obj, coerce(LuaUserdata.userdataOf(obj), Object.class));
    }

    private Object coerce(LuaValue luaValue, Class<Object> targetType) {
        return CoerceLuaToJava.coerceArg(luaValue, targetType);
    }

}
