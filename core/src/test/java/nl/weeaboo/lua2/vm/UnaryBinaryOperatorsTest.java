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
import static nl.weeaboo.lua2.vm.LuaConstants.META_ADD;
import static nl.weeaboo.lua2.vm.LuaConstants.META_CONCAT;
import static nl.weeaboo.lua2.vm.LuaConstants.META_DIV;
import static nl.weeaboo.lua2.vm.LuaConstants.META_EQ;
import static nl.weeaboo.lua2.vm.LuaConstants.META_LE;
import static nl.weeaboo.lua2.vm.LuaConstants.META_LT;
import static nl.weeaboo.lua2.vm.LuaConstants.META_MOD;
import static nl.weeaboo.lua2.vm.LuaConstants.META_MUL;
import static nl.weeaboo.lua2.vm.LuaConstants.META_POW;
import static nl.weeaboo.lua2.vm.LuaConstants.META_SUB;
import static nl.weeaboo.lua2.vm.LuaNil.NIL;

import java.lang.reflect.InvocationTargetException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.LuaRunState;
import nl.weeaboo.lua2.lib.TwoArgFunction;

/**
 * Tests of basic unary and binary operators on main value types.
 */
public class UnaryBinaryOperatorsTest {

    private static final float EPSILON = 0.001f;

    @Before
    public void before() throws LuaException {
        LuaRunState.create();
    }

    @Test
    public void testEqualsBool() {
        Assert.assertEquals(FALSE, FALSE);
        Assert.assertEquals(TRUE, TRUE);
        Assert.assertTrue(FALSE.equals(FALSE));
        Assert.assertTrue(TRUE.equals(TRUE));
        Assert.assertTrue(!FALSE.equals(TRUE));
        Assert.assertTrue(!TRUE.equals(FALSE));
        Assert.assertTrue(FALSE.eq_b(FALSE));
        Assert.assertTrue(TRUE.eq_b(TRUE));
        Assert.assertFalse(FALSE.eq_b(TRUE));
        Assert.assertFalse(TRUE.eq_b(FALSE));
        Assert.assertEquals(TRUE, FALSE.eq(FALSE));
        Assert.assertEquals(TRUE, TRUE.eq(TRUE));
        Assert.assertEquals(FALSE, FALSE.eq(TRUE));
        Assert.assertEquals(FALSE, TRUE.eq(FALSE));
        Assert.assertFalse(FALSE.neq_b(FALSE));
        Assert.assertFalse(TRUE.neq_b(TRUE));
        Assert.assertTrue(FALSE.neq_b(TRUE));
        Assert.assertTrue(TRUE.neq_b(FALSE));
        Assert.assertEquals(FALSE, FALSE.neq(FALSE));
        Assert.assertEquals(FALSE, TRUE.neq(TRUE));
        Assert.assertEquals(TRUE, FALSE.neq(TRUE));
        Assert.assertEquals(TRUE, TRUE.neq(FALSE));
        Assert.assertTrue(TRUE.toboolean());
        Assert.assertFalse(FALSE.toboolean());
    }

    @Test
    public void testNot() {
        LuaValue ia = LuaValue.valueOf(3);
        LuaValue da = LuaValue.valueOf(.25);
        LuaValue sa = LuaValue.valueOf("1.5");
        LuaValue ba = TRUE;
        LuaValue bb = FALSE;

        // like kinds
        Assert.assertEquals(FALSE, ia.not());
        Assert.assertEquals(FALSE, da.not());
        Assert.assertEquals(FALSE, sa.not());
        Assert.assertEquals(FALSE, ba.not());
        Assert.assertEquals(TRUE, bb.not());
    }

    @Test
    public void testNeg() {
        LuaValue ia = LuaValue.valueOf(3);
        LuaValue ib = LuaValue.valueOf(-4);
        LuaValue da = LuaValue.valueOf(.25);
        LuaValue db = LuaValue.valueOf(-.5);
        LuaValue sa = LuaValue.valueOf("1.5");
        LuaValue sb = LuaValue.valueOf("-2.0");

        // like kinds
        Assert.assertEquals(-3., ia.neg().todouble(), EPSILON);
        Assert.assertEquals(-.25, da.neg().todouble(), EPSILON);
        Assert.assertEquals(-1.5, sa.neg().todouble(), EPSILON);
        Assert.assertEquals(4., ib.neg().todouble(), EPSILON);
        Assert.assertEquals(.5, db.neg().todouble(), EPSILON);
        Assert.assertEquals(2.0, sb.neg().todouble(), EPSILON);
    }

    @Test
    public void testDoublesBecomeInts() {
        // DoubleValue.valueOf should return int
        LuaValue ia = LuaInteger.valueOf(345);
        LuaValue da = LuaDouble.valueOf(345.0);
        LuaValue db = LuaDouble.valueOf(345.5);
        LuaValue sa = LuaValue.valueOf("3.0");
        LuaValue sb = LuaValue.valueOf("3");
        LuaValue sc = LuaValue.valueOf("-2.0");
        LuaValue sd = LuaValue.valueOf("-2");

        Assert.assertEquals(ia, da);
        Assert.assertTrue(ia instanceof LuaInteger);
        Assert.assertTrue(da instanceof LuaInteger);
        Assert.assertTrue(db instanceof LuaDouble);
        Assert.assertEquals(ia.toint(), 345);
        Assert.assertEquals(da.toint(), 345);
        Assert.assertEquals(da.todouble(), 345.0, EPSILON);
        Assert.assertEquals(db.todouble(), 345.5, EPSILON);

        Assert.assertTrue(sa instanceof LuaString);
        Assert.assertTrue(sb instanceof LuaString);
        Assert.assertTrue(sc instanceof LuaString);
        Assert.assertTrue(sd instanceof LuaString);
        Assert.assertEquals(3., sa.todouble(), EPSILON);
        Assert.assertEquals(3., sb.todouble(), EPSILON);
        Assert.assertEquals(-2., sc.todouble(), EPSILON);
        Assert.assertEquals(-2., sd.todouble(), EPSILON);

    }

    @Test
    public void testEqualsInt() {
        LuaValue ia = LuaInteger.valueOf(345);
        LuaValue ib = LuaInteger.valueOf(345);
        LuaValue ic = LuaInteger.valueOf(-345);
        LuaString sa = LuaString.valueOf("345");
        LuaValue sb = LuaString.valueOf("345");
        LuaValue sc = LuaString.valueOf("-345");

        // assert equals for same type
        Assert.assertEquals(ia, ib);
        Assert.assertEquals(sa, sb);
        Assert.assertFalse(ia.equals(ic));
        Assert.assertFalse(sa.equals(sc));

        // check object equality for different types
        Assert.assertFalse(ia.equals(sa));
        Assert.assertFalse(sa.equals(ia));
    }

    @Test
    public void testEqualsDouble() {
        LuaValue da = LuaDouble.valueOf(345.5);
        LuaValue db = LuaDouble.valueOf(345.5);
        LuaValue dc = LuaDouble.valueOf(-345.5);
        LuaString sa = LuaString.valueOf("345.5");
        LuaValue sb = LuaString.valueOf("345.5");
        LuaValue sc = LuaString.valueOf("-345.5");

        // assert equals for same type
        Assert.assertEquals(da, db);
        Assert.assertEquals(sa, sb);
        Assert.assertFalse(da.equals(dc));
        Assert.assertFalse(sa.equals(sc));

        // check object equality for different types
        Assert.assertFalse(da.equals(sa));
        Assert.assertFalse(sa.equals(da));
    }

    @Test
    public void testEqInt() {
        LuaValue ia = LuaInteger.valueOf(345);
        LuaValue ib = LuaInteger.valueOf(345);
        LuaValue ic = LuaInteger.valueOf(-123);
        LuaValue sa = LuaString.valueOf("345");
        LuaValue sb = LuaString.valueOf("345");
        LuaValue sc = LuaString.valueOf("-345");

        // check arithmetic equality among same types
        Assert.assertEquals(ia.eq(ib), TRUE);
        Assert.assertEquals(sa.eq(sb), TRUE);
        Assert.assertEquals(ia.eq(ic), FALSE);
        Assert.assertEquals(sa.eq(sc), FALSE);

        // check arithmetic equality among different types
        Assert.assertEquals(ia.eq(sa), FALSE);
        Assert.assertEquals(sa.eq(ia), FALSE);

        // equals with mismatched types
        LuaValue t = new LuaTable();
        Assert.assertEquals(ia.eq(t), FALSE);
        Assert.assertEquals(t.eq(ia), FALSE);
        Assert.assertEquals(ia.eq(FALSE), FALSE);
        Assert.assertEquals(FALSE.eq(ia), FALSE);
        Assert.assertEquals(ia.eq(NIL), FALSE);
        Assert.assertEquals(NIL.eq(ia), FALSE);
    }

