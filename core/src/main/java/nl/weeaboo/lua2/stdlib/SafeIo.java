package nl.weeaboo.lua2.stdlib;

import java.io.IOException;

import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.lib.LuaFileHandle;

@LuaSerializable
final class SafeIo implements ILuaIoImpl {

    private static final long serialVersionUID = 1L;

    @Override
    public LuaFileHandle openProgram(String prog, String mode) throws IOException {
        throw new IOException("Unable to open program: " + prog);
    }

    @Override
    public LuaFileHandle createTempFile() throws IOException {
        throw new IOException("Unable to create temp file");
    }

}
