package nl.weeaboo.lua2.luajava;

import static nl.weeaboo.lua2.vm.LuaNil.NIL;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;

import nl.weeaboo.lua2.vm.LuaBoolean;
import nl.weeaboo.lua2.vm.LuaDouble;
import nl.weeaboo.lua2.vm.LuaError;
import nl.weeaboo.lua2.vm.LuaInteger;
import nl.weeaboo.lua2.vm.LuaString;
import nl.weeaboo.lua2.vm.LuaTable;
import nl.weeaboo.lua2.vm.LuaUserdata;
import nl.weeaboo.lua2.vm.LuaValue;
import nl.weeaboo.lua2.vm.Varargs;

public final class CoerceLuaToJava {

    interface Coercion {

        Object coerce(LuaValue value);

        int score(LuaValue value);

    }

    private static Map<Class<?>, Coercion> COERCIONS = new HashMap<Class<?>, Coercion>();

    static {
        Coercion boolCoercion = new Coercion() {
            @Override
            public Object coerce(LuaValue value) {
                return value.toboolean() ? Boolean.TRUE : Boolean.FALSE;
            }

            @Override
            public int score(LuaValue value) {
                if (value.isboolean() || value.isnil()) {
                    return 0;
                }
                if (value.isnumber()) {
                    return 1;
                }
                return 4;
            }
        };
        Coercion byteCoercion = new Coercion() {
            @Override
            public Object coerce(LuaValue value) {
                return Byte.valueOf(value.tobyte());
            }

            @Override
            public int score(LuaValue value) {
                if (value.isinttype()) {
                    return 0;
                }
                if (value.isnumber()) {
                    return 1;
                }
                return 4;
            }
        };
        Coercion charCoercion = new Coercion() {
            @Override
            public Object coerce(LuaValue value) {
                return Character.valueOf(value.tochar());
            }

            @Override
            public int score(LuaValue value) {
                if (value.isinttype()) {
                    return 0;
                }
                if (value.isnumber()) {
                    return 1;
                }
                return 4;
            }
        };
        Coercion shortCoercion = new Coercion() {
            @Override
            public Object coerce(LuaValue value) {
                return Short.valueOf(value.toshort());
            }

            @Override
            public int score(LuaValue value) {
                if (value.isinttype()) {
                    return 0;
                }
                if (value.isnumber()) {
                    return 1;
                }
                return 4;
            }
        };
        Coercion intCoercion = new Coercion() {
            @Override
            public Object coerce(LuaValue value) {
                return Integer.valueOf(value.toint());
            }

            @Override
            public int score(LuaValue value) {
                if (value.isinttype()) {
                    return 0;
                }
                if (value.isnumber()) {
                    return 1;
                }
                if (value.isboolean() || value.isnil()) {
                    return 2;
                }
                return 4;
            }
        };
        Coercion longCoercion = new Coercion() {
            @Override
            public Object coerce(LuaValue value) {
                return Long.valueOf(value.tolong());
            }

            @Override
            public int score(LuaValue value) {
                if (value.isinttype()) {
                    return 0;
                }
                if (value.isnumber()) {
                    return 1;
                }
                return 4;
            }
        };
        Coercion floatCoercion = new Coercion() {
            // Cache the two most common values
            private final Float zero = Float.valueOf(0f);
            private final Float one  = Float.valueOf(1f);

            @Override
            public Object coerce(LuaValue value) {
                float f = value.tofloat();
                if (f == 0.0) {
                    return zero;
                }
                if (f == 1.0) {
                    return one;
                }
                return Float.valueOf(f);
            }

            @Override
            public int score(LuaValue value) {
                if (value.isnumber()) {
                    return 1;
                }
                return 4;
            }
        };
        Coercion doubleCoercion = new Coercion() {
            //Cache the two most common values
            private final Double zero = Double.valueOf(0.0);
            private final Double one  = Double.valueOf(1.0);

            @Override
            public Object coerce(LuaValue value) {
                double d = value.todouble();
                if (d == 0.0) {
                    return zero;
                }
                if (d == 1.0) {
                    return one;
                }
                return Double.valueOf(d);
            }

            @Override
            public int score(LuaValue value) {
                if (value.isnumber()) {
                    return 1;
                }
                return 4;
            }
        };
        Coercion stringCoercion = new Coercion() {
            @Override
            public Object coerce(LuaValue value) {
                return (value.isnil() ? null : value.tojstring());
            }

            @Override
            public int score(LuaValue value) {
                if (value.isstring()) {
                    return 0;
                }
                return 2;
            }
        };
        Coercion objectCoercion = new Coercion() {
            @Override
            public Object coerce(LuaValue value) {
                if (value instanceof LuaUserdata) {
                    return ((LuaUserdata) value).userdata();
                }
                if (value instanceof LuaString) {
                    return value.tojstring();
                }
                if (value instanceof LuaInteger) {
                    return Integer.valueOf(value.toint());
                }
                if (value instanceof LuaDouble) {
                    return Double.valueOf(value.todouble());
                }
                if (value instanceof LuaBoolean) {
                    return Boolean.valueOf(value.toboolean());
                }
                if (value.isnil()) {
                    return null;
                }
                return value;
            }

            @Override
            public int score(LuaValue value) {
                if (value.isuserdata()) {
                    return 0;
                }
                if (value.isstring()) {
                    return 1;
                }
                return 16;
            }
        };

        COERCIONS.put(Boolean.TYPE, boolCoercion);
        COERCIONS.put(Boolean.class, boolCoercion);
        COERCIONS.put(Byte.TYPE, byteCoercion);
        COERCIONS.put(Byte.class, byteCoercion);
        COERCIONS.put(Character.TYPE, charCoercion);
        COERCIONS.put(Character.class, charCoercion);
        COERCIONS.put(Short.TYPE, shortCoercion);
        COERCIONS.put(Short.class, shortCoercion);
        COERCIONS.put(Integer.TYPE, intCoercion);
        COERCIONS.put(Integer.class, intCoercion);
        COERCIONS.put(Long.TYPE, longCoercion);
        COERCIONS.put(Long.class, longCoercion);
        COERCIONS.put(Float.TYPE, floatCoercion);
        COERCIONS.put(Float.class, floatCoercion);
        COERCIONS.put(Double.TYPE, doubleCoercion);
        COERCIONS.put(Double.class, doubleCoercion);
        COERCIONS.put(String.class, stringCoercion);
        COERCIONS.put(Object.class, objectCoercion);
    }