    @Test
    public void testEqDouble() {
        LuaValue da = LuaDouble.valueOf(345.5);
        LuaValue db = LuaDouble.valueOf(345.5);
        LuaValue dc = LuaDouble.valueOf(-345.5);
        LuaValue sa = LuaString.valueOf("345.5");
        LuaValue sb = LuaString.valueOf("345.5");
        LuaValue sc = LuaString.valueOf("-345.5");

        // check arithmetic equality among same types
        Assert.assertEquals(da.eq(db), TRUE);
        Assert.assertEquals(sa.eq(sb), TRUE);
        Assert.assertEquals(da.eq(dc), FALSE);
        Assert.assertEquals(sa.eq(sc), FALSE);

        // check arithmetic equality among different types
        Assert.assertEquals(da.eq(sa), FALSE);
        Assert.assertEquals(sa.eq(da), FALSE);

        // equals with mismatched types
        LuaValue t = new LuaTable();
        Assert.assertEquals(da.eq(t), FALSE);
        Assert.assertEquals(t.eq(da), FALSE);
        Assert.assertEquals(da.eq(FALSE), FALSE);
        Assert.assertEquals(FALSE.eq(da), FALSE);
        Assert.assertEquals(da.eq(NIL), FALSE);
        Assert.assertEquals(NIL.eq(da), FALSE);
    }

    private static final TwoArgFunction RETURN_NIL = new TwoArgFunction() {
        private static final long serialVersionUID = 1L;

        @Override
        public LuaValue call(LuaValue lhs, LuaValue rhs) {
            return NIL;
        }
    };

    private static final TwoArgFunction RETURN_ONE = new TwoArgFunction() {
        private static final long serialVersionUID = 1L;

        @Override
        public LuaValue call(LuaValue lhs, LuaValue rhs) {
            return LuaInteger.valueOf(1);
        }
    };

    @Test
    public void testEqualsMetatag() {
        LuaValue tru = TRUE;
        LuaValue fal = FALSE;
        LuaValue zer = LuaInteger.valueOf(0);
        LuaValue one = LuaInteger.valueOf(1);
        LuaValue abc = LuaValue.valueOf("abcdef").substring(0, 3);
        LuaValue def = LuaValue.valueOf("abcdef").substring(3, 6);
        LuaValue pi = LuaValue.valueOf(Math.PI);
        LuaValue ee = LuaValue.valueOf(Math.E);
        LuaValue tbl = new LuaTable();
        LuaValue tbl2 = new LuaTable();
        LuaValue tbl3 = new LuaTable();
        LuaValue uda = new LuaUserdata(new Object());
        LuaValue udb = new LuaUserdata(uda.touserdata());
        LuaValue uda2 = new LuaUserdata(new Object());
        LuaValue uda3 = new LuaUserdata(uda.touserdata());
        LuaValue nilb = LuaValue.valueOf(NIL.toboolean());
        LuaValue oneb = LuaValue.valueOf(one.toboolean());
        Assert.assertEquals(FALSE, nilb);
        Assert.assertEquals(TRUE, oneb);
        LuaValue smt = LuaString.s_metatable;
        try {
            // always return nil0
            LuaBoolean.s_metatable = LuaValue.tableOf(new LuaValue[] { META_EQ, RETURN_NIL, });
            LuaNumber.s_metatable = LuaValue.tableOf(new LuaValue[] { META_EQ, RETURN_NIL, });
            LuaString.s_metatable = LuaValue.tableOf(new LuaValue[] { META_EQ, RETURN_NIL, });
            tbl.setmetatable(LuaValue.tableOf(new LuaValue[] { META_EQ, RETURN_NIL, }));
            tbl2.setmetatable(LuaValue.tableOf(new LuaValue[] { META_EQ, RETURN_NIL, }));
            uda.setmetatable(LuaValue.tableOf(new LuaValue[] { META_EQ, RETURN_NIL, }));
            udb.setmetatable(uda.getmetatable());
            uda2.setmetatable(LuaValue.tableOf(new LuaValue[] { META_EQ, RETURN_NIL, }));
            // diff metatag function
            tbl3.setmetatable(LuaValue.tableOf(new LuaValue[] { META_EQ, RETURN_ONE, }));
            uda3.setmetatable(LuaValue.tableOf(new LuaValue[] { META_EQ, RETURN_ONE, }));

            // primitive types or same valu do not invoke metatag as per C implementation
            Assert.assertEquals(tru, tru.eq(tru));
            Assert.assertEquals(tru, one.eq(one));
            Assert.assertEquals(tru, abc.eq(abc));
            Assert.assertEquals(tru, tbl.eq(tbl));
            Assert.assertEquals(tru, uda.eq(uda));
            Assert.assertEquals(tru, uda.eq(udb));
            Assert.assertEquals(fal, tru.eq(fal));
            Assert.assertEquals(fal, fal.eq(tru));
            Assert.assertEquals(fal, zer.eq(one));
            Assert.assertEquals(fal, one.eq(zer));
            Assert.assertEquals(fal, pi.eq(ee));
            Assert.assertEquals(fal, ee.eq(pi));
            Assert.assertEquals(fal, pi.eq(one));
            Assert.assertEquals(fal, one.eq(pi));
            Assert.assertEquals(fal, abc.eq(def));
            Assert.assertEquals(fal, def.eq(abc));
            // different types. not comparable
            Assert.assertEquals(fal, fal.eq(tbl));
            Assert.assertEquals(fal, tbl.eq(fal));
            Assert.assertEquals(fal, tbl.eq(one));
            Assert.assertEquals(fal, one.eq(tbl));
            Assert.assertEquals(fal, fal.eq(one));
            Assert.assertEquals(fal, one.eq(fal));
            Assert.assertEquals(fal, abc.eq(one));
            Assert.assertEquals(fal, one.eq(abc));
            Assert.assertEquals(fal, tbl.eq(uda));
            Assert.assertEquals(fal, uda.eq(tbl));
            // same type, same value, does not invoke metatag op
            Assert.assertEquals(tru, tbl.eq(tbl));
            // same type, different value, same metatag op. comparabile via metatag op
            Assert.assertEquals(nilb, tbl.eq(tbl2));
            Assert.assertEquals(nilb, tbl2.eq(tbl));
            Assert.assertEquals(nilb, uda.eq(uda2));
            Assert.assertEquals(nilb, uda2.eq(uda));
            // same type, different metatag ops. not comparable
            Assert.assertEquals(fal, tbl.eq(tbl3));
            Assert.assertEquals(fal, tbl3.eq(tbl));
            Assert.assertEquals(fal, uda.eq(uda3));
            Assert.assertEquals(fal, uda3.eq(uda));

            // always use right argument
            LuaBoolean.s_metatable = LuaValue.tableOf(new LuaValue[] { META_EQ, RETURN_ONE, });
            LuaNumber.s_metatable = LuaValue.tableOf(new LuaValue[] { META_EQ, RETURN_ONE, });
            LuaString.s_metatable = LuaValue.tableOf(new LuaValue[] { META_EQ, RETURN_ONE, });
            tbl.setmetatable(LuaValue.tableOf(new LuaValue[] { META_EQ, RETURN_ONE, }));
            tbl2.setmetatable(LuaValue.tableOf(new LuaValue[] { META_EQ, RETURN_ONE, }));
            uda.setmetatable(LuaValue.tableOf(new LuaValue[] { META_EQ, RETURN_ONE, }));
            udb.setmetatable(uda.getmetatable());
            uda2.setmetatable(LuaValue.tableOf(new LuaValue[] { META_EQ, RETURN_ONE, }));
            // diff metatag function
            tbl3.setmetatable(LuaValue.tableOf(new LuaValue[] { META_EQ, RETURN_NIL, }));
            uda3.setmetatable(LuaValue.tableOf(new LuaValue[] { META_EQ, RETURN_NIL, }));

            // primitive types or same value do not invoke metatag as per C implementation
            Assert.assertEquals(tru, tru.eq(tru));
            Assert.assertEquals(tru, one.eq(one));
            Assert.assertEquals(tru, abc.eq(abc));
            Assert.assertEquals(tru, tbl.eq(tbl));
            Assert.assertEquals(tru, uda.eq(uda));
            Assert.assertEquals(tru, uda.eq(udb));
            Assert.assertEquals(fal, tru.eq(fal));
            Assert.assertEquals(fal, fal.eq(tru));
            Assert.assertEquals(fal, zer.eq(one));
            Assert.assertEquals(fal, one.eq(zer));
            Assert.assertEquals(fal, pi.eq(ee));
            Assert.assertEquals(fal, ee.eq(pi));
            Assert.assertEquals(fal, pi.eq(one));
            Assert.assertEquals(fal, one.eq(pi));
            Assert.assertEquals(fal, abc.eq(def));
            Assert.assertEquals(fal, def.eq(abc));
            // different types. not comparable
            Assert.assertEquals(fal, fal.eq(tbl));
            Assert.assertEquals(fal, tbl.eq(fal));
            Assert.assertEquals(fal, tbl.eq(one));
            Assert.assertEquals(fal, one.eq(tbl));
            Assert.assertEquals(fal, fal.eq(one));
            Assert.assertEquals(fal, one.eq(fal));
            Assert.assertEquals(fal, abc.eq(one));
            Assert.assertEquals(fal, one.eq(abc));
            Assert.assertEquals(fal, tbl.eq(uda));
            Assert.assertEquals(fal, uda.eq(tbl));
            // same type, same value, does not invoke metatag op
            Assert.assertEquals(tru, tbl.eq(tbl));
            // same type, different value, same metatag op. comparabile via metatag op
            Assert.assertEquals(oneb, tbl.eq(tbl2));
            Assert.assertEquals(oneb, tbl2.eq(tbl));
            Assert.assertEquals(oneb, uda.eq(uda2));
            Assert.assertEquals(oneb, uda2.eq(uda));
            // same type, different metatag ops. not comparable
            Assert.assertEquals(fal, tbl.eq(tbl3));
            Assert.assertEquals(fal, tbl3.eq(tbl));
            Assert.assertEquals(fal, uda.eq(uda3));
            Assert.assertEquals(fal, uda3.eq(uda));

        } finally {
            LuaBoolean.s_metatable = null;
            LuaNumber.s_metatable = null;
            LuaString.s_metatable = smt;
        }
    }

