package nl.weeaboo.lua2.lib;

import static nl.weeaboo.lua2.vm.LuaNil.NIL;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.LuaRunState;
import nl.weeaboo.lua2.vm.LuaString;
import nl.weeaboo.lua2.vm.LuaValue;

public class StringLibTest {

    @Before
    public void before() throws LuaException {
        LuaRunState.newInstance();
    }

    @Test
    public void testMatchShortPatterns() {
        LuaValue[] args = { LuaString.valueOf("%bxy") };
        LuaString empty = LuaString.valueOf("");

        LuaString a = LuaString.valueOf("a");
        LuaString ax = LuaString.valueOf("ax");
        LuaString axb = LuaString.valueOf("axb");
        LuaString axby = LuaString.valueOf("axby");
        LuaString xbya = LuaString.valueOf("xbya");
        LuaString bya = LuaString.valueOf("bya");
        LuaString xby = LuaString.valueOf("xby");
        LuaString axbya = LuaString.valueOf("axbya");

        Assert.assertEquals(NIL, empty.invokemethod("match", args));
        Assert.assertEquals(NIL, a.invokemethod("match", args));
        Assert.assertEquals(NIL, ax.invokemethod("match", args));
        Assert.assertEquals(NIL, axb.invokemethod("match", args));
        Assert.assertEquals(xby, axby.invokemethod("match", args));
        Assert.assertEquals(xby, xbya.invokemethod("match", args));
        Assert.assertEquals(NIL, bya.invokemethod("match", args));
        Assert.assertEquals(xby, xby.invokemethod("match", args));
        Assert.assertEquals(xby, axbya.invokemethod("match", args));
        Assert.assertEquals(xby, axbya.substring(0, 4).invokemethod("match", args));
        Assert.assertEquals(NIL, axbya.substring(0, 3).invokemethod("match", args));
        Assert.assertEquals(xby, axbya.substring(1, 5).invokemethod("match", args));
        Assert.assertEquals(NIL, axbya.substring(2, 5).invokemethod("match", args));
    }

}
