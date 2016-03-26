package org.luaj.vm2;

import static org.luaj.vm2.LuaValue.valueOf;

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
     * String array constant containing names of each of the lua value types
     *
     * @see #type()
     * @see #typename()
     */
    public static final String[] TYPE_NAMES = { "nil", "boolean", "lightuserdata", "number", "string",
            "table", "function", "userdata", "thread", "value", };

    /** LuaValue constant corresponding to a {@link Varargs} list of no values */
    public static final LuaValue NONE = None.NONE;

    /** LuaValue array constant with no values */
    public static final LuaValue[] NOVALS = {};

    /** LuaString constant with value "__index" for use as metatag */
    public static final LuaString INDEX = valueOf("__index");

    /** LuaString constant with value "__newindex" for use as metatag */
    public static final LuaString NEWINDEX = valueOf("__newindex");

    /** LuaString constant with value "__call" for use as metatag */
    public static final LuaString CALL = valueOf("__call");

    /** LuaString constant with value "__mode" for use as metatag */
    public static final LuaString MODE = valueOf("__mode");

    /** LuaString constant with value "__metatable" for use as metatag */
    public static final LuaString METATABLE = valueOf("__metatable");

    /** LuaString constant with value "__add" for use as metatag */
    public static final LuaString ADD = valueOf("__add");

    /** LuaString constant with value "__sub" for use as metatag */
    public static final LuaString SUB = valueOf("__sub");

    /** LuaString constant with value "__div" for use as metatag */
    public static final LuaString DIV = valueOf("__div");

    /** LuaString constant with value "__mul" for use as metatag */
    public static final LuaString MUL = valueOf("__mul");

    /** LuaString constant with value "__pow" for use as metatag */
    public static final LuaString POW = valueOf("__pow");

    /** LuaString constant with value "__mod" for use as metatag */
    public static final LuaString MOD = valueOf("__mod");

    /** LuaString constant with value "__unm" for use as metatag */
    public static final LuaString UNM = valueOf("__unm");

    /** LuaString constant with value "__len" for use as metatag */
    public static final LuaString LEN = valueOf("__len");

    /** LuaString constant with value "__eq" for use as metatag */
    public static final LuaString EQ = valueOf("__eq");

    /** LuaString constant with value "__lt" for use as metatag */
    public static final LuaString LT = valueOf("__lt");

    /** LuaString constant with value "__le" for use as metatag */
    public static final LuaString LE = valueOf("__le");

    /** LuaString constant with value "__tostring" for use as metatag */
    public static final LuaString TOSTRING = valueOf("__tostring");

    /** LuaString constant with value "__concat" for use as metatag */
    public static final LuaString CONCAT = valueOf("__concat");

    /** LuaString constant with value "" */
    public static final LuaString EMPTYSTRING = valueOf("");

    /** Limit on lua stack size */
    public static int MAXSTACK = 250;

}
