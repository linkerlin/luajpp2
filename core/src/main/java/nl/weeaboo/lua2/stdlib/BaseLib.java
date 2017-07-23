package nl.weeaboo.lua2.stdlib;

import static nl.weeaboo.lua2.vm.LuaBoolean.FALSE;
import static nl.weeaboo.lua2.vm.LuaBoolean.TRUE;
import static nl.weeaboo.lua2.vm.LuaConstants.INEXT;
import static nl.weeaboo.lua2.vm.LuaConstants.METATABLE;
import static nl.weeaboo.lua2.vm.LuaConstants.NEXT;
import static nl.weeaboo.lua2.vm.LuaConstants.NONE;
import static nl.weeaboo.lua2.vm.LuaConstants.TOSTRING;
import static nl.weeaboo.lua2.vm.LuaNil.NIL;
import static nl.weeaboo.lua2.vm.LuaValue.argerror;
import static nl.weeaboo.lua2.vm.LuaValue.valueOf;
import static nl.weeaboo.lua2.vm.LuaValue.varargsOf;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.LuaRunState;
import nl.weeaboo.lua2.compiler.LoadState;
import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.lib.LuaResource;
import nl.weeaboo.lua2.lib2.LuaBoundFunction;
import nl.weeaboo.lua2.lib2.LuaLib;
import nl.weeaboo.lua2.vm.LuaConstants;
import nl.weeaboo.lua2.vm.LuaError;
import nl.weeaboo.lua2.vm.LuaInteger;
import nl.weeaboo.lua2.vm.LuaString;
import nl.weeaboo.lua2.vm.LuaTable;
import nl.weeaboo.lua2.vm.LuaThread;
import nl.weeaboo.lua2.vm.LuaValue;
import nl.weeaboo.lua2.vm.Varargs;

@LuaSerializable
public final class BaseLib extends LuaLib {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(BaseLib.class);

    static InputStream STDIN = null;
    static PrintStream STDOUT = System.out;
    static PrintStream STDERR = System.err;

    @Override
    public void register() throws LuaException {
        LuaTable globals = getGlobals();
        globals.set("_G", globals);
        globals.set("_VERSION", LuaConstants.getEngineVersion());

        registerFunctions(globals);
    }

    private LuaTable getGlobals() {
        LuaRunState lrs = LuaRunState.getCurrent();
        LuaTable globals = lrs.getGlobalEnvironment();
        return globals;
    }

    /**
     * ( opt [,arg] ) -> value
     */
    @LuaBoundFunction
    public Varargs collectgarbage(Varargs args) {
        String s = args.checkjstring(1);
        if ("collect".equals(s)) {
            System.gc();
            return LuaInteger.valueOf(0);
        } else if ("count".equals(s)) {
            Runtime rt = Runtime.getRuntime();
            long used = rt.totalMemory() - rt.freeMemory();
            return valueOf(used / 1024.0);
        } else if ("step".equals(s)) {
            System.gc();
            return TRUE;
        } else {
            argerror(1, "gc op");
        }
        return NIL;
    }

    /**
     * ( message [,level] ) -> ERR
     */
    @LuaBoundFunction
    public Varargs error(Varargs args) {
        String message = args.optjstring(1, "");
        throw new LuaError(message, null, args.optint(2, 1));
    }

    /**
     * (f, table) -> void
     */
    @LuaBoundFunction
    public Varargs setfenv(Varargs args) {
        LuaValue f = getfenvobj(args.arg(1));
        LuaTable t = args.checktable(2);
        if (!f.isfunction() && !f.isclosure()) {
            throw new LuaError("'setfenv' cannot change environment of given object");
        }
        f.setfenv(t);
        return f.isthread() ? NONE : f;
    }

    /**
     * ( v [,message] ) -> v, message | ERR
     */
    @LuaBoundFunction(luaName = "assert")
    public Varargs assert_(Varargs args) {
        if (!args.arg1().toboolean()) {
            String message = "assertion failed!";
            if (args.narg() > 1) {
                message = args.optjstring(2, "assertion failed!");
            }
            throw new LuaError(message);
        }
        return args;
    }

    /**
     * ( filename ) -> result1, ...
     */
    @LuaBoundFunction
    public Varargs dofile(Varargs args) {
        Varargs v;
        if (args.isnil(1)) {
            v = loadStream(STDIN, "=stdin");
        } else {
            v = loadFile(args.checkjstring(1));
        }

        if (v.isnil(1)) {
            throw new LuaError(v.tojstring(2));
        }
        return v.arg1().invoke();
    }

