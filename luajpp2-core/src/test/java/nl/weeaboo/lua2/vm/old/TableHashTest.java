/*******************************************************************************
 * Copyright (c) 2009 Luaj.org. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ******************************************************************************/

package nl.weeaboo.lua2.vm.old;

import static nl.weeaboo.lua2.vm.LuaConstants.META_INDEX;
import static nl.weeaboo.lua2.vm.LuaConstants.TNUMBER;
import static nl.weeaboo.lua2.vm.LuaNil.NIL;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.LuaRunState;
import nl.weeaboo.lua2.lib.TwoArgFunction;
import nl.weeaboo.lua2.vm.LuaString;
import nl.weeaboo.lua2.vm.LuaTable;
import nl.weeaboo.lua2.vm.LuaValue;
import nl.weeaboo.lua2.vm.TableTester;
import nl.weeaboo.lua2.vm.Varargs;

/**
 * Tests for tables used as lists.
 */
public class TableHashTest {

    @Before
    public void before() throws LuaException {
        LuaRunState.create();
    }

    protected LuaTable new_Table() {
        return new LuaTable();
    }

    protected LuaTable new_Table(int n, int m) {
        return new LuaTable(n, m);
    }

    @Test
    public void testSetRemove() {
        LuaTable t = new_Table();

        Assert.assertEquals(0, TableTester.getHashLength(t));
        Assert.assertEquals(0, t.length());
        Assert.assertEquals(0, t.keyCount());

        String[] keys = { "abc", "def", "ghi", "jkl", "mno", "pqr", "stu", "wxy", "z01", "cd", "ef", "g",
                "hi", "jk", "lm", "no", "pq", "rs", };
        int[] capacities = { 0, 2, 2, 4, 4, 8, 8, 8, 8, 16, 16, 16, 16, 16, 16, 16, 16, 32, 32, 32 };
        for (int i = 0; i < keys.length; ++i) {
            Assert.assertEquals(capacities[i], TableTester.getHashLength(t));
            String si = "Test Value! " + i;
            t.set(keys[i], si);
            Assert.assertEquals(0, t.length());
            Assert.assertEquals(i + 1, t.keyCount());
        }
        Assert.assertEquals(capacities[keys.length], TableTester.getHashLength(t));
        for (int i = 0; i < keys.length; ++i) {
            LuaValue vi = LuaString.valueOf("Test Value! " + i);
            Assert.assertEquals(vi, t.get(keys[i]));
            Assert.assertEquals(vi, t.get(LuaString.valueOf(keys[i])));
            Assert.assertEquals(vi, t.rawget(keys[i]));
            Assert.assertEquals(vi, t.rawget(keys[i]));
        }

        // replace with new values
        for (int i = 0; i < keys.length; ++i) {
            t.set(keys[i], LuaString.valueOf("Replacement Value! " + i));
            Assert.assertEquals(0, t.length());
            Assert.assertEquals(keys.length, t.keyCount());
            Assert.assertEquals(capacities[keys.length], TableTester.getHashLength(t));
        }
        for (int i = 0; i < keys.length; ++i) {
            LuaValue vi = LuaString.valueOf("Replacement Value! " + i);
            Assert.assertEquals(vi, t.get(keys[i]));
        }

        // remove
        for (int i = 0; i < keys.length; ++i) {
            t.set(keys[i], NIL);
            Assert.assertEquals(0, t.length());
            Assert.assertEquals(keys.length - i - 1, t.keyCount());
            if (i < keys.length - 1) {
                Assert.assertEquals(capacities[keys.length], TableTester.getHashLength(t));
            } else {
                Assert.assertTrue(0 <= TableTester.getHashLength(t));
            }
        }
        for (int i = 0; i < keys.length; ++i) {
            Assert.assertEquals(NIL, t.get(keys[i]));
        }
    }

