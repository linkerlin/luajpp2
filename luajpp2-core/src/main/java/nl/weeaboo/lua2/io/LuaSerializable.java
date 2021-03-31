package nl.weeaboo.lua2.io;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Whitelists the annotated class to allow it to be serialized as part of the Lua VM state.
 *
 * @see ObjectSerializer#setAllowedClasses(java.util.Collection)
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface LuaSerializable {

}
