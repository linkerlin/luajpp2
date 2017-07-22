/*******************************************************************************
 * Copyright (c) 2009-2011 Luaj.org. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ******************************************************************************/

package nl.weeaboo.lua2.lib;

import static nl.weeaboo.lua2.vm.LuaBoolean.FALSE;
import static nl.weeaboo.lua2.vm.LuaBoolean.TRUE;
import static nl.weeaboo.lua2.vm.LuaConstants.EMPTYSTRING;
import static nl.weeaboo.lua2.vm.LuaConstants.NONE;
import static nl.weeaboo.lua2.vm.LuaConstants.TBOOLEAN;
import static nl.weeaboo.lua2.vm.LuaConstants.TFUNCTION;
import static nl.weeaboo.lua2.vm.LuaConstants.TNIL;
import static nl.weeaboo.lua2.vm.LuaConstants.TNUMBER;
import static nl.weeaboo.lua2.vm.LuaConstants.TSTRING;
import static nl.weeaboo.lua2.vm.LuaConstants.TTHREAD;
import static nl.weeaboo.lua2.vm.LuaNil.NIL;

import java.util.ArrayList;
import java.util.List;

import nl.weeaboo.lua2.LuaRunState;
import nl.weeaboo.lua2.compiler.ILuaCompiler;
import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.vm.Lua;
import nl.weeaboo.lua2.vm.LuaBoolean;
import nl.weeaboo.lua2.vm.LuaClosure;
import nl.weeaboo.lua2.vm.LuaConstants;
import nl.weeaboo.lua2.vm.LuaError;
import nl.weeaboo.lua2.vm.LuaFunction;
import nl.weeaboo.lua2.vm.LuaInteger;
import nl.weeaboo.lua2.vm.LuaNil;
import nl.weeaboo.lua2.vm.LuaNumber;
import nl.weeaboo.lua2.vm.LuaString;
import nl.weeaboo.lua2.vm.LuaTable;
import nl.weeaboo.lua2.vm.LuaThread;
import nl.weeaboo.lua2.vm.LuaValue;
import nl.weeaboo.lua2.vm.Prototype;
import nl.weeaboo.lua2.vm.Varargs;

/**
 * Subclass of {@link LibFunction} which implements the lua standard {@code debug} library.
 * <p>
 * The debug library in luaj tries to emulate the behavior of the corresponding C-based lua library. To do
 * this, it must maintain a separate stack of calls to {@link LuaClosure} and {@link LibFunction} instances.
 * Especially when lua-to-java bytecode compiling is being used via a {@link ILuaCompiler} such as
 * {@code LuaJC}, this cannot be done in all cases.
 * <p>
 * To instantiate and use it directly, link it into your globals table via {@link LuaValue#load(LuaValue)}
 * using code such as:
 *
 * <pre>
 * {
 *     &#064;code
 *     LuaTable _G = new LuaTable();
 *     _G.load(new DebugLib());
 * }
 * </pre>
 *
 * Doing so will ensure the library is properly initialized and loaded into the globals table.
 * <p>
 *
 * @see LibFunction
 * @see <a href="http://www.lua.org/manual/5.1/manual.html#5.9">http://www.lua.org/manual/5.1/manual.html#5.9
 *      </a>
 */
@LuaSerializable
public class DebugLib extends VarArgFunction {

    private static final long serialVersionUID = -8649866434693973510L;

    // leave this unset to allow obfuscators to
    // remove it in production builds
    public static boolean DEBUG_ENABLED;

    private static final String[] NAMES = { "debug", "getfenv", "gethook", "getinfo", "getlocal",
            "getmetatable", "getregistry", "getupvalue", "setfenv", "sethook", "setlocal", "setmetatable",
            "setupvalue", "traceback", };