    @Test
    public void testIndexMetatag() {
        LuaTable t = new_Table();
        LuaTable mt = new_Table();
        LuaTable fb = new_Table();

        // set basic values
        t.set("ppp", "abc");
        t.set(123, "def");
        mt.set(META_INDEX, fb);
        fb.set("qqq", "ghi");
        fb.set(456, "jkl");

        // check before setting metatable
        Assert.assertEquals("abc", t.get("ppp").tojstring());
        Assert.assertEquals("def", t.get(123).tojstring());
        Assert.assertEquals("nil", t.get("qqq").tojstring());
        Assert.assertEquals("nil", t.get(456).tojstring());
        Assert.assertEquals("nil", fb.get("ppp").tojstring());
        Assert.assertEquals("nil", fb.get(123).tojstring());
        Assert.assertEquals("ghi", fb.get("qqq").tojstring());
        Assert.assertEquals("jkl", fb.get(456).tojstring());
        Assert.assertEquals("nil", mt.get("ppp").tojstring());
        Assert.assertEquals("nil", mt.get(123).tojstring());
        Assert.assertEquals("nil", mt.get("qqq").tojstring());
        Assert.assertEquals("nil", mt.get(456).tojstring());

        // check before setting metatable
        t.setmetatable(mt);
        Assert.assertEquals(mt, t.getmetatable());
        Assert.assertEquals("abc", t.get("ppp").tojstring());
        Assert.assertEquals("def", t.get(123).tojstring());
        Assert.assertEquals("ghi", t.get("qqq").tojstring());
        Assert.assertEquals("jkl", t.get(456).tojstring());
        Assert.assertEquals("nil", fb.get("ppp").tojstring());
        Assert.assertEquals("nil", fb.get(123).tojstring());
        Assert.assertEquals("ghi", fb.get("qqq").tojstring());
        Assert.assertEquals("jkl", fb.get(456).tojstring());
        Assert.assertEquals("nil", mt.get("ppp").tojstring());
        Assert.assertEquals("nil", mt.get(123).tojstring());
        Assert.assertEquals("nil", mt.get("qqq").tojstring());
        Assert.assertEquals("nil", mt.get(456).tojstring());

        // set metatable to metatable without values
        t.setmetatable(fb);
        Assert.assertEquals("abc", t.get("ppp").tojstring());
        Assert.assertEquals("def", t.get(123).tojstring());
        Assert.assertEquals("nil", t.get("qqq").tojstring());
        Assert.assertEquals("nil", t.get(456).tojstring());

        // set metatable to null
        t.setmetatable(NIL);
        Assert.assertEquals("abc", t.get("ppp").tojstring());
        Assert.assertEquals("def", t.get(123).tojstring());
        Assert.assertEquals("nil", t.get("qqq").tojstring());
        Assert.assertEquals("nil", t.get(456).tojstring());
    }

    @Test
    public void testIndexFunction() {
        final LuaTable t = new_Table();
        final LuaTable mt = new_Table();

        final TwoArgFunction fb = new TwoArgFunction() {
            private static final long serialVersionUID = 1L;

            @Override
            public LuaValue call(LuaValue tbl, LuaValue key) {
                Assert.assertEquals(tbl, t);
                return valueOf("from mt: " + key);
            }
        };

        // set basic values
        t.set("ppp", "abc");
        t.set(123, "def");
        mt.set(META_INDEX, fb);

        // check before setting metatable
        Assert.assertEquals("abc", t.get("ppp").tojstring());
        Assert.assertEquals("def", t.get(123).tojstring());
        Assert.assertEquals("nil", t.get("qqq").tojstring());
        Assert.assertEquals("nil", t.get(456).tojstring());

        // check before setting metatable
        t.setmetatable(mt);
        Assert.assertEquals(mt, t.getmetatable());
        Assert.assertEquals("abc", t.get("ppp").tojstring());
        Assert.assertEquals("def", t.get(123).tojstring());
        Assert.assertEquals("from mt: qqq", t.get("qqq").tojstring());
        Assert.assertEquals("from mt: 456", t.get(456).tojstring());

        // use raw set
        t.rawset("qqq", "alt-qqq");
        t.rawset(456, "alt-456");
        Assert.assertEquals("abc", t.get("ppp").tojstring());
        Assert.assertEquals("def", t.get(123).tojstring());
        Assert.assertEquals("alt-qqq", t.get("qqq").tojstring());
        Assert.assertEquals("alt-456", t.get(456).tojstring());

        // remove using raw set
        t.rawset("qqq", NIL);
        t.rawset(456, NIL);
        Assert.assertEquals("abc", t.get("ppp").tojstring());
        Assert.assertEquals("def", t.get(123).tojstring());
        Assert.assertEquals("from mt: qqq", t.get("qqq").tojstring());
        Assert.assertEquals("from mt: 456", t.get(456).tojstring());

        // set metatable to null
        t.setmetatable(NIL);
        Assert.assertEquals("abc", t.get("ppp").tojstring());
        Assert.assertEquals("def", t.get(123).tojstring());
        Assert.assertEquals("nil", t.get("qqq").tojstring());
        Assert.assertEquals("nil", t.get(456).tojstring());
    }

