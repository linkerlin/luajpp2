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

import static nl.weeaboo.lua2.vm.LuaBoolean.FALSE;
import static nl.weeaboo.lua2.vm.LuaBoolean.TRUE;
import static nl.weeaboo.lua2.vm.LuaConstants.NONE;
import static nl.weeaboo.lua2.vm.LuaNil.NIL;

import java.lang.reflect.InvocationTargetException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.LuaRunState;
import nl.weeaboo.lua2.lib.ZeroArgFunction;
import nl.weeaboo.lua2.vm.LuaClosure;
import nl.weeaboo.lua2.vm.LuaFunction;
import nl.weeaboo.lua2.vm.LuaInteger;
import nl.weeaboo.lua2.vm.LuaNumber;
import nl.weeaboo.lua2.vm.LuaString;
import nl.weeaboo.lua2.vm.LuaTable;
import nl.weeaboo.lua2.vm.LuaThread;
import nl.weeaboo.lua2.vm.LuaUserdata;
import nl.weeaboo.lua2.vm.LuaValue;
import nl.weeaboo.lua2.vm.Prototype;

public class TypeTest {

    private static final float EPSILON = 0.001f;

    private final int sampleint = 77;
    private final long samplelong = 123400000000L;
    private final double sampledouble = 55.25;
    private final String samplestringstring = "abcdef";
    private final String samplestringint = String.valueOf(sampleint);
    private final String samplestringlong = String.valueOf(samplelong);
    private final String samplestringdouble = String.valueOf(sampledouble);
    private final Object sampleobject = new Object();
    private final MyData sampledata = new MyData();

    private final LuaValue somenil = NIL;
    private final LuaValue sometrue = TRUE;
    private final LuaValue somefalse = FALSE;
    private final LuaValue zero = LuaInteger.valueOf(0);
    private final LuaValue intint = LuaValue.valueOf(sampleint);
    private final LuaValue longdouble = LuaValue.valueOf(samplelong);
    private final LuaValue doubledouble = LuaValue.valueOf(sampledouble);
    private final LuaValue stringstring = LuaValue.valueOf(samplestringstring);
    private final LuaValue stringint = LuaValue.valueOf(samplestringint);
    private final LuaValue stringlong = LuaValue.valueOf(samplestringlong);
    private final LuaValue stringdouble = LuaValue.valueOf(samplestringdouble);
    private final LuaTable table = LuaValue.tableOf();

    private final LuaFunction somefunc = new ZeroArgFunction() {
        private static final long serialVersionUID = 1L;

        @Override
        public LuaValue call() {
            return NONE;
        }
    };
    private final LuaClosure someclosure = new LuaClosure(new Prototype(), new LuaTable());
    private final LuaUserdata userdataobj = LuaValue.userdataOf(sampleobject);
    private final LuaUserdata userdatacls = LuaValue.userdataOf(sampledata);

    private LuaThread thread;

    public static final class MyData {
        public MyData() {
        }
    }

    @Before
    public void before() throws LuaException {
        LuaRunState lrs = LuaRunState.create();
        thread = new LuaThread(lrs, null);
    }

    // ===================== type checks =======================

    @Test
    public void testIsBoolean() {
        Assert.assertEquals(false, somenil.isboolean());
        Assert.assertEquals(true, sometrue.isboolean());
        Assert.assertEquals(true, somefalse.isboolean());
        Assert.assertEquals(false, zero.isboolean());
        Assert.assertEquals(false, intint.isboolean());
        Assert.assertEquals(false, longdouble.isboolean());
        Assert.assertEquals(false, doubledouble.isboolean());
        Assert.assertEquals(false, stringstring.isboolean());
        Assert.assertEquals(false, stringint.isboolean());
        Assert.assertEquals(false, stringlong.isboolean());
        Assert.assertEquals(false, stringdouble.isboolean());
        Assert.assertEquals(false, thread.isboolean());
        Assert.assertEquals(false, table.isboolean());
        Assert.assertEquals(false, userdataobj.isboolean());
        Assert.assertEquals(false, userdatacls.isboolean());
        Assert.assertEquals(false, somefunc.isboolean());
        Assert.assertEquals(false, someclosure.isboolean());
    }

    @Test
    public void testIsClosure() {
        Assert.assertEquals(false, somenil.isclosure());
        Assert.assertEquals(false, sometrue.isclosure());
        Assert.assertEquals(false, somefalse.isclosure());
        Assert.assertEquals(false, zero.isclosure());
        Assert.assertEquals(false, intint.isclosure());
        Assert.assertEquals(false, longdouble.isclosure());
        Assert.assertEquals(false, doubledouble.isclosure());
        Assert.assertEquals(false, stringstring.isclosure());
        Assert.assertEquals(false, stringint.isclosure());
        Assert.assertEquals(false, stringlong.isclosure());
        Assert.assertEquals(false, stringdouble.isclosure());
        Assert.assertEquals(false, thread.isclosure());
        Assert.assertEquals(false, table.isclosure());
        Assert.assertEquals(false, userdataobj.isclosure());
        Assert.assertEquals(false, userdatacls.isclosure());
        Assert.assertEquals(false, somefunc.isclosure());
        Assert.assertEquals(true, someclosure.isclosure());
    }

    @Test
    public void testIsFunction() {
        Assert.assertEquals(false, somenil.isfunction());
        Assert.assertEquals(false, sometrue.isfunction());
        Assert.assertEquals(false, somefalse.isfunction());
        Assert.assertEquals(false, zero.isfunction());
        Assert.assertEquals(false, intint.isfunction());
        Assert.assertEquals(false, longdouble.isfunction());
        Assert.assertEquals(false, doubledouble.isfunction());
        Assert.assertEquals(false, stringstring.isfunction());
        Assert.assertEquals(false, stringint.isfunction());
        Assert.assertEquals(false, stringlong.isfunction());
        Assert.assertEquals(false, stringdouble.isfunction());
        Assert.assertEquals(false, thread.isfunction());
        Assert.assertEquals(false, table.isfunction());
        Assert.assertEquals(false, userdataobj.isfunction());
        Assert.assertEquals(false, userdatacls.isfunction());
        Assert.assertEquals(true, somefunc.isfunction());
        Assert.assertEquals(true, someclosure.isfunction());
    }

    @Test
    public void testIsInt() {
        Assert.assertEquals(false, somenil.isint());
        Assert.assertEquals(false, sometrue.isint());
        Assert.assertEquals(false, somefalse.isint());
        Assert.assertEquals(true, zero.isint());
        Assert.assertEquals(true, intint.isint());
        Assert.assertEquals(false, longdouble.isint());
        Assert.assertEquals(false, doubledouble.isint());
        Assert.assertEquals(false, stringstring.isint());
        Assert.assertEquals(true, stringint.isint());
        Assert.assertEquals(false, stringdouble.isint());
        Assert.assertEquals(false, thread.isint());
        Assert.assertEquals(false, table.isint());
        Assert.assertEquals(false, userdataobj.isint());
        Assert.assertEquals(false, userdatacls.isint());
        Assert.assertEquals(false, somefunc.isint());
        Assert.assertEquals(false, someclosure.isint());
    }

    @Test
    public void testIsIntType() {
        Assert.assertEquals(false, somenil.isinttype());
        Assert.assertEquals(false, sometrue.isinttype());
        Assert.assertEquals(false, somefalse.isinttype());
        Assert.assertEquals(true, zero.isinttype());
        Assert.assertEquals(true, intint.isinttype());
        Assert.assertEquals(false, longdouble.isinttype());
        Assert.assertEquals(false, doubledouble.isinttype());
        Assert.assertEquals(false, stringstring.isinttype());
        Assert.assertEquals(false, stringint.isinttype());
        Assert.assertEquals(false, stringlong.isinttype());
        Assert.assertEquals(false, stringdouble.isinttype());
        Assert.assertEquals(false, thread.isinttype());
        Assert.assertEquals(false, table.isinttype());
        Assert.assertEquals(false, userdataobj.isinttype());
        Assert.assertEquals(false, userdatacls.isinttype());
        Assert.assertEquals(false, somefunc.isinttype());
        Assert.assertEquals(false, someclosure.isinttype());
    }

    @Test
    public void testIsLong() {
        Assert.assertEquals(false, somenil.islong());
        Assert.assertEquals(false, sometrue.islong());
        Assert.assertEquals(false, somefalse.islong());
        Assert.assertEquals(true, intint.isint());
        Assert.assertEquals(true, longdouble.islong());
        Assert.assertEquals(false, doubledouble.islong());
        Assert.assertEquals(false, stringstring.islong());
        Assert.assertEquals(true, stringint.islong());
        Assert.assertEquals(true, stringlong.islong());
        Assert.assertEquals(false, stringdouble.islong());
        Assert.assertEquals(false, thread.islong());
        Assert.assertEquals(false, table.islong());
        Assert.assertEquals(false, userdataobj.islong());
        Assert.assertEquals(false, userdatacls.islong());
        Assert.assertEquals(false, somefunc.islong());
        Assert.assertEquals(false, someclosure.islong());
    }

