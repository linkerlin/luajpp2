/*******************************************************************************
 * Copyright (c) 2012 Luaj.org. All rights reserved.
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

import static nl.weeaboo.lua2.vm.LuaConstants.NONE;
import static nl.weeaboo.lua2.vm.LuaNil.NIL;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests of basic unary and binary operators on main value types.
 */
public class VarargsTest {

    static LuaValue A = LuaValue.valueOf("a");
    static LuaValue B = LuaValue.valueOf("b");
    static LuaValue C = LuaValue.valueOf("c");
    static LuaValue D = LuaValue.valueOf("d");
    static LuaValue E = LuaValue.valueOf("e");
    static LuaValue F = LuaValue.valueOf("f");
    static LuaValue G = LuaValue.valueOf("g");
    static LuaValue H = LuaValue.valueOf("h");
    static LuaValue Z = LuaValue.valueOf("z");
    static Varargs A_G = LuaValue.varargsOf(new LuaValue[] { A, B, C, D, E, F, G });
    static Varargs B_E = LuaValue.varargsOf(new LuaValue[] { B, C, D, E });
    static Varargs C_G = LuaValue.varargsOf(new LuaValue[] { C, D, E, F, G });
    static Varargs C_E = LuaValue.varargsOf(new LuaValue[] { C, D, E });
    static Varargs DE = LuaValue.varargsOf(new LuaValue[] { D, E });
    static Varargs E_G = LuaValue.varargsOf(new LuaValue[] { E, F, G });
    static Varargs FG = LuaValue.varargsOf(new LuaValue[] { F, G });
    static LuaValue[] Z_H_array = {Z, A, B, C, D, E, F, G, H };
    static Varargs A_G_alt = new ArrayPartVarargs(Z_H_array, 1, 7);
    static Varargs B_E_alt = new ArrayPartVarargs(Z_H_array, 2, 4);
    static Varargs C_G_alt = new ArrayPartVarargs(Z_H_array, 3, 5);
    static Varargs C_E_alt = new ArrayPartVarargs(Z_H_array, 3, 3);
    static Varargs C_E_alt2 = LuaValue.varargsOf(C, D, E);
    static Varargs DE_alt = new PairVarargs(D,E);
    static Varargs DE_alt2 = LuaValue.varargsOf(D,E);
    static Varargs E_G_alt = new ArrayPartVarargs(Z_H_array, 5, 3);
    static Varargs FG_alt = new PairVarargs(F, G);

    static void expectEquals(Varargs x, Varargs y) {
        Assert.assertEquals(x.narg(), y.narg());
        Assert.assertEquals(x.arg1(), y.arg1());
        Assert.assertEquals(x.arg(0), y.arg(0));
        Assert.assertEquals(x.arg(-1), y.arg(-1));
        Assert.assertEquals(x.arg(2), y.arg(2));
        Assert.assertEquals(x.arg(3), y.arg(3));
        for (int i = 4; i < x.narg() + 2; ++i) {
            Assert.assertEquals(x.arg(i), y.arg(i));
        }
    }

    @Test
    public void testSanity() {
        expectEquals(A_G, A_G);
        expectEquals(A_G_alt, A_G_alt);
        expectEquals(A_G, A_G_alt);
        expectEquals(B_E, B_E_alt);
        expectEquals(C_G, C_G_alt);
        expectEquals(C_E, C_E_alt);
        expectEquals(C_E, C_E_alt2);
        expectEquals(DE, DE_alt);
        expectEquals(DE, DE_alt2);
        expectEquals(E_G, E_G_alt);
        expectEquals(FG, FG_alt);
        expectEquals(FG_alt, FG_alt);
        expectEquals(A, A);
        expectEquals(NONE, NONE);
        expectEquals(NIL, NIL);
    }

    static void standardTestsA_G(Varargs ag) {
        expectEquals(A_G, ag);
        expectEquals(A_G, ag.subargs(1));
        expectEquals(C_G, ag.subargs(3).subargs(1));
        expectEquals(E_G, ag.subargs(5));
        expectEquals(E_G, ag.subargs(5).subargs(1));
        expectEquals(FG, ag.subargs(6));
        expectEquals(FG, ag.subargs(6).subargs(1));
        expectEquals(G, ag.subargs(7));
        expectEquals(G, ag.subargs(7).subargs(1));
        expectEquals(NONE, ag.subargs(8));
        expectEquals(NONE, ag.subargs(8).subargs(1));
        standardTestsC_G(A_G.subargs(3));
    }

