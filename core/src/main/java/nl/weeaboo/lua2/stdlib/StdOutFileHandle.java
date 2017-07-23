package nl.weeaboo.lua2.stdlib;

import java.io.IOException;
import java.io.OutputStream;

import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.lib.LuaFileHandle;
import nl.weeaboo.lua2.vm.LuaString;
import nl.weeaboo.lua2.vm.LuaTable;

@LuaSerializable
final class StdOutFileHandle extends LuaFileHandle {

    private static final long serialVersionUID = 1L;

    private boolean isErr;

    /**
     * @param isErr If {@code true} use {@link System#err}, else {@link System#out}.
     */
    public StdOutFileHandle(LuaTable fileMethods, boolean isErr) {
        super(fileMethods);

        this.isErr = isErr;
    }

    @Override
    public boolean isstdfile() {
        return true;
    }

    protected OutputStream getOutputStream() {
        return (isErr ? System.err : System.out);
    }

    @Override
    public boolean isclosed() {
        return false;
    }

    @Override
    public void setvbuf(String mode, int size) {
        // Ignore
    }

    @Override
    public void write(LuaString string) throws IOException {
        OutputStream out = getOutputStream();
        string.write(out, 0, string.rawlen());
        out.flush();
    }

    @Override
    public void flush() throws IOException {
        getOutputStream().flush();
    }

}