package nl.weeaboo.lua2.stdlib;

import java.io.IOException;
import java.io.InputStream;

import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.lib.LuaFileHandle;
import nl.weeaboo.lua2.vm.LuaTable;

@LuaSerializable
final class StdInFileHandle extends LuaFileHandle {

    private static final long serialVersionUID = 1L;

    public StdInFileHandle(LuaTable fileMethods) {
        super(fileMethods);
    }

    @Override
    public boolean isstdfile() {
        return true;
    }

    protected InputStream getInputStream() {
        return System.in;
    }

    @Override
    public boolean isclosed() {
        return false;
    }

    @Override
    public int peek() throws IOException {
        InputStream in = getInputStream();
        in.mark(1);
        int read = in.read();
        in.reset();
        return read;
    }

    @Override
    public int read() throws IOException {
        return getInputStream().read();
    }

    @Override
    public int read(byte[] bytes, int offset, int length) throws IOException {
        return getInputStream().read(bytes, offset, length);
    }

}