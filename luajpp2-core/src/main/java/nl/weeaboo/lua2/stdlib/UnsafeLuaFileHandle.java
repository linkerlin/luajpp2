package nl.weeaboo.lua2.stdlib;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.weeaboo.lua2.lib.LuaFileHandle;
import nl.weeaboo.lua2.vm.LuaString;
import nl.weeaboo.lua2.vm.LuaTable;

final class UnsafeLuaFileHandle extends LuaFileHandle {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(UnsafeLuaFileHandle.class);

    private static final Set<String> openFiles = new CopyOnWriteArraySet<>();

    private final String fileName;
    private final FileOpenMode mode;

    private FileBufferMode bufferMode = FileBufferMode.NO;
    private transient @Nullable ByteBuffer buffer; // Must have a backing array

    // Mark file as transient so it 'auto-closes' during serialization
    private transient @Nullable RandomAccessFile file;

    public UnsafeLuaFileHandle(LuaTable fileMethods, String fileName, RandomAccessFile file, FileOpenMode mode) {
        super(fileMethods);

        this.fileName = fileName;
        this.file = file;
        this.mode = mode;

        openFiles.add(fileName);
        LOG.debug("Opening file: {}", fileName);
    }

    @SuppressWarnings("checkstyle:NoFinalizer")
    @Override
    protected void finalize() throws Throwable {

        try {
            if (file != null) {
                doClose();

                LOG.warn("File handle leak closed by finalizer: {}", fileName);
            }
        } finally {
            super.finalize();
        }
    }

    private void checkOpen() throws IOException {
        if (isClosed()) {
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
        flushBuffer();

        super.close();

        if (file != null) {
            doClose();
        }
    }

    private void doClose() throws IOException {
        LOG.debug("Closing file: {}", fileName);
        openFiles.remove(fileName);
        try {
            file.close();
        } finally {
            file = null;
        }
    }

    @Override
    public boolean isClosed() {
        return file == null;
    }

    @Override
    public int seek(String whence, int bytecount) throws IOException {
        checkOpen();
        flushBuffer();

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
        flushBuffer();

        bufferMode = FileBufferMode.fromString(mode);
        if (bufferMode == FileBufferMode.NO || size == 0) {
            buffer = null;
        } else {
            buffer = ByteBuffer.allocate(size);
        }
    }

    @Override
    public int remaining() {
        try {
            checkOpen();
            flushBuffer(); // Not strictly necessary, but it saves us a bit of complexity here.

            return (int)Math.min(Integer.MAX_VALUE, file.length() - file.getFilePointer());
        } catch (IOException ioe) {
            LOG.trace("Unable to determine remaining bytes for {} due to an I/O error", this, ioe);
            return -1;
        }
    }

    @Override
    public int peek() throws IOException {
        checkOpen();
        checkReadable();
        flushBuffer();

        long oldpos = file.getFilePointer();
        int r = file.read();
        file.seek(oldpos);
        return r;
    }

    @Override
    public int read() throws IOException {
        checkOpen();
        checkReadable();
        flushBuffer();

        return file.read();
    }

    @Override
    public int read(byte[] bytes, int offset, int length) throws IOException {
        checkOpen();
        checkReadable();
        flushBuffer();

        return file.read(bytes, offset, length);
    }

    @Override
    public void write(LuaString string) throws IOException {
        checkOpen();
        checkWritable();

        if (buffer == null) {
            writeToFile(string);
        } else {
            writeToBuffer(string);
        }
    }

    private void writeToBuffer(LuaString string) throws IOException {
        for (int n = 0; n < string.rawlen(); n++) {
            int b = string.luaByte(n);
            buffer.put((byte)b);

            if (buffer.remaining() == 0 || (bufferMode == FileBufferMode.LINE && b == '\n')) {
                flushBuffer();
            }
        }
    }

    private void flushBuffer() throws IOException {
        if (buffer == null || buffer.position() == 0) {
            return;
        }

        if (mode.isAppend()) {
            final long oldpos = file.getFilePointer();
            file.seek(file.length());
            try {
                file.write(buffer.array(), buffer.arrayOffset(), buffer.position());
            } finally {
                file.seek(oldpos);
            }
        } else {
            file.write(buffer.array(), buffer.arrayOffset(), buffer.position());
        }
        buffer.rewind();
    }

    private void writeToFile(LuaString string) throws IOException {
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
