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
package nl.weeaboo.lua2.lib;

import static nl.weeaboo.lua2.vm.LuaConstants.NONE;
import static nl.weeaboo.lua2.vm.LuaNil.NIL;

import java.util.Random;

import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.vm.LuaDouble;
import nl.weeaboo.lua2.vm.LuaInteger;
import nl.weeaboo.lua2.vm.LuaTable;
import nl.weeaboo.lua2.vm.LuaValue;
import nl.weeaboo.lua2.vm.Varargs;

/**
 * Subclass of {@link LibFunction} which implements the lua standard {@code math} library.
 * <p>
 * It contains only the math library support that is possible on JME. For a more complete implementation based
 * on math functions specific to JSE use {@code JseMathLib}. In Particular the following math functions are
 * <b>not</b> implemented by this library:
 * <ul>
 * <li>acos</li>
 * <li>asin</li>
 * <li>atan</li>
 * <li>cosh</li>
 * <li>log</li>
 * <li>log10</li>
 * <li>sinh</li>
 * <li>tanh</li>
 * <li>atan2</li>
 * </ul>
 * <p>
 * The implementations of {@code exp()} and {@code pow()} are constructed by hand for JME, so will be slower
 * and less accurate than when executed on the JSE platform.
 * <p>
 * Typically, this library is included as part of a call to either {@code JmePlatform.standardGlobals()}
 * <p>
 * To instantiate and use it directly, link it into your globals table via {@link LuaValue#load(LuaValue)}
 * using code such as:
 *
 * <pre>
 * {
 *     &#064;code
 *     LuaTable _G = new LuaTable();
 *     LuaThread.setGlobals(_G);
 *     _G.load(new BaseLib());
 *     _G.load(new PackageLib());
 *     _G.load(new MathLib());
 *     System.out.println(_G.get(&quot;math&quot;).get(&quot;sqrt&quot;).call(LuaValue.valueOf(2)));
 * }
 * </pre>
 *
 * Doing so will ensure the library is properly initialized and loaded into the globals table.
 * <p>
 * This has been implemented to match as closely as possible the behavior in the corresponding library in C.
 *
 * @see LibFunction
 * @see <a href="http://www.lua.org/manual/5.1/manual.html#5.6">http://www.lua.org/manual/5.1/manual.html#5.6
 *      </a>
 */
@LuaSerializable
public class MathLib extends LuaLibrary {

    private static final long serialVersionUID = 1L;

    private static final String[] NAMES = {
        "abs", "ceil", "cos", "deg", "exp", "floor", "rad", "sin", "sqrt", "tan",
        "acos", "asin", "atan", "cosh", "log", "log10", "sinh", "tanh",
        "fmod", "ldexp", "pow", "atan2",
        "frexp", "max", "min", "modf", "randomseed", "random",
        "fastSin", "fastCos", "fastTan", "fastArcSin", "fastArcCos", "fastArcTan", "fastArcTan2"
    };

    private static final int INIT = 0;
    private static final int ABS = 1;
    private static final int CEIL = 2;
    private static final int COS = 3;
    private static final int DEG = 4;
    private static final int EXP = 5;
    private static final int FLOOR = 6;
    private static final int RAD = 7;
    private static final int SIN = 8;
    private static final int SQRT = 9;
    private static final int TAN = 10;
    private static final int ACOS = 11;
    private static final int ASIN = 12;
    private static final int ATAN = 13;
    private static final int COSH = 14;
    private static final int LOG = 15;
    private static final int LOG10 = 16;
    private static final int SINH = 17;
    private static final int TANH = 18;
    private static final int FMOD = 19;
    private static final int LDEXP = 20;
    private static final int POW = 21;
    private static final int ATAN2 = 22;
    private static final int FREXP = 23;
    private static final int MAX = 24;
    private static final int MIN = 25;
    private static final int MODF = 26;
    private static final int RANDOMSEED = 27;
    private static final int RANDOM = 28;
    private static final int FAST_SIN = 29;
    private static final int FAST_COS = 30;
    private static final int FAST_TAN = 31;
    private static final int FAST_ASIN = 32;
    private static final int FAST_ACOS = 33;
    private static final int FAST_ATAN = 34;
    private static final int FAST_ATAN2 = 35;

