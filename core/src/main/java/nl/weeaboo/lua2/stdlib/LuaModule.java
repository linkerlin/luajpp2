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
        LuaRunState lrs = LuaRunState.getCurrent();
        LuaTable globals = lrs.getGlobalEnvironment();

        LuaTable table = new LuaTable();
        registerFunctions(globals, table);
        globals.rawset(moduleName, table);

        registerAdditional(globals, table);
        lrs.setIsLoaded(moduleName, table);
    }

    /**
     * This overridable method can be used to register additional items.
     *
     * @param globals The globals table ({@link LuaRunState#getGlobalEnvironment()}).
     * @param libTable The table containing the module's methods.
     * @throws LuaException If a fatal error occurs
     */
    protected void registerAdditional(LuaTable globals, LuaTable libTable) throws LuaException {
    }

}
