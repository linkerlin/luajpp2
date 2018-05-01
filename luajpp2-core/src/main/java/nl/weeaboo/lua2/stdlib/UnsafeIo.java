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
    public LuaFileHandle createTempFile(LuaTable fileTable) throws IOException {
        File tempFile = File.createTempFile("luaj", ".tmp");
        tempFile.deleteOnExit();

        RandomAccessFile rfile = new RandomAccessFile(tempFile, "rw");
        return new UnsafeLuaFileHandle(fileTable, tempFile.getAbsolutePath(), rfile, FileOpenMode.fromString("r+"));
    }

    @Override
    public LuaFileHandle openFile(LuaTable fileTable, String filename, FileOpenMode mode) throws IOException {
        RandomAccessFile rfile = new RandomAccessFile(filename, mode.isWritable() ? "rw" : "r");
        if (mode.isTruncate()) {
            rfile.setLength(0);
        }
        return new UnsafeLuaFileHandle(fileTable, filename, rfile, mode);
    }

    @Override
    public boolean deleteFile(String filename) throws IOException {
        File file = new File(filename);

        if (!file.exists()) {
            return false;
        } else if (file.delete()) {
            return true;
        } else {
            throw new IOException("Delete of " + filename + " failed");
        }
    }

    @Override
    public void renameFile(String oldFilename, String newFilename) throws IOException {
        File oldFile = new File(oldFilename);
        File newFile = new File(newFilename);
        if (!oldFile.renameTo(newFile)) {
            throw new IOException("Rename of " + oldFile + " to " + newFile + " failed");
        }
    }

}