    static void standardTestsC_G(Varargs cg) {
        expectEquals(C_G, cg.subargs(1));
        expectEquals(E_G, cg.subargs(3));
        expectEquals(E_G, cg.subargs(3).subargs(1));
        expectEquals(FG, cg.subargs(4));
        expectEquals(FG, cg.subargs(4).subargs(1));
        expectEquals(G, cg.subargs(5));
        expectEquals(G, cg.subargs(5).subargs(1));
        expectEquals(NONE, cg.subargs(6));
        expectEquals(NONE, cg.subargs(6).subargs(1));
        standardTestsE_G(cg.subargs(3));
    }

    static void standardTestsE_G(Varargs eg) {
        expectEquals(E_G, eg.subargs(1));
        expectEquals(FG, eg.subargs(2));
        expectEquals(FG, eg.subargs(2).subargs(1));
        expectEquals(G, eg.subargs(3));
        expectEquals(G, eg.subargs(3).subargs(1));
        expectEquals(NONE, eg.subargs(4));
        expectEquals(NONE, eg.subargs(4).subargs(1));
        standardTestsFG(eg.subargs(2));
    }

    static void standardTestsFG(Varargs fg) {
        expectEquals(FG, fg.subargs(1));
        expectEquals(G, fg.subargs(2));
        expectEquals(G, fg.subargs(2).subargs(1));
        expectEquals(NONE, fg.subargs(3));
        expectEquals(NONE, fg.subargs(3).subargs(1));
    }

    static void standardTestsNone(Varargs none) {
        expectEquals(NONE, none.subargs(1));
        expectEquals(NONE, none.subargs(2));
    }

    @Test
    public void testVarargsSubargs() {
        standardTestsA_G(A_G);
        standardTestsA_G(A_G_alt);
        standardTestsC_G(C_G);
        standardTestsC_G(C_G_alt);
        standardTestsE_G(E_G);
        standardTestsE_G(E_G_alt);
        standardTestsFG(FG);
        standardTestsFG(FG_alt);
        standardTestsNone(NONE);
    }

    @Test
    public void testVarargsMore() {
        Varargs ag;
        ag = LuaValue.varargsOf(new LuaValue[] { A, }, LuaValue.varargsOf( new LuaValue[] { B, C, D, E, F, G }));
        standardTestsA_G(ag);
        ag = LuaValue.varargsOf(new LuaValue[] { A, B, }, LuaValue.varargsOf( new LuaValue[] { C, D, E, F, G }));
        standardTestsA_G(ag);
        ag = LuaValue.varargsOf(new LuaValue[] { A, B, C, }, LuaValue.varargsOf( new LuaValue[] { D, E, F, G }));
        standardTestsA_G(ag);
        ag = LuaValue.varargsOf(new LuaValue[] { A, B, C, D, }, LuaValue.varargsOf(E, F, G));
        standardTestsA_G(ag);
        ag = LuaValue.varargsOf(new LuaValue[] { A, B, C, D, E }, LuaValue.varargsOf( F, G ));
        standardTestsA_G(ag);
        ag = LuaValue.varargsOf(new LuaValue[] { A, B, C, D, E, F, }, G );
        standardTestsA_G(ag);
    }

    @Test
    public void testPairVarargsMore() {
        Varargs ag = new PairVarargs(A,
                    new PairVarargs(B,
                    new PairVarargs(C,
                    new PairVarargs(D,
                    new PairVarargs(E,
                    new PairVarargs(F, G))))));
        standardTestsA_G(ag);
    }

    @Test
    public void testArrayPartMore() {
        Varargs ag;
        ag = new ArrayPartVarargs(Z_H_array, 1, 1, new ArrayPartVarargs(Z_H_array, 2, 6));
        standardTestsA_G(ag);
        ag = new ArrayPartVarargs(Z_H_array, 1, 2, new ArrayPartVarargs(Z_H_array, 3, 5));
        standardTestsA_G(ag);
        ag = new ArrayPartVarargs(Z_H_array, 1, 3, new ArrayPartVarargs(Z_H_array, 4, 4));
        standardTestsA_G(ag);
        ag = new ArrayPartVarargs(Z_H_array, 1, 4, new ArrayPartVarargs(Z_H_array, 5, 3));
        standardTestsA_G(ag);
        ag = new ArrayPartVarargs(Z_H_array, 1, 5, new ArrayPartVarargs(Z_H_array, 6, 2));
        standardTestsA_G(ag);
        ag = new ArrayPartVarargs(Z_H_array, 1, 6, new ArrayPartVarargs(Z_H_array, 7, 1));
        standardTestsA_G(ag);
    }

    static void expectNegSubargsError(Varargs v) {
        String expectedMessage = "bad argument #1: start must be > 0";
        try {
            v.subargs(0);
            Assert.fail("Failed to throw exception for index 0");
        } catch ( LuaError e ) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
        try {
            v.subargs(-1);
            Assert.fail("Failed to throw exception for index -1");
        } catch ( LuaError e ) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }
}
