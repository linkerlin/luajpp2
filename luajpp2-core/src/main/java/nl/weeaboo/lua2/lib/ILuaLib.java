package nl.weeaboo.lua2.lib;

import java.io.Serializable;

import nl.weeaboo.lua2.LuaException;

/**
 * Library of Lua functions.
 */
public interface ILuaLib extends Serializable {

    /**
     * Loads the library.
     * @throws LuaException If something goes wrong.
     */
    void register() throws LuaException;

}
