package nl.weeaboo.lua2.vm;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public final class WeakTableTest {

    private LuaTable table;

    @Before
    public void before() {
        table = TableTester.newWeakTable(true, true);
    }

    /**
     * Check that weak values are garbage collected, and that when they're garbage collected they actually
     * disappear from the table.
     */
    @Test
    public void testWeakValueAutoGC() {
        LuaInteger i1 = LuaInteger.valueOf(1);
        LuaInteger i2 = LuaInteger.valueOf(2);
        LuaInteger i3 = LuaInteger.valueOf(3);
        LuaTable a = new LuaTable();
        LuaTable b = new LuaTable();
        LuaTable c = new LuaTable();
        LuaString s = LuaString.valueOf("short-string");
        table.set(i1, a);
        table.set(i2, b);
        table.set(i3, c);
        table.set(s, s);

        for (int i = 4; i < 10; i++) {
            // Collectible value
            table.set(i, new LuaTable());

            // Collectible key
            table.set(new LuaTable(), LuaInteger.valueOf(i));

            // Collectible key+value
            LuaTable t = new LuaTable();
            table.set(t, t);
        }

        TableTester.collectGarbage();

        /*
         * Only key/value combinations remaining are where both key and value are still available as local
         * variables in this method.
         */
        Assert.assertArrayEquals(new LuaValue[] {i1, i2, i3, s}, table.keys());
    }

}
