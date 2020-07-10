package nl.weeaboo.lua2.luajava;

import static nl.weeaboo.lua2.vm.LuaNil.NIL;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.vm.LuaBoolean;
import nl.weeaboo.lua2.vm.LuaDouble;
import nl.weeaboo.lua2.vm.LuaInteger;
import nl.weeaboo.lua2.vm.LuaNil;
import nl.weeaboo.lua2.vm.LuaString;
import nl.weeaboo.lua2.vm.LuaTable;
import nl.weeaboo.lua2.vm.LuaUserdata;
import nl.weeaboo.lua2.vm.LuaValue;

@SuppressWarnings("ImmutableEnumChecker") // We don't have Guava, so can't use ImmutableMap
enum DefaultTypeCoercions implements ITypeCoercions {

    INSTANCE;

    private final Map<Class<?>, IJavaToLua> javaToLuaCoercions = new HashMap<>();
    private final Map<Class<?>, ILuaToJava<?>> luaToJavaCoercions = new HashMap<>();

    private DefaultTypeCoercions() {
        initDefaultJavaToLua();
        initDefaultLuaToJava();
    }

    private void initDefaultJavaToLua() {
        javaToLuaCoercions.put(Void.TYPE, JavaToLua.NULL);
        javaToLuaCoercions.put(Boolean.class, JavaToLua.BOOL);
        javaToLuaCoercions.put(Boolean.TYPE, JavaToLua.BOOL);
        javaToLuaCoercions.put(Character.class, JavaToLua.CHAR);
        javaToLuaCoercions.put(Character.TYPE, JavaToLua.CHAR);
        javaToLuaCoercions.put(Byte.class, JavaToLua.INT);
        javaToLuaCoercions.put(Byte.TYPE, JavaToLua.INT);
        javaToLuaCoercions.put(Short.class, JavaToLua.INT);
        javaToLuaCoercions.put(Short.TYPE, JavaToLua.INT);
        javaToLuaCoercions.put(Integer.class, JavaToLua.INT);
        javaToLuaCoercions.put(Integer.TYPE, JavaToLua.INT);
        javaToLuaCoercions.put(Long.class, JavaToLua.LONG);
        javaToLuaCoercions.put(Long.TYPE, JavaToLua.LONG);
        javaToLuaCoercions.put(Float.class, JavaToLua.DOUBLE);
        javaToLuaCoercions.put(Float.TYPE, JavaToLua.DOUBLE);
        javaToLuaCoercions.put(Double.class, JavaToLua.DOUBLE);
        javaToLuaCoercions.put(Double.TYPE, JavaToLua.DOUBLE);
        javaToLuaCoercions.put(String.class, JavaToLua.STRING);
    }

    private void initDefaultLuaToJava() {
        luaToJavaCoercions.put(Boolean.TYPE, LuaToJava.BOOL);
        luaToJavaCoercions.put(Boolean.class, LuaToJava.BOOL);
        luaToJavaCoercions.put(Byte.TYPE, LuaToJava.BYTE);
        luaToJavaCoercions.put(Byte.class, LuaToJava.BYTE);
        luaToJavaCoercions.put(Character.TYPE, LuaToJava.CHAR);
        luaToJavaCoercions.put(Character.class, LuaToJava.CHAR);
        luaToJavaCoercions.put(Short.TYPE, LuaToJava.SHORT);
        luaToJavaCoercions.put(Short.class, LuaToJava.SHORT);
        luaToJavaCoercions.put(Integer.TYPE, LuaToJava.INT);
        luaToJavaCoercions.put(Integer.class, LuaToJava.INT);
        luaToJavaCoercions.put(Long.TYPE, LuaToJava.LONG);
        luaToJavaCoercions.put(Long.class, LuaToJava.LONG);
        luaToJavaCoercions.put(Float.TYPE, LuaToJava.FLOAT);
        luaToJavaCoercions.put(Float.class, LuaToJava.FLOAT);
        luaToJavaCoercions.put(Double.TYPE, LuaToJava.DOUBLE);
        luaToJavaCoercions.put(Double.class, LuaToJava.DOUBLE);
        luaToJavaCoercions.put(String.class, LuaToJava.STRING);
        luaToJavaCoercions.put(Object.class, LuaToJava.OBJECT);
    }

    @Override
    public @Nullable <T> LuaValue toLua(@Nullable T javaValue) {
        return toLua(javaValue, javaValue != null ? javaValue.getClass() : Object.class);
    }