    private static final int INIT = 0;
    private static final int DEBUG = 1;
    private static final int GETFENV = 2;
    private static final int GETHOOK = 3;
    private static final int GETINFO = 4;
    private static final int GETLOCAL = 5;
    private static final int GETMETATABLE = 6;
    private static final int GETREGISTRY = 7;
    private static final int GETUPVALUE = 8;
    private static final int SETFENV = 9;
    private static final int SETHOOK = 10;
    private static final int SETLOCAL = 11;
    private static final int SETMETATABLE = 12;
    private static final int SETUPVALUE = 13;
    private static final int TRACEBACK = 14;

    private static final LuaString LUA = valueOf("Lua");
    private static final LuaString JAVA = valueOf("Java");
    private static final LuaString QMARK = valueOf("?");
    private static final LuaString GLOBAL = valueOf("global");
    private static final LuaString LOCAL = valueOf("local");
    private static final LuaString METHOD = valueOf("method");
    private static final LuaString UPVALUE = valueOf("upvalue");
    private static final LuaString FIELD = valueOf("field");
    private static final LuaString CALL = valueOf("call");
    private static final LuaString LINE = valueOf("line");
    private static final LuaString COUNT = valueOf("count");
    private static final LuaString RETURN = valueOf("return");

    private static final LuaString FUNC = valueOf("func");
    private static final LuaString NUPS = valueOf("nups");
    private static final LuaString NAME = valueOf("name");
    private static final LuaString NAMEWHAT = valueOf("namewhat");
    private static final LuaString WHAT = valueOf("what");
    private static final LuaString SOURCE = valueOf("source");
    private static final LuaString SHORT_SRC = valueOf("short_src");
    private static final LuaString LINEDEFINED = valueOf("linedefined");
    private static final LuaString LASTLINEDEFINED = valueOf("lastlinedefined");
    private static final LuaString CURRENTLINE = valueOf("currentline");
    private static final LuaString ACTIVELINES = valueOf("activelines");

    public DebugLib() {
    }

    private LuaTable init() {
        DEBUG_ENABLED = true;
        LuaTable t = new LuaTable();
        bind(t, DebugLib.class, NAMES, DEBUG);
        env.set("debug", t);
        LuaRunState.getCurrent().setIsLoaded("debug", t);
        return t;
    }

    @Override
    public Varargs invoke(Varargs args) {
        switch (opcode) {
        case INIT: return init();
        case DEBUG: return _debug();
        case GETFENV: return _getfenv(args);
        case GETHOOK: return _gethook(args);
        case GETINFO: return _getinfo(args, this);
        case GETLOCAL: return _getlocal(args);
        case GETMETATABLE: return _getmetatable(args);
        case GETREGISTRY: return _getregistry();
        case GETUPVALUE: return _getupvalue(args);
        case SETFENV: return _setfenv(args);
        case SETHOOK: return _sethook(args);
        case SETLOCAL: return _setlocal(args);
        case SETMETATABLE: return _setmetatable(args);
        case SETUPVALUE: return _setupvalue(args);
        case TRACEBACK: return _traceback(args);
        default: return NONE;
        }
    }

    private static DebugState getDebugState(LuaThread thread) {
        if (thread.debugState == null) {
            thread.debugState = new DebugState(thread);
        }
        return (DebugState)thread.debugState;
    }

    /** Called by Closures to set up stack and arguments to next call */
    public static void debugSetupCall(LuaThread thread, Varargs args, LuaValue[] stack) {
        DebugState ds = getDebugState(thread);
        if (ds.inhook) {
            return;
        }
        ds.nextInfo().setargs(args, stack);
    }

    /**
     * Called by Closures and recursing java functions on entry
     *
     * @param thread the thread for the call
     * @param calls the number of calls in the call stack
     * @param func the function called
     */
    public static void debugOnCall(LuaThread thread, int calls, LuaFunction func) {
        DebugState ds = getDebugState(thread);

        DebugInfo di = ds.pushInfo(calls);
        di.setfunction(func);

        if (!ds.inhook && ds.hookcall) {
            ds.callHookFunc(CALL, NIL);
        }
    }

