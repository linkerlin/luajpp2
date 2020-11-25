package nl.weeaboo.lua2;

import java.io.Serializable;

import nl.weeaboo.lua2.vm.LuaThread;

/**
 * Catches exceptions thrown from Lua scripts.
 */
public interface ILuaExceptionHandler extends Serializable {

    /**
     * Called when script thread execution throws an exception.
     */
    void onScriptException(LuaThread thread, Exception exception);

}
