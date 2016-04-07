package nl.weeaboo.lua2.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import nl.weeaboo.lua2.AbstractLuaTest;
import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.LuaRunState;
import nl.weeaboo.lua2.LuaTestUtil;
import nl.weeaboo.lua2.vm.LuaTable;

public class SerializeTest extends AbstractLuaTest {

    @Test
    public void serialize1() throws IOException, LuaException {
        loadScript("serialize/serialize1.lua");
        runToCompletion();

        assertSerialize1(luaRunState);

        LuaRunState lrs2 = serialize();
        assertSerialize1(lrs2);
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

    private LuaRunState serialize() throws IOException {
        LuaSerializer ls = new LuaSerializer();
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectSerializer out = ls.openSerializer(bout);
        out.setCollectStats(true);
        try {
            out.writeObject(luaRunState);
        } finally {
            out.close();
        }

        LuaRunState lrs;
        ObjectDeserializer in = ls.openDeserializer(new ByteArrayInputStream(bout.toByteArray()));
        in.setCollectStats(true);
        try {
            lrs = (LuaRunState)in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        } finally {
            in.close();
        }
        return lrs;
    }

}