    @Test
    public void testAdd() {
        LuaValue ia = LuaValue.valueOf(111);
        LuaValue ib = LuaValue.valueOf(44);
        LuaValue da = LuaValue.valueOf(55.25);
        LuaValue db = LuaValue.valueOf(3.5);
        LuaValue sa = LuaValue.valueOf("22.125");
        LuaValue sb = LuaValue.valueOf("7.25");

        // check types
        Assert.assertTrue(ia instanceof LuaInteger);
        Assert.assertTrue(ib instanceof LuaInteger);
        Assert.assertTrue(da instanceof LuaDouble);
        Assert.assertTrue(db instanceof LuaDouble);
        Assert.assertTrue(sa instanceof LuaString);
        Assert.assertTrue(sb instanceof LuaString);

        // like kinds
        Assert.assertEquals(155.0, ia.add(ib).todouble(), EPSILON);
        Assert.assertEquals(58.75, da.add(db).todouble(), EPSILON);
        Assert.assertEquals(29.375, sa.add(sb).todouble(), EPSILON);

        // unlike kinds
        Assert.assertEquals(166.25, ia.add(da).todouble(), EPSILON);
        Assert.assertEquals(166.25, da.add(ia).todouble(), EPSILON);
        Assert.assertEquals(133.125, ia.add(sa).todouble(), EPSILON);
        Assert.assertEquals(133.125, sa.add(ia).todouble(), EPSILON);
        Assert.assertEquals(77.375, da.add(sa).todouble(), EPSILON);
        Assert.assertEquals(77.375, sa.add(da).todouble(), EPSILON);
    }

    @Test
    public void testSub() {
        LuaValue ia = LuaValue.valueOf(111);
        LuaValue ib = LuaValue.valueOf(44);
        LuaValue da = LuaValue.valueOf(55.25);
        LuaValue db = LuaValue.valueOf(3.5);
        LuaValue sa = LuaValue.valueOf("22.125");
        LuaValue sb = LuaValue.valueOf("7.25");

        // like kinds
        Assert.assertEquals(67.0, ia.sub(ib).todouble(), EPSILON);
        Assert.assertEquals(51.75, da.sub(db).todouble(), EPSILON);
        Assert.assertEquals(14.875, sa.sub(sb).todouble(), EPSILON);

        // unlike kinds
        Assert.assertEquals(55.75, ia.sub(da).todouble(), EPSILON);
        Assert.assertEquals(-55.75, da.sub(ia).todouble(), EPSILON);
        Assert.assertEquals(88.875, ia.sub(sa).todouble(), EPSILON);
        Assert.assertEquals(-88.875, sa.sub(ia).todouble(), EPSILON);
        Assert.assertEquals(33.125, da.sub(sa).todouble(), EPSILON);
        Assert.assertEquals(-33.125, sa.sub(da).todouble(), EPSILON);
    }

    @Test
    public void testMul() {
        LuaValue ia = LuaValue.valueOf(3);
        LuaValue ib = LuaValue.valueOf(4);
        LuaValue da = LuaValue.valueOf(.25);
        LuaValue db = LuaValue.valueOf(.5);
        LuaValue sa = LuaValue.valueOf("1.5");
        LuaValue sb = LuaValue.valueOf("2.0");

        // like kinds
        Assert.assertEquals(12.0, ia.mul(ib).todouble(), EPSILON);
        Assert.assertEquals(.125, da.mul(db).todouble(), EPSILON);
        Assert.assertEquals(3.0, sa.mul(sb).todouble(), EPSILON);

        // unlike kinds
        Assert.assertEquals(.75, ia.mul(da).todouble(), EPSILON);
        Assert.assertEquals(.75, da.mul(ia).todouble(), EPSILON);
        Assert.assertEquals(4.5, ia.mul(sa).todouble(), EPSILON);
        Assert.assertEquals(4.5, sa.mul(ia).todouble(), EPSILON);
        Assert.assertEquals(.375, da.mul(sa).todouble(), EPSILON);
        Assert.assertEquals(.375, sa.mul(da).todouble(), EPSILON);
    }

    @Test
    public void testDiv() {
        LuaValue ia = LuaValue.valueOf(3);
        LuaValue ib = LuaValue.valueOf(4);
        LuaValue da = LuaValue.valueOf(.25);
        LuaValue db = LuaValue.valueOf(.5);
        LuaValue sa = LuaValue.valueOf("1.5");
        LuaValue sb = LuaValue.valueOf("2.0");

        // like kinds
        Assert.assertEquals(3. / 4., ia.div(ib).todouble(), EPSILON);
        Assert.assertEquals(.25 / .5, da.div(db).todouble(), EPSILON);
        Assert.assertEquals(1.5 / 2., sa.div(sb).todouble(), EPSILON);

        // unlike kinds
        Assert.assertEquals(3. / .25, ia.div(da).todouble(), EPSILON);
        Assert.assertEquals(.25 / 3., da.div(ia).todouble(), EPSILON);
        Assert.assertEquals(3. / 1.5, ia.div(sa).todouble(), EPSILON);
        Assert.assertEquals(1.5 / 3., sa.div(ia).todouble(), EPSILON);
        Assert.assertEquals(.25 / 1.5, da.div(sa).todouble(), EPSILON);
        Assert.assertEquals(1.5 / .25, sa.div(da).todouble(), EPSILON);
    }

