package nl.weeaboo.lua2.stdlib;

import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.LuaRunState;
import nl.weeaboo.lua2.lib2.LuaLib;
import nl.weeaboo.lua2.vm.LuaTable;

public abstract class LuaModule extends LuaLib {

    private static final long serialVersionUID = 1L;

    private final String moduleName;

    public LuaModule(String moduleName) {
        if (moduleName == null) {
            throw new IllegalArgumentException("Module name may not be null");
        }
        this.moduleName = moduleName;
    }

    @Override
    public final void register() throws LuaException {
        LuaTable table = new LuaTable();
        registerFunctions(table);
        registerInEnv(table);
    }

    /**
     * Handles any required bookkeeping to register the table of Lua functions into the global environment.
     * @throws LuaException If a fatal error occurs
     */
    protected void registerInEnv(LuaTable table) throws LuaException {
        LuaRunState lrs = LuaRunState.getCurrent();
        LuaTable globals = lrs.getGlobalEnvironment();
        globals.rawset(moduleName, table);
        lrs.setIsLoaded(moduleName, table);
    }

}