    @Test
    public void testIsNil() {
        Assert.assertEquals(true, somenil.isnil());
        Assert.assertEquals(false, sometrue.isnil());
        Assert.assertEquals(false, somefalse.isnil());
        Assert.assertEquals(false, zero.isnil());
        Assert.assertEquals(false, intint.isnil());
        Assert.assertEquals(false, longdouble.isnil());
        Assert.assertEquals(false, doubledouble.isnil());
        Assert.assertEquals(false, stringstring.isnil());
        Assert.assertEquals(false, stringint.isnil());
        Assert.assertEquals(false, stringlong.isnil());
        Assert.assertEquals(false, stringdouble.isnil());
        Assert.assertEquals(false, thread.isnil());
        Assert.assertEquals(false, table.isnil());
        Assert.assertEquals(false, userdataobj.isnil());
        Assert.assertEquals(false, userdatacls.isnil());
        Assert.assertEquals(false, somefunc.isnil());
        Assert.assertEquals(false, someclosure.isnil());
    }

    @Test
    public void testIsNumber() {
        Assert.assertEquals(false, somenil.isnumber());
        Assert.assertEquals(false, sometrue.isnumber());
        Assert.assertEquals(false, somefalse.isnumber());
        Assert.assertEquals(true, zero.isnumber());
        Assert.assertEquals(true, intint.isnumber());
        Assert.assertEquals(true, longdouble.isnumber());
        Assert.assertEquals(true, doubledouble.isnumber());
        Assert.assertEquals(false, stringstring.isnumber());
        Assert.assertEquals(true, stringint.isnumber());
        Assert.assertEquals(true, stringlong.isnumber());
        Assert.assertEquals(true, stringdouble.isnumber());
        Assert.assertEquals(false, thread.isnumber());
        Assert.assertEquals(false, table.isnumber());
        Assert.assertEquals(false, userdataobj.isnumber());
        Assert.assertEquals(false, userdatacls.isnumber());
        Assert.assertEquals(false, somefunc.isnumber());
        Assert.assertEquals(false, someclosure.isnumber());
    }

    @Test
    public void testIsString() {
        Assert.assertEquals(false, somenil.isstring());
        Assert.assertEquals(false, sometrue.isstring());
        Assert.assertEquals(false, somefalse.isstring());
        Assert.assertEquals(true, zero.isstring());
        Assert.assertEquals(true, longdouble.isstring());
        Assert.assertEquals(true, doubledouble.isstring());
        Assert.assertEquals(true, stringstring.isstring());
        Assert.assertEquals(true, stringint.isstring());
        Assert.assertEquals(true, stringlong.isstring());
        Assert.assertEquals(true, stringdouble.isstring());
        Assert.assertEquals(false, thread.isstring());
        Assert.assertEquals(false, table.isstring());
        Assert.assertEquals(false, userdataobj.isstring());
        Assert.assertEquals(false, userdatacls.isstring());
        Assert.assertEquals(false, somefunc.isstring());
        Assert.assertEquals(false, someclosure.isstring());
    }

    @Test
    public void testIsThread() {
        Assert.assertEquals(false, somenil.isthread());
        Assert.assertEquals(false, sometrue.isthread());
        Assert.assertEquals(false, somefalse.isthread());
        Assert.assertEquals(false, intint.isthread());
        Assert.assertEquals(false, longdouble.isthread());
        Assert.assertEquals(false, doubledouble.isthread());
        Assert.assertEquals(false, stringstring.isthread());
        Assert.assertEquals(false, stringint.isthread());
        Assert.assertEquals(false, stringdouble.isthread());
        Assert.assertEquals(true, thread.isthread());
        Assert.assertEquals(false, table.isthread());
        Assert.assertEquals(false, userdataobj.isthread());
        Assert.assertEquals(false, userdatacls.isthread());
        Assert.assertEquals(false, somefunc.isthread());
        Assert.assertEquals(false, someclosure.isthread());
    }

    @Test
    public void testIsTable() {
        Assert.assertEquals(false, somenil.istable());
        Assert.assertEquals(false, sometrue.istable());
        Assert.assertEquals(false, somefalse.istable());
        Assert.assertEquals(false, intint.istable());
        Assert.assertEquals(false, longdouble.istable());
        Assert.assertEquals(false, doubledouble.istable());
        Assert.assertEquals(false, stringstring.istable());
        Assert.assertEquals(false, stringint.istable());
        Assert.assertEquals(false, stringdouble.istable());
        Assert.assertEquals(false, thread.istable());
        Assert.assertEquals(true, table.istable());
        Assert.assertEquals(false, userdataobj.istable());
        Assert.assertEquals(false, userdatacls.istable());
        Assert.assertEquals(false, somefunc.istable());
        Assert.assertEquals(false, someclosure.istable());
    }

    @Test
    public void testIsUserdata() {
        Assert.assertEquals(false, somenil.isuserdata());
        Assert.assertEquals(false, sometrue.isuserdata());
        Assert.assertEquals(false, somefalse.isuserdata());
        Assert.assertEquals(false, intint.isuserdata());
        Assert.assertEquals(false, longdouble.isuserdata());
        Assert.assertEquals(false, doubledouble.isuserdata());
        Assert.assertEquals(false, stringstring.isuserdata());
        Assert.assertEquals(false, stringint.isuserdata());
        Assert.assertEquals(false, stringdouble.isuserdata());
        Assert.assertEquals(false, thread.isuserdata());
        Assert.assertEquals(false, table.isuserdata());
        Assert.assertEquals(true, userdataobj.isuserdata());
        Assert.assertEquals(true, userdatacls.isuserdata());
        Assert.assertEquals(false, somefunc.isuserdata());
        Assert.assertEquals(false, someclosure.isuserdata());
    }

    @Test
    public void testIsUserdataObject() {
        Assert.assertEquals(false, somenil.isuserdata(Object.class));
        Assert.assertEquals(false, sometrue.isuserdata(Object.class));
        Assert.assertEquals(false, somefalse.isuserdata(Object.class));
        Assert.assertEquals(false, longdouble.isuserdata(Object.class));
        Assert.assertEquals(false, doubledouble.isuserdata(Object.class));
        Assert.assertEquals(false, stringstring.isuserdata(Object.class));
        Assert.assertEquals(false, stringint.isuserdata(Object.class));
        Assert.assertEquals(false, stringdouble.isuserdata(Object.class));
        Assert.assertEquals(false, thread.isuserdata(Object.class));
        Assert.assertEquals(false, table.isuserdata(Object.class));
        Assert.assertEquals(true, userdataobj.isuserdata(Object.class));
        Assert.assertEquals(true, userdatacls.isuserdata(Object.class));
        Assert.assertEquals(false, somefunc.isuserdata(Object.class));
        Assert.assertEquals(false, someclosure.isuserdata(Object.class));
    }

    @Test
    public void testIsUserdataMyData() {
        Assert.assertEquals(false, somenil.isuserdata(MyData.class));
        Assert.assertEquals(false, sometrue.isuserdata(MyData.class));
        Assert.assertEquals(false, somefalse.isuserdata(MyData.class));
        Assert.assertEquals(false, longdouble.isuserdata(MyData.class));
        Assert.assertEquals(false, doubledouble.isuserdata(MyData.class));
        Assert.assertEquals(false, stringstring.isuserdata(MyData.class));
        Assert.assertEquals(false, stringint.isuserdata(MyData.class));
        Assert.assertEquals(false, stringdouble.isuserdata(MyData.class));
        Assert.assertEquals(false, thread.isuserdata(MyData.class));
        Assert.assertEquals(false, table.isuserdata(MyData.class));
        Assert.assertEquals(false, userdataobj.isuserdata(MyData.class));
        Assert.assertEquals(true, userdatacls.isuserdata(MyData.class));
        Assert.assertEquals(false, somefunc.isuserdata(MyData.class));
        Assert.assertEquals(false, someclosure.isuserdata(MyData.class));
    }

    // ===================== Coerce to Java =======================

    @Test
    public void testToBoolean() {
        Assert.assertEquals(false, somenil.toboolean());
        Assert.assertEquals(true, sometrue.toboolean());
        Assert.assertEquals(false, somefalse.toboolean());
        Assert.assertEquals(true, zero.toboolean());
        Assert.assertEquals(true, intint.toboolean());
        Assert.assertEquals(true, longdouble.toboolean());
        Assert.assertEquals(true, doubledouble.toboolean());
        Assert.assertEquals(true, stringstring.toboolean());
        Assert.assertEquals(true, stringint.toboolean());
        Assert.assertEquals(true, stringlong.toboolean());
        Assert.assertEquals(true, stringdouble.toboolean());
        Assert.assertEquals(true, thread.toboolean());
        Assert.assertEquals(true, table.toboolean());
        Assert.assertEquals(true, userdataobj.toboolean());
        Assert.assertEquals(true, userdatacls.toboolean());
        Assert.assertEquals(true, somefunc.toboolean());
        Assert.assertEquals(true, someclosure.toboolean());
    }

