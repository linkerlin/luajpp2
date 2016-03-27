package nl.weeaboo.lua2.vm;

import static nl.weeaboo.lua2.vm.LuaValue.valueOf;

public class LuaConstants {

    public static final String ENGINE_VERSION = "LuaJPP2";

    /**
     * Type enumeration constant for lua numbers that are ints, for compatibility with lua 5.1 number patch
     * only
     */
    public static final int TINT = (-2);

    /**
     * Type enumeration constant for lua values that have no type, for example weak table entries
     */
    public static final int TNONE = (-1);

    /** Type enumeration constant for lua nil */
    public static final int TNIL = 0;

    /** Type enumeration constant for lua booleans */
    public static final int TBOOLEAN = 1;

    /**
     * Type enumeration constant for lua light userdata, for compatibility with C-based lua only
     */
    public static final int TLIGHTUSERDATA = 2;

    /** Type enumeration constant for lua numbers */
    public static final int TNUMBER = 3;

    /** Type enumeration constant for lua strings */
    public static final int TSTRING = 4;

    /** Type enumeration constant for lua tables */
    public static final int TTABLE = 5;

    /** Type enumeration constant for lua functions */
    public static final int TFUNCTION = 6;

    /** Type enumeration constant for lua userdatas */
    public static final int TUSERDATA = 7;

    /** Type enumeration constant for lua threads */
    public static final int TTHREAD = 8;

    /**
     * Type enumeration constant for unknown values, for compatibility with C-based lua only
     */
    public static final int TVALUE = 9;

    /**
     * format corresponding to non-number-patched lua, all numbers are floats or doubles
     */
    public static final int NUMBER_FORMAT_FLOATS_OR_DOUBLES = 0;

    /** format corresponding to non-number-patched lua, all numbers are ints */
    public static final int NUMBER_FORMAT_INTS_ONLY = 1;

    /**
     * format corresponding to number-patched lua, all numbers are 32-bit (4 byte) ints
     */
    public static final int NUMBER_FORMAT_NUM_PATCH_INT32 = 4;

    /** LuaValue constant corresponding to a {@link Varargs} list of no values */
    public static final LuaValue NONE = None.NONE;

    /** LuaValue array constant with no values */
    public static final LuaValue[] NOVALS = {};

    public static final LuaString INDEX = valueOf("__index");
    public static final LuaString NEWINDEX = valueOf("__newindex");
    public static final LuaString CALL = valueOf("__call");
    public static final LuaString MODE = valueOf("__mode");
    public static final LuaString METATABLE = valueOf("__metatable");
    public static final LuaString ADD = valueOf("__add");
    public static final LuaString SUB = valueOf("__sub");
    public static final LuaString DIV = valueOf("__div");
    public static final LuaString MUL = valueOf("__mul");
    public static final LuaString POW = valueOf("__pow");
    public static final LuaString MOD = valueOf("__mod");
    public static final LuaString UNM = valueOf("__unm");
    public static final LuaString LEN = valueOf("__len");
    public static final LuaString EQ = valueOf("__eq");
    public static final LuaString LT = valueOf("__lt");
    public static final LuaString LE = valueOf("__le");
    public static final LuaString TOSTRING = valueOf("__tostring");
    public static final LuaString CONCAT = valueOf("__concat");
    public static final LuaString EMPTYSTRING = valueOf("");

    /** Limit on lua stack size */
    public static int MAXSTACK = 250;

}