    private Random random;

    public MathLib() {
        this(new Random(1));
    }
    protected MathLib(Random random) {
        this.random = random;
    }

    @Override
    protected LuaLibrary newInstance() {
        return new MathLib(random);
    }

    @Override
    protected void initLibrary(LuaTable t, String[] names, int opcodeOffset) {
        super.initLibrary(t, names, opcodeOffset);

        t.set("pi", Math.PI);
        t.set("huge", LuaDouble.POSINF);
    }

    @Override
    public Varargs invoke(Varargs args) {
        switch (opcode) {
        case INIT:
            return initLibrary("math", NAMES, 1);
        case ABS:
            return abs(args.arg1());
        case CEIL:
            return ceil(args.arg1());
        case COS:
            return cos(args.arg1());
        case DEG:
            return toDegrees(args.arg1());
        case EXP:
            return exp(args.arg1());
        case FLOOR:
            return floor(args.arg1());
        case RAD:
            return toRadians(args.arg1());
        case SIN:
            return sin(args.arg1());
        case SQRT:
            return sqrt(args.arg1());
        case TAN:
            return tan(args.arg1());
        case ACOS:
            return acos(args.arg1());
        case ASIN:
            return asin(args.arg1());
        case ATAN:
            return atan(args.arg1());
        case COSH:
            return cosh(args.arg1());
        case LOG:
            return log(args.arg1());
        case LOG10:
            return log10(args.arg1());
        case SINH:
            return sinh(args.arg1());
        case TANH:
            return tanh(args.arg1());
        case FMOD:
            return fmod(args.arg(1), args.arg(2));
        case LDEXP:
            return ldexp(args.arg(1), args.arg(2));
        case POW:
            return pow(args.arg(1), args.arg(2));
        case ATAN2:
            return atan2(args.arg(1), args.arg(2));
        case FREXP:
            return frexp(args);
        case MAX:
            return max(args);
        case MIN:
            return min(args);
        case MODF:
            return modf(args);
        case RANDOMSEED:
            return randomseed(args);
        case RANDOM:
            return random(args);
        case FAST_SIN:
            return valueOf(FastMath.fastSin(args.tofloat(1)));
        case FAST_COS:
            return valueOf(FastMath.fastCos(args.tofloat(1)));
        case FAST_TAN:
            return valueOf(Math.tan(FastMath.fastAngleScale * args.tofloat(1)));
        case FAST_ASIN:
            return valueOf(FastMath.fastArcSin(args.tofloat(1)));
        case FAST_ACOS:
            return valueOf(FastMath.fastArcCos(args.tofloat(1)));
        case FAST_ATAN:
            return valueOf(Math.tan(args.tofloat(1)) * FastMath.fastAngleScale);
        case FAST_ATAN2:
            return valueOf(FastMath.fastArcTan2(args.tofloat(1), args.tofloat(2)));
        default:
            return super.invoke(args);
        }
    }

    public LuaValue abs(LuaValue arg) {
        return valueOf(Math.abs(arg.checkdouble()));
    }

    public LuaValue ceil(LuaValue arg) {
        return valueOf(Math.ceil(arg.checkdouble()));
    }

    public LuaValue cos(LuaValue arg) {
        return valueOf(Math.cos(arg.checkdouble()));
    }

    public LuaValue toDegrees(LuaValue arg) {
        return valueOf(Math.toDegrees(arg.checkdouble()));
    }

    public LuaValue pow(LuaValue arg1, LuaValue arg2) {
        return valueOf(dpow(arg1.checkdouble(), arg2.checkdouble()));
    }

