package nl.weeaboo.lua2.compiler;

final class BlockCnt {

    BlockCnt previous; /* chain */
    IntPtr breaklist = new IntPtr(); /* list of jumps out of this loop */
    short nactvar; /* # active locals outside the breakable structure */
    boolean upval; /* true if some variable in the block is an upvalue */
    boolean isbreakable; /* true if `block' is a loop */

}