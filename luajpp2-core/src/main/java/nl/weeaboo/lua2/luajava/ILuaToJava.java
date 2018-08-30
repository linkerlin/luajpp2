package nl.weeaboo.lua2.luajava;

import javax.annotation.Nullable;

import nl.weeaboo.lua2.vm.LuaValue;

interface ILuaToJava<T> {

    @Nullable T toJava(LuaValue value);

    int score(LuaValue value);

}