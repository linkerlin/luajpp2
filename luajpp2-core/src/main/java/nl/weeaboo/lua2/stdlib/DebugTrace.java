package nl.weeaboo.lua2.stdlib;

import static nl.weeaboo.lua2.vm.LuaValue.valueOf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.vm.Lua;
import nl.weeaboo.lua2.vm.LuaConstants;
import nl.weeaboo.lua2.vm.LuaStackTraceElement;
import nl.weeaboo.lua2.vm.LuaString;
import nl.weeaboo.lua2.vm.LuaThread;
import nl.weeaboo.lua2.vm.LuaValue;
import nl.weeaboo.lua2.vm.Prototype;

/**
 * Provides access to debugging information for the currently executing Lua thread.
 */
public final class DebugTrace {

    private static final LuaString QMARK = valueOf("?");
    private static final LuaString GLOBAL = valueOf("global");
    private static final LuaString LOCAL = valueOf("local");
    private static final LuaString METHOD = valueOf("method");
    private static final LuaString UPVALUE = valueOf("upvalue");
    private static final LuaString FIELD = valueOf("field");

    private DebugTrace() {
    }

    /**
     * Get a traceback for a particular thread.
     *
     * @param thread LuaThread to provide stack trace for
     * @param level 0-based level to start reporting on
     * @return String containing the stack trace.
     *
     * @see #stackTrace(LuaThread)
     */
    public static String traceback(LuaThread thread, int level) {
        DebugState ds = DebugLib.getDebugState(thread);

        StringBuilder sb = new StringBuilder();
        sb.append("stack traceback:");
        DebugInfo di = ds.getDebugInfo(level);
        if (di != null) {
            sb.append("\n\t");
            sb.append(di.sourceline());
            sb.append(" in ");
            while ((di = ds.getDebugInfo(++level)) != null) {
                sb.append(di.tracename());
                sb.append("\n\t");
                sb.append(di.sourceline());
                sb.append(" in ");
            }
            sb.append("main chunk");
        }
        return sb.toString();
    }

    /**
     * @deprecated For internal use only.
     */
    @Deprecated
    public static StackTraceElement[] getStackTrace(LuaThread thread, int levelOffset, int count) {
        if (thread == null) {
            return new StackTraceElement[0];
        }

        DebugState ds = DebugLib.getDebugState(thread);

        List<StackTraceElement> out = new ArrayList<>();
        for (int level = 1; level <= count; level++) {
            DebugInfo di = ds.getDebugInfo(levelOffset + level);
            if (di == null) {
                break;
            }
            out.add(new StackTraceElement("Lua", di.tracename(), di.source(), di.currentline()));
        }
        return out.toArray(new StackTraceElement[out.size()]);
    }

    /**
     * Get file and line for the nearest calling closure.
     *
     * @return String identifying the file and line of the nearest lua closure, or the function name of the
     *         Java call if no closure is being called.
     * @deprecated Use {@link #stackTraceElem(LuaThread, int)} instead.
     */
    @Deprecated
    public static @Nullable String fileline() {
        LuaThread running = LuaThread.getRunning();
        DebugState ds = DebugLib.getDebugState(running);
        int limit = ds.debugCalls;
        for (int level = 1; level <= limit; level++) {
            DebugInfo di = ds.getDebugInfo(level);
            if (di != null && di.closure != null) {
                return di.sourceline();
            }
        }
        return fileline(1);
    }

    /**
     * @see #fileline(LuaThread, int)
     * @deprecated Use {@link #stackTraceElem(LuaThread, int)} instead.
     */
    @Deprecated
    public static @Nullable String fileline(int level) {
        return fileline(LuaThread.getRunning(), level);
    }

    /**
     * Get file and line for a particular level, even if it is a java function.
     *
     * @param level 1-based index of level to get
     * @return String containing file and line info if available
     * @deprecated Use {@link #stackTraceElem(LuaThread, int)} instead.
     */
    @Deprecated
    public static @Nullable String fileline(LuaThread running, int level) {
        DebugState ds = DebugLib.getDebugState(running);
        DebugInfo di = ds.getDebugInfo(level);

        if (di == null) {
            return null;
        }
        return di.sourceline();
    }

    /**
     * Returns the Lua call stack of the given thread.
     */
    public static List<LuaStackTraceElement> stackTrace(LuaThread thread) {
        return stackTrace(thread, 0, 8);
    }