    public static double dpow(double arg1, double arg2) {
        return Math.pow(arg1, arg2);
    }

    public LuaValue atan2(LuaValue arg1, LuaValue arg2) {
        return valueOf(Math.atan2(arg1.checkdouble(), arg2.checkdouble()));
    }

    public LuaValue floor(LuaValue arg) {
        return valueOf(Math.floor(arg.checkdouble()));
    }

    public LuaValue toRadians(LuaValue arg) {
        return valueOf(Math.toRadians(arg.checkdouble()));
    }

    public LuaValue sin(LuaValue arg) {
        return valueOf(Math.sin(arg.checkdouble()));
    }

    public LuaValue sqrt(LuaValue arg) {
        return valueOf(Math.sqrt(arg.checkdouble()));
    }

    public LuaValue tan(LuaValue arg) {
        return valueOf(Math.tan(arg.checkdouble()));
    }

    public LuaValue acos(LuaValue arg) {
        return valueOf(Math.acos(arg.checkdouble()));
    }

    public LuaValue asin(LuaValue arg) {
        return valueOf(Math.asin(arg.checkdouble()));
    }

    public LuaValue atan(LuaValue arg) {
        return valueOf(Math.atan(arg.checkdouble()));
    }

    public LuaValue cosh(LuaValue arg) {
        return valueOf(Math.cosh(arg.checkdouble()));
    }

    public LuaValue exp(LuaValue arg) {
        return valueOf(Math.exp(arg.checkdouble()));
    }

    public LuaValue log(LuaValue arg) {
        return valueOf(Math.log(arg.checkdouble()));
    }

    public LuaValue log10(LuaValue arg) {
        return valueOf(Math.log10(arg.checkdouble()));
    }

    public LuaValue sinh(LuaValue arg) {
        return valueOf(Math.sinh(arg.checkdouble()));
    }

    public LuaValue tanh(LuaValue arg) {
        return valueOf(Math.tanh(arg.checkdouble()));
    }

    public LuaValue fmod(LuaValue arg1, LuaValue arg2) {
        double x = arg1.checkdouble();
        double y = arg2.checkdouble();
        double q = x / y;
        double f = x - y * (q >= 0 ? Math.floor(q) : Math.ceil(q));
        return valueOf(f);
    }

    public LuaValue ldexp(LuaValue arg1, LuaValue arg2) {
        double x = arg1.checkdouble();
        double y = arg2.checkdouble() + 1023.5;
        long e = (long)((0 != (1 & ((int)y))) ? Math.floor(y) : Math.ceil(y - 1));
        return valueOf(x * Double.longBitsToDouble(e << 52));
    }

    public Varargs frexp(Varargs args) {
        double x = args.checkdouble(1);
        if (x == 0) {
            return varargsOf(LuaInteger.valueOf(0), LuaInteger.valueOf(0));
        }
        long bits = Double.doubleToLongBits(x);
        double m = ((bits & (~(-1L << 52))) + (1L << 52))
                * ((bits >= 0) ? (.5 / (1L << 52)) : (-.5 / (1L << 52)));
        double e = (((int)(bits >> 52)) & 0x7ff) - 1022;
        return varargsOf(valueOf(m), valueOf(e));
    }

    public LuaValue max(Varargs args) {
        double m = args.checkdouble(1);
        for (int i = 2, n = args.narg(); i <= n; ++i) {
            m = Math.max(m, args.checkdouble(i));
        }
        return valueOf(m);
    }

    public LuaValue min(Varargs args) {
        double m = args.checkdouble(1);
        for (int i = 2, n = args.narg(); i <= n; ++i) {
            m = Math.min(m, args.checkdouble(i));
        }
        return valueOf(m);
    }

    public Varargs modf(Varargs args) {
        double x = args.checkdouble(1);
        double intPart = (x > 0) ? Math.floor(x) : Math.ceil(x);
        double fracPart = x - intPart;
        return varargsOf(valueOf(intPart), valueOf(fracPart));
    }