    /**
     * Called by Closures and recursing java functions on return
     *
     * @param thread the thread for the call
     * @param calls the number of calls in the call stack
     */
    public static void debugOnReturn(LuaThread thread, int calls) {
        DebugState ds = getDebugState(thread);
        try {
            if (!ds.inhook && ds.hookrtrn) {
                ds.callHookFunc(RETURN, NIL);
            }
        } finally {
            getDebugState(thread).popInfo(calls);
        }
    }

    /** Called by Closures on bytecode execution. */
    public static void debugBytecode(LuaThread thread, int pc, Varargs extras, int top) {
        DebugState ds = getDebugState(thread);
        if (ds.inhook) {
            return;
        }

        DebugInfo di = ds.getDebugInfo();
        if (di == null) {
            return; // No debug information available
        }

        // if (TRACE) {
        // Print.printState(di.closure, pc, di.stack, top, di.varargs);
        // }

        di.pc = pc;
        di.extras = extras;
        di.top = top;

        if (ds.hookcount > 0) {
            ds.hookcodes++;
            if (ds.hookcodes >= ds.hookcount) {
                ds.hookcodes = 0;
                ds.callHookFunc(COUNT, NIL);
            }
        }

        if (ds.hookline) {
            int newline = di.currentline();
            if (newline != ds.line) {
                int c = di.closure.getPrototype().code[pc];
                if ((c & 0x3f) != Lua.OP_JMP || ((c >>> 14) - 0x1ffff) >= 0) {
                    ds.line = newline;
                    ds.callHookFunc(LINE, valueOf(newline));
                }
            }
        }
    }

    private static Varargs _debug() {
        return NONE;
    }

    private static Varargs _gethook(Varargs args) {
        int a = 1;
        LuaThread thread = args.isthread(a) ? args.checkthread(a++) : LuaThread.getRunning();

        DebugState ds = getDebugState(thread);
        return varargsOf(ds.hookfunc,
                valueOf((ds.hookcall ? "c" : "") + (ds.hookline ? "l" : "") + (ds.hookrtrn ? "r" : "")),
                valueOf(ds.hookcount));
    }

    private static Varargs _sethook(Varargs args) {
        int a = 1;
        LuaThread thread = args.isthread(a) ? args.checkthread(a++) : LuaThread.getRunning();
        LuaValue func = args.optfunction(a++, null);
        String str = args.optjstring(a++, "");
        boolean call = str.contains("c");
        boolean line = str.contains("l");
        boolean rtrn = str.contains("r");
        int count = args.optint(a++, 0);

        DebugState ds = getDebugState(thread);
        ds.sethook(func, call, line, rtrn, count);
        return NONE;
    }

    private static Varargs _getfenv(Varargs args) {
        LuaValue object = args.arg1();
        LuaValue env = object.getfenv();
        return (env != null ? env : NIL);
    }

    private static Varargs _setfenv(Varargs args) {
        LuaValue object = args.arg1();
        LuaTable table = args.checktable(2);
        object.setfenv(table);
        return object;
    }

