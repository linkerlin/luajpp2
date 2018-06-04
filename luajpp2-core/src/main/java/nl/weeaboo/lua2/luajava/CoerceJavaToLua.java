package nl.weeaboo.lua2.luajava;

import static nl.weeaboo.lua2.vm.LuaNil.NIL;

import nl.weeaboo.lua2.vm.LuaValue;
import nl.weeaboo.lua2.vm.Varargs;

public final class CoerceJavaToLua {

    private CoerceJavaToLua() {
    }

    /**
     * Coerces multiple Java objects to their equivalent Lua values (automatically tries to deduce their
     * types).
     *
     * @see #coerce(Object)
     */
    public static Varargs coerceArgs(Object[] values) {
        LuaValue[] luaArgs = new LuaValue[values.length];
        for (int n = 0; n < luaArgs.length; n++) {
            luaArgs[n] = coerce(values[n]);
        }
        return LuaValue.varargsOf(luaArgs);
    }

    /**
     * Coerces a single Java object to its equivalent Lua value (automatically tries to deduce a type).
     *
     * @see #coerce(Object, Class)
     */
    public static LuaValue coerce(Object obj) {
        if (obj == null) {
            return NIL;
        }
        return coerce(obj, obj.getClass());
    }

    /**
     * Coerces a Java objects to its equivalent Lua value.
     *
     * @param declaredType This determines which interface/class methods are available to Lua. This can be
     *        used to avoid accidentally too many methods to Lua. For example, when a Java method returns an
     *        interface you'd want Lua to only have access to the methods in that interface and not also all
     *        methods available in whatever the runtime type of the returned value is.
     */
    public static LuaValue coerce(Object obj, Class<?> declaredType) {
        if (obj == null) {
            return NIL;
        }

        TypeCoercions typeCoercions = TypeCoercions.getInstance();
        IJavaToLua javaToLua = typeCoercions.findJavaToLua(declaredType);
        if (javaToLua != null) {
            // A specialized coercion was found, use it
            return javaToLua.toLua(obj);
        }

        if (LuaValue.class.isAssignableFrom(declaredType)) {
            // Java object is a Lua type
            return (LuaValue)obj;
        }

        // Use the general Java Object -> Lua conversion
        return LuajavaLib.toUserdata(obj, declaredType);
    }

}
