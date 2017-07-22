package nl.weeaboo.lua2.stdlib;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.vm.LuaValue;

@LuaSerializable
final class StringInputStream extends InputStream implements Serializable {

    private static final long serialVersionUID = 1L;

    private LuaValue func;
    private byte[] bytes;
    private int offset;

    StringInputStream(LuaValue func) {
        this.func = func;
    }

    @Override
    public int read() throws IOException {
        if (func == null) {
            return -1;
        }

        if (bytes == null) {
            LuaValue s = func.call();
            if (s.isnil()) {
                func = null;
                bytes = null;
                return -1;
            }
            bytes = s.tojstring().getBytes();
            offset = 0;
        }
        if (offset >= bytes.length) {
            return -1;
        }
        return bytes[offset++];
    }
}