    @Override
    public <T> LuaValue toLua(@Nullable T javaValue, Class<?> declaredType) {
        if (javaValue == null) {
            return NIL;
        }

        IJavaToLua javaToLua = javaToLuaCoercions.get(declaredType);
        if (javaToLua != null) {
            // A specialized coercion was found, use it
            return javaToLua.toLua(javaValue);
        }

        if (LuaValue.class.isAssignableFrom(declaredType)) {
            // Java object is a Lua type
            return (LuaValue)javaValue;
        }

        // Use the general Java Object -> Lua conversion
        return LuajavaLib.toUserdata(javaValue, declaredType);
    }

    @Override
    public @Nullable <T> T toJava(LuaValue lv, Class<T> javaType) {
        /*
         * The java arg is a Lua type. Check that javaType is a subclass of LuaValue to prevent using this case for
         * Object params.
         */
        if (LuaValue.class.isAssignableFrom(javaType) && javaType.isAssignableFrom(lv.getClass())) {
            return javaType.cast(lv);
        }

        // The lua arg is a Java object
        if (lv instanceof LuaUserdata) {
            Object obj = ((LuaUserdata)lv).userdata();
            if (javaType.isAssignableFrom(obj.getClass())) {
                return javaType.cast(obj);
            }
        }

        // Try to use a specialized coercion function if one is available
        ILuaToJava<T> customCoercion = findLuaToJava(javaType);
        if (customCoercion != null) {
            return customCoercion.toJava(lv);
        }

        // Special coercion for arrays
        if (javaType.isArray()) {
            Class<?> inner = javaType.getComponentType();
            if (lv instanceof LuaTable) {
                // LTable -> Array
                LuaTable table = (LuaTable)lv;
                int len = table.length();
                Object result = Array.newInstance(inner, len);
                for (int n = 0; n < len; n++) {
                    LuaValue val = table.get(n + 1);
                    if (val != null) {
                        Array.set(result, n, toJava(val, inner));
                    }
                }
                return javaType.cast(result);
            } else {
                // Single element -> Array
                Object result = Array.newInstance(inner, 1);
                Array.set(result, 0, toJava(lv, inner));
                return javaType.cast(result);
            }
        }

        // Special case for nil
        if (lv.isnil()) {
            return null;
        }

        // String -> Enum
        if (javaType.isEnum() && lv.isstring()) {
            @SuppressWarnings({ "unchecked", "rawtypes" })
            Enum enumVal = Enum.valueOf((Class<Enum>)javaType, lv.tojstring());
            return javaType.cast(enumVal);
        }

        throw new LuaException("Invalid coercion: " + lv.getClass() + " -> " + javaType);
    }

    @Override
    public int scoreParam(LuaValue luaValue, Class<?> javaType) {
        // Java function uses Lua types
        if (javaType.isAssignableFrom(luaValue.getClass())) {
            return 0;
        }

        // The lua arg is a Java object
        if (luaValue instanceof LuaUserdata) {
            Object o = ((LuaUserdata)luaValue).userdata();
            if (javaType.isAssignableFrom(o.getClass())) {
                return 0; // Perfect match
            }
        }

        // Check type-specific functions
        ILuaToJava<?> customCoercion = findLuaToJava(javaType);
        if (customCoercion != null) {
            return customCoercion.score(luaValue);
        }

        // Special scoring for arrays
        if (javaType.isArray()) {
            Class<?> inner = javaType.getComponentType();
            if (luaValue instanceof LuaTable) {
                // Supplying a table as an array arg, compare element types
                return scoreParam(((LuaTable)luaValue).get(1), inner);
            } else {
                // Supplying a single element as an array argument
                return 0x10 + (scoreParam(luaValue, inner) << 8);
            }
        }

        return 0x1000;
    }

    /**
     * Returns the registered type coercion for converting Lua objects to Java objects of the given type.
     *
     * @return The type coercion, or {@code null} if not found.
     */
    private <T> ILuaToJava<T> findLuaToJava(Class<T> javaType) {
        // This cast is safe -- coercions are stored mapped from Class<T> -> ILuaToJava<T>
        @SuppressWarnings("unchecked")
        ILuaToJava<T> typed = (ILuaToJava<T>)luaToJavaCoercions.get(javaType);
        return typed;
    }

    private interface JavaToLua {
        IJavaToLua NULL = new IJavaToLua() {
            @Override
            public LuaValue toLua(Object javaValue) {
                return LuaNil.NIL;
            }
        };

