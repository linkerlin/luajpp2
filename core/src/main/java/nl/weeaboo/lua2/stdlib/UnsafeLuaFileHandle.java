package nl.weeaboo.lua2.stdlib;

import java.io.IOException;
import java.io.RandomAccessFile;

import nl.weeaboo.lua2.lib.LuaFileHandle;
import nl.weeaboo.lua2.vm.LuaString;
import nl.weeaboo.lua2.vm.LuaTable;

final class UnsafeLuaFileHandle extends LuaFileHandle {

    private static final long serialVersionUID = 1L;

    // Mark file as transient so it 'auto-closes' during serialization
    private transient RandomAccessFile file;

    public UnsafeLuaFileHandle(LuaTable fileMethods, RandomAccessFile file) {
        super(fileMethods);

        this.file = file;
    }

    private void checkOpen() throws IOException {
        if (isclosed()) {
            throw new IOException("File is closed");
        }
    }

    @Override
    public void close() throws IOException {
        super.close();

        if (file != null) {
            file.close();
            file = null;
        }
    }

    @Override
    public boolean isclosed() {
        return file == null;
    }

    @Override
    public int seek(String whence, int bytecount) throws IOException {
        checkOpen();

        if (whence.equals("set")) {
            file.seek(bytecount);
        } else if (whence.equals("cur")) {
            file.seek(file.getFilePointer() + bytecount);
        }  else if (whence.equals("end")) {
            file.seek(file.length() + bytecount);
        } else {
            throw new IOException("Unsupported whence: " + whence);
        }

        long cur = file.getFilePointer();
        if (cur > Integer.MAX_VALUE) {
            throw new IOException("File offset overflow: greater than INT_MAX");
        }
        return (int)cur;
    }

    @Override
    public void setvbuf(String mode, int size) throws IOException {
        checkOpen();

        // Not implemented yet
    }

    @Override
    public int remaining() throws IOException {
        checkOpen();

        return (int)Math.min(Integer.MAX_VALUE, file.length() - file.getFilePointer());
    }

    @Override
    public int peek() throws IOException {
        checkOpen();

        long oldpos = file.getFilePointer();
        int r = file.read();
        file.seek(oldpos);
        return r;
    }

    @Override
    public int read() throws IOException {
        checkOpen();

        return file.read();
    }

    @Override
    public int read(byte[] bytes, int offset, int length) throws IOException {
        checkOpen();

        return file.read(bytes, offset, length);
    }

    @Override
    public void write(LuaString string) throws IOException {
        checkOpen();

        string.write(file, 0, string.length());
    }

}