    private static Varargs _getinfo(Varargs args, LuaValue level0func) {
        int a = 1;
        LuaThread thread = args.isthread(a) ? args.checkthread(a++) : LuaThread.getRunning();
        LuaValue func = args.arg(a++);
        String what = args.optjstring(a++, "nSluf");

        // find the stack info
        DebugState ds = getDebugState(thread);
        DebugInfo di = null;
        if (func.isnumber()) {
            int level = func.checkint();
            di = (level > 0 ? ds.getDebugInfo(level) : new DebugInfo(level0func));
        } else {
            di = ds.findDebugInfo(func.checkfunction());
        }
        if (di == null) {
            return NIL;
        }

        // start a table
        LuaTable info = new LuaTable();
        LuaClosure c = di.closure;
        for (int i = 0, j = what.length(); i < j; i++) {
            switch (what.charAt(i)) {
            case 'S': {
                if (c != null) {
                    Prototype p = c.getPrototype();
                    info.set(WHAT, LUA);
                    info.set(SOURCE, p.source);
                    info.set(SHORT_SRC, valueOf(sourceshort(p)));
                    info.set(LINEDEFINED, valueOf(p.linedefined));
                    info.set(LASTLINEDEFINED, valueOf(p.lastlinedefined));
                } else {
                    String shortName = di.func.tojstring();
                    info.set(WHAT, JAVA);
                    info.set(SOURCE, valueOf("[Java] " + shortName));
                    info.set(SHORT_SRC, valueOf(shortName));
                    info.set(LINEDEFINED, LuaInteger.valueOf(-1));
                    info.set(LASTLINEDEFINED, LuaInteger.valueOf(-1));
                }
                break;
            }
            case 'l': {
                int line = di.currentline();
                info.set(CURRENTLINE, valueOf(line));
                break;
            }
            case 'u': {
                info.set(NUPS, valueOf(c != null ? c.getPrototype().nups : 0));
                break;
            }
            case 'n': {
                LuaString[] kind = di.getfunckind();
                info.set(NAME, kind != null ? kind[0] : QMARK);
                info.set(NAMEWHAT, kind != null ? kind[1] : EMPTYSTRING);
                break;
            }
            case 'f': {
                info.set(FUNC, di.func);
                break;
            }
            case 'L': {
                LuaTable lines = new LuaTable();
                info.set(ACTIVELINES, lines);
                // if ( di.luainfo != null ) {
                // int line = di.luainfo.currentline();
                // if ( line >= 0 )
                // lines.set(1, IntValue.valueOf(line));
                // }
                break;
            }
            }
        }
        return info;
    }

    private static String sourceshort(Prototype p) {
        String name = p.source.tojstring();
        if (name.startsWith("@") || name.startsWith("=")) {
            return name.substring(1);
        } else if (name.startsWith("\033")) {
            return "binary string";
        } else {
            return name;
        }
    }

    private static Varargs _getlocal(Varargs args) {
        int a = 1;
        LuaThread thread = args.isthread(a) ? args.checkthread(a++) : LuaThread.getRunning();
        int level = args.checkint(a++);
        int local = args.checkint(a++);

        DebugState ds = getDebugState(thread);
        DebugInfo di = ds.getDebugInfo(level);
        LuaString name = (di != null ? di.getlocalname(local) : null);
        if (name != null) {
            LuaValue value = di.stack[local - 1];
            return varargsOf(name, value);
        } else {
            return NIL;
        }
    }

    private static Varargs _setlocal(Varargs args) {
        int a = 1;
        LuaThread thread = args.isthread(a) ? args.checkthread(a++) : LuaThread.getRunning();
        int level = args.checkint(a++);
        int local = args.checkint(a++);
        LuaValue value = args.arg(a++);

        DebugState ds = getDebugState(thread);
        DebugInfo di = ds.getDebugInfo(level);
        LuaString name = (di != null ? di.getlocalname(local) : null);
        if (name != null) {
            di.stack[local - 1] = value;
            return name;
        } else {
            return NIL;
        }
    }

    private static LuaValue _getmetatable(Varargs args) {
        LuaValue object = args.arg(1);
        LuaValue mt = object.getmetatable();
        return (mt != null ? mt : NIL);
    }

    private static Varargs _setmetatable(Varargs args) {
        LuaValue object = args.arg(1);
        try {
            LuaValue mt = args.opttable(2, null);
            switch (object.type()) {
            case TNIL:
                LuaNil.s_metatable = mt;
                break;
            case TNUMBER:
                LuaNumber.s_metatable = mt;
                break;
            case TBOOLEAN:
                LuaBoolean.s_metatable = mt;
                break;
            case TSTRING:
                LuaString.s_metatable = mt;
                break;
            case TFUNCTION:
                LuaFunction.s_metatable = mt;
                break;
            case TTHREAD:
                LuaThread.s_metatable = mt;
                break;
            default:
                object.setmetatable(mt);
            }
            return TRUE;
        } catch (LuaError e) {
            return varargsOf(FALSE, valueOf(e.toString()));
        }
    }

