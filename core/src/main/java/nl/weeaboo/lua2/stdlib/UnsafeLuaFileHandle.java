package nl.weeaboo.lua2.stdlib;

import java.io.IOException;
import java.io.RandomAccessFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.weeaboo.lua2.lib.LuaFileHandle;
import nl.weeaboo.lua2.vm.LuaString;
import nl.weeaboo.lua2.vm.LuaTable;

final class UnsafeLuaFileHandle extends LuaFileHandle {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(UnsafeLuaFileHandle.class);

    private final FileOpenMode mode;

    // Mark file as transient so it 'auto-closes' during serialization
    private transient RandomAccessFile file;

    public UnsafeLuaFileHandle(LuaTable fileMethods, RandomAccessFile file, FileOpenMode mode) {
        super(fileMethods);

        this.file = file;
        this.mode = mode;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            if (file != null) {
                file.close();

                LOG.warn("File handle leak closed by finalizer");
            }
        } finally {
            super.finalize();
        }
    }

    private void checkOpen() throws IOException {
        if (isclosed()) {
            throw new IOException("File is closed");
        }
    }

    private void checkReadable() throws IOException {
        if (!mode.isReadable()) {
            throw new IOException("File is not readable");
        }
    }

    private void checkWritable() throws IOException {
        if (!mode.isWritable()) {
            throw new IOException("File is not writable");
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
        checkReadable();

        long oldpos = file.getFilePointer();
        int r = file.read();
        file.seek(oldpos);
        return r;
    }

    @Override
    public int read() throws IOException {
        checkOpen();
        checkReadable();

        return file.read();
    }

    @Override
    public int read(byte[] bytes, int offset, int length) throws IOException {
        checkOpen();
        checkReadable();

        return file.read(bytes, offset, length);
    }

    @Override
    public void write(LuaString string) throws IOException {
        checkOpen();
        checkWritable();

        if (mode.isAppend()) {
            final long oldpos = file.getFilePointer();
            file.seek(file.length());
            try {
                string.write(file, 0, string.length());
            } finally {
                file.seek(oldpos);
            }
        } else {
            string.write(file, 0, string.length());
        }
    }

}
