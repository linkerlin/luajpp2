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
package nl.weeaboo.lua2.vm;

import static nl.weeaboo.lua2.vm.LuaConstants.INDEX;
import static nl.weeaboo.lua2.vm.LuaConstants.NEWINDEX;
import static nl.weeaboo.lua2.vm.LuaConstants.NONE;
import static nl.weeaboo.lua2.vm.LuaNil.NIL;

import junit.framework.TestCase;
import nl.weeaboo.lua2.LuaRunState;
import nl.weeaboo.lua2.lib.ThreeArgFunction;
import nl.weeaboo.lua2.lib.TwoArgFunction;
import nl.weeaboo.lua2.lib.ZeroArgFunction;
import nl.weeaboo.lua2.vm.TypeTest.MyData;

public class MetatableTest extends TestCase {

    private final Object sampleobject = new Object();
    private final MyData sampledata = new MyData();

    private final LuaBoolean bool = LuaBoolean.TRUE;
    private final LuaInteger number = LuaInteger.valueOf(1);
    private final LuaTable table = LuaValue.tableOf();

    private final LuaFunction function = new ZeroArgFunction() {
        private static final long serialVersionUID = 1L;

        @Override
        public LuaValue call() {
            return NONE;
        }
    };

    private final LuaClosure closure = new LuaClosure(new Prototype(), new LuaTable());
    private final LuaUserdata userdata = LuaValue.userdataOf(sampleobject);
    private final LuaUserdata userdatamt = LuaValue.userdataOf(sampledata, table);

    private LuaThread thread;

    @Override
    protected void setUp() throws Exception {
        LuaRunState lrs = new LuaRunState();
        thread = new LuaThread(lrs, null);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        LuaBoolean.s_metatable = null;
        LuaFunction.s_metatable = null;
        LuaNil.s_metatable = null;
        LuaNumber.s_metatable = null;
        // LuaString.s_metatable = null;
        LuaThread.s_metatable = null;
    }

    public void testGetMetatable() {
        assertEquals(null, NIL.getmetatable());
        assertEquals(null, bool.getmetatable());
        assertEquals(null, number.getmetatable());
        // assertEquals( null, string.getmetatable() );
        assertEquals(null, table.getmetatable());
        assertEquals(null, function.getmetatable());
        assertEquals(null, thread.getmetatable());
        assertEquals(null, closure.getmetatable());
        assertEquals(null, userdata.getmetatable());
        assertEquals(table, userdatamt.getmetatable());
    }

    public void testSetMetatable() {
        LuaValue mt = LuaValue.tableOf();
        assertEquals(null, table.getmetatable());
        assertEquals(null, userdata.getmetatable());
        assertEquals(table, userdatamt.getmetatable());
        assertEquals(table, table.setmetatable(mt));
        assertEquals(userdata, userdata.setmetatable(mt));
        assertEquals(userdatamt, userdatamt.setmetatable(mt));
        assertEquals(mt, table.getmetatable());
        assertEquals(mt, userdata.getmetatable());
        assertEquals(mt, userdatamt.getmetatable());

        // these all get metatable behind-the-scenes
        assertEquals(null, NIL.getmetatable());
        assertEquals(null, bool.getmetatable());
        assertEquals(null, number.getmetatable());
        // assertEquals( null, string.getmetatable() );
        assertEquals(null, function.getmetatable());
        assertEquals(null, thread.getmetatable());
        assertEquals(null, closure.getmetatable());
        LuaNil.s_metatable = mt;
        assertEquals(mt, NIL.getmetatable());
        assertEquals(null, bool.getmetatable());
        assertEquals(null, number.getmetatable());
        // assertEquals( null, string.getmetatable() );
        assertEquals(null, function.getmetatable());
        assertEquals(null, thread.getmetatable());
        assertEquals(null, closure.getmetatable());
        LuaBoolean.s_metatable = mt;
        assertEquals(mt, bool.getmetatable());
        assertEquals(null, number.getmetatable());
        // assertEquals( null, string.getmetatable() );
        assertEquals(null, function.getmetatable());
        assertEquals(null, thread.getmetatable());
        assertEquals(null, closure.getmetatable());
        LuaNumber.s_metatable = mt;
        assertEquals(mt, number.getmetatable());
        assertEquals(mt, LuaValue.valueOf(1.25).getmetatable());
        // assertEquals( null, string.getmetatable() );
        assertEquals(null, function.getmetatable());
        assertEquals(null, thread.getmetatable());
        assertEquals(null, closure.getmetatable());
        // LuaString.s_metatable = mt;
        // assertEquals( mt, string.getmetatable() );
        assertEquals(null, function.getmetatable());
        assertEquals(null, thread.getmetatable());
        assertEquals(null, closure.getmetatable());
        LuaFunction.s_metatable = mt;
        assertEquals(mt, function.getmetatable());
        assertEquals(null, thread.getmetatable());
        LuaThread.s_metatable = mt;
        assertEquals(mt, thread.getmetatable());
    }