    @Test
    public void testPow() {
        LuaValue ia = LuaValue.valueOf(3);
        LuaValue ib = LuaValue.valueOf(4);
        LuaValue da = LuaValue.valueOf(4.);
        LuaValue db = LuaValue.valueOf(.5);
        LuaValue sa = LuaValue.valueOf("1.5");
        LuaValue sb = LuaValue.valueOf("2.0");

        // like kinds
        Assert.assertEquals(Math.pow(3., 4.), ia.pow(ib).todouble(), EPSILON);
        Assert.assertEquals(Math.pow(4., .5), da.pow(db).todouble(), EPSILON);
        Assert.assertEquals(Math.pow(1.5, 2.), sa.pow(sb).todouble(), EPSILON);

        // unlike kinds
        Assert.assertEquals(Math.pow(3., 4.), ia.pow(da).todouble(), EPSILON);
        Assert.assertEquals(Math.pow(4., 3.), da.pow(ia).todouble(), EPSILON);
        Assert.assertEquals(Math.pow(3., 1.5), ia.pow(sa).todouble(), EPSILON);
        Assert.assertEquals(Math.pow(1.5, 3.), sa.pow(ia).todouble(), EPSILON);
        Assert.assertEquals(Math.pow(4., 1.5), da.pow(sa).todouble(), EPSILON);
        Assert.assertEquals(Math.pow(1.5, 4.), sa.pow(da).todouble(), EPSILON);
    }

    private static double luaMod(double x, double y) {
        return y != 0 ? x - y * Math.floor(x / y) : Double.NaN;
    }

    @Test
    public void testMod() {
        LuaValue ia = LuaValue.valueOf(3);
        LuaValue ib = LuaValue.valueOf(-4);
        LuaValue da = LuaValue.valueOf(.25);
        LuaValue db = LuaValue.valueOf(-.5);
        LuaValue sa = LuaValue.valueOf("1.5");
        LuaValue sb = LuaValue.valueOf("-2.0");

        // like kinds
        Assert.assertEquals(luaMod(3., -4.), ia.mod(ib).todouble(), EPSILON);
        Assert.assertEquals(luaMod(.25, -.5), da.mod(db).todouble(), EPSILON);
        Assert.assertEquals(luaMod(1.5, -2.), sa.mod(sb).todouble(), EPSILON);

        // unlike kinds
        Assert.assertEquals(luaMod(3., .25), ia.mod(da).todouble(), EPSILON);
        Assert.assertEquals(luaMod(.25, 3.), da.mod(ia).todouble(), EPSILON);
        Assert.assertEquals(luaMod(3., 1.5), ia.mod(sa).todouble(), EPSILON);
        Assert.assertEquals(luaMod(1.5, 3.), sa.mod(ia).todouble(), EPSILON);
        Assert.assertEquals(luaMod(.25, 1.5), da.mod(sa).todouble(), EPSILON);
        Assert.assertEquals(luaMod(1.5, .25), sa.mod(da).todouble(), EPSILON);
    }

    @Test
    public void testArithErrors() {
        String[] ops = { "add", "sub", "mul", "div", "mod", "pow" };
        LuaValue[] vals = { NIL, TRUE, LuaValue.tableOf() };
        LuaValue[] numerics = { LuaValue.valueOf(111), LuaValue.valueOf(55.25), LuaValue.valueOf("22.125") };
        for (int i = 0; i < ops.length; i++) {
            for (int j = 0; j < vals.length; j++) {
                for (int k = 0; k < numerics.length; k++) {
                    checkArithError(vals[j], numerics[k], ops[i], vals[j].typename());
                    checkArithError(numerics[k], vals[j], ops[i], vals[j].typename());
                }
            }
        }
    }

    private void checkArithError(LuaValue a, LuaValue b, String op, String type) {
        try {
            LuaValue.class.getMethod(op, new Class[] { LuaValue.class }).invoke(a, new Object[] { b });
        } catch (InvocationTargetException ite) {
            String actual = ite.getTargetException().getMessage();
            if ((!actual.startsWith("attempt to perform arithmetic")) || actual.indexOf(type) < 0) {
                Assert.fail(
                        "(" + a.typename() + "," + op + "," + b.typename() + ") reported '" + actual + "'");
            }
        } catch (Exception e) {
            Assert.fail("(" + a.typename() + "," + op + "," + b.typename() + ") threw " + e);
        }
    }

    private static final TwoArgFunction RETURN_LHS = new TwoArgFunction() {
        private static final long serialVersionUID = 1L;

        @Override
        public LuaValue call(LuaValue lhs, LuaValue rhs) {
            return lhs;
        }
    };

    private static final TwoArgFunction RETURN_RHS = new TwoArgFunction() {
        private static final long serialVersionUID = 1L;

        @Override
        public LuaValue call(LuaValue lhs, LuaValue rhs) {
            return rhs;
        }
    };

    @Test
    public void testArithMetatag() {
        LuaValue tru = TRUE;
        LuaValue fal = FALSE;
        LuaValue tbl = new LuaTable();
        LuaValue tbl2 = new LuaTable();
        try {
            try {
                tru.add(tbl);
                Assert.fail("did not throw error");
            } catch (LuaError le) {
                // Expected
            }
            try {
                tru.sub(tbl);
                Assert.fail("did not throw error");
            } catch (LuaError le) {
                // Expected
            }
            try {
                tru.mul(tbl);
                Assert.fail("did not throw error");
            } catch (LuaError le) {
                // Expected
            }
            try {
                tru.div(tbl);
                Assert.fail("did not throw error");
            } catch (LuaError le) {
                // Expected
            }
            try {
                tru.pow(tbl);
                Assert.fail("did not throw error");
            } catch (LuaError le) {
                // Expected
            }
            try {
                tru.mod(tbl);
                Assert.fail("did not throw error");
            } catch (LuaError le) {
                // Expected
            }

            // always use left argument
            LuaBoolean.s_metatable = LuaValue.tableOf(new LuaValue[] { META_ADD, RETURN_LHS, });
            Assert.assertEquals(tru, tru.add(fal));
            Assert.assertEquals(tru, tru.add(tbl));
            Assert.assertEquals(tbl, tbl.add(tru));
            try {
                tbl.add(tbl2);
                Assert.fail("did not throw error");
            } catch (LuaError le) {
                // Expected
            }
            try {
                tru.sub(tbl);
                Assert.fail("did not throw error");
            } catch (LuaError le) {
                // Expected
            }

            LuaBoolean.s_metatable = LuaValue.tableOf(new LuaValue[] { META_SUB, RETURN_LHS, });
            Assert.assertEquals(tru, tru.sub(fal));
            Assert.assertEquals(tru, tru.sub(tbl));
            Assert.assertEquals(tbl, tbl.sub(tru));
            try {
                tbl.sub(tbl2);
                Assert.fail("did not throw error");
            } catch (LuaError le) {
                // Expected
            }
            try {
                tru.add(tbl);
                Assert.fail("did not throw error");
            } catch (LuaError le) {
                // Expected
            }

            LuaBoolean.s_metatable = LuaValue.tableOf(new LuaValue[] { META_MUL, RETURN_LHS, });
            Assert.assertEquals(tru, tru.mul(fal));
            Assert.assertEquals(tru, tru.mul(tbl));
            Assert.assertEquals(tbl, tbl.mul(tru));
            try {
                tbl.mul(tbl2);
                Assert.fail("did not throw error");
            } catch (LuaError le) {
                // Expected
            }
            try {
                tru.sub(tbl);
                Assert.fail("did not throw error");
            } catch (LuaError le) {
                // Expected
            }

            LuaBoolean.s_metatable = LuaValue.tableOf(new LuaValue[] { META_DIV, RETURN_LHS, });
            Assert.assertEquals(tru, tru.div(fal));
            Assert.assertEquals(tru, tru.div(tbl));
            Assert.assertEquals(tbl, tbl.div(tru));
            try {
                tbl.div(tbl2);
                Assert.fail("did not throw error");
            } catch (LuaError le) {
                // Expected
            }
            try {
                tru.sub(tbl);
                Assert.fail("did not throw error");
            } catch (LuaError le) {
                // Expected
            }

            LuaBoolean.s_metatable = LuaValue.tableOf(new LuaValue[] { META_POW, RETURN_LHS, });
            Assert.assertEquals(tru, tru.pow(fal));
            Assert.assertEquals(tru, tru.pow(tbl));
            Assert.assertEquals(tbl, tbl.pow(tru));
            try {
                tbl.pow(tbl2);
                Assert.fail("did not throw error");
            } catch (LuaError le) {
                // Expected
            }
            try {
                tru.sub(tbl);
                Assert.fail("did not throw error");
            } catch (LuaError le) {
                // Expected
            }

            LuaBoolean.s_metatable = LuaValue.tableOf(new LuaValue[] { META_MOD, RETURN_LHS, });
            Assert.assertEquals(tru, tru.mod(fal));
            Assert.assertEquals(tru, tru.mod(tbl));
            Assert.assertEquals(tbl, tbl.mod(tru));
            try {
                tbl.mod(tbl2);
                Assert.fail("did not throw error");
            } catch (LuaError le) {
                // Expected
            }
            try {
                tru.sub(tbl);
                Assert.fail("did not throw error");
            } catch (LuaError le) {
                // Expected
            }

            // always use right argument
            LuaBoolean.s_metatable = LuaValue.tableOf(new LuaValue[] { META_ADD, RETURN_RHS, });
            Assert.assertEquals(fal, tru.add(fal));
            Assert.assertEquals(tbl, tru.add(tbl));
            Assert.assertEquals(tru, tbl.add(tru));
            try {
                tbl.add(tbl2);
                Assert.fail("did not throw error");
            } catch (LuaError le) {
                // Expected
            }
            try {
                tru.sub(tbl);
                Assert.fail("did not throw error");
            } catch (LuaError le) {
                // Expected
            }

            LuaBoolean.s_metatable = LuaValue.tableOf(new LuaValue[] { META_SUB, RETURN_RHS, });
            Assert.assertEquals(fal, tru.sub(fal));
            Assert.assertEquals(tbl, tru.sub(tbl));
            Assert.assertEquals(tru, tbl.sub(tru));
            try {
                tbl.sub(tbl2);
                Assert.fail("did not throw error");
            } catch (LuaError le) {
                // Expected
            }
            try {
                tru.add(tbl);
                Assert.fail("did not throw error");
            } catch (LuaError le) {
                // Expected
            }

            LuaBoolean.s_metatable = LuaValue.tableOf(new LuaValue[] { META_MUL, RETURN_RHS, });
            Assert.assertEquals(fal, tru.mul(fal));
            Assert.assertEquals(tbl, tru.mul(tbl));
            Assert.assertEquals(tru, tbl.mul(tru));
            try {
                tbl.mul(tbl2);
                Assert.fail("did not throw error");
            } catch (LuaError le) {
                // Expected
            }
            try {
                tru.sub(tbl);
                Assert.fail("did not throw error");
            } catch (LuaError le) {
                // Expected
            }

            LuaBoolean.s_metatable = LuaValue.tableOf(new LuaValue[] { META_DIV, RETURN_RHS, });
            Assert.assertEquals(fal, tru.div(fal));
            Assert.assertEquals(tbl, tru.div(tbl));
            Assert.assertEquals(tru, tbl.div(tru));
            try {
                tbl.div(tbl2);
                Assert.fail("did not throw error");
            } catch (LuaError le) {
                // Expected
            }
            try {
                tru.sub(tbl);
                Assert.fail("did not throw error");
            } catch (LuaError le) {
                // Expected
            }

            LuaBoolean.s_metatable = LuaValue.tableOf(new LuaValue[] { META_POW, RETURN_RHS, });
            Assert.assertEquals(fal, tru.pow(fal));
            Assert.assertEquals(tbl, tru.pow(tbl));
            Assert.assertEquals(tru, tbl.pow(tru));
            try {
                tbl.pow(tbl2);
                Assert.fail("did not throw error");
            } catch (LuaError le) {
                // Expected
            }
            try {
                tru.sub(tbl);
                Assert.fail("did not throw error");
            } catch (LuaError le) {
                // Expected
            }

            LuaBoolean.s_metatable = LuaValue.tableOf(new LuaValue[] { META_MOD, RETURN_RHS, });
            Assert.assertEquals(fal, tru.mod(fal));
            Assert.assertEquals(tbl, tru.mod(tbl));
            Assert.assertEquals(tru, tbl.mod(tru));
            try {
                tbl.mod(tbl2);
                Assert.fail("did not throw error");
            } catch (LuaError le) {
                // Expected
            }
            try {
                tru.sub(tbl);
                Assert.fail("did not throw error");
            } catch (LuaError le) {
                // Expected
            }
        } finally {
            LuaBoolean.s_metatable = null;
        }
    }