    @Test
    public void testToByte() {
        Assert.assertEquals((byte)0, somenil.tobyte());
        Assert.assertEquals((byte)0, somefalse.tobyte());
        Assert.assertEquals((byte)0, sometrue.tobyte());
        Assert.assertEquals((byte)0, zero.tobyte());
        Assert.assertEquals((byte)sampleint, intint.tobyte());
        Assert.assertEquals((byte)samplelong, longdouble.tobyte());
        Assert.assertEquals((byte)sampledouble, doubledouble.tobyte());
        Assert.assertEquals((byte)0, stringstring.tobyte());
        Assert.assertEquals((byte)sampleint, stringint.tobyte());
        Assert.assertEquals((byte)samplelong, stringlong.tobyte());
        Assert.assertEquals((byte)sampledouble, stringdouble.tobyte());
        Assert.assertEquals((byte)0, thread.tobyte());
        Assert.assertEquals((byte)0, table.tobyte());
        Assert.assertEquals((byte)0, userdataobj.tobyte());
        Assert.assertEquals((byte)0, userdatacls.tobyte());
        Assert.assertEquals((byte)0, somefunc.tobyte());
        Assert.assertEquals((byte)0, someclosure.tobyte());
    }

    @Test
    public void testToChar() {
        Assert.assertEquals((char)0, somenil.tochar());
        Assert.assertEquals((char)0, somefalse.tochar());
        Assert.assertEquals((char)0, sometrue.tochar());
        Assert.assertEquals((char)0, zero.tochar());
        Assert.assertEquals((char)sampleint, intint.tochar());
        Assert.assertEquals((char)samplelong, longdouble.tochar());
        Assert.assertEquals((char)sampledouble, doubledouble.tochar());
        Assert.assertEquals((char)0, stringstring.tochar());
        Assert.assertEquals((char)sampleint, stringint.tochar());
        Assert.assertEquals((char)samplelong, stringlong.tochar());
        Assert.assertEquals((char)sampledouble, stringdouble.tochar());
        Assert.assertEquals((char)0, thread.tochar());
        Assert.assertEquals((char)0, table.tochar());
        Assert.assertEquals((char)0, userdataobj.tochar());
        Assert.assertEquals((char)0, userdatacls.tochar());
        Assert.assertEquals((char)0, somefunc.tochar());
        Assert.assertEquals((char)0, someclosure.tochar());
    }

    @Test
    public void testToDouble() {
        Assert.assertEquals(0., somenil.todouble(), EPSILON);
        Assert.assertEquals(0., somefalse.todouble(), EPSILON);
        Assert.assertEquals(0., sometrue.todouble(), EPSILON);
        Assert.assertEquals(0., zero.todouble(), EPSILON);
        Assert.assertEquals(sampleint, intint.todouble(), EPSILON);
        Assert.assertEquals(samplelong, longdouble.todouble(), EPSILON);
        Assert.assertEquals(sampledouble, doubledouble.todouble(), EPSILON);
        Assert.assertEquals(0, stringstring.todouble(), EPSILON);
        Assert.assertEquals(sampleint, stringint.todouble(), EPSILON);
        Assert.assertEquals(samplelong, stringlong.todouble(), EPSILON);
        Assert.assertEquals(sampledouble, stringdouble.todouble(), EPSILON);
        Assert.assertEquals(0., thread.todouble(), EPSILON);
        Assert.assertEquals(0., table.todouble(), EPSILON);
        Assert.assertEquals(0., userdataobj.todouble(), EPSILON);
        Assert.assertEquals(0., userdatacls.todouble(), EPSILON);
        Assert.assertEquals(0., somefunc.todouble(), EPSILON);
        Assert.assertEquals(0., someclosure.todouble(), EPSILON);
    }

    @Test
    public void testToFloat() {
        Assert.assertEquals(0.f, somenil.tofloat(), EPSILON);
        Assert.assertEquals(0.f, somefalse.tofloat(), EPSILON);
        Assert.assertEquals(0.f, sometrue.tofloat(), EPSILON);
        Assert.assertEquals(0.f, zero.tofloat(), EPSILON);
        Assert.assertEquals(sampleint, intint.tofloat(), EPSILON);
        Assert.assertEquals(samplelong, longdouble.tofloat(), EPSILON);
        Assert.assertEquals((float)sampledouble, doubledouble.tofloat(), EPSILON);
        Assert.assertEquals(0, stringstring.tofloat(), EPSILON);
        Assert.assertEquals(sampleint, stringint.tofloat(), EPSILON);
        Assert.assertEquals(samplelong, stringlong.tofloat(), EPSILON);
        Assert.assertEquals((float)sampledouble, stringdouble.tofloat(), EPSILON);
        Assert.assertEquals(0.f, thread.tofloat(), EPSILON);
        Assert.assertEquals(0.f, table.tofloat(), EPSILON);
        Assert.assertEquals(0.f, userdataobj.tofloat(), EPSILON);
        Assert.assertEquals(0.f, userdatacls.tofloat(), EPSILON);
        Assert.assertEquals(0.f, somefunc.tofloat(), EPSILON);
        Assert.assertEquals(0.f, someclosure.tofloat(), EPSILON);
    }

    @Test
    public void testToInt() {
        Assert.assertEquals(0, somenil.toint());
        Assert.assertEquals(0, somefalse.toint());
        Assert.assertEquals(0, sometrue.toint());
        Assert.assertEquals(0, zero.toint());
        Assert.assertEquals(sampleint, intint.toint());
        Assert.assertEquals((int)samplelong, longdouble.toint());
        Assert.assertEquals((int)sampledouble, doubledouble.toint());
        Assert.assertEquals(0, stringstring.toint());
        Assert.assertEquals(sampleint, stringint.toint());
        Assert.assertEquals((int)samplelong, stringlong.toint());
        Assert.assertEquals((int)sampledouble, stringdouble.toint());
        Assert.assertEquals(0, thread.toint());
        Assert.assertEquals(0, table.toint());
        Assert.assertEquals(0, userdataobj.toint());
        Assert.assertEquals(0, userdatacls.toint());
        Assert.assertEquals(0, somefunc.toint());
        Assert.assertEquals(0, someclosure.toint());
    }

    @Test
    public void testToLong() {
        Assert.assertEquals(0L, somenil.tolong());
        Assert.assertEquals(0L, somefalse.tolong());
        Assert.assertEquals(0L, sometrue.tolong());
        Assert.assertEquals(0L, zero.tolong());
        Assert.assertEquals(sampleint, intint.tolong());
        Assert.assertEquals(samplelong, longdouble.tolong());
        Assert.assertEquals((long)sampledouble, doubledouble.tolong());
        Assert.assertEquals(0, stringstring.tolong());
        Assert.assertEquals(sampleint, stringint.tolong());
        Assert.assertEquals(samplelong, stringlong.tolong());
        Assert.assertEquals((long)sampledouble, stringdouble.tolong());
        Assert.assertEquals(0L, thread.tolong());
        Assert.assertEquals(0L, table.tolong());
        Assert.assertEquals(0L, userdataobj.tolong());
        Assert.assertEquals(0L, userdatacls.tolong());
        Assert.assertEquals(0L, somefunc.tolong());
        Assert.assertEquals(0L, someclosure.tolong());
    }

    @Test
    public void testToShort() {
        Assert.assertEquals((short)0, somenil.toshort());
        Assert.assertEquals((short)0, somefalse.toshort());
        Assert.assertEquals((short)0, sometrue.toshort());
        Assert.assertEquals((short)0, zero.toshort());
        Assert.assertEquals((short)sampleint, intint.toshort());
        Assert.assertEquals((short)samplelong, longdouble.toshort());
        Assert.assertEquals((short)sampledouble, doubledouble.toshort());
        Assert.assertEquals((short)0, stringstring.toshort());
        Assert.assertEquals((short)sampleint, stringint.toshort());
        Assert.assertEquals((short)samplelong, stringlong.toshort());
        Assert.assertEquals((short)sampledouble, stringdouble.toshort());
        Assert.assertEquals((short)0, thread.toshort());
        Assert.assertEquals((short)0, table.toshort());
        Assert.assertEquals((short)0, userdataobj.toshort());
        Assert.assertEquals((short)0, userdatacls.toshort());
        Assert.assertEquals((short)0, somefunc.toshort());
        Assert.assertEquals((short)0, someclosure.toshort());
    }