    public void testMetatableIndex() {
        assertEquals(table, table.setmetatable(null));
        assertEquals(userdata, userdata.setmetatable(null));
        assertEquals(userdatamt, userdatamt.setmetatable(null));
        assertEquals(NIL, table.get(1));
        assertEquals(NIL, userdata.get(1));
        assertEquals(NIL, userdatamt.get(1));

        // empty metatable
        LuaValue mt = LuaValue.tableOf();
        assertEquals(table, table.setmetatable(mt));
        assertEquals(userdata, userdata.setmetatable(mt));
        LuaBoolean.s_metatable = mt;
        LuaFunction.s_metatable = mt;
        LuaNil.s_metatable = mt;
        LuaNumber.s_metatable = mt;
        // LuaString.s_metatable = mt;
        LuaThread.s_metatable = mt;
        assertEquals(mt, table.getmetatable());
        assertEquals(mt, userdata.getmetatable());
        assertEquals(mt, NIL.getmetatable());
        assertEquals(mt, bool.getmetatable());
        assertEquals(mt, number.getmetatable());
        // assertEquals( StringLib.instance, string.getmetatable() );
        assertEquals(mt, function.getmetatable());
        assertEquals(mt, thread.getmetatable());

        // plain metatable
        LuaValue abc = LuaValue.valueOf("abc");
        mt.set(INDEX, LuaValue.listOf(new LuaValue[] { abc }));
        assertEquals(abc, table.get(1));
        assertEquals(abc, userdata.get(1));
        assertEquals(abc, NIL.get(1));
        assertEquals(abc, bool.get(1));
        assertEquals(abc, number.get(1));
        // assertEquals( abc, string.get(1) );
        assertEquals(abc, function.get(1));
        assertEquals(abc, thread.get(1));

        // plain metatable
        mt.set(INDEX, new TwoArgFunction() {
            private static final long serialVersionUID = 1L;

            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2) {
                return LuaValue.valueOf(arg1.typename() + "[" + arg2.tojstring() + "]=xyz");
            }

        });
        assertEquals("table[1]=xyz", table.get(1).tojstring());
        assertEquals("userdata[1]=xyz", userdata.get(1).tojstring());
        assertEquals("nil[1]=xyz", NIL.get(1).tojstring());
        assertEquals("boolean[1]=xyz", bool.get(1).tojstring());
        assertEquals("number[1]=xyz", number.get(1).tojstring());
        // assertEquals( "string[1]=xyz", string.get(1).tojstring() );
        assertEquals("function[1]=xyz", function.get(1).tojstring());
        assertEquals("thread[1]=xyz", thread.get(1).tojstring());
    }

    public void testMetatableNewIndex() {
        // empty metatable
        LuaValue mt = LuaValue.tableOf();
        assertEquals(table, table.setmetatable(mt));
        assertEquals(userdata, userdata.setmetatable(mt));
        LuaBoolean.s_metatable = mt;
        LuaFunction.s_metatable = mt;
        LuaNil.s_metatable = mt;
        LuaNumber.s_metatable = mt;
        // LuaString.s_metatable = mt;
        LuaThread.s_metatable = mt;

        // plain metatable
        final LuaValue fallback = LuaValue.tableOf();
        LuaValue abc = LuaValue.valueOf("abc");
        mt.set(NEWINDEX, fallback);
        table.set(2, abc);
        userdata.set(3, abc);
        NIL.set(4, abc);
        bool.set(5, abc);
        number.set(6, abc);
        // string.set(7,abc);
        function.set(8, abc);
        thread.set(9, abc);
        assertEquals(abc, fallback.get(2));
        assertEquals(abc, fallback.get(3));
        assertEquals(abc, fallback.get(4));
        assertEquals(abc, fallback.get(5));
        assertEquals(abc, fallback.get(6));
        // assertEquals( abc, StringLib.instance.get(7) );
        assertEquals(abc, fallback.get(8));
        assertEquals(abc, fallback.get(9));

        // metatable with function call
        mt.set(NEWINDEX, new ThreeArgFunction() {
            private static final long serialVersionUID = 1L;

            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
                fallback.rawset(arg2, LuaValue.valueOf("via-func-" + arg3));
                return NONE;
            }

        });
        table.set(12, abc);
        userdata.set(13, abc);
        NIL.set(14, abc);
        bool.set(15, abc);
        number.set(16, abc);
        // string.set(17,abc);
        function.set(18, abc);
        thread.set(19, abc);
        LuaValue via = LuaValue.valueOf("via-func-abc");
        assertEquals(via, fallback.get(12));
        assertEquals(via, fallback.get(13));
        assertEquals(via, fallback.get(14));
        assertEquals(via, fallback.get(15));
        assertEquals(via, fallback.get(16));
        // assertEquals( via, StringLib.instance.get(17) );
        assertEquals(via, fallback.get(18));
        assertEquals(via, fallback.get(19));
    }

    private void checkTable(LuaValue t, LuaValue aa, LuaValue bb, LuaValue cc, LuaValue dd, LuaValue ee,
            LuaValue ff, LuaValue gg, LuaValue ra, LuaValue rb, LuaValue rc, LuaValue rd, LuaValue re,
            LuaValue rf, LuaValue rg) {
        assertEquals(aa, t.get("aa"));
        assertEquals(bb, t.get("bb"));
        assertEquals(cc, t.get("cc"));
        assertEquals(dd, t.get("dd"));
        assertEquals(ee, t.get("ee"));
        assertEquals(ff, t.get("ff"));
        assertEquals(gg, t.get("gg"));
        assertEquals(ra, t.rawget("aa"));
        assertEquals(rb, t.rawget("bb"));
        assertEquals(rc, t.rawget("cc"));
        assertEquals(rd, t.rawget("dd"));
        assertEquals(re, t.rawget("ee"));
        assertEquals(rf, t.rawget("ff"));
        assertEquals(rg, t.rawget("gg"));
    }

    private LuaValue makeTable(String key1, String val1, String key2, String val2) {
        return LuaValue.tableOf(new LuaValue[] { LuaValue.valueOf(key1), LuaValue.valueOf(val1),
                LuaValue.valueOf(key2), LuaValue.valueOf(val2), });
    }

    public void testRawsetMetatableSet() {
        // set up tables
        LuaValue m = makeTable("aa", "aaa", "bb", "bbb");
        m.set(INDEX, m);
        m.set(NEWINDEX, m);
        LuaValue s = makeTable("cc", "ccc", "dd", "ddd");
        LuaValue t = makeTable("cc", "ccc", "dd", "ddd");
        t.setmetatable(m);
        LuaValue aaa = LuaValue.valueOf("aaa");
        LuaValue bbb = LuaValue.valueOf("bbb");
        LuaValue ccc = LuaValue.valueOf("ccc");
        LuaValue ddd = LuaValue.valueOf("ddd");
        LuaValue ppp = LuaValue.valueOf("ppp");
        LuaValue qqq = LuaValue.valueOf("qqq");
        LuaValue rrr = LuaValue.valueOf("rrr");
        LuaValue sss = LuaValue.valueOf("sss");
        LuaValue ttt = LuaValue.valueOf("ttt");
        LuaValue www = LuaValue.valueOf("www");
        LuaValue xxx = LuaValue.valueOf("xxx");
        LuaValue yyy = LuaValue.valueOf("yyy");
        LuaValue zzz = LuaValue.valueOf("zzz");

        // check initial values
        // values via "bet()" values via "rawget()"
        checkTable(s, NIL, NIL, ccc, ddd, NIL, NIL, NIL, NIL, NIL, ccc, ddd, NIL, NIL, NIL);
        checkTable(t, aaa, bbb, ccc, ddd, NIL, NIL, NIL, NIL, NIL, ccc, ddd, NIL, NIL, NIL);
        checkTable(m, aaa, bbb, NIL, NIL, NIL, NIL, NIL, aaa, bbb, NIL, NIL, NIL, NIL, NIL);

        // rawset()
        s.rawset("aa", www);
        checkTable(s, www, NIL, ccc, ddd, NIL, NIL, NIL, www, NIL, ccc, ddd, NIL, NIL, NIL);
        checkTable(t, aaa, bbb, ccc, ddd, NIL, NIL, NIL, NIL, NIL, ccc, ddd, NIL, NIL, NIL);
        checkTable(m, aaa, bbb, NIL, NIL, NIL, NIL, NIL, aaa, bbb, NIL, NIL, NIL, NIL, NIL);
        s.rawset("cc", xxx);
        checkTable(s, www, NIL, xxx, ddd, NIL, NIL, NIL, www, NIL, xxx, ddd, NIL, NIL, NIL);
        checkTable(t, aaa, bbb, ccc, ddd, NIL, NIL, NIL, NIL, NIL, ccc, ddd, NIL, NIL, NIL);
        checkTable(m, aaa, bbb, NIL, NIL, NIL, NIL, NIL, aaa, bbb, NIL, NIL, NIL, NIL, NIL);
        t.rawset("bb", yyy);
        checkTable(s, www, NIL, xxx, ddd, NIL, NIL, NIL, www, NIL, xxx, ddd, NIL, NIL, NIL);
        checkTable(t, aaa, yyy, ccc, ddd, NIL, NIL, NIL, NIL, yyy, ccc, ddd, NIL, NIL, NIL);
        checkTable(m, aaa, bbb, NIL, NIL, NIL, NIL, NIL, aaa, bbb, NIL, NIL, NIL, NIL, NIL);
        t.rawset("dd", zzz);
        checkTable(s, www, NIL, xxx, ddd, NIL, NIL, NIL, www, NIL, xxx, ddd, NIL, NIL, NIL);
        checkTable(t, aaa, yyy, ccc, zzz, NIL, NIL, NIL, NIL, yyy, ccc, zzz, NIL, NIL, NIL);
        checkTable(m, aaa, bbb, NIL, NIL, NIL, NIL, NIL, aaa, bbb, NIL, NIL, NIL, NIL, NIL);

        // set() invoking metatables
        s.set("ee", ppp);
        checkTable(s, www, NIL, xxx, ddd, ppp, NIL, NIL, www, NIL, xxx, ddd, ppp, NIL, NIL);
        checkTable(t, aaa, yyy, ccc, zzz, NIL, NIL, NIL, NIL, yyy, ccc, zzz, NIL, NIL, NIL);
        checkTable(m, aaa, bbb, NIL, NIL, NIL, NIL, NIL, aaa, bbb, NIL, NIL, NIL, NIL, NIL);
        s.set("cc", qqq);
        checkTable(s, www, NIL, qqq, ddd, ppp, NIL, NIL, www, NIL, qqq, ddd, ppp, NIL, NIL);
        checkTable(t, aaa, yyy, ccc, zzz, NIL, NIL, NIL, NIL, yyy, ccc, zzz, NIL, NIL, NIL);
        checkTable(m, aaa, bbb, NIL, NIL, NIL, NIL, NIL, aaa, bbb, NIL, NIL, NIL, NIL, NIL);
        t.set("ff", rrr);
        checkTable(s, www, NIL, qqq, ddd, ppp, NIL, NIL, www, NIL, qqq, ddd, ppp, NIL, NIL);
        checkTable(t, aaa, yyy, ccc, zzz, NIL, rrr, NIL, NIL, yyy, ccc, zzz, NIL, NIL, NIL);
        checkTable(m, aaa, bbb, NIL, NIL, NIL, rrr, NIL, aaa, bbb, NIL, NIL, NIL, rrr, NIL);
        t.set("dd", sss);
        checkTable(s, www, NIL, qqq, ddd, ppp, NIL, NIL, www, NIL, qqq, ddd, ppp, NIL, NIL);
        checkTable(t, aaa, yyy, ccc, sss, NIL, rrr, NIL, NIL, yyy, ccc, sss, NIL, NIL, NIL);
        checkTable(m, aaa, bbb, NIL, NIL, NIL, rrr, NIL, aaa, bbb, NIL, NIL, NIL, rrr, NIL);
        m.set("gg", ttt);
        checkTable(s, www, NIL, qqq, ddd, ppp, NIL, NIL, www, NIL, qqq, ddd, ppp, NIL, NIL);
        checkTable(t, aaa, yyy, ccc, sss, NIL, rrr, ttt, NIL, yyy, ccc, sss, NIL, NIL, NIL);
        checkTable(m, aaa, bbb, NIL, NIL, NIL, rrr, ttt, aaa, bbb, NIL, NIL, NIL, rrr, ttt);

        // make s fall back to t
        s.setmetatable(LuaValue.tableOf(new LuaValue[] { INDEX, t, NEWINDEX, t }));
        checkTable(s, www, yyy, qqq, ddd, ppp, rrr, ttt, www, NIL, qqq, ddd, ppp, NIL, NIL);
        checkTable(t, aaa, yyy, ccc, sss, NIL, rrr, ttt, NIL, yyy, ccc, sss, NIL, NIL, NIL);
        checkTable(m, aaa, bbb, NIL, NIL, NIL, rrr, ttt, aaa, bbb, NIL, NIL, NIL, rrr, ttt);
        s.set("aa", www);
        checkTable(s, www, yyy, qqq, ddd, ppp, rrr, ttt, www, NIL, qqq, ddd, ppp, NIL, NIL);
        checkTable(t, aaa, yyy, ccc, sss, NIL, rrr, ttt, NIL, yyy, ccc, sss, NIL, NIL, NIL);
        checkTable(m, aaa, bbb, NIL, NIL, NIL, rrr, ttt, aaa, bbb, NIL, NIL, NIL, rrr, ttt);
        s.set("bb", zzz);
        checkTable(s, www, zzz, qqq, ddd, ppp, rrr, ttt, www, NIL, qqq, ddd, ppp, NIL, NIL);
        checkTable(t, aaa, zzz, ccc, sss, NIL, rrr, ttt, NIL, zzz, ccc, sss, NIL, NIL, NIL);
        checkTable(m, aaa, bbb, NIL, NIL, NIL, rrr, ttt, aaa, bbb, NIL, NIL, NIL, rrr, ttt);
        s.set("ee", xxx);
        checkTable(s, www, zzz, qqq, ddd, xxx, rrr, ttt, www, NIL, qqq, ddd, xxx, NIL, NIL);
        checkTable(t, aaa, zzz, ccc, sss, NIL, rrr, ttt, NIL, zzz, ccc, sss, NIL, NIL, NIL);
        checkTable(m, aaa, bbb, NIL, NIL, NIL, rrr, ttt, aaa, bbb, NIL, NIL, NIL, rrr, ttt);
        s.set("ff", yyy);
        checkTable(s, www, zzz, qqq, ddd, xxx, yyy, ttt, www, NIL, qqq, ddd, xxx, NIL, NIL);
        checkTable(t, aaa, zzz, ccc, sss, NIL, yyy, ttt, NIL, zzz, ccc, sss, NIL, NIL, NIL);
        checkTable(m, aaa, bbb, NIL, NIL, NIL, yyy, ttt, aaa, bbb, NIL, NIL, NIL, yyy, ttt);

    }

}
