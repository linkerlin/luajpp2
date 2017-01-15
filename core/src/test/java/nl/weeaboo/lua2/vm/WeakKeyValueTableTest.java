package nl.weeaboo.lua2.vm;

import static nl.weeaboo.lua2.vm.LuaNil.NIL;

import java.lang.ref.WeakReference;

import org.junit.Assert;
import org.junit.Test;

public class WeakKeyValueTableTest extends WeakTableTest {

    @Override
    protected LuaTable newTable() {
        return WeakTable.make(true, true);
    }

    @Override
    protected LuaTable newTable(int narray, int nhash) {
        return WeakTable.make(true, true);
    }

    @Test
    public void testWeakKeysValuesTable() {
        LuaTable t = WeakTable.make(true, true);

        LuaValue key = LuaValue.userdataOf(new MyData(111));
        LuaValue val = LuaValue.userdataOf(new MyData(222));
        LuaValue key2 = LuaValue.userdataOf(new MyData(333));
        LuaValue val2 = LuaValue.userdataOf(new MyData(444));
        LuaValue key3 = LuaValue.userdataOf(new MyData(555));
        LuaValue val3 = LuaValue.userdataOf(new MyData(666));

        // set up the table
        t.set(key, val);
        t.set(key2, val2);
        t.set(key3, val3);
        Assert.assertEquals(val, t.get(key));
        Assert.assertEquals(val2, t.get(key2));
        Assert.assertEquals(val3, t.get(key3));
        System.gc();
        Assert.assertEquals(val, t.get(key));
        Assert.assertEquals(val2, t.get(key2));
        Assert.assertEquals(val3, t.get(key3));

        // drop key and value references, replace them with new ones
        final WeakReference<LuaValue> origkey = new WeakReference<LuaValue>(key);
        final WeakReference<LuaValue> origval = new WeakReference<LuaValue>(val);
        final WeakReference<LuaValue> origkey2 = new WeakReference<LuaValue>(key2);
        final WeakReference<LuaValue> origval2 = new WeakReference<LuaValue>(val2);
        final WeakReference<LuaValue> origkey3 = new WeakReference<LuaValue>(key3);
        final WeakReference<LuaValue> origval3 = new WeakReference<LuaValue>(val3);
        key = LuaValue.userdataOf(new MyData(111));
        val = LuaValue.userdataOf(new MyData(222));
        key2 = LuaValue.userdataOf(new MyData(333));
        // don't drop val2, or key3
        val3 = LuaValue.userdataOf(new MyData(666));

        // no values should be reachable after gc
        collectGarbage();
        Assert.assertEquals(null, origkey.get());
        Assert.assertEquals(null, origval.get());
        Assert.assertEquals(null, origkey2.get());
        Assert.assertEquals(null, origval3.get());
        Assert.assertEquals(NIL, t.get(key));
        Assert.assertEquals(NIL, t.get(key2));
        Assert.assertEquals(NIL, t.get(key3));

        // all originals should be gone after gc, then access
        val2 = null;
        key3 = null;
        collectGarbage();
        Assert.assertEquals(null, origval2.get());
        Assert.assertEquals(null, origkey3.get());
    }

    @Test
    public void testReplace() {
        LuaTable t = WeakTable.make(true, true);

        LuaValue key = LuaValue.userdataOf(new MyData(111));
        LuaValue val = LuaValue.userdataOf(new MyData(222));
        LuaValue key2 = LuaValue.userdataOf(new MyData(333));
        LuaValue val2 = LuaValue.userdataOf(new MyData(444));
        LuaValue key3 = LuaValue.userdataOf(new MyData(555));
        LuaValue val3 = LuaValue.userdataOf(new MyData(666));

        // set up the table
        t.set(key, val);
        t.set(key2, val2);
        t.set(key3, val3);

        LuaValue val4 = LuaValue.userdataOf(new MyData(777));
        t.set(key2, val4);

        // table should have 3 entries
        int size = 0;
        for (LuaValue k = t.next(NIL).arg1(); !k.isnil() && size < 1000; k = t.next(k).arg1()) {
            size++;
        }
        Assert.assertEquals(3, size);
    }
}