    @Test
    public void testToString() {
        Assert.assertEquals("nil", somenil.tojstring());
        Assert.assertEquals("false", somefalse.tojstring());
        Assert.assertEquals("true", sometrue.tojstring());
        Assert.assertEquals("0", zero.tojstring());
        Assert.assertEquals(String.valueOf(sampleint), intint.tojstring());
        Assert.assertEquals(String.valueOf(samplelong), longdouble.tojstring());
        Assert.assertEquals(String.valueOf(sampledouble), doubledouble.tojstring());
        Assert.assertEquals(samplestringstring, stringstring.tojstring());
        Assert.assertEquals(String.valueOf(sampleint), stringint.tojstring());
        Assert.assertEquals(String.valueOf(samplelong), stringlong.tojstring());
        Assert.assertEquals(String.valueOf(sampledouble), stringdouble.tojstring());
        Assert.assertEquals("thread: ", thread.tojstring().substring(0, 8));
        Assert.assertEquals("table: ", table.tojstring().substring(0, 7));
        Assert.assertEquals(sampleobject.toString(), userdataobj.tojstring());
        Assert.assertEquals(sampledata.toString(), userdatacls.tojstring());
        Assert.assertEquals("function: ", somefunc.tojstring().substring(0, 10));
        Assert.assertEquals("function: ", someclosure.tojstring().substring(0, 10));
    }

    @Test
    public void testToUserdata() {
        Assert.assertEquals(null, somenil.touserdata());
        Assert.assertEquals(null, somefalse.touserdata());
        Assert.assertEquals(null, sometrue.touserdata());
        Assert.assertEquals(null, zero.touserdata());
        Assert.assertEquals(null, intint.touserdata());
        Assert.assertEquals(null, longdouble.touserdata());
        Assert.assertEquals(null, doubledouble.touserdata());
        Assert.assertEquals(null, stringstring.touserdata());
        Assert.assertEquals(null, stringint.touserdata());
        Assert.assertEquals(null, stringlong.touserdata());
        Assert.assertEquals(null, stringdouble.touserdata());
        Assert.assertEquals(null, thread.touserdata());
        Assert.assertEquals(null, table.touserdata());
        Assert.assertEquals(sampleobject, userdataobj.touserdata());
        Assert.assertEquals(sampledata, userdatacls.touserdata());
        Assert.assertEquals(null, somefunc.touserdata());
        Assert.assertEquals(null, someclosure.touserdata());
    }

    // ===================== Optional argument conversion =======================

    private void throwsError(LuaValue obj, String method, Class<?> argtype, Object argument) {
        try {
            obj.getClass().getMethod(method, argtype).invoke(obj, argument);
        } catch (InvocationTargetException e) {
            if (!(e.getTargetException() instanceof LuaException)) {
                Assert.fail("not a LuaError: " + e.getTargetException());
            }
            return; // pass
        } catch (Exception e) {
            Assert.fail("bad exception: " + e);
        }
        Assert.fail("failed to throw LuaError as required");
    }

    @Test
    public void testOptBoolean() {
        Assert.assertEquals(true, somenil.optboolean(true));
        Assert.assertEquals(false, somenil.optboolean(false));
        Assert.assertEquals(true, sometrue.optboolean(false));
        Assert.assertEquals(false, somefalse.optboolean(true));
        throwsError(zero, "optboolean", boolean.class, Boolean.FALSE);
        throwsError(intint, "optboolean", boolean.class, Boolean.FALSE);
        throwsError(longdouble, "optboolean", boolean.class, Boolean.FALSE);
        throwsError(doubledouble, "optboolean", boolean.class, Boolean.FALSE);
        throwsError(somefunc, "optboolean", boolean.class, Boolean.FALSE);
        throwsError(someclosure, "optboolean", boolean.class, Boolean.FALSE);
        throwsError(stringstring, "optboolean", boolean.class, Boolean.FALSE);
        throwsError(stringint, "optboolean", boolean.class, Boolean.FALSE);
        throwsError(stringlong, "optboolean", boolean.class, Boolean.FALSE);
        throwsError(stringdouble, "optboolean", boolean.class, Boolean.FALSE);
        throwsError(thread, "optboolean", boolean.class, Boolean.FALSE);
        throwsError(table, "optboolean", boolean.class, Boolean.FALSE);
        throwsError(userdataobj, "optboolean", boolean.class, Boolean.FALSE);
        throwsError(userdatacls, "optboolean", boolean.class, Boolean.FALSE);
    }

    @Test
    public void testOptClosure() {
        Assert.assertEquals(someclosure, somenil.optclosure(someclosure));
        Assert.assertEquals(null, somenil.optclosure(null));
        throwsError(sometrue, "optclosure", LuaClosure.class, someclosure);
        throwsError(somefalse, "optclosure", LuaClosure.class, someclosure);
        throwsError(zero, "optclosure", LuaClosure.class, someclosure);
        throwsError(intint, "optclosure", LuaClosure.class, someclosure);
        throwsError(longdouble, "optclosure", LuaClosure.class, someclosure);
        throwsError(doubledouble, "optclosure", LuaClosure.class, someclosure);
        throwsError(somefunc, "optclosure", LuaClosure.class, someclosure);
        Assert.assertEquals(someclosure, someclosure.optclosure(someclosure));
        Assert.assertEquals(someclosure, someclosure.optclosure(null));
        throwsError(stringstring, "optclosure", LuaClosure.class, someclosure);
        throwsError(stringint, "optclosure", LuaClosure.class, someclosure);
        throwsError(stringlong, "optclosure", LuaClosure.class, someclosure);
        throwsError(stringdouble, "optclosure", LuaClosure.class, someclosure);
        throwsError(thread, "optclosure", LuaClosure.class, someclosure);
        throwsError(table, "optclosure", LuaClosure.class, someclosure);
        throwsError(userdataobj, "optclosure", LuaClosure.class, someclosure);
        throwsError(userdatacls, "optclosure", LuaClosure.class, someclosure);
    }

    @Test
    public void testOptDouble() {
        Assert.assertEquals(33., somenil.optdouble(33.), EPSILON);
        throwsError(sometrue, "optdouble", double.class, 33.);
        throwsError(somefalse, "optdouble", double.class, 33.);
        Assert.assertEquals(0., zero.optdouble(33.), EPSILON);
        Assert.assertEquals(sampleint, intint.optdouble(33.), EPSILON);
        Assert.assertEquals(samplelong, longdouble.optdouble(33.), EPSILON);
        Assert.assertEquals(sampledouble, doubledouble.optdouble(33.), EPSILON);
        throwsError(somefunc, "optdouble", double.class, 33.);
        throwsError(someclosure, "optdouble", double.class, 33.);
        throwsError(stringstring, "optdouble", double.class, 33.);
        Assert.assertEquals(sampleint, stringint.optdouble(33.), EPSILON);
        Assert.assertEquals(samplelong, stringlong.optdouble(33.), EPSILON);
        Assert.assertEquals(sampledouble, stringdouble.optdouble(33.), EPSILON);
        throwsError(thread, "optdouble", double.class, 33.);
        throwsError(table, "optdouble", double.class, 33.);
        throwsError(userdataobj, "optdouble", double.class, 33.);
        throwsError(userdatacls, "optdouble", double.class, 33.);
    }

    @Test
    public void testOptFunction() {
        Assert.assertEquals(somefunc, somenil.optfunction(somefunc));
        Assert.assertEquals(null, somenil.optfunction(null));
        throwsError(sometrue, "optfunction", LuaFunction.class, somefunc);
        throwsError(somefalse, "optfunction", LuaFunction.class, somefunc);
        throwsError(zero, "optfunction", LuaFunction.class, somefunc);
        throwsError(intint, "optfunction", LuaFunction.class, somefunc);
        throwsError(longdouble, "optfunction", LuaFunction.class, somefunc);
        throwsError(doubledouble, "optfunction", LuaFunction.class, somefunc);
        Assert.assertEquals(somefunc, somefunc.optfunction(null));
        Assert.assertEquals(someclosure, someclosure.optfunction(null));
        Assert.assertEquals(somefunc, somefunc.optfunction(somefunc));
        Assert.assertEquals(someclosure, someclosure.optfunction(somefunc));
        throwsError(stringstring, "optfunction", LuaFunction.class, somefunc);
        throwsError(stringint, "optfunction", LuaFunction.class, somefunc);
        throwsError(stringlong, "optfunction", LuaFunction.class, somefunc);
        throwsError(stringdouble, "optfunction", LuaFunction.class, somefunc);
        throwsError(thread, "optfunction", LuaFunction.class, somefunc);
        throwsError(table, "optfunction", LuaFunction.class, somefunc);
        throwsError(userdataobj, "optfunction", LuaFunction.class, somefunc);
        throwsError(userdatacls, "optfunction", LuaFunction.class, somefunc);
    }

