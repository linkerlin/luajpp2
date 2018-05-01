package nl.weeaboo.lua2.vm.old;

import static nl.weeaboo.lua2.vm.LuaNil.NIL;

import java.lang.ref.WeakReference;

import org.junit.Assert;
import org.junit.Test;

import nl.weeaboo.lua2.vm.LuaTable;
import nl.weeaboo.lua2.vm.LuaValue;
import nl.weeaboo.lua2.vm.TableTester;

public class WeakKeyTableTest extends WeakTableTest {

    @Override
    protected LuaTable newTable() {
        return TableTester.newWeakTable(true, false);
    }

    @Override
    protected LuaTable newTable(int narray, int nhash) {
        return TableTester.newWeakTable(true, false);
    }

    @Test
    public void testWeakKeysTable() {
        LuaTable t = TableTester.newWeakTable(true, false);

        LuaValue key = LuaValue.userdataOf(new MyData(111));
        LuaValue val = LuaValue.userdataOf(new MyData(222));

        // set up the table
        t.set(key, val);
        Assert.assertEquals(val, t.get(key));
        System.gc();
        Assert.assertEquals(val, t.get(key));

        // drop key and value references, replace them with new ones
        final WeakReference<LuaValue> origkey = new WeakReference<LuaValue>(key);
        final WeakReference<LuaValue> origval = new WeakReference<LuaValue>(val);
        key = LuaValue.userdataOf(new MyData(111));
        val = LuaValue.userdataOf(new MyData(222));

        // new key and value should be interchangeable (feature of this test class)
        Assert.assertEquals(key, origkey.get());
        Assert.assertEquals(val, origval.get());
        Assert.assertEquals(val, t.get(key));
        Assert.assertEquals(val, t.get(origkey.get()));
        Assert.assertEquals(origval.get(), t.get(key));

        // value should not be reachable after gc
        collectGarbage();
        Assert.assertEquals(null, origkey.get());
        Assert.assertEquals(NIL, t.get(key));
        collectGarbage();
        Assert.assertEquals(null, origval.get());
    }

    @Test
    public void testNext() {
        LuaTable t = TableTester.newWeakTable(true, true);

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

        // forget one of the keys
        key2 = null;
        val2 = null;
        collectGarbage();

        // table should have 2 entries
        int size = 0;
        for (LuaValue k = t.next(NIL).arg1(); !k.isnil(); k = t.next(k).arg1()) {
            size++;
        }
        Assert.assertEquals(2, size);
    }
}