package nl.weeaboo.lua2.compiler;

import java.io.IOException;
import java.io.InputStream;

import nl.weeaboo.lua2.vm.LuaClosure;
import nl.weeaboo.lua2.vm.LuaValue;

/**
 * Interface for the compiler, if it is installed.
 * <p>
 * See the {@link LuaClosure} documentation for examples of how to use the compiler.
 *
 * @see LuaClosure
 * @see #load(InputStream, String, LuaValue)
 */
public interface LuaCompiler {

    /**
     * Load into a Closure or LuaFunction from a Stream and initializes the environment.
     */
    LuaClosure load(InputStream stream, String filename, LuaValue env) throws IOException;

}