    @Test
    public void testOptInt() {
        Assert.assertEquals(33, somenil.optint(33));
        throwsError(sometrue, "optint", int.class, Integer.valueOf(33));
        throwsError(somefalse, "optint", int.class, Integer.valueOf(33));
        Assert.assertEquals(0, zero.optint(33));
        Assert.assertEquals(sampleint, intint.optint(33));
        Assert.assertEquals((int)samplelong, longdouble.optint(33));
        Assert.assertEquals((int)sampledouble, doubledouble.optint(33));
        throwsError(somefunc, "optint", int.class, Integer.valueOf(33));
        throwsError(someclosure, "optint", int.class, Integer.valueOf(33));
        throwsError(stringstring, "optint", int.class, Integer.valueOf(33));
        Assert.assertEquals(sampleint, stringint.optint(33));
        Assert.assertEquals((int)samplelong, stringlong.optint(33));
        Assert.assertEquals((int)sampledouble, stringdouble.optint(33));
        throwsError(thread, "optint", int.class, Integer.valueOf(33));
        throwsError(table, "optint", int.class, Integer.valueOf(33));
        throwsError(userdataobj, "optint", int.class, Integer.valueOf(33));
        throwsError(userdatacls, "optint", int.class, Integer.valueOf(33));
    }

    @Test
    public void testOptInteger() {
        Assert.assertEquals(LuaValue.valueOf(33), somenil.optinteger(LuaValue.valueOf(33)));
        throwsError(sometrue, "optinteger", LuaInteger.class, LuaValue.valueOf(33));
        throwsError(somefalse, "optinteger", LuaInteger.class, LuaValue.valueOf(33));
        Assert.assertEquals(zero, zero.optinteger(LuaValue.valueOf(33)));
        Assert.assertEquals(LuaValue.valueOf(sampleint), intint.optinteger(LuaValue.valueOf(33)));
        Assert.assertEquals(LuaValue.valueOf((int)samplelong), longdouble.optinteger(LuaValue.valueOf(33)));
        Assert.assertEquals(LuaValue.valueOf((int)sampledouble),
                doubledouble.optinteger(LuaValue.valueOf(33)));
        throwsError(somefunc, "optinteger", LuaInteger.class, LuaValue.valueOf(33));
        throwsError(someclosure, "optinteger", LuaInteger.class, LuaValue.valueOf(33));
        throwsError(stringstring, "optinteger", LuaInteger.class, LuaValue.valueOf(33));
        Assert.assertEquals(LuaValue.valueOf(sampleint), stringint.optinteger(LuaValue.valueOf(33)));
        Assert.assertEquals(LuaValue.valueOf((int)samplelong), stringlong.optinteger(LuaValue.valueOf(33)));
        Assert.assertEquals(LuaValue.valueOf((int)sampledouble),
                stringdouble.optinteger(LuaValue.valueOf(33)));
        throwsError(thread, "optinteger", LuaInteger.class, LuaValue.valueOf(33));
        throwsError(table, "optinteger", LuaInteger.class, LuaValue.valueOf(33));
        throwsError(userdataobj, "optinteger", LuaInteger.class, LuaValue.valueOf(33));
        throwsError(userdatacls, "optinteger", LuaInteger.class, LuaValue.valueOf(33));
    }

    @Test
    public void testOptLong() {
        Assert.assertEquals(33L, somenil.optlong(33));
        throwsError(sometrue, "optlong", long.class, Long.valueOf(33));
        throwsError(somefalse, "optlong", long.class, Long.valueOf(33));
        Assert.assertEquals(0L, zero.optlong(33));
        Assert.assertEquals(sampleint, intint.optlong(33));
        Assert.assertEquals(samplelong, longdouble.optlong(33));
        Assert.assertEquals((long)sampledouble, doubledouble.optlong(33));
        throwsError(somefunc, "optlong", long.class, Long.valueOf(33));
        throwsError(someclosure, "optlong", long.class, Long.valueOf(33));
        throwsError(stringstring, "optlong", long.class, Long.valueOf(33));
        Assert.assertEquals(sampleint, stringint.optlong(33));
        Assert.assertEquals(samplelong, stringlong.optlong(33));
        Assert.assertEquals((long)sampledouble, stringdouble.optlong(33));
        throwsError(thread, "optlong", long.class, Long.valueOf(33));
        throwsError(table, "optlong", long.class, Long.valueOf(33));
        throwsError(userdataobj, "optlong", long.class, Long.valueOf(33));
        throwsError(userdatacls, "optlong", long.class, Long.valueOf(33));
    }

    @Test
    public void testOptNumber() {
        Assert.assertEquals(LuaValue.valueOf(33), somenil.optnumber(LuaValue.valueOf(33)));
        throwsError(sometrue, "optnumber", LuaNumber.class, LuaValue.valueOf(33));
        throwsError(somefalse, "optnumber", LuaNumber.class, LuaValue.valueOf(33));
        Assert.assertEquals(zero, zero.optnumber(LuaValue.valueOf(33)));
        Assert.assertEquals(LuaValue.valueOf(sampleint), intint.optnumber(LuaValue.valueOf(33)));
        Assert.assertEquals(LuaValue.valueOf(samplelong), longdouble.optnumber(LuaValue.valueOf(33)));
        Assert.assertEquals(LuaValue.valueOf(sampledouble), doubledouble.optnumber(LuaValue.valueOf(33)));
        throwsError(somefunc, "optnumber", LuaNumber.class, LuaValue.valueOf(33));
        throwsError(someclosure, "optnumber", LuaNumber.class, LuaValue.valueOf(33));
        throwsError(stringstring, "optnumber", LuaNumber.class, LuaValue.valueOf(33));
        Assert.assertEquals(LuaValue.valueOf(sampleint), stringint.optnumber(LuaValue.valueOf(33)));
        Assert.assertEquals(LuaValue.valueOf(samplelong), stringlong.optnumber(LuaValue.valueOf(33)));
        Assert.assertEquals(LuaValue.valueOf(sampledouble), stringdouble.optnumber(LuaValue.valueOf(33)));
        throwsError(thread, "optnumber", LuaNumber.class, LuaValue.valueOf(33));
        throwsError(table, "optnumber", LuaNumber.class, LuaValue.valueOf(33));
        throwsError(userdataobj, "optnumber", LuaNumber.class, LuaValue.valueOf(33));
        throwsError(userdatacls, "optnumber", LuaNumber.class, LuaValue.valueOf(33));
    }

    @Test
    public void testOptTable() {
        Assert.assertEquals(table, somenil.opttable(table));
        Assert.assertEquals(null, somenil.opttable(null));
        throwsError(sometrue, "opttable", LuaTable.class, table);
        throwsError(somefalse, "opttable", LuaTable.class, table);
        throwsError(zero, "opttable", LuaTable.class, table);
        throwsError(intint, "opttable", LuaTable.class, table);
        throwsError(longdouble, "opttable", LuaTable.class, table);
        throwsError(doubledouble, "opttable", LuaTable.class, table);
        throwsError(somefunc, "opttable", LuaTable.class, table);
        throwsError(someclosure, "opttable", LuaTable.class, table);
        throwsError(stringstring, "opttable", LuaTable.class, table);
        throwsError(stringint, "opttable", LuaTable.class, table);
        throwsError(stringlong, "opttable", LuaTable.class, table);
        throwsError(stringdouble, "opttable", LuaTable.class, table);
        throwsError(thread, "opttable", LuaTable.class, table);
        Assert.assertEquals(table, table.opttable(table));
        Assert.assertEquals(table, table.opttable(null));
        throwsError(userdataobj, "opttable", LuaTable.class, table);
        throwsError(userdatacls, "opttable", LuaTable.class, table);
    }

    @Test
    public void testOptThread() {
        Assert.assertEquals(thread, somenil.optthread(thread));
        Assert.assertEquals(null, somenil.optthread(null));
        throwsError(sometrue, "optthread", LuaThread.class, thread);
        throwsError(somefalse, "optthread", LuaThread.class, thread);
        throwsError(zero, "optthread", LuaThread.class, thread);
        throwsError(intint, "optthread", LuaThread.class, thread);
        throwsError(longdouble, "optthread", LuaThread.class, thread);
        throwsError(doubledouble, "optthread", LuaThread.class, thread);
        throwsError(somefunc, "optthread", LuaThread.class, thread);
        throwsError(someclosure, "optthread", LuaThread.class, thread);
        throwsError(stringstring, "optthread", LuaThread.class, thread);
        throwsError(stringint, "optthread", LuaThread.class, thread);
        throwsError(stringlong, "optthread", LuaThread.class, thread);
        throwsError(stringdouble, "optthread", LuaThread.class, thread);
        throwsError(table, "optthread", LuaThread.class, thread);
        Assert.assertEquals(thread, thread.optthread(thread));
        Assert.assertEquals(thread, thread.optthread(null));
        throwsError(userdataobj, "optthread", LuaThread.class, thread);
        throwsError(userdatacls, "optthread", LuaThread.class, thread);
    }