    @Test
    public void testArithMetatagNumberTable() {
        final LuaValue zero = LuaInteger.valueOf(0);
        final LuaValue one = LuaInteger.valueOf(1);
        final LuaValue tbl = new LuaTable();

        try {
            tbl.add(zero);
            Assert.fail("did not throw error");
        } catch (LuaError le) {
            // Expected
        }
        try {
            zero.add(tbl);
            Assert.fail("did not throw error");
        } catch (LuaError le) {
            // Expected
        }
        tbl.setmetatable(LuaValue.tableOf(new LuaValue[] { META_ADD, RETURN_ONE, }));
        Assert.assertEquals(one, tbl.add(zero));
        Assert.assertEquals(one, zero.add(tbl));

        try {
            tbl.sub(zero);
            Assert.fail("did not throw error");
        } catch (LuaError le) {
            // Expected
        }
        try {
            zero.sub(tbl);
            Assert.fail("did not throw error");
        } catch (LuaError le) {
            // Expected
        }
        tbl.setmetatable(LuaValue.tableOf(new LuaValue[] { META_SUB, RETURN_ONE, }));
        Assert.assertEquals(one, tbl.sub(zero));
        Assert.assertEquals(one, zero.sub(tbl));

        try {
            tbl.mul(zero);
            Assert.fail("did not throw error");
        } catch (LuaError le) {
            // Expected
        }
        try {
            zero.mul(tbl);
            Assert.fail("did not throw error");
        } catch (LuaError le) {
            // Expected
        }
        tbl.setmetatable(LuaValue.tableOf(new LuaValue[] { META_MUL, RETURN_ONE, }));
        Assert.assertEquals(one, tbl.mul(zero));
        Assert.assertEquals(one, zero.mul(tbl));

        try {
            tbl.div(zero);
            Assert.fail("did not throw error");
        } catch (LuaError le) {
            // Expected
        }
        try {
            zero.div(tbl);
            Assert.fail("did not throw error");
        } catch (LuaError le) {
            // Expected
        }
        tbl.setmetatable(LuaValue.tableOf(new LuaValue[] { META_DIV, RETURN_ONE, }));
        Assert.assertEquals(one, tbl.div(zero));
        Assert.assertEquals(one, zero.div(tbl));

        try {
            tbl.pow(zero);
            Assert.fail("did not throw error");
        } catch (LuaError le) {
            // Expected
        }
        try {
            zero.pow(tbl);
            Assert.fail("did not throw error");
        } catch (LuaError le) {
            // Expected
        }
        tbl.setmetatable(LuaValue.tableOf(new LuaValue[] { META_POW, RETURN_ONE, }));
        Assert.assertEquals(one, tbl.pow(zero));
        Assert.assertEquals(one, zero.pow(tbl));

        try {
            tbl.mod(zero);
            Assert.fail("did not throw error");
        } catch (LuaError le) {
            // Expected
        }
        try {
            zero.mod(tbl);
            Assert.fail("did not throw error");
        } catch (LuaError le) {
            // Expected
        }
        tbl.setmetatable(LuaValue.tableOf(new LuaValue[] { META_MOD, RETURN_ONE, }));
        Assert.assertEquals(one, tbl.mod(zero));
        Assert.assertEquals(one, zero.mod(tbl));
    }

    @Test
    public void testCompareStrings() {
        // these are lexical compare!
        LuaValue sa = LuaValue.valueOf("-1.5");
        LuaValue sb = LuaValue.valueOf("-2.0");
        LuaValue sc = LuaValue.valueOf("1.5");
        LuaValue sd = LuaValue.valueOf("2.0");

        Assert.assertEquals(FALSE, sa.lt(sa));
        Assert.assertEquals(TRUE, sa.lt(sb));
        Assert.assertEquals(TRUE, sa.lt(sc));
        Assert.assertEquals(TRUE, sa.lt(sd));
        Assert.assertEquals(FALSE, sb.lt(sa));
        Assert.assertEquals(FALSE, sb.lt(sb));
        Assert.assertEquals(TRUE, sb.lt(sc));
        Assert.assertEquals(TRUE, sb.lt(sd));
        Assert.assertEquals(FALSE, sc.lt(sa));
        Assert.assertEquals(FALSE, sc.lt(sb));
        Assert.assertEquals(FALSE, sc.lt(sc));
        Assert.assertEquals(TRUE, sc.lt(sd));
        Assert.assertEquals(FALSE, sd.lt(sa));
        Assert.assertEquals(FALSE, sd.lt(sb));
        Assert.assertEquals(FALSE, sd.lt(sc));
        Assert.assertEquals(FALSE, sd.lt(sd));
    }

