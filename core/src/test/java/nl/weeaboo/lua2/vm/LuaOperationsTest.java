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

import static nl.weeaboo.lua2.vm.LuaBoolean.FALSE;
import static nl.weeaboo.lua2.vm.LuaBoolean.TRUE;
import static nl.weeaboo.lua2.vm.LuaConstants.INDEX;
import static nl.weeaboo.lua2.vm.LuaConstants.NONE;
import static nl.weeaboo.lua2.vm.LuaNil.NIL;
import static nl.weeaboo.lua2.vm.LuaValue.valueOf;

import java.lang.reflect.InvocationTargetException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.LuaRunState;
import nl.weeaboo.lua2.compiler.LuaC;
import nl.weeaboo.lua2.lib.ZeroArgFunction;
import nl.weeaboo.lua2.vm.TypeTest.MyData;

public class LuaOperationsTest {

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
    private final LuaValue zero = valueOf(0);
    private final LuaValue intint = valueOf(sampleint);
    private final LuaValue longdouble = valueOf(samplelong);
    private final LuaValue doubledouble = valueOf(sampledouble);
    private final LuaValue stringstring = valueOf(samplestringstring);
    private final LuaValue stringint = valueOf(samplestringint);
    private final LuaValue stringlong = valueOf(samplestringlong);
    private final LuaValue stringdouble = valueOf(samplestringdouble);
    private final LuaTable table = LuaValue.listOf(new LuaValue[] { valueOf("aaa"), valueOf("bbb") });

    private final LuaValue somefunc = new ZeroArgFunction() {
        private static final long serialVersionUID = 1L;

        @Override
        public LuaValue call() {
            return NONE;
        }
    };
    private final Prototype proto = new Prototype();
    private final LuaClosure someclosure = new LuaClosure(proto, table);
    private final LuaUserdata userdataobj = LuaValue.userdataOf(sampleobject);
    private final LuaUserdata userdatacls = LuaValue.userdataOf(sampledata);

    private LuaThread thread;

    @Before
    public void before() throws LuaException {
        LuaRunState lrs = LuaRunState.newInstance();
        thread = new LuaThread(lrs, null);
    }

    private void throwsLuaError(String methodName, Object obj) {
        try {
            LuaValue.class.getMethod(methodName).invoke(obj);
            Assert.fail("failed to throw LuaError as required");
        } catch (InvocationTargetException e) {
            if (!(e.getTargetException() instanceof LuaError)) {
                Assert.fail("not a LuaError: " + e.getTargetException());
            }
            return; // pass
        } catch (Exception e) {
            Assert.fail("bad exception: " + e);
        }
    }

    @Test
    public void testLen() {
        throwsLuaError("len", somenil);
        throwsLuaError("len", sometrue);
        throwsLuaError("len", somefalse);
        throwsLuaError("len", zero);
        throwsLuaError("len", intint);
        throwsLuaError("len", longdouble);
        throwsLuaError("len", doubledouble);
        Assert.assertEquals(LuaInteger.valueOf(samplestringstring.length()), stringstring.len());
        Assert.assertEquals(LuaInteger.valueOf(samplestringint.length()), stringint.len());
        Assert.assertEquals(LuaInteger.valueOf(samplestringlong.length()), stringlong.len());
        Assert.assertEquals(LuaInteger.valueOf(samplestringdouble.length()), stringdouble.len());
        Assert.assertEquals(LuaInteger.valueOf(2), table.len());
        throwsLuaError("len", somefunc);
        throwsLuaError("len", thread);
        throwsLuaError("len", someclosure);
        throwsLuaError("len", userdataobj);
        throwsLuaError("len", userdatacls);
    }

    @Test
    public void testLength() {
        throwsLuaError("length", somenil);
        throwsLuaError("length", sometrue);
        throwsLuaError("length", somefalse);
        throwsLuaError("length", zero);
        throwsLuaError("length", intint);
        throwsLuaError("length", longdouble);
        throwsLuaError("length", doubledouble);
        Assert.assertEquals(samplestringstring.length(), stringstring.length());
        Assert.assertEquals(samplestringint.length(), stringint.length());
        Assert.assertEquals(samplestringlong.length(), stringlong.length());
        Assert.assertEquals(samplestringdouble.length(), stringdouble.length());
        Assert.assertEquals(2, table.length());
        throwsLuaError("length", somefunc);
        throwsLuaError("length", thread);
        throwsLuaError("length", someclosure);
        throwsLuaError("length", userdataobj);
        throwsLuaError("length", userdatacls);
    }

    public Prototype createPrototype(String script, String name) {
        try {
            return LuaC.compile(script, name);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.toString());
            return null;
        }
    }

    @Test
    public void testFunctionClosureThreadEnv() {
        final LuaTable globals = new LuaTable();

        // set up suitable environments for execution
        LuaValue aaa = valueOf("aaa");
        LuaValue eee = valueOf("eee");
        LuaTable newenv = LuaValue
                .tableOf(new LuaValue[] { valueOf("a"), valueOf("aaa"), valueOf("b"), valueOf("bbb"), });
        LuaTable mt = LuaValue.tableOf(new LuaValue[] { INDEX, globals });
        newenv.setmetatable(mt);
        globals.set("a", aaa);
        newenv.set("a", eee);

        // function tests
        {
            LuaFunction f = new ZeroArgFunction() {
                private static final long serialVersionUID = 1L;

                @Override
                public LuaValue call() {
                    return globals.get("a");
                }
            };
            Assert.assertEquals(aaa, f.call());
        }

        // closure tests
        {
            Prototype p = createPrototype("return a\n", "closuretester");
            LuaClosure c = new LuaClosure(p, globals);

            // Test that a clusure with a custom enviroment uses that environment.
            Assert.assertEquals(aaa, c.call());
            c = new LuaClosure(p, newenv);
            Assert.assertEquals(eee, c.call());
        }
    }
}