    @Test
    public void testOptJavaString() {
        Assert.assertEquals("xyz", somenil.optjstring("xyz"));
        Assert.assertEquals(null, somenil.optjstring(null));
        throwsError(sometrue, "optjstring", String.class, "xyz");
        throwsError(somefalse, "optjstring", String.class, "xyz");
        Assert.assertEquals(String.valueOf(zero), zero.optjstring("xyz"));
        Assert.assertEquals(String.valueOf(intint), intint.optjstring("xyz"));
        Assert.assertEquals(String.valueOf(longdouble), longdouble.optjstring("xyz"));
        Assert.assertEquals(String.valueOf(doubledouble), doubledouble.optjstring("xyz"));
        throwsError(somefunc, "optjstring", String.class, "xyz");
        throwsError(someclosure, "optjstring", String.class, "xyz");
        Assert.assertEquals(samplestringstring, stringstring.optjstring("xyz"));
        Assert.assertEquals(samplestringint, stringint.optjstring("xyz"));
        Assert.assertEquals(samplestringlong, stringlong.optjstring("xyz"));
        Assert.assertEquals(samplestringdouble, stringdouble.optjstring("xyz"));
        throwsError(thread, "optjstring", String.class, "xyz");
        throwsError(table, "optjstring", String.class, "xyz");
        throwsError(userdataobj, "optjstring", String.class, "xyz");
        throwsError(userdatacls, "optjstring", String.class, "xyz");
    }

    @Test
    public void testOptLuaString() {
        Assert.assertEquals(LuaValue.valueOf("xyz"), somenil.optstring(LuaValue.valueOf("xyz")));
        Assert.assertEquals(null, somenil.optstring(null));
        throwsError(sometrue, "optstring", LuaString.class, LuaValue.valueOf("xyz"));
        throwsError(somefalse, "optstring", LuaString.class, LuaValue.valueOf("xyz"));
        Assert.assertEquals(LuaValue.valueOf("0"), zero.optstring(LuaValue.valueOf("xyz")));
        Assert.assertEquals(stringint, intint.optstring(LuaValue.valueOf("xyz")));
        Assert.assertEquals(stringlong, longdouble.optstring(LuaValue.valueOf("xyz")));
        Assert.assertEquals(stringdouble, doubledouble.optstring(LuaValue.valueOf("xyz")));
        throwsError(somefunc, "optstring", LuaString.class, LuaValue.valueOf("xyz"));
        throwsError(someclosure, "optstring", LuaString.class, LuaValue.valueOf("xyz"));
        Assert.assertEquals(stringstring, stringstring.optstring(LuaValue.valueOf("xyz")));
        Assert.assertEquals(stringint, stringint.optstring(LuaValue.valueOf("xyz")));
        Assert.assertEquals(stringlong, stringlong.optstring(LuaValue.valueOf("xyz")));
        Assert.assertEquals(stringdouble, stringdouble.optstring(LuaValue.valueOf("xyz")));
        throwsError(thread, "optstring", LuaString.class, LuaValue.valueOf("xyz"));
        throwsError(table, "optstring", LuaString.class, LuaValue.valueOf("xyz"));
        throwsError(userdataobj, "optstring", LuaString.class, LuaValue.valueOf("xyz"));
        throwsError(userdatacls, "optstring", LuaString.class, LuaValue.valueOf("xyz"));
    }

    @Test
    public void testOptUserdata() {
        Assert.assertEquals(sampleobject, somenil.optuserdata(sampleobject));
        Assert.assertEquals(sampledata, somenil.optuserdata(sampledata));
        Assert.assertEquals(null, somenil.optuserdata(null));
        throwsError(sometrue, "optuserdata", Object.class, sampledata);
        throwsError(somefalse, "optuserdata", Object.class, sampledata);
        throwsError(zero, "optuserdata", Object.class, sampledata);
        throwsError(intint, "optuserdata", Object.class, sampledata);
        throwsError(longdouble, "optuserdata", Object.class, sampledata);
        throwsError(doubledouble, "optuserdata", Object.class, sampledata);
        throwsError(somefunc, "optuserdata", Object.class, sampledata);
        throwsError(someclosure, "optuserdata", Object.class, sampledata);
        throwsError(stringstring, "optuserdata", Object.class, sampledata);
        throwsError(stringint, "optuserdata", Object.class, sampledata);
        throwsError(stringlong, "optuserdata", Object.class, sampledata);
        throwsError(stringdouble, "optuserdata", Object.class, sampledata);
        throwsError(table, "optuserdata", Object.class, sampledata);
        Assert.assertEquals(sampleobject, userdataobj.optuserdata(sampledata));
        Assert.assertEquals(sampleobject, userdataobj.optuserdata(null));
        Assert.assertEquals(sampledata, userdatacls.optuserdata(sampleobject));
        Assert.assertEquals(sampledata, userdatacls.optuserdata(null));
    }

    private void throwsErrorOptUserdataClass(LuaValue obj, Class<?> arg1, Object arg2) {
        try {
            obj.getClass().getMethod("optuserdata", Class.class, Object.class).invoke(obj, arg1, arg2);
        } catch (InvocationTargetException e) {
            if (!(e.getTargetException() instanceof LuaException)) {
                Assert.fail("not a LuaError: " + e.getTargetException());
            }
            return; // pass
        } catch (Exception e) {
            Assert.fail("bad exception: " + e);
        }
        Assert.fail("failed to throw LuaError as required");
    }

    @Test
    public void testOptUserdataClass() {
        Assert.assertEquals(sampledata, somenil.optuserdata(MyData.class, sampledata));
        Assert.assertEquals(sampleobject, somenil.optuserdata(Object.class, sampleobject));
        Assert.assertEquals(null, somenil.optuserdata(null));
        throwsErrorOptUserdataClass(sometrue, Object.class, sampledata);
        throwsErrorOptUserdataClass(zero, MyData.class, sampledata);
        throwsErrorOptUserdataClass(intint, MyData.class, sampledata);
        throwsErrorOptUserdataClass(longdouble, MyData.class, sampledata);
        throwsErrorOptUserdataClass(somefunc, MyData.class, sampledata);
        throwsErrorOptUserdataClass(someclosure, MyData.class, sampledata);
        throwsErrorOptUserdataClass(stringstring, MyData.class, sampledata);
        throwsErrorOptUserdataClass(stringint, MyData.class, sampledata);
        throwsErrorOptUserdataClass(stringlong, MyData.class, sampledata);
        throwsErrorOptUserdataClass(stringlong, MyData.class, sampledata);
        throwsErrorOptUserdataClass(stringdouble, MyData.class, sampledata);
        throwsErrorOptUserdataClass(table, MyData.class, sampledata);
        throwsErrorOptUserdataClass(thread, MyData.class, sampledata);
        Assert.assertEquals(sampleobject, userdataobj.optuserdata(Object.class, sampleobject));
        Assert.assertEquals(sampleobject, userdataobj.optuserdata(null));
        Assert.assertEquals(sampledata, userdatacls.optuserdata(MyData.class, sampledata));
        Assert.assertEquals(sampledata, userdatacls.optuserdata(Object.class, sampleobject));
        Assert.assertEquals(sampledata, userdatacls.optuserdata(null));
        // should fail due to wrong class
        try {
            Object o = userdataobj.optuserdata(MyData.class, sampledata);
            Assert.fail("did not throw bad type error");
            Assert.assertTrue(o instanceof MyData);
        } catch (LuaException le) {
            Assert.assertEquals(MyData.class.getName() + " expected, got userdata", le.getMessage());
        }
    }

    @Test
    public void testOptValue() {
        Assert.assertEquals(zero, somenil.optvalue(zero));
        Assert.assertEquals(stringstring, somenil.optvalue(stringstring));
        Assert.assertEquals(sometrue, sometrue.optvalue(TRUE));
        Assert.assertEquals(somefalse, somefalse.optvalue(TRUE));
        Assert.assertEquals(zero, zero.optvalue(TRUE));
        Assert.assertEquals(intint, intint.optvalue(TRUE));
        Assert.assertEquals(longdouble, longdouble.optvalue(TRUE));
        Assert.assertEquals(somefunc, somefunc.optvalue(TRUE));
        Assert.assertEquals(someclosure, someclosure.optvalue(TRUE));
        Assert.assertEquals(stringstring, stringstring.optvalue(TRUE));
        Assert.assertEquals(stringint, stringint.optvalue(TRUE));
        Assert.assertEquals(stringlong, stringlong.optvalue(TRUE));
        Assert.assertEquals(stringdouble, stringdouble.optvalue(TRUE));
        Assert.assertEquals(thread, thread.optvalue(TRUE));
        Assert.assertEquals(table, table.optvalue(TRUE));
        Assert.assertEquals(userdataobj, userdataobj.optvalue(TRUE));
        Assert.assertEquals(userdatacls, userdatacls.optvalue(TRUE));
    }

    // ===================== Required argument conversion =======================

    private void throwsErrorReq(LuaValue obj, String method) {
        try {
            obj.getClass().getMethod(method).invoke(obj);
        } catch (InvocationTargetException e) {
            if (!(e.getTargetException() instanceof LuaException)) {
                Assert.fail("not a LuaError: " + e.getTargetException());
            }
            return; // pass
        } catch (Exception e) {
            Assert.fail("bad exception: " + e);
        }
        Assert.fail("failed to throw LuaError as required");
    }

