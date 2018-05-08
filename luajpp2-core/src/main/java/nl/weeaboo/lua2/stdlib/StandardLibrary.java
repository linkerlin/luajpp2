package nl.weeaboo.lua2.stdlib;

import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.LuaRunState;
import nl.weeaboo.lua2.luajava.LuajavaLib;
import nl.weeaboo.lua2.vm.LuaTable;

public final class StandardLibrary {

    private final PackageLib packageLib;
    private final LuajavaLib luajavaLib;

    private boolean debugEnabled = true;
    private boolean unsafeIo;

    public StandardLibrary() {
        packageLib = new PackageLib();
        luajavaLib = new LuajavaLib();
    }

    public void setDebugEnabled(boolean enable) {
        debugEnabled = enable;
    }

    public void setAllowUnsafeIO(boolean allow) {
        unsafeIo = allow;
    }

    public void setAllowUnsafeClassLoading(boolean allow) {
        luajavaLib.setAllowUnsafeClassLoading(allow);
    }

    public void register() throws LuaException {
        LuaRunState runState = LuaRunState.getCurrent();
        final LuaTable globals = runState.getGlobalEnvironment();

        new BaseLib().register();
        packageLib.register();
        new TableLib().register();
        new StringLib().register();
        new CoroutineLib().register();
        new MathLib().register();
        ILuaIoImpl ioImpl = createIoImpl();
        new IoLib(ioImpl).register();
        new OsLib(ioImpl).register();
        luajavaLib.register();
        new ThreadLib().register();

        if (debugEnabled) {
            new DebugLib().register();
        }

        // Set Thread.yield() as a global yield function
        globals.rawset("yield", globals.rawget("Thread").rawget("yield"));
    }

    private ILuaIoImpl createIoImpl() {
        if (unsafeIo) {
            return new UnsafeIo();
        } else {
            return new SafeIo();
        }
    }

}
