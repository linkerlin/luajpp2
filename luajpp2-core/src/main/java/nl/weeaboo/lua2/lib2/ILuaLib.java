package nl.weeaboo.lua2.lib2;

import java.io.Serializable;

import nl.weeaboo.lua2.LuaException;

public interface ILuaLib extends Serializable {

    void register() throws LuaException;

}
