package nl.weeaboo.lua2.stdlib;

import static nl.weeaboo.lua2.stdlib.IoLib.checkfile;
import static nl.weeaboo.lua2.vm.LuaBoolean.TRUE;
import static nl.weeaboo.lua2.vm.LuaValue.valueOf;

import java.io.IOException;

import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.LuaRunState;
import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.lib.LuaFileHandle;
import nl.weeaboo.lua2.lib.VarArgFunction;
import nl.weeaboo.lua2.lib2.LuaBoundFunction;
import nl.weeaboo.lua2.lib2.LuaLib;
import nl.weeaboo.lua2.vm.LuaError;
import nl.weeaboo.lua2.vm.LuaTable;
import nl.weeaboo.lua2.vm.Varargs;

@LuaSerializable
public final class FileLib extends LuaLib {

    private static final long serialVersionUID = 1L;

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
     */
    @LuaBoundFunction
    public Varargs close(Varargs args) throws IOException {
        return IoLib.doClose(checkfile(args.arg1()));
    }

    /**
     * file:flush() -> void.
     */
    @LuaBoundFunction
    public Varargs flush(Varargs args) throws IOException {
        checkfile(args.arg1()).flush();
        return TRUE;
    }

    /**
     * file:setvbuf(mode,[size]) -> void.
     */
    @LuaBoundFunction
    public Varargs setvbuf(Varargs args) {
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
        return new LinesIterFunction(file);
    }

    /**
     * file:seek([whence][,offset]) -> pos | nil,error.
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
    public Varargs read(Varargs args) throws IOException {
        LuaFileHandle file = checkfile(args.arg1());
        Varargs subargs = args.subargs(2);

        return IoLib.doRead(file, subargs);
    }

    /**
     * file:write(...) -> void
     */
    @LuaBoundFunction
    public Varargs write(Varargs args) throws IOException {
        LuaFileHandle file = checkfile(args.arg1());
        Varargs subargs = args.subargs(2);

        return IoLib.doWrite(file, subargs);
    }

    @LuaSerializable
    private static class LinesIterFunction extends VarArgFunction {

        private static final long serialVersionUID = 1L;

        private final LuaFileHandle file;

        public LinesIterFunction(LuaFileHandle file) {
            this.file = file;
        }

        /** lines iterator(s,var) -> var'. */
        @Override
        public Varargs invoke(Varargs args) {
            try {
                return IoLib.freadline(checkfile(file));
            } catch (IOException ioe) {
                throw new LuaError("I/O error from lines iterator for: " + file, ioe);
            }
        }

    }

}
