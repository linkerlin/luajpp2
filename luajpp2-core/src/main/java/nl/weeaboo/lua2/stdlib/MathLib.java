package nl.weeaboo.lua2.stdlib;

import static nl.weeaboo.lua2.vm.LuaConstants.NONE;
import static nl.weeaboo.lua2.vm.LuaValue.argerror;
import static nl.weeaboo.lua2.vm.LuaValue.valueOf;
import static nl.weeaboo.lua2.vm.LuaValue.varargsOf;

import java.util.Random;

import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.lib.LuaBoundFunction;
import nl.weeaboo.lua2.vm.LuaDouble;
import nl.weeaboo.lua2.vm.LuaInteger;
import nl.weeaboo.lua2.vm.LuaTable;
import nl.weeaboo.lua2.vm.Varargs;

/**
 * Math library
 */
@LuaSerializable
public final class MathLib extends LuaModule {

    private static final long serialVersionUID = 1L;

    private final Random random = new Random();

    MathLib() {
        super("math");
    }

    @Override
    protected void registerAdditional(LuaTable globals, LuaTable libTable) throws LuaException {
        super.registerAdditional(globals, globals);

        libTable.set("pi", Math.PI);
        libTable.set("huge", LuaDouble.POSINF);
    }

    /**
     * math.abs (x)
     * <p>
     * Returns the absolute value of x.
     */
    @LuaBoundFunction
    public Varargs abs(Varargs args) {
        return valueOf(Math.abs(args.checkdouble(1)));
    }

    /**
     * math.ceil (x)
     * <p>
     * Returns the smallest integer larger than or equal to x.
     */
    @LuaBoundFunction
    public Varargs ceil(Varargs args) {
        return valueOf(Math.ceil(args.checkdouble(1)));
    }

    /**
     * math.cos (x)
     * <p>
     * Returns the cosine of x (assumed to be in radians).
     */
    @LuaBoundFunction
    public Varargs cos(Varargs args) {
        return valueOf(Math.cos(args.checkdouble(1)));
    }

    /**
     * math.deg (x)
     * <p>
     * Returns the angle x (given in radians) in degrees.
     */
    @LuaBoundFunction
    public Varargs deg(Varargs args) {
        return valueOf(Math.toDegrees(args.checkdouble(1)));
    }

    /**
     * math.exp (x)
     * <p>
     * Returns the value e<sup>x</sup>.
     */
    @LuaBoundFunction
    public Varargs exp(Varargs args) {
        return valueOf(Math.exp(args.checkdouble(1)));
    }

    /**
     * math.floor (x)
     * <p>
     * Returns the largest integer smaller than or equal to x.
     */
    @LuaBoundFunction
    public Varargs floor(Varargs args) {
        return valueOf(Math.floor(args.checkdouble(1)));
    }

    /**
     * math.rad (x)
     * <p>
     * Returns the angle x (given in degrees) in radians.
     */
    @LuaBoundFunction
    public Varargs rad(Varargs args) {
        return valueOf(Math.toRadians(args.checkdouble(1)));
    }

    /**
     * math.sin (x)
     * <p>
     * Returns the sine of x (assumed to be in radians).
     */
    @LuaBoundFunction
    public Varargs sin(Varargs args) {
        return valueOf(Math.sin(args.checkdouble(1)));
    }

    /**
     * math.sqrt (x)
     * <p>
     * Returns the square root of x. (You can also use the expression x^0.5 to compute this value.)
     */
    @LuaBoundFunction
    public Varargs sqrt(Varargs args) {
        return valueOf(Math.sqrt(args.checkdouble(1)));
    }

    /**
     * math.tan (x)
     * <p>
     * Returns the tangent of x (assumed to be in radians).
     */
    @LuaBoundFunction
    public Varargs tan(Varargs args) {
        return valueOf(Math.tan(args.checkdouble(1)));
    }

    /**
     * math.asin (x)
     * <p>
     * Returns the arc sine of x (in radians).
     */
    @LuaBoundFunction
    public Varargs asin(Varargs args) {
        return valueOf(Math.asin(args.checkdouble(1)));
    }

    /**
     * math.acos (x)
     * <p>
     * Returns the arc cosine of x (in radians).
     */
    @LuaBoundFunction
    public Varargs acos(Varargs args) {
        return valueOf(Math.acos(args.checkdouble(1)));
    }

    /**
     * math.atan (x)
     * <p>
     * Returns the arc tangent of x (in radians).
     */
    @LuaBoundFunction
    public Varargs atan(Varargs args) {
        return valueOf(Math.atan(args.checkdouble(1)));
    }

    /**
     * math.atan2 (x)
     * <p>
     * Returns the arc tangent of y/x (in radians), but uses the signs of both parameters to find the quadrant
     * of the result. (It also handles correctly the case of x being zero.)
     */
    @LuaBoundFunction
    public Varargs atan2(Varargs args) {
        return valueOf(Math.atan2(args.checkdouble(1), args.checkdouble(2)));
    }

    /**
     * math.pow (x)
     * <p>
     * Returns x<pow>y</pow>. (You can also use the expression x^y to compute this value.)
     */
    @LuaBoundFunction
    public Varargs pow(Varargs args) {
        return args.arg(1).pow(args.checkdouble(2));
    }

