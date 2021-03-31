package nl.weeaboo.lua2.compiler;

final class BlockCnt {

    /** chain */
    BlockCnt previous;

    /** list of jumps out of this loop */
    IntPtr breakList = new IntPtr();

    /** # active locals outside the breakable structure */
    short activeLocalVarCount;

    /** true if some variable in the block is an upvalue */
    boolean containsUpValue;

    /** true if `block' is a loop */
    boolean isBreakable;

}