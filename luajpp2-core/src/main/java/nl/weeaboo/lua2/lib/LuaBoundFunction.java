package nl.weeaboo.lua2.lib;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark Java methods that should be made available to Lua.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
public @interface LuaBoundFunction {

    /**
     * The name of the function in Lua. If omitted, the Lua name is equal to the method name in Java.
     */
    String luaName() default "";

    /**
     * If {@code true}, the function will be bound as a global function rather than as part of its lib table.
     */
    boolean global() default false;
}
