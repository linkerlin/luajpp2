package nl.weeaboo.lua2.stdlib;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.vm.LuaString;
import nl.weeaboo.lua2.vm.LuaValue;

@LuaSerializable
final class StringInputStream extends InputStream implements Serializable {

    private static final long serialVersionUID = 1L;

    private LuaValue func;
    private byte[] bytes = new byte[0];
    private int offset;

    StringInputStream(LuaValue func) {
        this.func = func;
    }

    @Override
    public int read() throws IOException {
        if (func == null) {
            return -1;
        }

        if (offset >= bytes.length) {
            LuaValue val = func.call();
            if (val.isnil() || val.length() == 0) {
                func = null;
                bytes = null;
                return -1;
            }

            LuaString str = val.checkstring();
            bytes = new byte[str.length()];
            str.copyInto(0, bytes, 0, bytes.length);

            offset = 0;
        }
        return bytes[offset++];
    }
}