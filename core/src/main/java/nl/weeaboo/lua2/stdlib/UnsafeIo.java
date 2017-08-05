package nl.weeaboo.lua2.stdlib;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.lib.LuaFileHandle;
import nl.weeaboo.lua2.vm.LuaTable;

@LuaSerializable
final class UnsafeIo implements ILuaIoImpl {

    private static final long serialVersionUID = 1L;

    @Override
    public LuaFileHandle openProgram(String prog, String mode) throws IOException {
        throw new IOException("openProgram is not implemented yet");
    }

    @Override
    public LuaFileHandle createTempFile() throws IOException {
        LuaTable fileTable = IoLib.getFileTable();

        File tempFile = File.createTempFile("luaj", ".tmp");
        tempFile.deleteOnExit();

        RandomAccessFile rfile = new RandomAccessFile(tempFile, "rw");
        return new UnsafeLuaFileHandle(fileTable, rfile);
    }

}