    /**
     * ( [f] ) -> env
     */
    @LuaBoundFunction
    public Varargs getfenv(Varargs args) {
        LuaValue f = getfenvobj(args.arg1());
        LuaValue e = f.getfenv();
        return (e != null ? e : NIL);
    }

    /**
     * ( object ) -> table
     */
    @LuaBoundFunction
    public Varargs getmetatable(Varargs args) {
        LuaValue mt = args.checkvalue(1).getmetatable();
        return (mt != null ? mt.rawget(METATABLE).optvalue(mt) : NIL);
    }

    /**
     * ( func [,chunkname] ) -> chunk | nil, msg
     */
    @LuaBoundFunction
    public Varargs load(Varargs args) {
        LuaValue func = args.checkfunction(1);
        String chunkname = args.optjstring(2, "function");

        StringInputStream in = new StringInputStream(func);
        try {
            return loadStream(in, chunkname);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    /**
     * ( [filename] ) -> chunk | nil, msg
     */
    @LuaBoundFunction
    public Varargs loadfile(Varargs args) {
        if (args.isnil(1)) {
            return loadStream(STDIN, "stdin");
        } else {
            return loadFile(args.checkjstring(1));
        }
    }

    /**
     * Load from a named file, returning the chunk or nil,error of can't load.
     *
     * @return Varargs containing chunk, or NIL,error-text on error
     */
    public static Varargs loadFile(String filename) {
        LuaRunState lrs = LuaRunState.getCurrent();
        LuaResource r = lrs.findResource(filename);
        if (r == null) {
            return varargsOf(NIL, valueOf("cannot open " + filename));
        }

        try {
            final InputStream in = r.open();
            try {
                return loadStream(in, "@" + r.getCanonicalName());
            } finally {
                in.close();
            }
        } catch (IOException e) {
            return varargsOf(NIL, valueOf("cannot open " + filename));
        }
    }

    /**
     * ( string [,chunkname] ) -> chunk | nil, msg
     */
    @LuaBoundFunction
    public Varargs loadstring(Varargs args) {
        LuaString script = args.checkstring(1);
        String chunkname = args.optjstring(2, "string");
        return loadStream(script.toInputStream(), chunkname);
    }

    public static Varargs loadStream(InputStream is, String chunkname) {
        try {
            if (is == null) {
                return varargsOf(NIL, valueOf("not found: " + chunkname));
            }
            LuaThread running = LuaThread.getRunning();
            return LoadState.load(is, chunkname, running.getfenv());
        } catch (Exception e) {
            String message = e.getMessage();
            if (message == null) {
                message = "";
            }
            return varargsOf(NIL, valueOf(message));
        }
    }

    /**
     * (f, arg1, ...) -> status, result1, ...
     */
    @LuaBoundFunction
    public Varargs pcall(Varargs args) {
        LuaValue func = args.checkvalue(1);
        return pcall(func, args.subargs(2), null);
    }

    /**
     * @param errfunc is ignored, should replace thread's errfunc which it doesn't have anymore
     */
    private static Varargs pcall(LuaValue func, Varargs args, LuaValue errfunc) {
        try {
            Varargs funcResult = func.invoke(args);
            return varargsOf(TRUE, funcResult);
        } catch (LuaError le) {
            LOG.debug("Error in pcall: {} {}", func, args, le);
            String m = le.getMessage();
            return varargsOf(FALSE, m != null ? valueOf(m) : NIL);
        } catch (Exception e) {
            LOG.debug("Error in pcall: {} {}", func, args, e);
            String m = e.getMessage();
            return varargsOf(FALSE, valueOf(m != null ? m : e.toString()));
        }
    }

    /**
     * (f, err) -> result1, ...
     */
    @LuaBoundFunction
    public Varargs xpcall(Varargs args) {
        return pcall(args.arg1(), NONE, args.checkvalue(2));
    }

    /**
     * (...) -> void
     */
    @LuaBoundFunction
    public Varargs print(Varargs args) {
        LuaThread running = LuaThread.getRunning();
        LuaValue tostring = running.getfenv().get("tostring");
        for (int i = 1, n = args.narg(); i <= n; i++) {
            if (i > 1) {
                STDOUT.write('\t');
            }
            LuaString s = tostring.call(args.arg(i)).strvalue();
            int z = s.indexOf((byte)0, 0);
            try {
                s.write(STDOUT, 0, z >= 0 ? z : s.length());
            } catch (IOException e) {
                LOG.warn("Unable to write to stdout", e);
            }
        }
        STDOUT.println();
        return NONE;
    }

    /**
     * (f, ...) -> value1, ...
     */
    @LuaBoundFunction
    public Varargs select(Varargs args) {
        int n = args.narg() - 1;
        if (args.arg1().equals(valueOf("#"))) {
            return valueOf(n);
        }
        int i = args.checkint(1);
        if (i == 0 || i < -n) {
            argerror(1, "index out of range");
        }
        return args.subargs(i < 0 ? n + i + 2 : i + 1);
    }

    /**
     * (list [,i [,j]]) -> result1, ...
     */
    @LuaBoundFunction
    public Varargs unpack(Varargs args) {
        int na = args.narg();
        LuaTable t = args.checktable(1);
        int n = t.length();
        int i = na >= 2 ? args.checkint(2) : 1;
        int j = na >= 3 ? args.checkint(3) : n;
        n = j - i + 1;
        if (n < 0) {
            return NONE;
        }
        if (n == 1) {
            return t.get(i);
        }
        if (n == 2) {
            return varargsOf(t.get(i), t.get(j));
        }
        LuaValue[] v = new LuaValue[n];
        for (int k = 0; k < n; k++) {
            v[k] = t.get(i + k);
        }
        return varargsOf(v);
    }

    /**
     * (v) -> value
     */
    @LuaBoundFunction
    public Varargs type(Varargs args) {
        return valueOf(args.checkvalue(1).typename());
    }

    /**
     * (v1, v2) -> boolean
     */
    @LuaBoundFunction
    public Varargs rawequal(Varargs args) {
        return valueOf(args.checkvalue(1) == args.checkvalue(2));
    }

    /**
     * (table, index) -> value
     */
    @LuaBoundFunction
    public Varargs rawget(Varargs args) {
        return args.checktable(1).rawget(args.checkvalue(2));
    }

    /**
     * (table, index, value) -> table
     */
    @LuaBoundFunction
    public Varargs rawset(Varargs args) {
        LuaTable t = args.checktable(1);
        t.rawset(args.checknotnil(2), args.checkvalue(3));
        return t;
    }

    /**
     * (table, metatable) -> table
     */
    @LuaBoundFunction
    public Varargs setmetatable(Varargs args) {
        final LuaValue t = args.arg1();
        final LuaValue mt0 = t.getmetatable();
        if (mt0 != null && !mt0.rawget(METATABLE).isnil()) {
            throw new LuaError("cannot change a protected metatable");
        }
        final LuaValue mt = args.checkvalue(2);
        return t.setmetatable(mt.isnil() ? null : mt.checktable());
    }

    /**
     * (e) -> value
     */
    @LuaBoundFunction
    public Varargs tostring(Varargs args) {
        LuaValue arg = args.checkvalue(1);
        LuaValue h = arg.metatag(TOSTRING);
        if (!h.isnil()) {
            return h.call(arg);
        }
        LuaValue v = arg.tostring();
        if (!v.isnil()) {
            return v;
        }
        return valueOf(arg.tojstring());
    }

    /**
     * (e [,base]) -> value
     */
    @LuaBoundFunction
    public Varargs tonumber(Varargs args) {
        LuaValue arg1 = args.checkvalue(1);
        final int base = args.optint(2, 10);
        if (base == 10) { /* standard conversion */
            return arg1.tonumber();
        } else {
            if (base < 2 || base > 36) {
                argerror(2, "base out of range");
            }
            return arg1.checkstring().tonumber(base);
        }
    }

    /**
     * "pairs" (t) -> iter-func, t, nil
     */
    @LuaBoundFunction
    public Varargs pairs(Varargs args) {
        LuaValue next = getGlobals().get(NEXT);
        return varargsOf(next, args.checktable(1), NIL);
    }

    /**
     * "ipairs", // (t) -> iter-func, t, 0
     */
    @LuaBoundFunction
    public Varargs ipairs(Varargs args) {
        LuaValue inext = getGlobals().get(INEXT);
        return varargsOf(inext, args.checktable(1), LuaInteger.valueOf(0));
    }

    /**
     * "next" ( table, [index] ) -> next-index, next-value
     */
    @LuaBoundFunction
    public Varargs next(Varargs args) {
        return args.checktable(1).next(args.arg(2));
    }

    /**
     * "inext" ( table, [int-index] ) -> next-index, next-value
     */
    @LuaBoundFunction
    public Varargs inext(Varargs args) {
        return args.checktable(1).inext(args.arg(2));
    }

    private static LuaValue getfenvobj(LuaValue arg) {
        if (arg.isfunction()) {
            return arg;
        }

        int level = arg.optint(1);
        arg.argcheck(level >= 0, 1, "level must be non-negative");
        if (level == 0) {
            return LuaThread.getRunning();
        }

        LuaThread running = LuaThread.getRunning();
        LuaValue f = running.getCallstackFunction(level);
        arg.argcheck(f != null, 1, "invalid level");
        return f;
    }

}
