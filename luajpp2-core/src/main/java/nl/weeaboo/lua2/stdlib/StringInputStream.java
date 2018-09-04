package nl.weeaboo.lua2.stdlib;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import javax.annotation.Nullable;

import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.vm.LuaValue;

@LuaSerializable
final class StringInputStream extends InputStream implements Serializable {

    private static final long serialVersionUID = 1L;

    private @Nullable LuaValue func;
    private @Nullable InputStream buffered;

    StringInputStream(LuaValue func) {
        this.func = func;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (func == null) {
            return -1;
        }

        int r = buffered.read(b, off, len);
        if (r < 0) {
            fillBuffer();
            if (buffered != null) {
                r = buffered.read(b, off, len);
            }
        }
        return r;
    }

    @Override
    public int read() throws IOException {
        if (func == null) {
            return -1;
        }

        int r = buffered.read();
        if (r < 0) {
            fillBuffer();
            if (buffered != null) {
                r = buffered.read();
            }
        }
        return r;
    }

    private void fillBuffer() {
        LuaValue val = func.call();
        if (val.isnil() || val.length() == 0) {
            func = null;
            buffered = null;
        } else {
            buffered = val.checkstring().toInputStream();
        }
    }
}