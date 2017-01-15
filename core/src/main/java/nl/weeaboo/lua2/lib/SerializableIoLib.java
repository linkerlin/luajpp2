package nl.weeaboo.lua2.lib;

import java.io.FileNotFoundException;
import java.io.IOException;

import nl.weeaboo.lua2.io.LuaSerializable;

@LuaSerializable
public final class SerializableIoLib extends IoLib {

	private static final long serialVersionUID = -3479467845682001638L;

	@Override
	protected LuaFileHandle openFile(String filename, boolean readMode, boolean appendMode, boolean updateMode,
			boolean binaryMode) throws IOException
	{
		throw new FileNotFoundException(filename);
	}

	@Override
	protected LuaFileHandle tmpFile() throws IOException {
		throw new IOException("Unable to create temp file");
	}

	@Override
	protected LuaFileHandle openProgram(String prog, String mode) throws IOException {
		throw new IOException("Unable to open program");
	}

    @Override
    protected LuaFileHandle wrapStdIn() {
        return new StdInFileHandle(fileMethods);
    }

    @Override
    protected LuaFileHandle wrapStdOut() {
        return new StdOutFileHandle(fileMethods, false);
    }

    @Override
    protected LuaFileHandle wrapStdErr() {
        return new StdOutFileHandle(fileMethods, true);
    }

}