    private static Varargs _getregistry() {
        return new LuaTable();
    }

    private static LuaString findupvalue(LuaClosure c, int up) {
        if (up > 0 && up <= c.getUpValueCount()) {
            Prototype p = c.getPrototype();
            if (p.upvalues != null && up <= p.upvalues.length) {
                return p.upvalues[up - 1];
            } else {
                return valueOf("." + up);
            }
        }
        return null;
    }

    private static Varargs _getupvalue(Varargs args) {
        LuaValue func = args.checkfunction(1);
        int up = args.checkint(2);

        if (func instanceof LuaClosure) {
            LuaClosure c = (LuaClosure)func;
            LuaString name = findupvalue(c, up);
            if (name != null) {
                return varargsOf(name, c.getUpvalue(up - 1));
            }
        }
        return NIL;
    }

    private static LuaValue _setupvalue(Varargs args) {
        LuaValue func = args.checkfunction(1);
        int up = args.checkint(2);
        LuaValue value = args.arg(3);

        if (func instanceof LuaClosure) {
            LuaClosure c = (LuaClosure)func;
            LuaString name = findupvalue(c, up);
            if (name != null) {
                c.setUpvalue(up - 1, value);
                return name;
            }
        }
        return NIL;
    }

    private static LuaValue _traceback(Varargs args) {
        int a = 1;
        LuaThread thread = args.isthread(a) ? args.checkthread(a++) : LuaThread.getRunning();
        String message = args.optjstring(a++, null);
        int level = args.optint(a++, 1);
        String tb = DebugLib.traceback(thread, level);
        return valueOf(message != null ? message + "\n" + tb : tb);
    }

    /**
     * Get a traceback as a string for the current thread
     */
    public static String traceback(int level) {
        return traceback(LuaThread.getRunning(), level);
    }

    /**
     * Get a traceback for a particular thread.
     *
     * @param thread LuaThread to provide stack trace for
     * @param level 0-based level to start reporting on
     * @return String containing the stack trace.
     */
    public static String traceback(LuaThread thread, int level) {
        DebugState ds = getDebugState(thread);

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

    public static StackTraceElement[] getStackTrace(LuaThread thread, int levelOffset, int count) {
        if (thread == null) {
            return new StackTraceElement[0];
        }

        DebugState ds = getDebugState(thread);

        List<StackTraceElement> out = new ArrayList<StackTraceElement>();
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
     */
    public static String fileline() {
        LuaThread running = LuaThread.getRunning();
        DebugState ds = getDebugState(running);
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
     * Get file and line for a particular level, even if it is a java function.
     *
     * @param level 1-based index of level to get
     * @return String containing file and line info if available
     */
    public static String fileline(int level) {
        return fileline(LuaThread.getRunning(), level);
    }

    public static String fileline(LuaThread running, int level) {
        DebugState ds = getDebugState(running);
        DebugInfo di = ds.getDebugInfo(level);

        if (di == null) {
            return null;
        }
        return di.sourceline();
    }

    private static void lua_assert(boolean x) {
        if (!x) {
            throw new LuaError("lua_assert failed");
        }
    }

    /**
     *
     * @return StrValue[] { name, namewhat } if found, null if not
     */
    static LuaString[] getobjname(DebugInfo di, int stackpos) {
        if (di.closure == null) {
            return null; // Not a Lua function
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
        int i = pt.code[(pc) + 1];

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
        if (!(precheck(pt))) {
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
                if (!(pt.k[b].isstring())) {
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
                    if (!(checkopenop(pt, pc))) {
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
                    if (!(checkopenop(pt, pc))) {
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