    @Test
    public void testLt() {
        LuaValue ia = LuaValue.valueOf(3);
        LuaValue ib = LuaValue.valueOf(4);
        LuaValue da = LuaValue.valueOf(.25);
        LuaValue db = LuaValue.valueOf(.5);

        // like kinds
        Assert.assertEquals(3. < 4., ia.lt(ib).toboolean());
        Assert.assertEquals(.25 < .5, da.lt(db).toboolean());
        Assert.assertEquals(3. < 4., ia.lt_b(ib));
        Assert.assertEquals(.25 < .5, da.lt_b(db));

        // unlike kinds
        Assert.assertEquals(3. < .25, ia.lt(da).toboolean());
        Assert.assertEquals(.25 < 3., da.lt(ia).toboolean());
        Assert.assertEquals(3. < .25, ia.lt_b(da));
        Assert.assertEquals(.25 < 3., da.lt_b(ia));
    }

    @Test
    public void testLtEq() {
        LuaValue ia = LuaValue.valueOf(3);
        LuaValue ib = LuaValue.valueOf(4);
        LuaValue da = LuaValue.valueOf(.25);
        LuaValue db = LuaValue.valueOf(.5);

        // like kinds
        Assert.assertEquals(3. <= 4., ia.lteq(ib).toboolean());
        Assert.assertEquals(.25 <= .5, da.lteq(db).toboolean());
        Assert.assertEquals(3. <= 4., ia.lteq_b(ib));
        Assert.assertEquals(.25 <= .5, da.lteq_b(db));

        // unlike kinds
        Assert.assertEquals(3. <= .25, ia.lteq(da).toboolean());
        Assert.assertEquals(.25 <= 3., da.lteq(ia).toboolean());
        Assert.assertEquals(3. <= .25, ia.lteq_b(da));
        Assert.assertEquals(.25 <= 3., da.lteq_b(ia));
    }

    @Test
    public void testGt() {
        LuaValue ia = LuaValue.valueOf(3);
        LuaValue ib = LuaValue.valueOf(4);
        LuaValue da = LuaValue.valueOf(.25);
        LuaValue db = LuaValue.valueOf(.5);

        // like kinds
        Assert.assertEquals(3. > 4., ia.gt(ib).toboolean());
        Assert.assertEquals(.25 > .5, da.gt(db).toboolean());
        Assert.assertEquals(3. > 4., ia.gt_b(ib));
        Assert.assertEquals(.25 > .5, da.gt_b(db));

        // unlike kinds
        Assert.assertEquals(3. > .25, ia.gt(da).toboolean());
        Assert.assertEquals(.25 > 3., da.gt(ia).toboolean());
        Assert.assertEquals(3. > .25, ia.gt_b(da));
        Assert.assertEquals(.25 > 3., da.gt_b(ia));
    }

    @Test
    public void testGtEq() {
        LuaValue ia = LuaValue.valueOf(3);
        LuaValue ib = LuaValue.valueOf(4);
        LuaValue da = LuaValue.valueOf(.25);
        LuaValue db = LuaValue.valueOf(.5);

        // like kinds
        Assert.assertEquals(3. >= 4., ia.gteq(ib).toboolean());
        Assert.assertEquals(.25 >= .5, da.gteq(db).toboolean());
        Assert.assertEquals(3. >= 4., ia.gteq_b(ib));
        Assert.assertEquals(.25 >= .5, da.gteq_b(db));

        // unlike kinds
        Assert.assertEquals(3. >= .25, ia.gteq(da).toboolean());
        Assert.assertEquals(.25 >= 3., da.gteq(ia).toboolean());
        Assert.assertEquals(3. >= .25, ia.gteq_b(da));
        Assert.assertEquals(.25 >= 3., da.gteq_b(ia));
    }

    @Test
    public void testNotEq() {
        LuaValue ia = LuaValue.valueOf(3);
        LuaValue ib = LuaValue.valueOf(4);
        LuaValue da = LuaValue.valueOf(.25);
        LuaValue db = LuaValue.valueOf(.5);
        LuaValue sa = LuaValue.valueOf("1.5");
        LuaValue sb = LuaValue.valueOf("2.0");

        // like kinds
        Assert.assertEquals(3. != 4., ia.neq(ib).toboolean());
        Assert.assertEquals(.25 != .5, da.neq(db).toboolean());
        Assert.assertEquals(1.5 != 2., sa.neq(sb).toboolean());
        Assert.assertEquals(3. != 4., ia.neq_b(ib));
        Assert.assertEquals(.25 != .5, da.neq_b(db));
        Assert.assertEquals(1.5 != 2., sa.neq_b(sb));

        // unlike kinds
        Assert.assertEquals(3. != .25, ia.neq(da).toboolean());
        Assert.assertEquals(.25 != 3., da.neq(ia).toboolean());
        Assert.assertEquals(3. != 1.5, ia.neq(sa).toboolean());
        Assert.assertEquals(1.5 != 3., sa.neq(ia).toboolean());
        Assert.assertEquals(.25 != 1.5, da.neq(sa).toboolean());
        Assert.assertEquals(1.5 != .25, sa.neq(da).toboolean());
        Assert.assertEquals(3. != .25, ia.neq_b(da));
        Assert.assertEquals(.25 != 3., da.neq_b(ia));
        Assert.assertEquals(3. != 1.5, ia.neq_b(sa));
        Assert.assertEquals(1.5 != 3., sa.neq_b(ia));
        Assert.assertEquals(.25 != 1.5, da.neq_b(sa));
        Assert.assertEquals(1.5 != .25, sa.neq_b(da));
    }

    @Test
    public void testCompareErrors() {
        String[] ops = { "lt", "lteq", };
        LuaValue[] vals = { NIL, TRUE, LuaValue.tableOf() };
        LuaValue[] numerics = { LuaValue.valueOf(111), LuaValue.valueOf(55.25), LuaValue.valueOf("22.125") };
        for (int i = 0; i < ops.length; i++) {
            for (int j = 0; j < vals.length; j++) {
                for (int k = 0; k < numerics.length; k++) {
                    checkCompareError(vals[j], numerics[k], ops[i], vals[j].typename());
                    checkCompareError(numerics[k], vals[j], ops[i], vals[j].typename());
                }
            }
        }
    }

    private void checkCompareError(LuaValue a, LuaValue b, String op, String type) {
        try {
            LuaValue.class.getMethod(op, new Class[] { LuaValue.class }).invoke(a, new Object[] { b });
        } catch (InvocationTargetException ite) {
            String actual = ite.getTargetException().getMessage();
            if ((!actual.startsWith("attempt to compare")) || actual.indexOf(type) < 0) {
                Assert.fail(
                        "(" + a.typename() + "," + op + "," + b.typename() + ") reported '" + actual + "'");
            }
        } catch (Exception e) {
            Assert.fail("(" + a.typename() + "," + op + "," + b.typename() + ") threw " + e);
        }
    }

    @Test
    public void testCompareMetatag() {
        LuaValue tru = TRUE;
        LuaValue fal = FALSE;
        LuaValue tbl = new LuaTable();
        LuaValue tbl2 = new LuaTable();
        LuaValue tbl3 = new LuaTable();
        try {
            // always use left argument
            LuaValue mt = LuaValue.tableOf(new LuaValue[] { META_LT, RETURN_LHS, META_LE, RETURN_RHS, });
            LuaBoolean.s_metatable = mt;
            tbl.setmetatable(mt);
            tbl2.setmetatable(mt);
            Assert.assertEquals(tru, tru.lt(fal));
            Assert.assertEquals(fal, fal.lt(tru));
            Assert.assertEquals(tbl, tbl.lt(tbl2));
            Assert.assertEquals(tbl2, tbl2.lt(tbl));
            Assert.assertEquals(tbl, tbl.lt(tbl3));
            Assert.assertEquals(tbl3, tbl3.lt(tbl));
            Assert.assertEquals(fal, tru.lteq(fal));
            Assert.assertEquals(tru, fal.lteq(tru));
            Assert.assertEquals(tbl2, tbl.lteq(tbl2));
            Assert.assertEquals(tbl, tbl2.lteq(tbl));
            Assert.assertEquals(tbl3, tbl.lteq(tbl3));
            Assert.assertEquals(tbl, tbl3.lteq(tbl));

            // always use right argument
            mt = LuaValue.tableOf(new LuaValue[] { META_LT, RETURN_RHS, META_LE, RETURN_LHS });
            LuaBoolean.s_metatable = mt;
            tbl.setmetatable(mt);
            tbl2.setmetatable(mt);
            Assert.assertEquals(fal, tru.lt(fal));
            Assert.assertEquals(tru, fal.lt(tru));
            Assert.assertEquals(tbl2, tbl.lt(tbl2));
            Assert.assertEquals(tbl, tbl2.lt(tbl));
            Assert.assertEquals(tbl3, tbl.lt(tbl3));
            Assert.assertEquals(tbl, tbl3.lt(tbl));
            Assert.assertEquals(tru, tru.lteq(fal));
            Assert.assertEquals(fal, fal.lteq(tru));
            Assert.assertEquals(tbl, tbl.lteq(tbl2));
            Assert.assertEquals(tbl2, tbl2.lteq(tbl));
            Assert.assertEquals(tbl, tbl.lteq(tbl3));
            Assert.assertEquals(tbl3, tbl3.lteq(tbl));

        } finally {
            LuaBoolean.s_metatable = null;
        }
    }

