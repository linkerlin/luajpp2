package nl.weeaboo.lua2.luajava;

import static nl.weeaboo.lua2.vm.LuaNil.NIL;

import java.lang.reflect.Array;
import java.util.List;

import javax.annotation.Nullable;

import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.vm.LuaTable;
import nl.weeaboo.lua2.vm.LuaUserdata;
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
     */
    public static @Nullable <T> T coerceArg(LuaValue lv, Class<T> c) {
        /*
         * The java arg is a Lua type. Check that c is a subclass of LuaValue to prevent using this case for
         * Object params.
         */
        if (LuaValue.class.isAssignableFrom(c) && c.isAssignableFrom(lv.getClass())) {
            return c.cast(lv);
        }

        // The lua arg is a Java object
        if (lv instanceof LuaUserdata) {
            Object obj = ((LuaUserdata)lv).userdata();
            if (c.isAssignableFrom(obj.getClass())) {
                return c.cast(obj);
            }
        }

        // Try to use a specialized coercion function if one is available
        TypeCoercions typeCoercions = TypeCoercions.getInstance();
        ILuaToJava<T> luaToJava = typeCoercions.findLuaToJava(c);
        if (luaToJava != null) {
            return luaToJava.toJava(lv);
        }

        // Special coercion for arrays
        if (c.isArray()) {
            Class<?> inner = c.getComponentType();
            if (lv instanceof LuaTable) {
                // LTable -> Array
                LuaTable table = (LuaTable)lv;
                int len = table.length();
                Object result = Array.newInstance(inner, len);
                for (int n = 0; n < len; n++) {
                    LuaValue val = table.get(n + 1);
                    if (val != null) {
                        Array.set(result, n, coerceArg(val, inner));
                    }
                }
                return c.cast(result);
            } else {
                // Single element -> Array
                Object result = Array.newInstance(inner, 1);
                Array.set(result, 0, coerceArg(lv, inner));
                return c.cast(result);
            }
        }

        // Special case for nil
        if (lv.isnil()) {
            return null;
        }

        // String -> Enum
        if (c.isEnum() && lv.isstring()) {
            @SuppressWarnings({ "unchecked", "rawtypes" })
            Enum enumVal = Enum.valueOf((Class<Enum>)c, lv.tojstring());
            return c.cast(enumVal);
        }

        throw new LuaException("Invalid coercion: " + lv.getClass() + " -> " + c);
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
        for (int n = 0; n < len; n++) {
            score += scoreParam(luaArgs.arg(1 + n), javaParams.get(n));
        }
        return score;
    }

    private static <T> int scoreParam(LuaValue a, Class<T> c) {
        // Java function uses Lua types
        if (c.isAssignableFrom(a.getClass())) {
            return 0;
        }

        // The lua arg is a Java object
        if (a instanceof LuaUserdata) {
            Object o = ((LuaUserdata)a).userdata();
            if (c.isAssignableFrom(o.getClass())) {
                return 0; // Perfect match
            }
        }

        // Try to use a specialized scoring function if one is available
        TypeCoercions typeCoercions = TypeCoercions.getInstance();
        ILuaToJava<T> luaToJava = typeCoercions.findLuaToJava(c);
        if (luaToJava != null) {
            return luaToJava.score(a);
        }

        // Special scoring for arrays
        if (c.isArray()) {
            Class<?> inner = c.getComponentType();
            if (a instanceof LuaTable) {
                // Supplying a table as an array arg, compare element types
                return scoreParam(((LuaTable)a).get(1), inner);
            } else {
                // Supplying a single element as an array argument
                return 0x10 + (scoreParam(a, inner) << 8);
            }
        }

        return 0x1000;
    }

}
