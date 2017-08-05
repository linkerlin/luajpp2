package nl.weeaboo.lua2.stdlib;

import java.io.IOException;
import java.io.Serializable;

import nl.weeaboo.lua2.io.LuaSerializable;

@LuaSerializable
final class FileOpenMode implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean readable;
    private boolean writable;
    private boolean truncate;
    private boolean append;

    private FileOpenMode() {
    }

    /**
     * @param mode File mode string compatible with {@code fopen} in C.
     */
    public static FileOpenMode fromString(String mode) throws IOException {
        FileOpenMode result = new FileOpenMode();
        if (mode.equals("r")) {
            result.readable = true;
        } else if (mode.equals("w")) {
            result.writable = true;
            result.truncate = true;
        } else if (mode.equals("a")) {
            result.writable = true;
            result.append = true;
        } else if (mode.equals("r+")) {
            result.readable = true;
            result.writable = true;
        } else if (mode.equals("w+")) {
            result.readable = true;
            result.writable = true;
            result.truncate = true;
        } else if (mode.equals("a+")) {
            result.readable = true;
            result.writable = true;
            result.append = true;
        } else {
            throw new IOException("Invalid file mode: " + mode);
        }
        return result;
    }

    public boolean isReadable() {
        return readable;
    }

    public boolean isWritable() {
        return writable;
    }

    public boolean isTruncate() {
        return truncate;
    }

    public boolean isAppend() {
        return append;
    }

}