    private CoerceLuaToJava() {
    }

    static void coerceArgs(Object[] out, Varargs luaArgs, Class<?>[] javaParams) {
        final int jlen = javaParams.length;
        if (jlen == 0) {
            return;
        }
        final int llen = luaArgs.narg();
        final int minlen = Math.min(llen, jlen);

        final int jlast = jlen - 1;

        //Treat java functions ending in an array param as varargs
        for (int n = 0; n < jlast; n++) {
            out[n] = coerceArg(luaArgs.arg(1 + n), javaParams[n]);
        }

        final int vaCount = llen - jlast;
        if (llen > jlen && javaParams[jlast].isArray()) {
            final Class<?> vaType = javaParams[jlast].getComponentType();
            Object temp = Array.newInstance(vaType, vaCount);
            for (int n = 0; n < vaCount; n++) {
                Array.set(temp, n, coerceArg(luaArgs.arg(1 + jlast + n), vaType));
            }
            out[jlast] = temp;
        } else if (llen > jlen && javaParams[jlast] == Varargs.class) {
            out[jlast] = luaArgs.subargs(1 + jlast);
        } else {
            if (jlast >= 0) {
                out[jlast] = coerceArg(luaArgs.arg(1 + jlast), javaParams[jlast]);
            }
            for (int n = minlen; n < jlen; n++) {
                out[n] = CoerceLuaToJava.coerceArg(NIL, javaParams[n]);
            }
        }
    }

