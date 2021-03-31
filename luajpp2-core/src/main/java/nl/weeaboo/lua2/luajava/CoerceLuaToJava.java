package nl.weeaboo.lua2.luajava;

import static nl.weeaboo.lua2.vm.LuaNil.NIL;

import java.lang.reflect.Array;
import java.util.List;

import javax.annotation.Nullable;

import nl.weeaboo.lua2.vm.LuaValue;
import nl.weeaboo.lua2.vm.Varargs;

/**
 * Converts Lua objects to their equivalent Java objects.
 */
public final class CoerceLuaToJava {

    private CoerceLuaToJava() {
    }

    static void coerceArgs(Object[] out, Varargs luaArgs, List<Class<?>> javaParams) {
        final int jlen = javaParams.size();
        if (jlen == 0) {
            return;
        }
        final int llen = luaArgs.narg();
        final int minlen = Math.min(llen, jlen);

        final int jlast = jlen - 1;

        // Treat java functions ending in an array param as varargs
        for (int n = 0; n < jlast; n++) {
            out[n] = coerceArg(luaArgs.arg(1 + n), javaParams.get(n));
        }

        final int vaCount = llen - jlast;
        if (llen > jlen && javaParams.get(jlast)
                .isArray()) {
            final Class<?> vaType = javaParams.get(jlast)
                    .getComponentType();
            Object temp = Array.newInstance(vaType, vaCount);
            for (int n = 0; n < vaCount; n++) {
                Array.set(temp, n, coerceArg(luaArgs.arg(1 + jlast + n), vaType));
            }
            out[jlast] = temp;
        } else if (llen > jlen && javaParams.get(jlast) == Varargs.class) {
            out[jlast] = luaArgs.subargs(1 + jlast);
        } else {
            if (jlast >= 0) {
                out[jlast] = coerceArg(luaArgs.arg(1 + jlast), javaParams.get(jlast));
            }
            for (int n = minlen; n < jlen; n++) {
                out[n] = CoerceLuaToJava.coerceArg(NIL, javaParams.get(n));
            }
        }
    }

    /**
     * Casts or converts the given Lua value to the given Java type.
     *
     * @see ITypeCoercions#toJava(LuaValue, Class)
     */
    public static @Nullable <T> T coerceArg(LuaValue lv, Class<T> c) {
        return ITypeCoercions.getCurrent().toJava(lv, c);
    }

    /**
     * Judges how well the given Lua and Java parameters match. The algorithm looks at the number of
     * parameters as well as their types. The output of this method is a score, where lower scores indicate a
     * better match.
     *
     * @return The score, lower scores are better matches
     */
    public static int scoreParamTypes(Varargs luaArgs, List<Class<?>> javaParams) {
        // Init score & minimum length
        int score;
        final int llen = luaArgs.narg();
        final int jlen = javaParams.size();
        final int len;
        if (jlen == llen) {
            // Same length or possible vararg
            score = 0;
            len = jlen;
        } else if (jlen > llen) {
            score = 0x4000;
            len = llen;
        } else {
            score = 0x8000;
            len = jlen;
        }

        // Compare args
        ITypeCoercions typeCoercions = ITypeCoercions.getCurrent();
        for (int n = 0; n < len; n++) {
            score += typeCoercions.scoreParam(luaArgs.arg(1 + n), javaParams.get(n));
        }
        return score;
    }

}
