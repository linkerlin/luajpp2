package nl.weeaboo.lua2.luajava;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import nl.weeaboo.lua2.vm.LuaBoolean;
import nl.weeaboo.lua2.vm.LuaDouble;
import nl.weeaboo.lua2.vm.LuaInteger;
import nl.weeaboo.lua2.vm.LuaString;
import nl.weeaboo.lua2.vm.LuaUserdata;
import nl.weeaboo.lua2.vm.LuaValue;

final class TypeCoercions {

    private static final TypeCoercions INSTANCE = new TypeCoercions();

    private final Map<Class<?>, IJavaToLua> javaToLuaCoercions = new HashMap<>();
    private final Map<Class<?>, ILuaToJava<?>> luaToJavaCoercions = new HashMap<>();

    private TypeCoercions() {
        initDefaultJavaToLua();
        initDefaultLuaToJava();
    }

    static TypeCoercions getInstance() {
        return INSTANCE;
    }

    private void initDefaultJavaToLua() {
        IJavaToLua boolCoercion = new IJavaToLua() {
            @Override
            public LuaValue toLua(Object javaValue) {
                boolean b = ((Boolean)javaValue).booleanValue();
                return (b ? LuaBoolean.TRUE : LuaBoolean.FALSE);
            }
        };
        putJavaToLua(Boolean.class, boolCoercion);
        putJavaToLua(Boolean.TYPE, boolCoercion);

        IJavaToLua charCoercion = new IJavaToLua() {
            @Override
            public LuaValue toLua(Object javaValue) {
                char c = ((Character)javaValue).charValue();
                return LuaInteger.valueOf(c);
            }
        };
        putJavaToLua(Character.class, charCoercion);
        putJavaToLua(Character.TYPE, charCoercion);

        IJavaToLua intCoercion = new IJavaToLua() {
            @Override
            public LuaValue toLua(Object javaValue) {
                int i = ((Number)javaValue).intValue();
                return LuaInteger.valueOf(i);
            }
        };
        putJavaToLua(Byte.class, intCoercion);
        putJavaToLua(Byte.TYPE, intCoercion);
        putJavaToLua(Short.class, intCoercion);
        putJavaToLua(Short.TYPE, intCoercion);
        putJavaToLua(Integer.class, intCoercion);
        putJavaToLua(Integer.TYPE, intCoercion);

        IJavaToLua longCoercion = new IJavaToLua() {
            @Override
            public LuaValue toLua(Object javaValue) {
                long i = ((Number)javaValue).longValue();
                return LuaDouble.valueOf(i);
            }
        };
        putJavaToLua(Long.class, longCoercion);
        putJavaToLua(Long.TYPE, longCoercion);

        IJavaToLua doubleCoercion = new IJavaToLua() {
            @Override
            public LuaValue toLua(Object javaValue) {
                double d = ((Number)javaValue).doubleValue();
                return LuaDouble.valueOf(d);
            }
        };
        putJavaToLua(Float.class, doubleCoercion);
        putJavaToLua(Float.TYPE, doubleCoercion);
        putJavaToLua(Double.class, doubleCoercion);
        putJavaToLua(Double.TYPE, doubleCoercion);

        IJavaToLua stringCoercion = new IJavaToLua() {
            @Override
            public LuaValue toLua(Object javaValue) {
                return LuaString.valueOf(javaValue.toString());
            }
        };
        putJavaToLua(String.class, stringCoercion);
    }

    private void initDefaultLuaToJava() {
        ILuaToJava<Boolean> boolCoercion = new ILuaToJava<Boolean>() {
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
        putLuaToJava(Boolean.TYPE, boolCoercion);
        putLuaToJava(Boolean.class, boolCoercion);

        ILuaToJava<Byte> byteCoercion = new ILuaToJava<Byte>() {
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
        putLuaToJava(Byte.TYPE, byteCoercion);
        putLuaToJava(Byte.class, byteCoercion);

        ILuaToJava<Character> charCoercion = new ILuaToJava<Character>() {
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
        putLuaToJava(Character.TYPE, charCoercion);
        putLuaToJava(Character.class, charCoercion);

        ILuaToJava<Short> shortCoercion = new ILuaToJava<Short>() {
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
        putLuaToJava(Short.TYPE, shortCoercion);
        putLuaToJava(Short.class, shortCoercion);

        ILuaToJava<Integer> intCoercion = new ILuaToJava<Integer>() {
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
        putLuaToJava(Integer.TYPE, intCoercion);
        putLuaToJava(Integer.class, intCoercion);

        ILuaToJava<Long> longCoercion = new ILuaToJava<Long>() {
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
        putLuaToJava(Long.TYPE, longCoercion);
        putLuaToJava(Long.class, longCoercion);

        ILuaToJava<Float> floatCoercion = new ILuaToJava<Float>() {
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
        putLuaToJava(Float.TYPE, floatCoercion);
        putLuaToJava(Float.class, floatCoercion);

        ILuaToJava<Double> doubleCoercion = new ILuaToJava<Double>() {
            // Cache the two most common values
            private final Double zero = Double.valueOf(0.0);
            private final Double one = Double.valueOf(1.0);

            @Override
            public Double toJava(LuaValue value) {
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
        putLuaToJava(Double.TYPE, doubleCoercion);
        putLuaToJava(Double.class, doubleCoercion);

        ILuaToJava<String> stringCoercion = new ILuaToJava<String>() {
            @Override
            public @Nullable String toJava(LuaValue value) {
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
        putLuaToJava(String.class, stringCoercion);

        ILuaToJava<Object> objectCoercion = new ILuaToJava<Object>() {
            @Override
            public @Nullable Object toJava(LuaValue value) {
                if (value instanceof LuaUserdata) {
                    return ((LuaUserdata)value).userdata();
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
        putLuaToJava(Object.class, objectCoercion);
    }

    private void putJavaToLua(Class<?> javaType, IJavaToLua toLuaFunc) {
        javaToLuaCoercions.put(javaType, toLuaFunc);
    }

    private <T> void putLuaToJava(Class<T> javaType, ILuaToJava<T> toJavaFunc) {
        luaToJavaCoercions.put(javaType, toJavaFunc);
    }

    /**
     * Returns the registered type coercion for converting Java objects of the given type to Lua values.
     *
     * @return The type coercion, or {@code null} if not found.
     */
    public IJavaToLua findJavaToLua(Class<?> javaType) {
        return javaToLuaCoercions.get(javaType);
    }

    /**
     * Returns the registered type coercion for converting Lua objects to Java objects of the given type.
     *
     * @return The type coercion, or {@code null} if not found.
     */
    public <T> ILuaToJava<T> findLuaToJava(Class<T> javaType) {
        // This cast is safe -- coercions are stored mapped from Class<T> -> ILuaToJava<T>
        @SuppressWarnings("unchecked")
        ILuaToJava<T> typed = (ILuaToJava<T>)luaToJavaCoercions.get(javaType);
        return typed;
    }

}