    @Test
    public void testAnd() {
        LuaValue ia = LuaValue.valueOf(3);
        LuaValue ib = LuaValue.valueOf(4);
        LuaValue da = LuaValue.valueOf(.25);
        LuaValue db = LuaValue.valueOf(.5);
        LuaValue sa = LuaValue.valueOf("1.5");
        LuaValue sb = LuaValue.valueOf("2.0");
        LuaValue ba = TRUE;
        LuaValue bb = FALSE;

        // like kinds
        Assert.assertSame(ib, ia.and(ib));
        Assert.assertSame(db, da.and(db));
        Assert.assertSame(sb, sa.and(sb));

        // unlike kinds
        Assert.assertSame(da, ia.and(da));
        Assert.assertSame(ia, da.and(ia));
        Assert.assertSame(sa, ia.and(sa));
        Assert.assertSame(ia, sa.and(ia));
        Assert.assertSame(sa, da.and(sa));
        Assert.assertSame(da, sa.and(da));

        // boolean values
        Assert.assertSame(bb, ba.and(bb));
        Assert.assertSame(bb, bb.and(ba));
        Assert.assertSame(ia, ba.and(ia));
        Assert.assertSame(bb, bb.and(ia));
    }

    @Test
    public void testOr() {
        LuaValue ia = LuaValue.valueOf(3);
        LuaValue ib = LuaValue.valueOf(4);
        LuaValue da = LuaValue.valueOf(.25);
        LuaValue db = LuaValue.valueOf(.5);
        LuaValue sa = LuaValue.valueOf("1.5");
        LuaValue sb = LuaValue.valueOf("2.0");
        LuaValue ba = TRUE;
        LuaValue bb = FALSE;

        // like kinds
        Assert.assertSame(ia, ia.or(ib));
        Assert.assertSame(da, da.or(db));
        Assert.assertSame(sa, sa.or(sb));

        // unlike kinds
        Assert.assertSame(ia, ia.or(da));
        Assert.assertSame(da, da.or(ia));
        Assert.assertSame(ia, ia.or(sa));
        Assert.assertSame(sa, sa.or(ia));
        Assert.assertSame(da, da.or(sa));
        Assert.assertSame(sa, sa.or(da));

        // boolean values
        Assert.assertSame(ba, ba.or(bb));
        Assert.assertSame(ba, bb.or(ba));
        Assert.assertSame(ba, ba.or(ia));
        Assert.assertSame(ia, bb.or(ia));
    }

    @Test
    public void testLexicalComparison() {
        final LuaValue aaa = LuaValue.valueOf("aaa");
        final LuaValue baa = LuaValue.valueOf("baa");
        final LuaValue Aaa = LuaValue.valueOf("Aaa");
        final LuaValue aba = LuaValue.valueOf("aba");
        final LuaValue aaaa = LuaValue.valueOf("aaaa");
        final LuaValue t = TRUE;
        final LuaValue f = FALSE;

        // basics
        Assert.assertEquals(t, aaa.eq(aaa));
        Assert.assertEquals(t, aaa.lt(baa));
        Assert.assertEquals(t, aaa.lteq(baa));
        Assert.assertEquals(f, aaa.gt(baa));
        Assert.assertEquals(f, aaa.gteq(baa));
        Assert.assertEquals(f, baa.lt(aaa));
        Assert.assertEquals(f, baa.lteq(aaa));
        Assert.assertEquals(t, baa.gt(aaa));
        Assert.assertEquals(t, baa.gteq(aaa));
        Assert.assertEquals(t, aaa.lteq(aaa));
        Assert.assertEquals(t, aaa.gteq(aaa));

        // different case
        Assert.assertEquals(t, Aaa.eq(Aaa));
        Assert.assertEquals(t, Aaa.lt(aaa));
        Assert.assertEquals(t, Aaa.lteq(aaa));
        Assert.assertEquals(f, Aaa.gt(aaa));
        Assert.assertEquals(f, Aaa.gteq(aaa));
        Assert.assertEquals(f, aaa.lt(Aaa));
        Assert.assertEquals(f, aaa.lteq(Aaa));
        Assert.assertEquals(t, aaa.gt(Aaa));
        Assert.assertEquals(t, aaa.gteq(Aaa));
        Assert.assertEquals(t, Aaa.lteq(Aaa));
        Assert.assertEquals(t, Aaa.gteq(Aaa));

        // second letter differs
        Assert.assertEquals(t, aaa.eq(aaa));
        Assert.assertEquals(t, aaa.lt(aba));
        Assert.assertEquals(t, aaa.lteq(aba));
        Assert.assertEquals(f, aaa.gt(aba));
        Assert.assertEquals(f, aaa.gteq(aba));
        Assert.assertEquals(f, aba.lt(aaa));
        Assert.assertEquals(f, aba.lteq(aaa));
        Assert.assertEquals(t, aba.gt(aaa));
        Assert.assertEquals(t, aba.gteq(aaa));
        Assert.assertEquals(t, aaa.lteq(aaa));
        Assert.assertEquals(t, aaa.gteq(aaa));

        // longer
        Assert.assertEquals(t, aaa.eq(aaa));
        Assert.assertEquals(t, aaa.lt(aaaa));
        Assert.assertEquals(t, aaa.lteq(aaaa));
        Assert.assertEquals(f, aaa.gt(aaaa));
        Assert.assertEquals(f, aaa.gteq(aaaa));
        Assert.assertEquals(f, aaaa.lt(aaa));
        Assert.assertEquals(f, aaaa.lteq(aaa));
        Assert.assertEquals(t, aaaa.gt(aaa));
        Assert.assertEquals(t, aaaa.gteq(aaa));
        Assert.assertEquals(t, aaa.lteq(aaa));
        Assert.assertEquals(t, aaa.gteq(aaa));
    }

    @Test
    public void testBuffer() {
        final LuaValue abc = LuaValue.valueOf("abcdefghi").substring(0, 3);
        final LuaValue def = LuaValue.valueOf("abcdefghi").substring(3, 6);
        final LuaValue ghi = LuaValue.valueOf("abcdefghi").substring(6, 9);
        final LuaValue n123 = LuaValue.valueOf(123);

        // basic append
        Buffer b = new Buffer();
        Assert.assertEquals("", b.value().tojstring());
        b.append(def);
        Assert.assertEquals("def", b.value().tojstring());
        b.append(abc);
        Assert.assertEquals("defabc", b.value().tojstring());
        b.append(ghi);
        Assert.assertEquals("defabcghi", b.value().tojstring());
        b.append(n123);
        Assert.assertEquals("defabcghi123", b.value().tojstring());

        // basic prepend
        b = new Buffer();
        Assert.assertEquals("", b.value().tojstring());
        b.prepend(def.strvalue());
        Assert.assertEquals("def", b.value().tojstring());
        b.prepend(ghi.strvalue());
        Assert.assertEquals("ghidef", b.value().tojstring());
        b.prepend(abc.strvalue());
        Assert.assertEquals("abcghidef", b.value().tojstring());
        b.prepend(n123.strvalue());
        Assert.assertEquals("123abcghidef", b.value().tojstring());

        // mixed append, prepend
        b = new Buffer();
        Assert.assertEquals("", b.value().tojstring());
        b.append(def);
        Assert.assertEquals("def", b.value().tojstring());
        b.append(abc);
        Assert.assertEquals("defabc", b.value().tojstring());
        b.prepend(ghi.strvalue());
        Assert.assertEquals("ghidefabc", b.value().tojstring());
        b.prepend(n123.strvalue());
        Assert.assertEquals("123ghidefabc", b.value().tojstring());
        b.append(def);
        Assert.assertEquals("123ghidefabcdef", b.value().tojstring());
        b.append(abc);
        Assert.assertEquals("123ghidefabcdefabc", b.value().tojstring());
        b.prepend(ghi.strvalue());
        Assert.assertEquals("ghi123ghidefabcdefabc", b.value().tojstring());
        b.prepend(n123.strvalue());
        Assert.assertEquals("123ghi123ghidefabcdefabc", b.value().tojstring());

        // value
        b = new Buffer(def);
        Assert.assertEquals("def", b.value().tojstring());
        b.append(abc);
        Assert.assertEquals("defabc", b.value().tojstring());
        b.prepend(ghi.strvalue());
        Assert.assertEquals("ghidefabc", b.value().tojstring());
        b.setvalue(def);
        Assert.assertEquals("def", b.value().tojstring());
        b.prepend(ghi.strvalue());
        Assert.assertEquals("ghidef", b.value().tojstring());
        b.append(abc);
        Assert.assertEquals("ghidefabc", b.value().tojstring());
    }

