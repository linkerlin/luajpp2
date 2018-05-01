package nl.weeaboo.lua2.compiler;

import static nl.weeaboo.lua2.vm.LuaNil.NIL;
import static nl.weeaboo.lua2.vm.LuaValue.valueOf;
import static nl.weeaboo.lua2.vm.LuaValue.varargsOf;

import java.io.IOException;
import java.io.InputStream;

import nl.weeaboo.lua2.LuaRunState;
import nl.weeaboo.lua2.lib.LuaResource;
import nl.weeaboo.lua2.vm.LuaThread;
import nl.weeaboo.lua2.vm.Varargs;

public final class LuaScriptLoader {

    private LuaScriptLoader() {
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

}
