package nl.weeaboo.lua2.internal;

import static nl.weeaboo.lua2.vm.LuaConstants.NONE;
import static nl.weeaboo.lua2.vm.LuaValue.varargsOf;

import nl.weeaboo.lua2.LuaUtil;
import nl.weeaboo.lua2.vm.LuaValue;
import nl.weeaboo.lua2.vm.Varargs;

/**
 * Contains various functions for working with Lua function arguments.
 */
public final class LuaArgsUtil {

    private LuaArgsUtil() {
    }

    /**
     * Returns a {@link Varargs} that's a concetenation of {@code firstArgs} and {@code secondArgs}.
     */
    public static Varargs concatVarargs(Varargs firstArgs, Varargs secondArgs) {
        if (firstArgs == null || firstArgs.narg() == 0) {
            return secondArgs;
        }
        if (secondArgs == null || secondArgs.narg() == 0) {
            return firstArgs;
        }

        int firstCount = firstArgs.narg();
        int secondCount = secondArgs.narg();

        LuaValue[] merged = new LuaValue[firstCount + secondCount];
        for (int n = 0; n < firstCount; n++) {
            merged[n] = firstArgs.arg(1 + n);
        }
        for (int n = 0; n < secondCount; n++) {
            merged[firstCount + n] = secondArgs.arg(1 + n);
        }
        return LuaValue.varargsOf(merged);
    }

    /**
     * Returns a {@link Varargs} that's a copy of part of the given stack.
     */
    public static Varargs copyArgs(LuaValue[] stack, int offset, int length) {
        if (length <= 0) {
            return NONE;
        }

        LuaValue[] array = new LuaValue[length];
        for (int n = 0; n < length; n++) {
            array[n] = stack[offset + n];
        }
        return varargsOf(array);
    }

    /**
     * Returns a {@link Varargs} that's a copy of part of the given stack with {@code extra} concatenated to the end.
     */
    public static Varargs copyArgs(LuaValue[] stack, int offset, int length, Varargs extra) {
        if (length <= 0) {
            return LuaUtil.copyArgs(extra);
        }

        if (extra == null) {
            extra = NONE;
        }
        int extraL = extra.narg();

        LuaValue[] array = new LuaValue[length + extraL];
        for (int n = 0; n < length; n++) {
            array[n] = stack[offset + n];
        }
        for (int n = 0; n < extraL; n++) {
            array[length + n] = extra.arg(1 + n);
        }
        return varargsOf(array);
    }

}