    @Test
    public void testConcat() {
        LuaValue abc = LuaValue.valueOf("abcdefghi").substring(0, 3);
        LuaValue def = LuaValue.valueOf("abcdefghi").substring(3, 6);
        LuaValue ghi = LuaValue.valueOf("abcdefghi").substring(6, 9);
        LuaValue n123 = LuaValue.valueOf(123);

        Assert.assertEquals("abc", abc.tojstring());
        Assert.assertEquals("def", def.tojstring());
        Assert.assertEquals("ghi", ghi.tojstring());
        Assert.assertEquals("123", n123.tojstring());
        Assert.assertEquals("abcabc", abc.concat(abc).tojstring());
        Assert.assertEquals("defghi", def.concat(ghi).tojstring());
        Assert.assertEquals("ghidef", ghi.concat(def).tojstring());
        Assert.assertEquals("ghidefabcghi", ghi.concat(def).concat(abc).concat(ghi).tojstring());
        Assert.assertEquals("123def", n123.concat(def).tojstring());
        Assert.assertEquals("def123", def.concat(n123).tojstring());
    }

    @Test
    public void testConcatBuffer() {
        final LuaValue abc = LuaValue.valueOf("abcdefghi").substring(0, 3);
        final LuaValue def = LuaValue.valueOf("abcdefghi").substring(3, 6);
        final LuaValue ghi = LuaValue.valueOf("abcdefghi").substring(6, 9);
        final LuaValue n123 = LuaValue.valueOf(123);

        Buffer b;

        b = new Buffer(def);
        Assert.assertEquals("def", b.value().tojstring());
        b = ghi.concat(b);
        Assert.assertEquals("ghidef", b.value().tojstring());
        b = abc.concat(b);
        Assert.assertEquals("abcghidef", b.value().tojstring());
        b = n123.concat(b);
        Assert.assertEquals("123abcghidef", b.value().tojstring());
        b.setvalue(n123);
        b = def.concat(b);
        Assert.assertEquals("def123", b.value().tojstring());
        b = abc.concat(b);
        Assert.assertEquals("abcdef123", b.value().tojstring());
    }

    @Test
    public void testConcatMetatag() {
        LuaValue def = LuaValue.valueOf("abcdefghi").substring(3, 6);
        LuaValue ghi = LuaValue.valueOf("abcdefghi").substring(6, 9);
        LuaValue tru = TRUE;
        LuaValue fal = FALSE;
        LuaValue tbl = new LuaTable();
        LuaValue uda = new LuaUserdata(new Object());
        try {
            // always use left argument
            LuaBoolean.s_metatable = LuaValue.tableOf(new LuaValue[] { META_CONCAT, RETURN_LHS });
            Assert.assertEquals(tru, tru.concat(tbl));
            Assert.assertEquals(tbl, tbl.concat(tru));
            Assert.assertEquals(tru, tru.concat(tbl));
            Assert.assertEquals(tbl, tbl.concat(tru));
            Assert.assertEquals(tru, tru.concat(tbl.buffer()).value());
            Assert.assertEquals(tbl, tbl.concat(tru.buffer()).value());
            Assert.assertEquals(fal, fal.concat(tbl.concat(tru.buffer())).value());
            Assert.assertEquals(uda, uda.concat(tru.concat(tbl.buffer())).value());
            try {
                tbl.concat(def);
                Assert.fail("did not throw error");
            } catch (LuaError le) {
                // Expected
            }
            try {
                def.concat(tbl);
                Assert.fail("did not throw error");
            } catch (LuaError le) {
                // Expected
            }
            try {
                tbl.concat(def.buffer()).value();
                Assert.fail("did not throw error");
            } catch (LuaError le) {
                // Expected
            }
            try {
                def.concat(tbl.buffer()).value();
                Assert.fail("did not throw error");
            } catch (LuaError le) {
                // Expected
            }
            try {
                uda.concat(def.concat(tbl.buffer())).value();
                Assert.fail("did not throw error");
            } catch (LuaError le) {
                // Expected
            }
            try {
                ghi.concat(tbl.concat(def.buffer())).value();
                Assert.fail("did not throw error");
            } catch (LuaError le) {
                // Expected
            }

            // always use right argument
            LuaBoolean.s_metatable = LuaValue.tableOf(new LuaValue[] { META_CONCAT, RETURN_RHS });
            Assert.assertEquals(tbl, tru.concat(tbl));
            Assert.assertEquals(tru, tbl.concat(tru));
            Assert.assertEquals(tbl, tru.concat(tbl.buffer()).value());
            Assert.assertEquals(tru, tbl.concat(tru.buffer()).value());
            Assert.assertEquals(tru, uda.concat(tbl.concat(tru.buffer())).value());
            Assert.assertEquals(tbl, fal.concat(tru.concat(tbl.buffer())).value());
            try {
                tbl.concat(def);
                Assert.fail("did not throw error");
            } catch (LuaError le) {
                // Expected
            }
            try {
                def.concat(tbl);
                Assert.fail("did not throw error");
            } catch (LuaError le) {
                // Expected
            }
            try {
                tbl.concat(def.buffer()).value();
                Assert.fail("did not throw error");
            } catch (LuaError le) {
                // Expected
            }
            try {
                def.concat(tbl.buffer()).value();
                Assert.fail("did not throw error");
            } catch (LuaError le) {
                // Expected
            }
            try {
                uda.concat(def.concat(tbl.buffer())).value();
                Assert.fail("did not throw error");
            } catch (LuaError le) {
                // Expected
            }
            try {
                uda.concat(tbl.concat(def.buffer())).value();
                Assert.fail("did not throw error");
            } catch (LuaError le) {
                // Expected
            }
        } finally {
            LuaBoolean.s_metatable = null;
        }
    }

    @Test
    public void testConcatErrors() {
        String[] ops = { "concat" };
        LuaValue[] vals = { NIL, TRUE, LuaValue.tableOf() };
        LuaValue[] numerics = { LuaValue.valueOf(111), LuaValue.valueOf(55.25), LuaValue.valueOf("22.125") };
        for (int i = 0; i < ops.length; i++) {
            for (int j = 0; j < vals.length; j++) {
                for (int k = 0; k < numerics.length; k++) {
                    checkConcatError(vals[j], numerics[k], ops[i], vals[j].typename());
                    checkConcatError(numerics[k], vals[j], ops[i], vals[j].typename());
                }
            }
        }
    }

    private void checkConcatError(LuaValue a, LuaValue b, String op, String type) {
        try {
            LuaValue.class.getMethod(op, new Class[] { LuaValue.class }).invoke(a, new Object[] { b });
        } catch (InvocationTargetException ite) {
            String actual = ite.getTargetException().getMessage();
            if ((!actual.startsWith("attempt to concatenate")) || actual.indexOf(type) < 0) {
                Assert.fail(
                        "(" + a.typename() + "," + op + "," + b.typename() + ") reported '" + actual + "'");
            }
        } catch (Exception e) {
            Assert.fail("(" + a.typename() + "," + op + "," + b.typename() + ") threw " + e);
        }
    }

}