    /**
     * Casts or converts the given Lua value to the given Java type.
     */
    public static <T> T coerceArg(LuaValue lv, Class<T> c) {
        /*
         * The java arg is a Lua type. Check that c is a subclass of LuaValue to prevent using this case for
         * Object params.
         */
        if (LuaValue.class.isAssignableFrom(c) && c.isAssignableFrom(lv.getClass())) {
            return c.cast(lv);
        }

        // The lua arg is a Java object
        if (lv instanceof LuaUserdata) {
            Object obj = ((LuaUserdata) lv).userdata();
            if (c.isAssignableFrom(obj.getClass())) {
                return c.cast(obj);
            }
        }

        //Try to use a specialized coercion function if one is available
        Coercion co = COERCIONS.get(c);
        if (co != null) {
            // Can't used checked cast, because Class.cast() doesn't work for primitive types
            @SuppressWarnings("unchecked")
            T coerced = (T)co.coerce(lv);
            return coerced;
        }

        //Special coercion for arrays
        if (c.isArray()) {
            Class<?> inner = c.getComponentType();
            if (lv instanceof LuaTable) {
                //LTable -> Array
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
                //Single element -> Array
                Object result = Array.newInstance(inner, 1);
                Array.set(result, 0, coerceArg(lv, inner));
                return c.cast(result);
            }
        }

        //Special case for nil
        if (lv.isnil()) {
            return null;
        }

        //String -> Enum
        if (c.isEnum() && lv.isstring()) {
            @SuppressWarnings({ "unchecked", "rawtypes" })
            Enum enumVal = Enum.valueOf((Class<Enum>)c, lv.tojstring());
            return c.cast(enumVal);
        }

        throw new LuaError("Invalid coercion: " + lv.getClass() + " -> " + c);
    }

    /**
     * Judges how well the given Lua and Java parameters match. The algorithm looks at the number of
     * parameters as well as their types. The output of this method is a score, where lower scores indicate a
     * better match.
     *
     * @return The score, lower scores are better matches
     */
    public static int scoreParamTypes(Varargs luaArgs, Class<?>[] javaParams) {
        //Init score & minimum length
        int score;
        final int llen = luaArgs.narg();
        final int jlen = javaParams.length;
        final int len;
        if (jlen == llen) {
            //Same length or possible vararg
            score = 0;
            len = jlen;
        } else if (jlen > llen) {
            score = 0x4000;
            len = llen;
        } else {
            score = 0x8000;
            len = jlen;
        }

        //Compare args
        for (int n = 0; n < len; n++) {
            score += scoreParam(luaArgs.arg(1 + n), javaParams[n]);
        }
        return score;
    }

    private static int scoreParam(LuaValue a, Class<?> c) {
        //Java function uses Lua types
        if (c.isAssignableFrom(a.getClass())) {
            return 0;
        }

        //The lua arg is a Java object
        if (a instanceof LuaUserdata) {
            Object o = ((LuaUserdata) a).userdata();
            if (c.isAssignableFrom(o.getClass())) {
                return 0; //Perfect match
            }
        }

        //Try to use a specialized scoring function if one is available
        Coercion co = COERCIONS.get(c);
        if (co != null) {
            return co.score(a);
        }

        //Special scoring for arrays
        if (c.isArray()) {
            Class<?> inner = c.getComponentType();
            if (a instanceof LuaTable) {
                //Supplying a table as an array arg, compare element types
                return scoreParam(((LuaTable) a).get(1), inner);
            } else {
                //Supplying a single element as an array argument
                return 0x10 + (scoreParam(a, inner) << 8);
            }
        }

        return 0x1000;
    }

}