    /**
     * math.sinh (x)
     * <p>
     * Returns the hyperbolic sine of x.
     */
    @LuaBoundFunction
    public Varargs sinh(Varargs args) {
        return valueOf(Math.sinh(args.checkdouble(1)));
    }

    /**
     * math.cosh (x)
     * <p>
     * Returns the hyperbolic cosine of x.
     */
    @LuaBoundFunction
    public Varargs cosh(Varargs args) {
        return valueOf(Math.cosh(args.checkdouble(1)));
    }

    /**
     * math.tanh (x)
     * <p>
     * Returns the hyperbolic tangent of x.
     */
    @LuaBoundFunction
    public Varargs tanh(Varargs args) {
        return valueOf(Math.tanh(args.checkdouble(1)));
    }

    /**
     * math.log (x)
     * <p>
     * Returns the natural logarithm of x.
     */
    @LuaBoundFunction
    public Varargs log(Varargs args) {
        return valueOf(Math.log(args.checkdouble(1)));
    }

    /**
     * math.log10 (x)
     * <p>
     * Returns the base-10 logarithm of x.
     */
    @LuaBoundFunction
    public Varargs log10(Varargs args) {
        return valueOf(Math.log10(args.checkdouble(1)));
    }

    /**
     * math.max (x, ...)
     * <p>
     * Returns the maximum value among its arguments.
     */
    @LuaBoundFunction
    public Varargs max(Varargs args) {
        double m = args.checkdouble(1);
        for (int i = 2, n = args.narg(); i <= n; ++i) {
            m = Math.max(m, args.checkdouble(i));
        }
        return valueOf(m);
    }

    /**
     * math.min (x, ...)
     * <p>
     * Returns the minimum value among its arguments.
     */
    @LuaBoundFunction
    public Varargs min(Varargs args) {
        double m = args.checkdouble(1);
        for (int i = 2, n = args.narg(); i <= n; ++i) {
            m = Math.min(m, args.checkdouble(i));
        }
        return valueOf(m);
    }

    /**
     * math.fmod (x)
     * <p>
     * Returns the remainder of the division of x by y that rounds the quotient towards zero.
     */
    @LuaBoundFunction
    public Varargs fmod(Varargs args) {
        double x = args.checkdouble(1);
        double y = args.checkdouble(2);
        double q = x / y;
        double f = x - y * (q >= 0 ? Math.floor(q) : Math.ceil(q));
        return valueOf(f);
    }

    /**
     * math.ldexp (m, e)
     * <p>
     * Returns m2<sup>e</sup> (e should be an integer).
     */
    @LuaBoundFunction
    public Varargs ldexp(Varargs args) {
        double x = args.checkdouble(1);
        double y = args.checkdouble(2) + 1023.5;
        long e = (long)((0 != (1 & ((int)y))) ? Math.floor(y) : Math.ceil(y - 1));
        return valueOf(x * Double.longBitsToDouble(e << 52));
    }

    /**
     * math.frexp (x)
     * <p>
     * Returns m and e such that x = m2<sup>e</sup>, e is an integer and the absolute value of m is in the
     * range [0.5, 1) (or zero when x is zero).
     */
    @LuaBoundFunction
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

    /**
     * @deprecated Use {@link #fmod(Varargs)} instead.
     */
    @Deprecated
    @LuaBoundFunction
    public Varargs mod(Varargs args) {
        return fmod(args);
    }

    /**
     * math.modf (x)
     * <p>
     * Returns two numbers, the integral part of x and the fractional part of x.
     */
    @LuaBoundFunction
    public Varargs modf(Varargs args) {
        double x = args.checkdouble(1);
        double intPart = (x > 0) ? Math.floor(x) : Math.ceil(x);
        double fracPart = x - intPart;
        return varargsOf(valueOf(intPart), valueOf(fracPart));
    }

    /**
     * math.randomseed (x)
     * <p>
     * Sets x as the "seed" for the pseudo-random generator: equal seeds produce equal sequences of numbers.
     */
    @LuaBoundFunction
    public Varargs randomseed(Varargs args) {
        long seed = args.checklong(1);
        random.setSeed(seed);
        return NONE;
    }

    /**
     * math.random ([m [, n]])
     * <p>
     * This function is an interface to the simple pseudo-random generator function rand provided by Java.
     * (No guarantees can be given for its statistical properties.)
     * <p>
     * When called without arguments, returns a uniform pseudo-random real number in the range [0,1). When
     * called with an integer number m, math.random returns a uniform pseudo-random integer in the range [1,
     * m]. When called with two integer numbers m and n, math.random returns a uniform pseudo-random integer
     * in the range [m, n].
     */
    @LuaBoundFunction
    public Varargs random(Varargs args) {
        switch (args.narg()) {
        case 0:
            return valueOf(random.nextDouble());
        case 1: {
            int m = args.checkint(1);
            if (m < 1) {
                argerror(1, "interval is empty");
            }
            return valueOf(1 + random.nextInt(m));
        }
        default: {
            int m = args.checkint(1);
            int n = args.checkint(2);
            if (n < m) {
                argerror(2, "interval is empty");
            }
            return valueOf(m + random.nextInt(n + 1 - m));
        }
        }
    }

}
