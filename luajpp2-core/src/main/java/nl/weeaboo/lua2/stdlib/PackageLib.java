package nl.weeaboo.lua2.stdlib;

import static nl.weeaboo.lua2.vm.LuaBoolean.TRUE;
import static nl.weeaboo.lua2.vm.LuaConstants.EMPTYSTRING;
import static nl.weeaboo.lua2.vm.LuaConstants.META_INDEX;
import static nl.weeaboo.lua2.vm.LuaConstants.NONE;
import static nl.weeaboo.lua2.vm.LuaNil.NIL;
import static nl.weeaboo.lua2.vm.LuaValue.listOf;
import static nl.weeaboo.lua2.vm.LuaValue.tableOf;
import static nl.weeaboo.lua2.vm.LuaValue.valueOf;
import static nl.weeaboo.lua2.vm.LuaValue.varargsOf;

import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.compiler.ScriptLoader;
import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.lib.VarArgFunction;
import nl.weeaboo.lua2.lib2.LuaBoundFunction;
import nl.weeaboo.lua2.vm.LuaError;
import nl.weeaboo.lua2.vm.LuaFunction;
import nl.weeaboo.lua2.vm.LuaString;
import nl.weeaboo.lua2.vm.LuaTable;
import nl.weeaboo.lua2.vm.LuaThread;
import nl.weeaboo.lua2.vm.LuaValue;
import nl.weeaboo.lua2.vm.Varargs;

@LuaSerializable
public final class PackageLib extends LuaModule {

    private static final long serialVersionUID = 1L;

    private static final LuaString S_M = valueOf("_M");
    private static final LuaString S_NAME = valueOf("_NAME");
    private static final LuaString S_PACKAGE = valueOf("_PACKAGE");
    private static final LuaString S_DOT = valueOf(".");
    private static final LuaString S_LOADERS = valueOf("loaders");
    private static final LuaString S_LOADED = valueOf("loaded");
    private static final LuaString S_PRELOAD = valueOf("preload");
    private static final LuaString S_PATH = valueOf("path");
    private static final LuaString S_CPATH = valueOf("cpath");
    private static final LuaString S_SENTINEL = valueOf("\u0001");

    public LuaTable loadedTable = new LuaTable();
    public LuaTable packageTable;

    PackageLib() {
        super("package");
    }

    @Override
    protected void registerAdditional(LuaTable globals, LuaTable libTable) throws LuaException {
        super.registerAdditional(globals, libTable);

        packageTable = libTable;
        packageTable.rawset(S_LOADED, loadedTable);
        packageTable.rawset(S_PRELOAD, new LuaTable());
        packageTable.rawset(S_PATH, valueOf("?.lua"));
        packageTable.rawset(S_CPATH, valueOf(""));

        packageTable.rawset(S_LOADERS, listOf(new LuaValue[] {
                new PreloadLoader(packageTable),
                new LuaLoader(packageTable)
        }));

        loadedTable.rawset("package", packageTable);
    }

    @LuaBoundFunction
    public Varargs loadlib(Varargs args) {
        args.checkstring(1);
        return varargsOf(NIL, valueOf("dynamic libraries not enabled"), valueOf("absent"));
    }

    @LuaBoundFunction
    public Varargs seeall(Varargs args) {
        LuaTable t = args.checktable(1);
        LuaValue m = t.getmetatable();
        if (m == null) {
            t.setmetatable(m = tableOf());
        }
        LuaThread running = LuaThread.getRunning();
        m.set(META_INDEX, running.getfenv());
        return NONE;
    }

    /**
     * require (modname)
     *
     * Loads the given module. The function starts by looking into the package.loaded table to determine
     * whether modname is already loaded. If it is, then require returns the value stored at
     * package.loaded[modname]. Otherwise, it tries to find a loader for the module.
     *
     * To find a loader, require is guided by the package.loaders array. By changing this array, we can change
     * how require looks for a module. The following explanation is based on the default configuration for
     * package.loaders.
     *
     * First require queries package.preload[modname]. If it has a value, this value (which should be a
     * function) is the loader. Otherwise require searches for a Lua loader using the path stored in
     * package.path. If that also fails, it searches for a C loader using the path stored in package.cpath. If
     * that also fails, it tries an all-in-one loader (see package.loaders).
     *
     * Once a loader is found, require calls the loader with a single argument, modname. If the loader returns
     * any value, require assigns the returned value to package.loaded[modname]. If the loader returns no
     * value and has not assigned any value to package.loaded[modname], then require assigns true to this
     * entry. In any case, require returns the final value of package.loaded[modname].
     *
     * If there is any error loading or running the module, or if it cannot find any loader for the module,
     * then require signals an error.
     */
    @LuaBoundFunction(global = true)
    public Varargs require(Varargs args) {
        LuaString name = args.checkstring(1);
        LuaValue loaded = loadedTable.get(name);
        if (loaded.toboolean()) {
            if (loaded == S_SENTINEL) {
                throw new LuaError("loop or previous error loading module '" + name + "'");
            }
            return loaded;
        }

        /* else must load it; iterate over available loaders */
        LuaTable tbl = packageTable.get(S_LOADERS).checktable();
        StringBuffer sb = new StringBuffer();
        LuaValue chunk = null;
        for (int i = 1; true; i++) {
            LuaValue loader = tbl.get(i);
            if (loader.isnil()) {
                throw new LuaError("module '" + name + "' not found: " + name + sb);
            }

            /* call loader with module name as argument */
            chunk = loader.call(name);
            if (chunk.isfunction()) {
                break;
            }
            if (chunk.isstring()) {
                sb.append(chunk.tojstring());
            }
        }

        // load the module using the loader
        loadedTable.set(name, S_SENTINEL);
        LuaValue result = chunk.call(name);
        if (!result.isnil()) {
            loadedTable.set(name, result);
        } else if ((result = loadedTable.get(name)) == S_SENTINEL) {
            loadedTable.set(name, result = TRUE);
        }
        return result;
    }