    /**
     * Returns the Lua call stack of the given thread.
     *
     * @param offset Skip the deepest {@code offset} levels of the call stack.
     * @param count Traverse at most this number of levels.
     */
    public static List<LuaStackTraceElement> stackTrace(LuaThread thread, int offset, int count) {
        List<LuaStackTraceElement> result = new ArrayList<>();
        for (int n = 0; n < count; n++) {
            result.add(stackTraceElem(thread, offset + n));
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Returns the stack trace element at the given offset.
     * @param offset Skip the deepest {@code offset} levels of the call stack.
     */
    public static @Nullable LuaStackTraceElement stackTraceElem(LuaThread thread, int offset) {
        DebugState ds = DebugLib.getDebugState(thread);
        DebugInfo di = ds.getDebugInfo(1 + offset);
        if (di == null) {
            return null;
        }
        return di.getStackTraceElement();
    }

    private static void lua_assert(boolean x) {
        if (!x) {
            throw new LuaException("lua_assert failed");
        }
    }

    /**
     * The name of the function that the given thread is currently in the process of calling. A function can
     * be known by multiple names. This is the name used to call the function in this particular case.
     */
    public static String getCalledFunctionName(LuaThread thread) {
        DebugState ds = DebugLib.getDebugState(thread);
        if (ds != null) {
            DebugInfo di = ds.getDebugInfo();
            if (di != null) {
                return di.getObjectName();
            }
        }
        return "?";
    }

    /**
     * @return StrValue[] { name, namewhat } if found, null if not
     */
    public static @Nullable LuaString[] getobjname(LuaValue value) {
        LuaThread thread = LuaThread.getRunning();
        DebugState ds = DebugLib.getDebugState(thread);
        if (ds == null) {
            return null;
        }

        DebugInfo di = ds.getDebugInfo();
        if (di == null) {
            return null;
        }

        // Search the stack for this object. If not found, search at the current stack pos.
        int stackPos = di.getStackPos();
        if (!value.isnil()) {
            /*
             * In the C implementation of Lua, 'objects' are actually passed along as pointers to the stack.
             * As a result, every objects corresponds to a unique stack index. In this Java implementation,
             * every NIL is the same instance. That's good for performance, but it also means that at this
             * point in the code we can no longer distinguish between different NIL instances.
             */
            for (int n = 0; n < di.stack.length; n++) {
                if (di.stack[n] == value) {
                    stackPos = n;
                    break;
                }
            }
        }
        return getobjname(di, stackPos);
    }

    /**
     * @return StrValue[] { name, namewhat } if found, null if not
     */
    static @Nullable LuaString[] getobjname(DebugInfo di, int stackpos) {
        if (di.closure == null || stackpos < 0) {
            return null; // Not a Lua function or stack pos invalid
        }

        Prototype p = di.closure.getPrototype();
        int pc = di.pc; // currentpc(L, ci);
        LuaString name = p.getlocalname(stackpos + 1, pc);
        if (name != null) { /* is a local? */
            return new LuaString[] { name, LOCAL };
        }

        int i = symbexec(p, pc, stackpos); /* try symbolic execution */
        lua_assert(pc != -1);
        switch (Lua.getOpcode(i)) {
        case Lua.OP_GETGLOBAL: {
            int g = Lua.getArgBx(i); /* global index */
            // lua_assert(p.k[g].isString());
            return new LuaString[] { p.k[g].strvalue(), GLOBAL };
        }
        case Lua.OP_MOVE: {
            int a = Lua.getArgA(i);
            int b = Lua.getArgB(i); /* move from `b' to `a' */
            if (b < a) {
                return getobjname(di, b); /* get name for `b' */
            }
            break;
        }
        case Lua.OP_GETTABLE: {
            int k = Lua.getArgC(i); /* key index */
            name = kname(p, k);
            return new LuaString[] { name, FIELD };
        }
        case Lua.OP_GETUPVAL: {
            int u = Lua.getArgB(i); /* upvalue index */
            name = u < p.upvalues.length ? p.upvalues[u] : QMARK;
            return new LuaString[] { name, UPVALUE };
        }
        case Lua.OP_SELF: {
            int k = Lua.getArgC(i); /* key index */
            name = kname(p, k);
            return new LuaString[] { name, METHOD };
        }
        }
        return null; /* no useful name found */
    }

    private static LuaString kname(Prototype p, int c) {
        if (Lua.isK(c) && p.k[Lua.getIndexK(c)].isstring()) {
            return p.k[Lua.getIndexK(c)].strvalue();
        } else {
            return QMARK;
        }
    }

    private static boolean checkreg(Prototype pt, int reg) {
        return (reg < pt.maxstacksize);
    }

    private static boolean precheck(Prototype pt) {
        if (!(pt.maxstacksize <= LuaConstants.MAXSTACK)) {
            return false;
        }
        lua_assert(pt.numparams + (pt.isVararg & Lua.VARARG_HASARG) <= pt.maxstacksize);
        lua_assert((pt.isVararg & Lua.VARARG_NEEDSARG) == 0 || (pt.isVararg & Lua.VARARG_HASARG) != 0);
        if (!(pt.upvalues.length <= pt.nups)) {
            return false;
        }
        if (!(pt.lineinfo.length == pt.code.length || pt.lineinfo.length == 0)) {
            return false;
        }
        if (!(Lua.getOpcode(pt.code[pt.code.length - 1]) == Lua.OP_RETURN)) {
            return false;
        }
        return true;
    }

    private static boolean checkopenop(Prototype pt, int pc) {
        int i = pt.code[pc + 1];

        switch (Lua.getOpcode(i)) {
        case Lua.OP_CALL:
        case Lua.OP_TAILCALL:
        case Lua.OP_RETURN:
        case Lua.OP_SETLIST: {
            return Lua.getArgB(i) == 0;
        }
        default:
            return false; /* invalid instruction after an open call */
        }
    }

    // static int checkArgMode (Prototype pt, int r, enum OpArgMask mode) {
    private static boolean checkArgMode(Prototype pt, int r, int mode) {
        switch (mode) {
        case Lua.OpArgN:
            if (!(r == 0)) {
                return false;
            }
            break;
        case Lua.OpArgU:
            break;
        case Lua.OpArgR:
            checkreg(pt, r);
            break;
        case Lua.OpArgK:
            if (!(Lua.isK(r) ? Lua.getIndexK(r) < pt.k.length : r < pt.maxstacksize)) {
                return false;
            }
            break;
        }
        return true;
    }

    // return last instruction, or 0 if error
    private static int symbexec(Prototype pt, int lastpc, int reg) {
        int last; /* stores position of last instruction that changed `reg' */
        last = pt.code.length - 1; /*
                                    * points to final return (a `neutral' instruction)
                                    */
        if (!precheck(pt)) {
            return 0;
        }
        for (int pc = 0; pc < lastpc; pc++) {
            int i = pt.code[pc];
            int op = Lua.getOpcode(i);
            int a = Lua.getArgA(i);
            int b = 0;
            int c = 0;
            if (!(op < Lua.NUM_OPCODES)) {
                return 0;
            }
            if (!checkreg(pt, a)) {
                return 0;
            }
            switch (Lua.getOpMode(op)) {
            case Lua.iABC: {
                b = Lua.getArgB(i);
                c = Lua.getArgC(i);
                if (!checkArgMode(pt, b, Lua.getBMode(op))) {
                    return 0;
                }
                if (!checkArgMode(pt, c, Lua.getCMode(op))) {
                    return 0;
                }
                break;
            }
            case Lua.iABx: {
                b = Lua.getArgBx(i);
                if (Lua.getBMode(op) == Lua.OpArgK && !(b < pt.k.length)) {
                    return 0;
                }
                break;
            }
            case Lua.iAsBx: {
                b = Lua.getArgSBx(i);
                if (Lua.getBMode(op) == Lua.OpArgR) {
                    int dest = pc + 1 + b;
                    if (!(0 <= dest && dest < pt.code.length)) {
                        return 0;
                    }
                    if (dest > 0) {
                        /* cannot jump to a setlist count */
                        int d = pt.code[dest - 1];
                        if (Lua.getOpcode(d) == Lua.OP_SETLIST && Lua.getArgC(d) == 0) {
                            return 0;
                        }
                    }
                }
                break;
            }
            }

            if (Lua.testAMode(op)) {
                if (a == reg) {
                    last = pc; /* change register `a' */
                }
            }
            if (Lua.testTMode(op)) {
                if (!(pc + 2 < pt.code.length)) {
                    return 0; /* check skip */
                }
                if (!(Lua.getOpcode(pt.code[pc + 1]) == Lua.OP_JMP)) {
                    return 0;
                }
            }
            switch (op) {
            case Lua.OP_LOADBOOL: {
                if (!(c == 0 || pc + 2 < pt.code.length)) {
                    return 0; /*
                                                                         * check its jump
                                                                         */
                }
                break;
            }
            case Lua.OP_LOADNIL: {
                if (a <= reg && reg <= b) {
                    last = pc; /*
                                                          * set registers from `a' to `b'
                                                          */
                }
                break;
            }
            case Lua.OP_GETUPVAL:
            case Lua.OP_SETUPVAL: {
                if (!(b < pt.nups)) {
                    return 0;
                }
                break;
            }
            case Lua.OP_GETGLOBAL:
            case Lua.OP_SETGLOBAL: {
                if (!pt.k[b].isstring()) {
                    return 0;
                }
                break;
            }
            case Lua.OP_SELF: {
                if (!checkreg(pt, a + 1)) {
                    return 0;
                }
                if (reg == a + 1) {
                    last = pc;
                }
                break;
            }
            case Lua.OP_CONCAT: {
                if (!(b < c)) {
                    return 0; /* at least two operands */
                }
                break;
            }
            case Lua.OP_TFORLOOP: {
                if (!(c >= 1)) {
                    return 0; /*
                                              * at least one result (control variable)
                                              */
                }
                if (!checkreg(pt, a + 2 + c)) {
                    return 0; /* space for results */
                }
                if (reg >= a + 2) {
                    last = pc; /* affect all regs above its base */
                }
                break;
            }
            case Lua.OP_FORLOOP:
            case Lua.OP_FORPREP:
                if (!checkreg(pt, a + 3)) {
                    return 0;
                }
                /* go through */
                //$FALL-THROUGH$
            case Lua.OP_JMP: {
                int dest = pc + 1 + b;
                /* not full check and jump is forward and do not skip `lastpc'? */
                if (reg != Lua.NO_REG && pc < dest && dest <= lastpc) {
                    pc += b; /*
                              * do the jump
                              */
                }
                break;
            }
            case Lua.OP_CALL:
            case Lua.OP_TAILCALL: {
                if (b != 0) {
                    if (!checkreg(pt, a + b - 1)) {
                        return 0;
                    }
                }
                c--; /* c = num. returns */
                if (c == Lua.LUA_MULTRET) {
                    if (!checkopenop(pt, pc)) {
                        return 0;
                    }
                } else if (c != 0) {
                    if (!checkreg(pt, a + c - 1)) {
                        return 0;
                    }
                }
                if (reg >= a) {
                    last = pc; /* affect all registers above base */
                }
                break;
            }
            case Lua.OP_RETURN: {
                b--; /* b = num. returns */
                if (b > 0) {
                    if (!checkreg(pt, a + b - 1)) {
                        return 0;
                    }
                }
                break;
            }
            case Lua.OP_SETLIST: {
                if (b > 0) {
                    if (!checkreg(pt, a + b)) {
                        return 0;
                    }
                }
                if (c == 0) {
                    pc++;
                }
                break;
            }
            case Lua.OP_CLOSURE: {
                if (b >= pt.p.length) {
                    return 0;
                }
                int nup = pt.p[b].nups;
                if (!(pc + nup < pt.code.length)) {
                    return 0;
                }
                for (int j = 1; j <= nup; j++) {
                    int op1 = Lua.getOpcode(pt.code[pc + j]);
                    if (!(op1 == Lua.OP_GETUPVAL || op1 == Lua.OP_MOVE)) {
                        return 0;
                    }
                }
                if (reg != Lua.NO_REG) {
                    pc += nup; /* do not 'execute' these pseudo-instructions */
                }
                break;
            }
            case Lua.OP_VARARG: {
                if (!((pt.isVararg & Lua.VARARG_ISVARARG) != 0 && (pt.isVararg & Lua.VARARG_NEEDSARG) == 0)) {
                    return 0;
                }
                b--;
                if (b == Lua.LUA_MULTRET) {
                    if (!checkopenop(pt, pc)) {
                        return 0;
                    }
                }
                if (!checkreg(pt, a + b - 1)) {
                    return 0;
                }
                break;
            }
            default:
                break;
            }
        }
        return pt.code[last];
    }

}