    @Test
    public void testNext() {
        final LuaTable t = new_Table();
        Assert.assertEquals(NIL, t.next(NIL));

        // insert array elements
        t.set(1, "LuaValue.valueOf(1)");
        Assert.assertEquals(LuaValue.valueOf(1), t.next(NIL).arg(1));
        Assert.assertEquals(LuaValue.valueOf("LuaValue.valueOf(1)"), t.next(NIL).arg(2));
        Assert.assertEquals(NIL, t.next(LuaValue.valueOf(1)));
        t.set(2, "two");
        Assert.assertEquals(LuaValue.valueOf(1), t.next(NIL).arg(1));
        Assert.assertEquals(LuaValue.valueOf("LuaValue.valueOf(1)"), t.next(NIL).arg(2));
        Assert.assertEquals(LuaValue.valueOf(2), t.next(LuaValue.valueOf(1)).arg(1));
        Assert.assertEquals(LuaValue.valueOf("two"), t.next(LuaValue.valueOf(1)).arg(2));
        Assert.assertEquals(NIL, t.next(LuaValue.valueOf(2)));

        // insert hash elements
        t.set("aa", "aaa");
        Assert.assertEquals(LuaValue.valueOf(1), t.next(NIL).arg(1));
        Assert.assertEquals(LuaValue.valueOf("LuaValue.valueOf(1)"), t.next(NIL).arg(2));
        Assert.assertEquals(LuaValue.valueOf(2), t.next(LuaValue.valueOf(1)).arg(1));
        Assert.assertEquals(LuaValue.valueOf("two"), t.next(LuaValue.valueOf(1)).arg(2));
        Assert.assertEquals(LuaValue.valueOf("aa"), t.next(LuaValue.valueOf(2)).arg(1));
        Assert.assertEquals(LuaValue.valueOf("aaa"), t.next(LuaValue.valueOf(2)).arg(2));
        Assert.assertEquals(NIL, t.next(LuaValue.valueOf("aa")));
        t.set("bb", "bbb");
        Assert.assertEquals(LuaValue.valueOf(1), t.next(NIL).arg(1));
        Assert.assertEquals(LuaValue.valueOf("LuaValue.valueOf(1)"), t.next(NIL).arg(2));
        Assert.assertEquals(LuaValue.valueOf(2), t.next(LuaValue.valueOf(1)).arg(1));
        Assert.assertEquals(LuaValue.valueOf("two"), t.next(LuaValue.valueOf(1)).arg(2));
        Assert.assertEquals(LuaValue.valueOf("aa"), t.next(LuaValue.valueOf(2)).arg(1));
        Assert.assertEquals(LuaValue.valueOf("aaa"), t.next(LuaValue.valueOf(2)).arg(2));
        Assert.assertEquals(LuaValue.valueOf("bb"), t.next(LuaValue.valueOf("aa")).arg(1));
        Assert.assertEquals(LuaValue.valueOf("bbb"), t.next(LuaValue.valueOf("aa")).arg(2));
        Assert.assertEquals(NIL, t.next(LuaValue.valueOf("bb")));
    }

    @Test
    public void testLoopWithRemoval() {
        final LuaTable t = new_Table();

        t.set(LuaValue.valueOf(1), LuaValue.valueOf("1"));
        t.set(LuaValue.valueOf(3), LuaValue.valueOf("3"));
        t.set(LuaValue.valueOf(8), LuaValue.valueOf("4"));
        t.set(LuaValue.valueOf(17), LuaValue.valueOf("5"));
        t.set(LuaValue.valueOf(26), LuaValue.valueOf("6"));
        t.set(LuaValue.valueOf(35), LuaValue.valueOf("7"));
        t.set(LuaValue.valueOf(42), LuaValue.valueOf("8"));
        t.set(LuaValue.valueOf(60), LuaValue.valueOf("10"));
        t.set(LuaValue.valueOf(63), LuaValue.valueOf("11"));

        Varargs entry = t.next(NIL);
        while (!entry.isnil(1)) {
            LuaValue k = entry.arg1();
            if ((k.toint() & 1) == 0) {
                t.set(k, NIL);
            }
            entry = t.next(k);
        }

        int numEntries = 0;
        entry = t.next(NIL);
        while (!entry.isnil(1)) {
            LuaValue k = entry.arg1();
            // Only odd keys should remain
            Assert.assertTrue((k.toint() & 1) == 1);
            numEntries++;
            entry = t.next(k);
        }
        Assert.assertEquals(5, numEntries);
    }

    @Test
    public void testLoopWithRemovalAndSet() {
        final LuaTable t = new_Table();

        t.set(LuaValue.valueOf(1), LuaValue.valueOf("1"));
        t.set(LuaValue.valueOf(3), LuaValue.valueOf("3"));
        t.set(LuaValue.valueOf(8), LuaValue.valueOf("4"));
        t.set(LuaValue.valueOf(17), LuaValue.valueOf("5"));
        t.set(LuaValue.valueOf(26), LuaValue.valueOf("6"));
        t.set(LuaValue.valueOf(35), LuaValue.valueOf("7"));
        t.set(LuaValue.valueOf(42), LuaValue.valueOf("8"));
        t.set(LuaValue.valueOf(60), LuaValue.valueOf("10"));
        t.set(LuaValue.valueOf(63), LuaValue.valueOf("11"));

        Varargs entry = t.next(NIL);
        Varargs entry2 = entry;
        while (!entry.isnil(1)) {
            LuaValue k = entry.arg1();
            LuaValue v = entry.arg(2);
            if ((k.toint() & 1) == 0) {
                t.set(k, NIL);
            } else {
                t.set(k, v.tonumber());
                entry2 = t.next(entry2.arg1());
            }
            entry = t.next(k);
        }

        int numEntries = 0;
        entry = t.next(NIL);
        while (!entry.isnil(1)) {
            LuaValue k = entry.arg1();
            // Only odd keys should remain
            Assert.assertTrue((k.toint() & 1) == 1);
            Assert.assertTrue(entry.arg(2).type() == TNUMBER);
            numEntries++;
            entry = t.next(k);
        }
        Assert.assertEquals(5, numEntries);
    }
}
