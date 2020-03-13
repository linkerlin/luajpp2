package nl.weeaboo.lua2.vm;

/**
 * State of a {@link LuaThread}
 */
public enum LuaThreadStatus {

    INITIAL,
    SUSPENDED,
    RUNNING,
    END_CALL,
    ERROR,
    DEAD;

}
