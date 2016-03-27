package org.luaj.vm2.lib;

import static org.luaj.vm2.LuaNil.NIL;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaValue;

import nl.weeaboo.lua2.LuaRunState;
import nl.weeaboo.lua2.lib.J2sePlatform;

public class StringLibTest {

    @Before
    public void before() {
        LuaRunState lrs = new LuaRunState();
        J2sePlatform.registerStandardLibs(lrs);
    }

    @Test
    public void testMatchShortPatterns() {
        LuaValue[] args = { LuaString.valueOf("%bxy") };
        LuaString _ = LuaString.valueOf("");

        LuaString a = LuaString.valueOf("a");
        LuaString ax = LuaString.valueOf("ax");
        LuaString axb = LuaString.valueOf("axb");
        LuaString axby = LuaString.valueOf("axby");
        LuaString xbya = LuaString.valueOf("xbya");
        LuaString bya = LuaString.valueOf("bya");
        LuaString xby = LuaString.valueOf("xby");
        LuaString axbya = LuaString.valueOf("axbya");

        Assert.assertEquals(NIL, _.invokemethod("match", args));
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
