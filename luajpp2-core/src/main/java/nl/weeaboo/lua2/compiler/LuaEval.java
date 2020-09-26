package nl.weeaboo.lua2.compiler;

import java.io.IOException;

import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.vm.LuaClosure;
import nl.weeaboo.lua2.vm.LuaConstants;
import nl.weeaboo.lua2.vm.LuaTable;
import nl.weeaboo.lua2.vm.LuaThread;
import nl.weeaboo.lua2.vm.Prototype;
import nl.weeaboo.lua2.vm.ThreadEvalEnv;
import nl.weeaboo.lua2.vm.Varargs;

/**
 * Provides a convenient way to compile and run additional Lua code in the context of an existing thread.
 */
public final class LuaEval {

    /**
     * Compiles a snippet of Lua code as a runnable closure.
     * @throws LuaException If an error occurs while trying to compile the code.
     */
    public static LuaClosure compileForEval(String code, LuaThread thread) throws LuaException {
        final String chunkName = "(eval)";
        try {
            LuaC compiler = new LuaC();

            Prototype p;
            try {
                // Try to evaluate as an expression
                p = compiler.compileLua("return " + code, chunkName);
            } catch (LuaException err) {
                // Try to evaluate as a statement, no value to return
                p = compiler.compileLua(code, chunkName);
            }

            LuaTable mt = new LuaTable();
            mt.rawset(LuaConstants.META_INDEX, new ThreadEvalEnv(thread));

            LuaTable evalEnvTable = new LuaTable();
            evalEnvTable.setmetatable(mt);
            return new LuaClosure(p, evalEnvTable);
        } catch (RuntimeException e) {
            throw LuaException.wrap("Error compiling code", e);
        } catch (IOException e) {
            throw LuaException.wrap("Error compiling code", e);
        }
    }

    /**
     * Compiles and runs a piece of Lua code in the given thread.
     * @throws LuaException If an error occurs while trying to compare or run the code.
     */
    public static Varargs eval(LuaThread thread, String code) throws LuaException {
        LuaClosure function = compileForEval(code, thread);
        return thread.callFunctionInThread(function, LuaConstants.NONE);
    }


}
