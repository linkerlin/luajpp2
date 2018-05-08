package nl.weeaboo.lua2.stdlib;

import static nl.weeaboo.lua2.vm.LuaBoolean.FALSE;
import static nl.weeaboo.lua2.vm.LuaBoolean.TRUE;
import static nl.weeaboo.lua2.vm.LuaConstants.EMPTYSTRING;
import static nl.weeaboo.lua2.vm.LuaConstants.META_CALL;
import static nl.weeaboo.lua2.vm.LuaConstants.NONE;
import static nl.weeaboo.lua2.vm.LuaConstants.TBOOLEAN;
import static nl.weeaboo.lua2.vm.LuaConstants.TFUNCTION;
import static nl.weeaboo.lua2.vm.LuaConstants.TNIL;
import static nl.weeaboo.lua2.vm.LuaConstants.TNUMBER;
import static nl.weeaboo.lua2.vm.LuaConstants.TSTRING;
import static nl.weeaboo.lua2.vm.LuaConstants.TTHREAD;
import static nl.weeaboo.lua2.vm.LuaNil.NIL;
import static nl.weeaboo.lua2.vm.LuaValue.valueOf;
import static nl.weeaboo.lua2.vm.LuaValue.varargsOf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.weeaboo.lua2.LuaRunState;
import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.lib.LuaBoundFunction;
import nl.weeaboo.lua2.vm.Lua;
import nl.weeaboo.lua2.vm.LuaBoolean;
import nl.weeaboo.lua2.vm.LuaClosure;
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
import nl.weeaboo.lua2.vm.UpValue;
import nl.weeaboo.lua2.vm.Varargs;

@LuaSerializable
public final class DebugLib extends LuaModule {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(DebugLib.class);

    private static final LuaString LUA = valueOf("Lua");
    private static final LuaString JAVA = valueOf("Java");
    private static final LuaString FUNC = valueOf("func");
    private static final LuaString NAME = valueOf("name");
    private static final LuaString LINE = valueOf("line");
    private static final LuaString COUNT = valueOf("count");
    private static final LuaString RETURN = valueOf("return");
    private static final LuaString NUPS = valueOf("nups");
    private static final LuaString NAMEWHAT = valueOf("namewhat");
    private static final LuaString WHAT = valueOf("what");
    private static final LuaString SOURCE = valueOf("source");
    private static final LuaString SHORT_SRC = valueOf("short_src");
    private static final LuaString LINEDEFINED = valueOf("linedefined");
    private static final LuaString LASTLINEDEFINED = valueOf("lastlinedefined");
    private static final LuaString CURRENTLINE = valueOf("currentline");
    private static final LuaString ACTIVELINES = valueOf("activelines");

    DebugLib() {
        super("debug");
    }

    /**
     * @param args Not used.
     */
    @LuaBoundFunction
    public Varargs debug(Varargs args) {
        throw new LuaError("not implemented");
    }

    @LuaBoundFunction
    public Varargs getfenv(Varargs args) {
        LuaValue object = args.arg1();
        LuaValue env = object.getfenv();
        return (env != null ? env : NIL);
    }

    @LuaBoundFunction
    public Varargs setfenv(Varargs args) {
        LuaValue object = args.arg1();
        LuaTable table = args.checktable(2);
        object.setfenv(table);
        return object;
    }

    @LuaBoundFunction
    public Varargs gethook(Varargs args) {
        int a = 1;
        LuaThread thread = args.isthread(a) ? args.checkthread(a++) : LuaThread.getRunning();

        DebugState ds = getDebugState(thread);
        return varargsOf(ds.hookfunc,
                valueOf((ds.hookcall ? "c" : "") + (ds.hookline ? "l" : "") + (ds.hookrtrn ? "r" : "")),
                valueOf(ds.hookcount));
    }

