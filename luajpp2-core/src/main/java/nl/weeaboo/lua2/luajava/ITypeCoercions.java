package nl.weeaboo.lua2.luajava;

import java.io.Serializable;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import nl.weeaboo.lua2.LuaRunState;
import nl.weeaboo.lua2.vm.LuaValue;

/**
 * Converts values between Java and Lua.
 */
public interface ITypeCoercions extends Serializable {

    static ITypeCoercions getCurrent() {
        LuaRunState lrs = LuaRunState.getCurrent();
        if (lrs == null) {
            return getDefault();
        }
        return lrs.getTypeCoercions();
    }

    static ITypeCoercions getDefault() {
        return DefaultTypeCoercions.INSTANCE;
    }

    /**
     * @see #toLua(Object, Class)
     */
    <T> LuaValue toLua(@Nullable T javaValue);

    /**
     * Converts a Java objects to its equivalent Lua value.
     *
     * @param declaredType This determines which interface/class methods are available to Lua. This can be
     *        used to avoid accidentally too many methods to Lua. For example, when a Java method returns an
     *        interface you'd want Lua to only have access to the methods in that interface and not also all
     *        methods available in whatever the runtime type of the returned value is.
     */
    <T> LuaValue toLua(@Nullable T javaValue, Class<?> declaredType);

    /**
     * Casts or converts the given Lua value to the given Java type.
     */
    @CheckForNull
    <T> T toJava(LuaValue luaValue, Class<T> javaType);

    /**
     * Judges how well the given Lua and Java parameters match.
     *
     * @return The similarity score, lower scores are better matches
     */
    int scoreParam(LuaValue arg, Class<?> class1);

}
