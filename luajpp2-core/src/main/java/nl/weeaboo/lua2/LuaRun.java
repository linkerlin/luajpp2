package nl.weeaboo.lua2;

import nl.weeaboo.lua2.compiler.ScriptLoader;
import nl.weeaboo.lua2.lib.FileResourceFinder;
import nl.weeaboo.lua2.stdlib.StandardLibrary;
import nl.weeaboo.lua2.vm.LuaConstants;
import nl.weeaboo.lua2.vm.LuaThread;
import nl.weeaboo.lua2.vm.Varargs;

/**
 * Compiles and runs a Lua file.
 */
final class LuaRun {

    public static void main(String[] args) {
        if (args.length != 1) {
            die("Usage: java cp luajpp2.jar nl.weeaboo.lua2.LuaRun [file.lua]");
        }

        String fileName = args[0];

        StandardLibrary stdlib = new StandardLibrary();
        stdlib.setAllowUnsafeIO(true);

        LuaRunState lrs = LuaRunState.create(stdlib);
        lrs.setResourceFinder(new FileResourceFinder());
        try {
            // Load script
            Varargs loadResult = ScriptLoader.loadFile(fileName);
            if (loadResult.isnil(1)) {
                throw new LuaException(loadResult.tojstring(2));
            }

            // Run script until it completes
            LuaThread mainThread = lrs.getMainThread();
            mainThread.pushPending(loadResult.checkclosure(1), LuaConstants.NONE);
            while (mainThread.isRunnable()) {
                lrs.update();
                if (Thread.interrupted()) {
                    die("Interrupted");
                }
            }
        } finally {
            lrs.destroy();
        }
    }

    private static void die(String message) {
        System.err.println(message);
        System.exit(1);
    }
}
