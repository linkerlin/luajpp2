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

    @LuaBoundFunction
    public Varargs abs(Varargs args) {
        return valueOf(Math.abs(args.checkdouble(1)));
    }

    @LuaBoundFunction
    public Varargs ceil(Varargs args) {
        return valueOf(Math.ceil(args.checkdouble(1)));
    }

    @LuaBoundFunction
    public Varargs cos(Varargs args) {
        return valueOf(Math.cos(args.checkdouble(1)));
    }

    @LuaBoundFunction
    public Varargs deg(Varargs args) {
        return valueOf(Math.toDegrees(args.checkdouble(1)));
    }

    @LuaBoundFunction
    public Varargs exp(Varargs args) {
        return valueOf(Math.exp(args.checkdouble(1)));
    }

    @LuaBoundFunction
    public Varargs floor(Varargs args) {
        return valueOf(Math.floor(args.checkdouble(1)));
    }

    @LuaBoundFunction
    public Varargs rad(Varargs args) {
        return valueOf(Math.toRadians(args.checkdouble(1)));
    }

    @LuaBoundFunction
    public Varargs sin(Varargs args) {
        return valueOf(Math.sin(args.checkdouble(1)));
    }

    @LuaBoundFunction
    public Varargs sqrt(Varargs args) {
        return valueOf(Math.sqrt(args.checkdouble(1)));
    }

    @LuaBoundFunction
    public Varargs tan(Varargs args) {
        return valueOf(Math.tan(args.checkdouble(1)));
    }

    @LuaBoundFunction
    public Varargs asin(Varargs args) {
        return valueOf(Math.asin(args.checkdouble(1)));
    }

    @LuaBoundFunction
    public Varargs acos(Varargs args) {
        return valueOf(Math.acos(args.checkdouble(1)));
    }

    @LuaBoundFunction
    public Varargs atan(Varargs args) {
        return valueOf(Math.atan(args.checkdouble(1)));
    }

    @LuaBoundFunction
    public Varargs atan2(Varargs args) {
        return valueOf(Math.atan2(args.checkdouble(1), args.checkdouble(2)));
    }

    @LuaBoundFunction
    public Varargs pow(Varargs args) {
        return args.arg(1).pow(args.checkdouble(2));
    }

    @LuaBoundFunction
    public Varargs sinh(Varargs args) {
        return valueOf(Math.sinh(args.checkdouble(1)));
    }

    @LuaBoundFunction
    public Varargs cosh(Varargs args) {
        return valueOf(Math.cosh(args.checkdouble(1)));
    }

    @LuaBoundFunction
    public Varargs tanh(Varargs args) {
        return valueOf(Math.tanh(args.checkdouble(1)));
    }

    @LuaBoundFunction
    public Varargs log(Varargs args) {
        return valueOf(Math.log(args.checkdouble(1)));
    }

    @LuaBoundFunction
    public Varargs log10(Varargs args) {
        return valueOf(Math.log10(args.checkdouble(1)));
    }

    @LuaBoundFunction
    public Varargs max(Varargs args) {
        double m = args.checkdouble(1);
        for (int i = 2, n = args.narg(); i <= n; ++i) {
            m = Math.max(m, args.checkdouble(i));
        }
        return valueOf(m);
    }

    @LuaBoundFunction
    public Varargs min(Varargs args) {
        double m = args.checkdouble(1);
        for (int i = 2, n = args.narg(); i <= n; ++i) {
            m = Math.min(m, args.checkdouble(i));
        }
        return valueOf(m);
    }

    @LuaBoundFunction
    public Varargs fmod(Varargs args) {
        double x = args.checkdouble(1);
        double y = args.checkdouble(2);
        double q = x / y;
        double f = x - y * (q >= 0 ? Math.floor(q) : Math.ceil(q));
        return valueOf(f);
    }

    @LuaBoundFunction
    public Varargs ldexp(Varargs args) {
        double x = args.checkdouble(1);
        double y = args.checkdouble(2) + 1023.5;
        long e = (long)((0 != (1 & ((int)y))) ? Math.floor(y) : Math.ceil(y - 1));
        return valueOf(x * Double.longBitsToDouble(e << 52));
    }

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

    @Deprecated
    @LuaBoundFunction
    public Varargs mod(Varargs args) {
        return fmod(args);
    }

    @LuaBoundFunction
    public Varargs modf(Varargs args) {
        double x = args.checkdouble(1);
        double intPart = (x > 0) ? Math.floor(x) : Math.ceil(x);
        double fracPart = x - intPart;
        return varargsOf(valueOf(intPart), valueOf(fracPart));
    }

    @LuaBoundFunction
    public Varargs randomseed(Varargs args) {
        long seed = args.checklong(1);
        random.setSeed(seed);
        return NONE;
    }

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
