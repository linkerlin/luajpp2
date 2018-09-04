package nl.weeaboo.lua2.vm;

/**
 * Subclass of Slot guaranteed to have a strongly-referenced key and value, to support weak tables.
 */
interface IStrongSlot extends ISlot {

    /** Return first entry's key */
    LuaValue key();

    /** Return first entry's value */
    LuaValue value();

    /** Return varargsOf(key(), value()) or equivalent */
    Varargs toVarargs();
}