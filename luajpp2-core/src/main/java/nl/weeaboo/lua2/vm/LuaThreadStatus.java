package nl.weeaboo.lua2.vm;

public enum LuaThreadStatus {

    INITIAL,
    SUSPENDED,
    RUNNING,
    END_CALL,
    ERROR,
    DEAD;

}