    public LuaValue randomseed(Varargs args) {
        long seed = args.checklong(1);
        random.setSeed(seed);
        return NONE;
    }

    public LuaValue random(Varargs args) {
        switch (args.narg()) {
        case 0:
            return valueOf(random.nextDouble());
        case 1: {
            int m = args.checkint(1);
            if (m < 1) argerror(1, "interval is empty");
            return valueOf(1 + random.nextInt(m));
        }
        default: {
            int m = args.checkint(1);
            int n = args.checkint(2);
            if (n < m) argerror(2, "interval is empty");
            return valueOf(m + random.nextInt(n + 1 - m));
        }
        }
    }

    @LuaSerializable
    final class MathLib1 extends OneArgFunction {

        private static final long serialVersionUID = 1L;

        private final MathLib mathlib = MathLib.this;

        @Override
        public LuaValue call(LuaValue arg) {
            switch (opcode) {
            case 0:
                return mathlib.abs(arg);
            case 1:
                return mathlib.ceil(arg);
            case 2:
                return mathlib.cos(arg);
            case 3:
                return mathlib.toDegrees(arg);
            case 4:
                return mathlib.pow(valueOf(Math.E), arg);
            case 5:
                return mathlib.floor(arg);
            case 6:
                return mathlib.toRadians(arg);
            case 7:
                return mathlib.sin(arg);
            case 8:
                return mathlib.sqrt(arg);
            case 9:
                return mathlib.tan(arg);
            case 10:
                return mathlib.acos(arg);
            case 11:
                return mathlib.asin(arg);
            case 12:
                return mathlib.atan(arg);
            case 13:
                return mathlib.cosh(arg);
            case 14:
                return mathlib.exp(arg);
            case 15:
                return mathlib.log(arg);
            case 16:
                return mathlib.log10(arg);
            case 17:
                return mathlib.sinh(arg);
            case 18:
                return mathlib.tanh(arg);
            case 19:
                return valueOf(FastMath.fastSin(arg.tofloat()));
            case 20:
                return valueOf(FastMath.fastCos(arg.tofloat()));
            case 21:
                return valueOf(Math.tan(FastMath.fastAngleScale * arg.tofloat()));
            case 22:
                return valueOf(FastMath.fastArcSin(arg.tofloat()));
            case 23:
                return valueOf(FastMath.fastArcCos(arg.tofloat()));
            case 24:
                return valueOf(Math.tan(arg.tofloat()) * FastMath.fastAngleScale);
            }
            return NIL;
        }
    }

    @LuaSerializable
    final class MathLib2 extends TwoArgFunction {

        private static final long serialVersionUID = 1L;

        private final MathLib mathlib = MathLib.this;

        @Override
        public LuaValue call(LuaValue arg1, LuaValue arg2) {
            switch (opcode) {
            case 0:
                return mathlib.fmod(arg1, arg2);
            case 1:
                return mathlib.ldexp(arg1, arg2);
            case 2:
                return mathlib.pow(arg1, arg2);
            case 3:
                return mathlib.atan2(arg1, arg2);
            case 4:
                return valueOf(FastMath.fastArcTan2(arg1.tofloat(), arg2.tofloat()));
            }
            return NIL;
        }
    }

    @LuaSerializable
    final class MathLibV extends VarArgFunction {

        private static final long serialVersionUID = 1L;

        private final MathLib mathlib = MathLib.this;

        @Override
        public Varargs invoke(Varargs args) {
            switch (opcode) {
            case 0:
                return mathlib.frexp(args);
            case 1:
                return mathlib.max(args);
            case 2:
                return mathlib.min(args);
            case 3:
                return mathlib.modf(args);
            case 4:
                return mathlib.randomseed(args);
            case 5:
                return mathlib.random(args);
            }
            return NONE;
        }
    }
}
