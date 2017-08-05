package nl.weeaboo.lua2.stdlib;

import java.io.FileNotFoundException;
import java.io.IOException;

import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.lib.LuaFileHandle;

@LuaSerializable
final class SafeIo implements ILuaIoImpl {

    private static final long serialVersionUID = 1L;

    @Override
    public LuaFileHandle openProgram(String prog, String mode) throws IOException {
        throw new IOException("openProgram is nog allowed: " + prog);
    }

    @Override
    public LuaFileHandle createTempFile() throws IOException {
        throw new IOException("createTempFile() is not allowed");
    }

    @Override
    public LuaFileHandle openFile(String filename, FileOpenMode mode) throws IOException {
        throw new FileNotFoundException("openFile() is not allowed");
    }

    @Override
    public void deleteFile(String filename) throws IOException {
        throw new IOException("deleteFile() is not allowed");
    }

    @Override
    public void renameFile(String oldFilename, String newFilename) throws IOException {
        throw new IOException("renameFile() is not allowed");
    }

}