    @Test
    public void testCheckBoolean() {
        throwsErrorReq(somenil, "checkboolean");
        Assert.assertEquals(true, sometrue.checkboolean());
        Assert.assertEquals(false, somefalse.checkboolean());
        throwsErrorReq(zero, "checkboolean");
        throwsErrorReq(intint, "checkboolean");
        throwsErrorReq(longdouble, "checkboolean");
        throwsErrorReq(doubledouble, "checkboolean");
        throwsErrorReq(somefunc, "checkboolean");
        throwsErrorReq(someclosure, "checkboolean");
        throwsErrorReq(stringstring, "checkboolean");
        throwsErrorReq(stringint, "checkboolean");
        throwsErrorReq(stringlong, "checkboolean");
        throwsErrorReq(stringdouble, "checkboolean");
        throwsErrorReq(thread, "checkboolean");
        throwsErrorReq(table, "checkboolean");
        throwsErrorReq(userdataobj, "checkboolean");
        throwsErrorReq(userdatacls, "checkboolean");
    }

    @Test
    public void testCheckClosure() {
        throwsErrorReq(somenil, "checkclosure");
        throwsErrorReq(sometrue, "checkclosure");
        throwsErrorReq(somefalse, "checkclosure");
        throwsErrorReq(zero, "checkclosure");
        throwsErrorReq(intint, "checkclosure");
        throwsErrorReq(longdouble, "checkclosure");
        throwsErrorReq(doubledouble, "checkclosure");
        throwsErrorReq(somefunc, "checkclosure");
        Assert.assertEquals(someclosure, someclosure.checkclosure());
        Assert.assertEquals(someclosure, someclosure.checkclosure());
        throwsErrorReq(stringstring, "checkclosure");
        throwsErrorReq(stringint, "checkclosure");
        throwsErrorReq(stringlong, "checkclosure");
        throwsErrorReq(stringdouble, "checkclosure");
        throwsErrorReq(thread, "checkclosure");
        throwsErrorReq(table, "checkclosure");
        throwsErrorReq(userdataobj, "checkclosure");
        throwsErrorReq(userdatacls, "checkclosure");
    }

    @Test
    public void testCheckDouble() {
        throwsErrorReq(somenil, "checkdouble");
        throwsErrorReq(sometrue, "checkdouble");
        throwsErrorReq(somefalse, "checkdouble");
        Assert.assertEquals(0., zero.checkdouble(), EPSILON);
        Assert.assertEquals(sampleint, intint.checkdouble(), EPSILON);
        Assert.assertEquals(samplelong, longdouble.checkdouble(), EPSILON);
        Assert.assertEquals(sampledouble, doubledouble.checkdouble(), EPSILON);
        throwsErrorReq(somefunc, "checkdouble");
        throwsErrorReq(someclosure, "checkdouble");
        throwsErrorReq(stringstring, "checkdouble");
        Assert.assertEquals(sampleint, stringint.checkdouble(), EPSILON);
        Assert.assertEquals(samplelong, stringlong.checkdouble(), EPSILON);
        Assert.assertEquals(sampledouble, stringdouble.checkdouble(), EPSILON);
        throwsErrorReq(thread, "checkdouble");
        throwsErrorReq(table, "checkdouble");
        throwsErrorReq(userdataobj, "checkdouble");
        throwsErrorReq(userdatacls, "checkdouble");
    }

    @Test
    public void testCheckFunction() {
        throwsErrorReq(somenil, "checkfunction");
        throwsErrorReq(sometrue, "checkfunction");
        throwsErrorReq(somefalse, "checkfunction");
        throwsErrorReq(zero, "checkfunction");
        throwsErrorReq(intint, "checkfunction");
        throwsErrorReq(longdouble, "checkfunction");
        throwsErrorReq(doubledouble, "checkfunction");
        Assert.assertEquals(somefunc, somefunc.checkfunction());
        Assert.assertEquals(someclosure, someclosure.checkfunction());
        Assert.assertEquals(somefunc, somefunc.checkfunction());
        Assert.assertEquals(someclosure, someclosure.checkfunction());
        throwsErrorReq(stringstring, "checkfunction");
        throwsErrorReq(stringint, "checkfunction");
        throwsErrorReq(stringlong, "checkfunction");
        throwsErrorReq(stringdouble, "checkfunction");
        throwsErrorReq(thread, "checkfunction");
        throwsErrorReq(table, "checkfunction");
        throwsErrorReq(userdataobj, "checkfunction");
        throwsErrorReq(userdatacls, "checkfunction");
    }

    @Test
    public void testCheckInt() {
        throwsErrorReq(somenil, "checkint");
        throwsErrorReq(sometrue, "checkint");
        throwsErrorReq(somefalse, "checkint");
        Assert.assertEquals(0, zero.checkint());
        Assert.assertEquals(sampleint, intint.checkint());
        Assert.assertEquals((int)samplelong, longdouble.checkint());
        Assert.assertEquals((int)sampledouble, doubledouble.checkint());
        throwsErrorReq(somefunc, "checkint");
        throwsErrorReq(someclosure, "checkint");
        throwsErrorReq(stringstring, "checkint");
        Assert.assertEquals(sampleint, stringint.checkint());
        Assert.assertEquals((int)samplelong, stringlong.checkint());
        Assert.assertEquals((int)sampledouble, stringdouble.checkint());
        throwsErrorReq(thread, "checkint");
        throwsErrorReq(table, "checkint");
        throwsErrorReq(userdataobj, "checkint");
        throwsErrorReq(userdatacls, "checkint");
    }

    @Test
    public void testCheckInteger() {
        throwsErrorReq(somenil, "checkinteger");
        throwsErrorReq(sometrue, "checkinteger");
        throwsErrorReq(somefalse, "checkinteger");
        Assert.assertEquals(zero, zero.checkinteger());
        Assert.assertEquals(LuaValue.valueOf(sampleint), intint.checkinteger());
        Assert.assertEquals(LuaValue.valueOf((int)samplelong), longdouble.checkinteger());
        Assert.assertEquals(LuaValue.valueOf((int)sampledouble), doubledouble.checkinteger());
        throwsErrorReq(somefunc, "checkinteger");
        throwsErrorReq(someclosure, "checkinteger");
        throwsErrorReq(stringstring, "checkinteger");
        Assert.assertEquals(LuaValue.valueOf(sampleint), stringint.checkinteger());
        Assert.assertEquals(LuaValue.valueOf((int)samplelong), stringlong.checkinteger());
        Assert.assertEquals(LuaValue.valueOf((int)sampledouble), stringdouble.checkinteger());
        throwsErrorReq(thread, "checkinteger");
        throwsErrorReq(table, "checkinteger");
        throwsErrorReq(userdataobj, "checkinteger");
        throwsErrorReq(userdatacls, "checkinteger");
    }

    @Test
    public void testCheckLong() {
        throwsErrorReq(somenil, "checklong");
        throwsErrorReq(sometrue, "checklong");
        throwsErrorReq(somefalse, "checklong");
        Assert.assertEquals(0L, zero.checklong());
        Assert.assertEquals(sampleint, intint.checklong());
        Assert.assertEquals(samplelong, longdouble.checklong());
        Assert.assertEquals((long)sampledouble, doubledouble.checklong());
        throwsErrorReq(somefunc, "checklong");
        throwsErrorReq(someclosure, "checklong");
        throwsErrorReq(stringstring, "checklong");
        Assert.assertEquals(sampleint, stringint.checklong());
        Assert.assertEquals(samplelong, stringlong.checklong());
        Assert.assertEquals((long)sampledouble, stringdouble.checklong());
        throwsErrorReq(thread, "checklong");
        throwsErrorReq(table, "checklong");
        throwsErrorReq(userdataobj, "checklong");
        throwsErrorReq(userdatacls, "checklong");
    }

    @Test
    public void testCheckNumber() {
        throwsErrorReq(somenil, "checknumber");
        throwsErrorReq(sometrue, "checknumber");
        throwsErrorReq(somefalse, "checknumber");
        Assert.assertEquals(zero, zero.checknumber());
        Assert.assertEquals(LuaValue.valueOf(sampleint), intint.checknumber());
        Assert.assertEquals(LuaValue.valueOf(samplelong), longdouble.checknumber());
        Assert.assertEquals(LuaValue.valueOf(sampledouble), doubledouble.checknumber());
        throwsErrorReq(somefunc, "checknumber");
        throwsErrorReq(someclosure, "checknumber");
        throwsErrorReq(stringstring, "checknumber");
        Assert.assertEquals(LuaValue.valueOf(sampleint), stringint.checknumber());
        Assert.assertEquals(LuaValue.valueOf(samplelong), stringlong.checknumber());
        Assert.assertEquals(LuaValue.valueOf(sampledouble), stringdouble.checknumber());
        throwsErrorReq(thread, "checknumber");
        throwsErrorReq(table, "checknumber");
        throwsErrorReq(userdataobj, "checknumber");
        throwsErrorReq(userdatacls, "checknumber");
    }