    @LuaBoundFunction
    public Varargs sethook(Varargs args) {
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

    @LuaBoundFunction
    public Varargs getlocal(Varargs args) {
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

    @LuaBoundFunction
    public Varargs setlocal(Varargs args) {
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

    @LuaBoundFunction
    public Varargs getupvalue(Varargs args) {
        LuaValue func = args.checkfunction(1);
        int up = args.checkint(2);

        if (func instanceof LuaClosure) {
            LuaClosure c = (LuaClosure)func;
            LuaString name = findupvalue(c, up);
            if (name != null) {
                UpValue[] upValues = c.getUpValues();
                return varargsOf(name, upValues[up - 1].getValue());
            }
        }
        return NIL;
    }

    @LuaBoundFunction
    public Varargs setupvalue(Varargs args) {
        LuaValue func = args.checkfunction(1);
        int up = args.checkint(2);
        LuaValue value = args.arg(3);

        if (func instanceof LuaClosure) {
            LuaClosure c = (LuaClosure)func;
            LuaString name = findupvalue(c, up);
            if (name != null) {
                UpValue[] upValues = c.getUpValues();
                upValues[up - 1].setValue(value);
                return name;
            }
        }
        return NIL;
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

    @LuaBoundFunction
    public Varargs getmetatable(Varargs args) {
        LuaValue object = args.arg(1);
        LuaValue mt = object.getmetatable();
        return (mt != null ? mt : NIL);
    }

    @LuaBoundFunction
    public Varargs setmetatable(Varargs args) {
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

    @LuaBoundFunction
    public Varargs getinfo(Varargs args) {
        int a = 1;
        LuaThread thread = args.isthread(a) ? args.checkthread(a++) : LuaThread.getRunning();
        LuaValue func = args.arg(a++);
        final String what = args.optjstring(a++, "nSluf");

        // find the stack info
        DebugState ds = getDebugState(thread);
        DebugInfo di = null;
        LuaString[] namewhat = null;
        if (func.isnumber()) {
            int level = func.checkint();
            if (level > 0) {
                di = ds.getDebugInfo(level);

                DebugInfo parentInfo = ds.getDebugInfo(level + 1);
                if (parentInfo != null) {
                    namewhat = parentInfo.getfunckind();
                }
            } else {
                di = new DebugInfo(thread.getCallstackFunction(1));
            }
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
                info.set(NAME, namewhat != null ? namewhat[0] : NIL);
                info.set(NAMEWHAT, namewhat != null ? namewhat[1] : EMPTYSTRING);
                break;
            }
            case 'f': {
                info.set(FUNC, di.func);
                break;
            }
            case 'L': {
                /*
                 * pushes onto the stack a table whose indices are the numbers of the lines that are valid on
                 * the function. (A valid line is a line with some associated code, that is, a line where you
                 * can put a break point. Non-valid lines include empty lines and comments.)
                 */
                LuaTable lines = new LuaTable();
                if (c != null) {
                    int[] lineinfo = c.getPrototype().lineinfo;
                    for (int line : lineinfo) {
                        lines.rawset(line, LuaBoolean.TRUE);
                    }
                }
                info.set(ACTIVELINES, lines);
                break;
            }
            }
        }
        return info;
    }

    /**
     * @param args Not used.
     */
    @LuaBoundFunction
    public Varargs getregistry(Varargs args) {
        return LuaRunState.getCurrent().getRegistry();
    }

    @LuaBoundFunction
    public Varargs traceback(Varargs args) {
        int a = 1;
        LuaThread thread = args.isthread(a) ? args.checkthread(a++) : LuaThread.getRunning();
        String message = args.optjstring(a++, null);
        int level = args.optint(a++, 1);
        String tb = DebugTrace.traceback(thread, level);
        return valueOf(message != null ? message + "\n" + tb : tb);
    }

    private static String sourceshort(Prototype p) {
        int maxChars = 60;

        String name = p.source.tojstring();
        if (name.startsWith("@") || name.startsWith("=")) {
            name = name.substring(1);
            if (name.length() > maxChars) {
                return "..." + name.substring(name.length() - (maxChars - 3), name.length());
            } else {
                return name;
            }
        } else if (name.startsWith("\033")) {
            return "binary string";
        } else {
            maxChars -= 14;

            StringBuilder sb = new StringBuilder("[string \"");
            int newlineIndex = name.indexOf('\n');
            if (name.length() <= maxChars && newlineIndex < 0) {
                sb.append(name);
            } else {
                int to = name.length();
                // Break at the first newline (if it exists)
                if (newlineIndex >= 0) {
                    to = newlineIndex;
                }
                sb.append(name, 0, to);
                sb.append("...");
            }
            sb.append("\"]");
            return sb.toString();
        }
    }

    static DebugState getDebugState(LuaThread thread) {
        if (thread.debugState == null) {
            thread.debugState = new DebugState(thread);
        }
        return (DebugState)thread.debugState;
    }

    public static boolean isDebugEnabled() {
        return LuaRunState.getCurrent().isDebugEnabled();
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
     * @param func the function called
     */
    public static void debugOnCall(LuaThread thread, LuaFunction func) {
        DebugState ds = getDebugState(thread);

        DebugInfo di = ds.pushInfo();
        di.setfunction(func);

        LOG.trace("debugOnCall: {}", di);

        if (!ds.inhook && ds.hookcall) {
            ds.callHookFunc(META_CALL, NIL);
        }
    }

    /**
     * Called by Closures and recursing java functions on return
     *
     * @param thread the thread for the call
     */
    public static void debugOnReturn(LuaThread thread) {
        DebugState ds = getDebugState(thread);

        LOG.trace("debugOnReturn: {}", ds.getDebugInfo());

        try {
            if (!ds.inhook && ds.hookrtrn) {
                ds.callHookFunc(RETURN, NIL);
            }
        } finally {
            ds.popInfo();
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

}