        IJavaToLua BOOL = new IJavaToLua() {
            @Override
            public LuaValue toLua(Object javaValue) {
                boolean b = ((Boolean)javaValue).booleanValue();
                return (b ? LuaBoolean.TRUE : LuaBoolean.FALSE);
            }
        };

        IJavaToLua CHAR = new IJavaToLua() {
            @Override
            public LuaValue toLua(Object javaValue) {
                char c = ((Character)javaValue).charValue();
                return LuaInteger.valueOf(c);
            }
        };

        IJavaToLua INT = new IJavaToLua() {
            @Override
            public LuaValue toLua(Object javaValue) {
                int i = ((Number)javaValue).intValue();
                return LuaInteger.valueOf(i);
            }
        };

        IJavaToLua LONG = new IJavaToLua() {
            @Override
            public LuaValue toLua(Object javaValue) {
                long i = ((Number)javaValue).longValue();
                return LuaDouble.valueOf(i);
            }
        };

        IJavaToLua DOUBLE = new IJavaToLua() {
            @Override
            public LuaValue toLua(Object javaValue) {
                double d = ((Number)javaValue).doubleValue();
                return LuaDouble.valueOf(d);
            }
        };

        IJavaToLua STRING = new IJavaToLua() {
            @Override
            public LuaValue toLua(Object javaValue) {
                return LuaString.valueOf(javaValue.toString());
            }
        };

    }

    private interface LuaToJava {

        ILuaToJava<Boolean> BOOL = new ILuaToJava<Boolean>() {
            @Override
            public Boolean toJava(LuaValue value) {
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

        ILuaToJava<Byte> BYTE = new ILuaToJava<Byte>() {
            @Override
            public Byte toJava(LuaValue value) {
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

        ILuaToJava<Character> CHAR = new ILuaToJava<Character>() {
            @Override
            public Character toJava(LuaValue value) {
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

        ILuaToJava<Short> SHORT = new ILuaToJava<Short>() {
            @Override
            public Short toJava(LuaValue value) {
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

        ILuaToJava<Integer> INT = new ILuaToJava<Integer>() {
            @Override
            public Integer toJava(LuaValue value) {
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

        ILuaToJava<Long> LONG = new ILuaToJava<Long>() {
            @Override
            public Long toJava(LuaValue value) {
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

        ILuaToJava<Float> FLOAT = new ILuaToJava<Float>() {
            // Cache the two most common values
            private final Float zero = Float.valueOf(0f);
            private final Float one = Float.valueOf(1f);

            @Override
            public Float toJava(LuaValue value) {
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

        ILuaToJava<Double> DOUBLE = new ILuaToJava<Double>() {
            // Cache the two most common values
            private final Double zero = Double.valueOf(0.0);
            private final Double one = Double.valueOf(1.0);

            @Override
            public Double toJava(LuaValue value) {
                double d = value.todouble();
                if (d == 0.0) {
                    return zero;
                } else if (d == 1.0) {
                    return one;
                } else {
                    return Double.valueOf(d);
                }
            }

            @Override
            public int score(LuaValue value) {
                if (value.isnumber()) {
                    return 1;
                } else {
                    return 4;
                }
            }
        };

        ILuaToJava<String> STRING = new ILuaToJava<String>() {
            @Override
            public @Nullable String toJava(LuaValue value) {
                return (value.isnil() ? null : value.tojstring());
            }

            @Override
            public int score(LuaValue value) {
                if (value.isstring()) {
                    return 0;
                } else {
                    return 2;
                }
            }
        };

        ILuaToJava<Object> OBJECT = new ILuaToJava<Object>() {
            @Override
            public @Nullable Object toJava(LuaValue value) {
                if (value instanceof LuaUserdata) {
                    return ((LuaUserdata)value).userdata();
                } else if (value instanceof LuaString) {
                    return value.tojstring();
                } else if (value instanceof LuaInteger) {
                    return Integer.valueOf(value.toint());
                } else if (value instanceof LuaDouble) {
                    return Double.valueOf(value.todouble());
                } else if (value instanceof LuaBoolean) {
                    return Boolean.valueOf(value.toboolean());
                } else if (value.isnil()) {
                    return null;
                } else {
                    return value;
                }
            }

            @Override
            public int score(LuaValue value) {
                if (value.isuserdata()) {
                    return 0;
                } else if (value.isstring()) {
                    return 1;
                } else {
                    return 16;
                }
            }
        };

    }

}
