package nl.weeaboo.lua2.lib2;

import java.io.Serializable;

import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.vm.LuaTable;

public interface ILuaLib extends Serializable {

    void register(LuaTable globals) throws LuaException;

}
