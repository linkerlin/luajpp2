package nl.weeaboo.lua2.luajava;

import nl.weeaboo.lua2.vm.LuaValue;

interface ILuaToJava<T> {

    T toJava(LuaValue value);

    int score(LuaValue value);

}