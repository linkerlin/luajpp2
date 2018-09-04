package nl.weeaboo.lua2.luajava;

import nl.weeaboo.lua2.vm.LuaValue;

interface IJavaToLua {

    LuaValue toLua(Object javaValue);

}