    @Test
    public void testCheckTable() {
        throwsErrorReq(somenil, "checktable");
        throwsErrorReq(sometrue, "checktable");
        throwsErrorReq(somefalse, "checktable");
        throwsErrorReq(zero, "checktable");
        throwsErrorReq(intint, "checktable");
        throwsErrorReq(longdouble, "checktable");
        throwsErrorReq(doubledouble, "checktable");
        throwsErrorReq(somefunc, "checktable");
        throwsErrorReq(someclosure, "checktable");
        throwsErrorReq(stringstring, "checktable");
        throwsErrorReq(stringint, "checktable");
        throwsErrorReq(stringlong, "checktable");
        throwsErrorReq(stringdouble, "checktable");
        throwsErrorReq(thread, "checktable");
        Assert.assertEquals(table, table.checktable());
        Assert.assertEquals(table, table.checktable());
        throwsErrorReq(userdataobj, "checktable");
        throwsErrorReq(userdatacls, "checktable");
    }

    @Test
    public void testCheckThread() {
        throwsErrorReq(somenil, "checkthread");
        throwsErrorReq(sometrue, "checkthread");
        throwsErrorReq(somefalse, "checkthread");
        throwsErrorReq(zero, "checkthread");
        throwsErrorReq(intint, "checkthread");
        throwsErrorReq(longdouble, "checkthread");
        throwsErrorReq(doubledouble, "checkthread");
        throwsErrorReq(somefunc, "checkthread");
        throwsErrorReq(someclosure, "checkthread");
        throwsErrorReq(stringstring, "checkthread");
        throwsErrorReq(stringint, "checkthread");
        throwsErrorReq(stringlong, "checkthread");
        throwsErrorReq(stringdouble, "checkthread");
        throwsErrorReq(table, "checkthread");
        Assert.assertEquals(thread, thread.checkthread());
        Assert.assertEquals(thread, thread.checkthread());
        throwsErrorReq(userdataobj, "checkthread");
        throwsErrorReq(userdatacls, "checkthread");
    }

    @Test
    public void testCheckJavaString() {
        throwsErrorReq(somenil, "checkjstring");
        throwsErrorReq(sometrue, "checkjstring");
        throwsErrorReq(somefalse, "checkjstring");
        Assert.assertEquals(String.valueOf(zero), zero.checkjstring());
        Assert.assertEquals(String.valueOf(intint), intint.checkjstring());
        Assert.assertEquals(String.valueOf(longdouble), longdouble.checkjstring());
        Assert.assertEquals(String.valueOf(doubledouble), doubledouble.checkjstring());
        throwsErrorReq(somefunc, "checkjstring");
        throwsErrorReq(someclosure, "checkjstring");
        Assert.assertEquals(samplestringstring, stringstring.checkjstring());
        Assert.assertEquals(samplestringint, stringint.checkjstring());
        Assert.assertEquals(samplestringlong, stringlong.checkjstring());
        Assert.assertEquals(samplestringdouble, stringdouble.checkjstring());
        throwsErrorReq(thread, "checkjstring");
        throwsErrorReq(table, "checkjstring");
        throwsErrorReq(userdataobj, "checkjstring");
        throwsErrorReq(userdatacls, "checkjstring");
    }

    @Test
    public void testCheckLuaString() {
        throwsErrorReq(somenil, "checkstring");
        throwsErrorReq(sometrue, "checkstring");
        throwsErrorReq(somefalse, "checkstring");
        Assert.assertEquals(LuaValue.valueOf("0"), zero.checkstring());
        Assert.assertEquals(stringint, intint.checkstring());
        Assert.assertEquals(stringlong, longdouble.checkstring());
        Assert.assertEquals(stringdouble, doubledouble.checkstring());
        throwsErrorReq(somefunc, "checkstring");
        throwsErrorReq(someclosure, "checkstring");
        Assert.assertEquals(stringstring, stringstring.checkstring());
        Assert.assertEquals(stringint, stringint.checkstring());
        Assert.assertEquals(stringlong, stringlong.checkstring());
        Assert.assertEquals(stringdouble, stringdouble.checkstring());
        throwsErrorReq(thread, "checkstring");
        throwsErrorReq(table, "checkstring");
        throwsErrorReq(userdataobj, "checkstring");
        throwsErrorReq(userdatacls, "checkstring");
    }

    @Test
    public void testCheckUserdata() {
        throwsErrorReq(somenil, "checkuserdata");
        throwsErrorReq(sometrue, "checkuserdata");
        throwsErrorReq(somefalse, "checkuserdata");
        throwsErrorReq(zero, "checkuserdata");
        throwsErrorReq(intint, "checkuserdata");
        throwsErrorReq(longdouble, "checkuserdata");
        throwsErrorReq(doubledouble, "checkuserdata");
        throwsErrorReq(somefunc, "checkuserdata");
        throwsErrorReq(someclosure, "checkuserdata");
        throwsErrorReq(stringstring, "checkuserdata");
        throwsErrorReq(stringint, "checkuserdata");
        throwsErrorReq(stringlong, "checkuserdata");
        throwsErrorReq(stringdouble, "checkuserdata");
        throwsErrorReq(table, "checkuserdata");
        Assert.assertEquals(sampleobject, userdataobj.checkuserdata());
        Assert.assertEquals(sampleobject, userdataobj.checkuserdata());
        Assert.assertEquals(sampledata, userdatacls.checkuserdata());
        Assert.assertEquals(sampledata, userdatacls.checkuserdata());
    }

    private void throwsErrorReqCheckUserdataClass(LuaValue obj, Class<?> arg) {
        try {
            obj.getClass().getMethod("checkuserdata", Class.class).invoke(obj, arg);
        } catch (InvocationTargetException e) {
            if (!(e.getTargetException() instanceof LuaException)) {
                Assert.fail("not a LuaError: " + e.getTargetException());
            }
            return; // pass
        } catch (Exception e) {
            Assert.fail("bad exception: " + e);
        }
        Assert.fail("failed to throw LuaError as required");
    }

    @Test
    public void testCheckUserdataClass() {
        throwsErrorReqCheckUserdataClass(somenil, Object.class);
        throwsErrorReqCheckUserdataClass(somenil, MyData.class);
        throwsErrorReqCheckUserdataClass(sometrue, Object.class);
        throwsErrorReqCheckUserdataClass(zero, MyData.class);
        throwsErrorReqCheckUserdataClass(intint, MyData.class);
        throwsErrorReqCheckUserdataClass(longdouble, MyData.class);
        throwsErrorReqCheckUserdataClass(somefunc, MyData.class);
        throwsErrorReqCheckUserdataClass(someclosure, MyData.class);
        throwsErrorReqCheckUserdataClass(stringstring, MyData.class);
        throwsErrorReqCheckUserdataClass(stringint, MyData.class);
        throwsErrorReqCheckUserdataClass(stringlong, MyData.class);
        throwsErrorReqCheckUserdataClass(stringlong, MyData.class);
        throwsErrorReqCheckUserdataClass(stringdouble, MyData.class);
        throwsErrorReqCheckUserdataClass(table, MyData.class);
        throwsErrorReqCheckUserdataClass(thread, MyData.class);
        Assert.assertEquals(sampleobject, userdataobj.checkuserdata(Object.class));
        Assert.assertEquals(sampleobject, userdataobj.checkuserdata());
        Assert.assertEquals(sampledata, userdatacls.checkuserdata(MyData.class));
        Assert.assertEquals(sampledata, userdatacls.checkuserdata(Object.class));
        Assert.assertEquals(sampledata, userdatacls.checkuserdata());
        // should fail due to wrong class
        try {
            Object o = userdataobj.checkuserdata(MyData.class);
            Assert.fail("did not throw bad type error");
            Assert.assertTrue(o instanceof MyData);
        } catch (LuaException le) {
            Assert.assertEquals(MyData.class.getName() + " expected, got userdata", le.getMessage());
        }
    }

    @Test
    public void testCheckValue() {
        throwsErrorReq(somenil, "checknotnil");
        Assert.assertEquals(sometrue, sometrue.checknotnil());
        Assert.assertEquals(somefalse, somefalse.checknotnil());
        Assert.assertEquals(zero, zero.checknotnil());
        Assert.assertEquals(intint, intint.checknotnil());
        Assert.assertEquals(longdouble, longdouble.checknotnil());
        Assert.assertEquals(somefunc, somefunc.checknotnil());
        Assert.assertEquals(someclosure, someclosure.checknotnil());
        Assert.assertEquals(stringstring, stringstring.checknotnil());
        Assert.assertEquals(stringint, stringint.checknotnil());
        Assert.assertEquals(stringlong, stringlong.checknotnil());
        Assert.assertEquals(stringdouble, stringdouble.checknotnil());
        Assert.assertEquals(thread, thread.checknotnil());
        Assert.assertEquals(table, table.checknotnil());
        Assert.assertEquals(userdataobj, userdataobj.checknotnil());
        Assert.assertEquals(userdatacls, userdatacls.checknotnil());
    }

}
