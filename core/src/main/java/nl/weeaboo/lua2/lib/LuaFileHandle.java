package nl.weeaboo.lua2.lib;

import java.io.EOFException;
import java.io.IOException;
import java.io.Serializable;

import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.vm.LuaConstants;
import nl.weeaboo.lua2.vm.LuaError;
import nl.weeaboo.lua2.vm.LuaString;
import nl.weeaboo.lua2.vm.LuaTable;
import nl.weeaboo.lua2.vm.LuaValue;

@LuaSerializable
public abstract class LuaFileHandle extends LuaValue implements Serializable {

    private static final long serialVersionUID = 2L;

    private final LuaTable fileMethods;

    public LuaFileHandle(LuaTable fileMethods) {
        this.fileMethods = fileMethods;
    }

    public boolean isstdfile() {
        return false;
    }

    public void close() throws IOException {
        if (isstdfile()) {
            throw new IOException("stdfile instances can't be closed");
        }
    }

    public boolean isclosed() {
        return false;
    }

    /**
     * @param whence The base position from where to skip.
     *               <ul>
     *               <li>"set": Skip relative to position {@code 0}.
     *               <li>"cur": Skip relative to the current position.
     *               <li>"end": Skip relative to the end of the file.
     *               </ul>
     * @param bytecount Skip relative by this number of bytes.
     * @returns The new position
     */
    public int seek(String whence, int bytecount) throws IOException {
        throw new IOException("seek not supported");
    }

    /**
     * @param mode The output buffering mode to use.
     *        <ul>
     *        <li>"no": No buffering
     *        <li>"full": Buffer everything. Only outputs when the buffer is full or {@code #flush()} is
     *        manually called.
     *        <li>"line": Buffer until a line ending is output, or input is received from a terminal device.
     *        </ul>
     * @param size Buffer size
     */
    public void setvbuf(String mode, int size) {
        throw new LuaError("setvbuf not supported");
    }

    /**
     * Get length remaining to read, or {@code -1} if unknown.
     */
    public int remaining() {
        return -1;
    }

    /**
     * Peek ahead one character
     *
     * @throws EOFException If the file contains no further data.
     */
    public int peek() throws IOException {
        throw new IOException("peek not supported");
    }

    /**
     * return char if read, -1 if eof, throw IOException on other exception.
     */
    public int read() throws IOException {
        throw new IOException("read not supported");
    }

    /**
     * @param bytes Store read bytes into this buffer
     * @param offset Offset to use with {@code bytes}.
     * @param length Number of bytes that should be read.
     * @return The number of bytes read if positive, {@code -1} if the end of the file has been reached.
     * @throws IOException on other exception
     */
    public int read(byte[] bytes, int offset, int length) throws IOException {
        throw new IOException("read not supported");
    }

    /**
     * @param string The string to write
     */
    public void write(LuaString string) throws IOException {
        throw new IOException("write not supported");
    }

    /**
     * @throws IOException If the flush failed.
     */
    public void flush() throws IOException {
        // Default behavior: do nothing
    }

    @Override
    public LuaValue get(LuaValue key) {
        // delegate method access to file methods table
        return fileMethods.get(key);
    }

    @Override
    public int type() {
        // essentially a userdata instance
        return LuaConstants.TUSERDATA;
    }

    @Override
    public String typename() {
        return "userdata";
    }

    @Override
    public String tojstring() {
        // displays as "file" type
        return "file: " + Integer.toHexString(hashCode());
    }

}