package nl.weeaboo.lua2.io;

import static nl.weeaboo.lua2.vm.LuaNil.NIL;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import nl.weeaboo.lua2.AbstractLuaTest;
import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.LuaRunState;
import nl.weeaboo.lua2.LuaTestUtil;
import nl.weeaboo.lua2.vm.LuaTable;
import nl.weeaboo.lua2.vm.LuaValue;

public final class SerializeTest extends AbstractLuaTest {

    @Test
    public void serialize1() throws IOException, LuaException {
        loadScript("io/serialize1.lua");
        runToCompletion();

        assertSerialize1(luaRunState);

        LuaRunState lrs2 = LuaTestUtil.serialize(luaRunState);
        assertSerialize1(lrs2);
    }

    /**
     * Test serialization of weak tables.
     */
    @Test
    public void testWeakTable() throws IOException, LuaException {
        loadScript("io/serialize1.lua");
        runToCompletion();

        luaRunState = LuaTestUtil.serialize(luaRunState);
        luaRunState.registerOnThread();

        // Collect globals
        final LuaTable weakKeys1 = LuaTestUtil.getGlobal("weakKeys1").checktable();
        final LuaTable weakValues1 = LuaTestUtil.getGlobal("weakValues1").checktable();
        final LuaTable weakDouble1 = LuaTestUtil.getGlobal("weakDouble1").checktable();

        // Check weak tables when all references exist
        collectGarbage();
        assertWeakTable(weakKeys1, getWeakRef(0), getWeakRef(1));
        assertWeakTable(weakValues1, getWeakRef(2), getWeakRef(3));
        assertWeakTable(weakDouble1, getWeakRef(4), getWeakRef(5));

        // Removing the weak key causes removal of the weaktable entry
        setWeakRef(0, NIL);
        setWeakRef(2, NIL);
        setWeakRef(4, NIL);
        collectGarbage();
        Assert.assertEquals(0, weakKeys1.keyCount());
        Assert.assertEquals(1, weakValues1.keyCount()); // Weak value is still referenced
        Assert.assertEquals(0, weakDouble1.keyCount());

        // Removing the weak value causes the weakvalue entry to be removed
        setWeakRef(3, NIL);
        collectGarbage();
        Assert.assertEquals(0, weakValues1.keyCount());
    }

    private static void assertSerialize1(LuaRunState lrs) {
        lrs.registerOnThread();

        // Simple values
        LuaTestUtil.assertGlobal("nil1", null);
        LuaTestUtil.assertGlobal("bool1", true);
        LuaTestUtil.assertGlobal("bool2", false);
        LuaTestUtil.assertGlobal("int1", 1);
        LuaTestUtil.assertGlobal("int2", 1000);
        LuaTestUtil.assertGlobal("double1", 0);
        LuaTestUtil.assertGlobal("double2", 12.34);
        LuaTestUtil.assertGlobal("string1", "");
        LuaTestUtil.assertGlobal("string2", "abc");
        LuaTestUtil.assertGlobal("string3", "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");

        // Tables
        LuaTable table1 = LuaTestUtil.getGlobal("table1").checktable();
        Assert.assertEquals(0, table1.keyCount());

        LuaTable table2 = LuaTestUtil.getGlobal("table2").checktable();
        Assert.assertEquals(3, table2.keyCount());
        LuaTestUtil.assertEquals(11, table2.get(1));
        LuaTestUtil.assertEquals(12, table2.get(2));
        LuaTestUtil.assertEquals(13, table2.get(3));

        LuaTable table3 = LuaTestUtil.getGlobal("table3").checktable();
        Assert.assertEquals(3, table3.keyCount());
        LuaTestUtil.assertEquals(11, table3.get("a"));
        LuaTestUtil.assertEquals(12, table3.get("b"));
        LuaTestUtil.assertEquals(13, table3.get("c"));

        // Complex tables
        LuaTable complexTable1 = LuaTestUtil.getGlobal("complexTable1").checktable();
        Assert.assertEquals(1, complexTable1.keyCount());
        LuaTable sub1 = complexTable1.get("sub1").checktable();
        Assert.assertEquals(1, sub1.keyCount());
        // Contains a reference to table3
        Assert.assertSame(table3, sub1.get(1));
    }

    private LuaValue getWeakRef(int index) {
        return LuaTestUtil.getGlobal("weakRef" + index);
    }

    private void setWeakRef(int index, LuaValue val) {
        LuaTestUtil.setGlobal("weakRef" + index, val);
    }

    private static void assertWeakTable(LuaTable table, LuaValue key, LuaValue expectedVal) {
        LuaValue actualVal = table.get(key);
        Assert.assertEquals(expectedVal, actualVal);
    }

    private static void collectGarbage() {
        Runtime rt = Runtime.getRuntime();
        rt.gc();
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Assert.fail(e.toString());
        }
        rt.gc();
    }
}
