package nl.weeaboo.lua2.stdlib;

import static nl.weeaboo.lua2.stdlib.IoLib.checkfile;
import static nl.weeaboo.lua2.stdlib.IoLib.optfile;
import static nl.weeaboo.lua2.vm.LuaBoolean.TRUE;
import static nl.weeaboo.lua2.vm.LuaConstants.NONE;
import static nl.weeaboo.lua2.vm.LuaNil.NIL;
import static nl.weeaboo.lua2.vm.LuaValue.valueOf;
import static nl.weeaboo.lua2.vm.LuaValue.varargsOf;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.LuaRunState;
import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.lib.LuaBoundFunction;
import nl.weeaboo.lua2.lib.LuaFileHandle;
import nl.weeaboo.lua2.lib.LuaLib;
import nl.weeaboo.lua2.lib.VarArgFunction;
import nl.weeaboo.lua2.vm.LuaTable;
import nl.weeaboo.lua2.vm.LuaValue;
import nl.weeaboo.lua2.vm.Varargs;

@LuaSerializable
public final class FileLib extends LuaLib {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(FileLib.class);

    FileLib() {
    }

    @Override
    public void register() throws LuaException {
        LuaRunState lrs = LuaRunState.getCurrent();
        LuaTable globals = lrs.getGlobalEnvironment();

        LuaTable fileTable = new LuaTable();
        registerFunctions(globals, fileTable);
        globals.rawset("file", fileTable);
    }

    /**
     * file:close() -> void.
     * @throws IOException if this operation fails.
     */
    @LuaBoundFunction
    public Varargs close(Varargs args) throws IOException {
        LuaFileHandle file = optfile(args.arg1());
        if (file == null) {
            return NONE;
        }
        return IoLib.doClose(file);
    }

    /**
     * file:flush() -> void.
     * @throws IOException if this operation fails.
     */
    @LuaBoundFunction
    public Varargs flush(Varargs args) throws IOException {
        checkfile(args.arg1()).flush();
        return TRUE;
    }

    /**
     * file:setvbuf(mode,[size]) -> void.
     * @throws IOException if this operation fails.
     */
    @LuaBoundFunction
    public Varargs setvbuf(Varargs args) throws IOException {
        LuaFileHandle file = checkfile(args.arg1());
        String mode = args.checkjstring(2);
        int size = args.optint(3, 1024);

        file.setvbuf(mode, size);
        return TRUE;
    }

    /**
     * file:lines() -> iterator.
     */
    @LuaBoundFunction
    public Varargs lines(Varargs args) {
        LuaFileHandle file = checkfile(args.arg1());
        return new LinesIterFunction(file, false);
    }

    /**
     * file:seek([whence][,offset]) -> pos | nil,error.
     * @throws IOException if this operation fails.
     */
    @LuaBoundFunction
    public Varargs seek(Varargs args) throws IOException {
        LuaFileHandle file = checkfile(args.arg1());
        String whence = args.optjstring(2, "cur");
        int offset = args.optint(3, 0);

        return valueOf(file.seek(whence, offset));
    }

    /**
     * file:read(...) -> (...)
     */
    @LuaBoundFunction
    public Varargs read(Varargs args) {
        LuaFileHandle file = checkfile(args.arg1());
        Varargs subargs = args.subargs(2);

        try {
            return IoLib.doRead(file, subargs);
        } catch (IOException e) {
            return varargsOf(NIL, valueOf(e.toString()), valueOf(1));
        }
    }

    /**
     * file:write(...) -> void
     */
    @LuaBoundFunction
    public Varargs write(Varargs args) {
        LuaFileHandle file = checkfile(args.arg1());
        Varargs subargs = args.subargs(2);

        try {
            return IoLib.doWrite(file, subargs);
        } catch (IOException e) {
            return varargsOf(NIL, valueOf(e.toString()), valueOf(1));
        }
    }

    @LuaSerializable
    static class LinesIterFunction extends VarArgFunction {

        private static final long serialVersionUID = 1L;

        private final LuaFileHandle file;
        private final boolean shouldClose;

        public LinesIterFunction(LuaFileHandle file, boolean shouldClose) {
            this.file = file;
            this.shouldClose = shouldClose;
        }

        /** lines iterator(s,var) -> var'. */
        @Override
        public Varargs invoke(Varargs args) {
            try {
                LuaValue line = IoLib.freadline(file);
                if (line.isnil()) {
                    closeFileIfNeeded();
                }
                return line;
            } catch (IOException ioe) {
                closeFileIfNeeded();
                throw LuaException.wrap("I/O error from lines iterator for: " + file, ioe);
            }
        }

        private void closeFileIfNeeded() {
            if (shouldClose && !file.isStdFile()) {
                LOG.debug("Closing file after lines() iterator finished: {}", file);

                try {
                    file.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }

    }

}