    /**
     * module (name [, ...])
     *
     * Creates a module. If there is a table in package.loaded[name], this table is the module. Otherwise, if
     * there is a global table t with the given name, this table is the module. Otherwise creates a new table
     * t and sets it as the value of the global name and the value of package.loaded[name]. This function also
     * initializes t._NAME with the given name, t._M with the module (t itself), and t._PACKAGE with the
     * package name (the full module name minus last component; see below). Finally, module sets t as the new
     * environment of the current function and the new value of package.loaded[name], so that require returns
     * t.
     *
     * If name is a compound name (that is, one with components separated by dots), module creates (or reuses,
     * if they already exist) tables for each component. For instance, if name is a.b.c, then module stores
     * the module table in field c of field b of global a.
     *
     * This function may receive optional options after the module name, where each option is a function to be
     * applied over the module.
     */
    @LuaBoundFunction(global = true)
    public Varargs module(Varargs args) {
        LuaThread running = LuaThread.getRunning();

        LuaString modname = args.checkstring(1);
        final int n = args.narg();
        LuaValue value = loadedTable.get(modname);
        LuaValue module;
        if (!value.istable()) { /* not found? */

            /* try global variable (and create one if it does not exist) */
            LuaValue globals = running.getfenv();
            module = findtable(globals, modname);
            if (module == null) {
                throw new LuaError("name conflict for module '" + modname + "'");
            }
            loadedTable.set(modname, module);
        } else {
            module = value;
        }

        /* check whether table already has a _NAME field */
        LuaValue name = module.get(S_NAME);
        if (name.isnil()) {
            module.set(S_M, module);
            int e = modname.lastIndexOf(S_DOT);
            module.set(S_NAME, modname);
            module.set(S_PACKAGE, (e < 0 ? EMPTYSTRING : modname.substring(0, e + 1)));
        }

        // set the environment of the current function
        LuaFunction f = running.getCallstackFunction(1);
        if (f == null) {
            throw new LuaError("no calling function");
        }
        if (!f.isclosure()) {
            throw new LuaError("'module' not called from a Lua function");
        }
        f.setfenv(module);

        // apply the functions
        for (int i = 2; i <= n; i++) {
            args.arg(i).call(module);
        }

        // returns no results
        return NONE;
    }

    /**
     * @param table the table at which to start the search
     * @param fname the name to look up or create, such as "abc.def.ghi"
     * @return the table for that name, possible a new one, or null if a non-table has that name already.
     */
    private static final LuaValue findtable(LuaValue table, LuaString fname) {
        int b;
        int e = -1;
        do {
            e = fname.indexOf(S_DOT, b = e + 1);
            if (e < 0) {
                e = fname.length();
            }
            LuaString key = fname.substring(b, e);
            LuaValue val = table.rawget(key);
            if (val.isnil()) { /* no such field? */
                LuaTable field = new LuaTable(); /* new table for field */
                table.set(key, field);
                table = field;
            } else if (!val.istable()) { /* field has a non-table value? */
                return null;
            } else {
                table = val;
            }
        } while (e < fname.length());
        return table;
    }

    @LuaSerializable
    private static final class PreloadLoader extends VarArgFunction {

        private static final long serialVersionUID = 1L;

        private final LuaTable packageTable;

        public PreloadLoader(LuaTable packageTable) {
            this.packageTable = packageTable;
        }

        @Override
        public Varargs invoke(Varargs args) {
            LuaString name = args.checkstring(1);

            LuaValue preloadTable = packageTable.get(S_PRELOAD);
            if (!preloadTable.istable()) {
                throw new LuaError("'package.preload' must be a table");
            }

            LuaValue entry = preloadTable.get(name);
            if (entry.isnil()) {
                return valueOf("no field package.preload['" + name + "']");
            } else {
                return entry;
            }
        }

    }

    @LuaSerializable
    private static final class LuaLoader extends VarArgFunction {

        private static final long serialVersionUID = 1L;

        private final LuaTable packageTable;

        public LuaLoader(LuaTable packageTable) {
            this.packageTable = packageTable;
        }

        @Override
        public Varargs invoke(Varargs args) {
            String name = args.checkjstring(1);

            // get package path
            LuaValue pp = packageTable.get(S_PATH);
            if (!pp.isstring()) {
                return valueOf("package.path is not a string");
            }
            String path = pp.tojstring();

            // check the path elements
            int e = -1;
            int n = path.length();
            StringBuffer sb = null;
            name = name.replace('.', '/');
            while (e < n) {

                // find next template
                int b = e + 1;
                e = path.indexOf(';', b);
                if (e < 0) {
                    e = path.length();
                }
                String template = path.substring(b, e);

                // create filename by replacing wildcards
                String filename = template.replace("?", name);

                // try loading the file
                Varargs v = ScriptLoader.loadFile(filename);
                if (v.arg1().isfunction()) {
                    return v.arg1();
                }

                // report error
                if (sb == null) {
                    sb = new StringBuffer();
                }
                sb.append("\n\t'" + filename + "': " + v.arg(2));
            }
            return valueOf(sb.toString());
        }
    }

}
