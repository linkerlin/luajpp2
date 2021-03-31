package nl.weeaboo.lua2.stdlib;

import static nl.weeaboo.lua2.vm.LuaBoolean.TRUE;
import static nl.weeaboo.lua2.vm.LuaConstants.TNUMBER;
import static nl.weeaboo.lua2.vm.LuaConstants.TSTRING;
import static nl.weeaboo.lua2.vm.LuaNil.NIL;
import static nl.weeaboo.lua2.vm.LuaValue.argerror;
import static nl.weeaboo.lua2.vm.LuaValue.tableOf;
import static nl.weeaboo.lua2.vm.LuaValue.valueOf;
import static nl.weeaboo.lua2.vm.LuaValue.varargsOf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;

import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.LuaRunState;
import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.lib.LuaBoundFunction;
import nl.weeaboo.lua2.lib.LuaFileHandle;
import nl.weeaboo.lua2.lib.VarArgFunction;
import nl.weeaboo.lua2.stdlib.FileLib.LinesIterFunction;
import nl.weeaboo.lua2.vm.LuaConstants;
import nl.weeaboo.lua2.vm.LuaString;
import nl.weeaboo.lua2.vm.LuaTable;
import nl.weeaboo.lua2.vm.LuaValue;
import nl.weeaboo.lua2.vm.Varargs;

/**
 * IO library
 */
@LuaSerializable
public final class IoLib extends LuaModule {

    private static final long serialVersionUID = 1L;

    private static final LuaValue STDIN = valueOf("stdin");
    private static final LuaValue STDOUT = valueOf("stdout");
    private static final LuaValue STDERR = valueOf("stderr");
    private static final LuaValue FILE = valueOf("file");
    private static final LuaValue CLOSED_FILE = valueOf("closed file");

    private final ILuaIoImpl impl;

    private LuaTable fileTable;
    private LuaFileHandle stdInHandle;
    private LuaFileHandle stdOutHandle;
    private LuaFileHandle stdErrHandle;

    // These values use custom serialization, see readObject/writeObject
    private transient AtomicReference<LuaFileHandle> currentInput;
    private transient AtomicReference<LuaFileHandle> currentOutput;

    IoLib(ILuaIoImpl impl) {
        super("io");

        this.impl = impl;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();

        out.writeObject(getCurrentInput());
        out.writeObject(getCurrentOutput());
    }

    private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
        in.defaultReadObject();

        setCurrentInput((LuaFileHandle)in.readObject());
        setCurrentOutput((LuaFileHandle)in.readObject());
    }

    @Override
    protected void registerAdditional(LuaTable globals, LuaTable libTable) throws LuaException {
        super.registerAdditional(globals, libTable);

        libTable.setmetatable(tableOf(new LuaValue[] {
                LuaConstants.META_INDEX, new IoIndexFunction()
        }));

        new FileLib().register();

        // Initialize file handles
        fileTable = getFileTable();
        stdInHandle = new StdInFileHandle(fileTable);
        setCurrentInput(stdInHandle);
        stdOutHandle = new StdOutFileHandle(fileTable, false);
        setCurrentOutput(stdOutHandle);
        stdErrHandle = new StdOutFileHandle(fileTable, true);
    }

    private LuaFileHandle getCurrentInput() {
        LuaFileHandle result = null;
        if (currentInput != null) {
            result = currentInput.get();
        }
        return (result != null ? result : stdInHandle);
    }

    private void setCurrentInput(LuaFileHandle file) {
        currentInput = new AtomicReference<>(file);
    }

    private LuaFileHandle getCurrentOutput() {
        LuaFileHandle result = null;
        if (currentOutput != null) {
            result = currentOutput.get();
        }
        return (result != null ? result : stdOutHandle);
    }

    private void setCurrentOutput(LuaFileHandle file) {
        currentOutput = new AtomicReference<>(file);
    }

    static LuaTable getFileTable() {
        LuaTable globals = LuaRunState.getCurrent().getGlobalEnvironment();
        return globals.rawget("file").checktable();
    }

    /**
     * {@code io.close([file]) -> void}
     * @throws IOException if this operation fails.
     */
    @LuaBoundFunction
    public Varargs close(Varargs args) throws IOException {
        LuaFileHandle f = args.isnil(1) ? getCurrentOutput() : checkfile(args.arg1());
        checkopen(f);
        return doClose(f);
    }

    /**
     * {@code io.flush() -> bool}
     * @param args Not used.
     * @throws IOException if this operation fails.
     */
    @LuaBoundFunction
    public Varargs flush(Varargs args) throws IOException {
        LuaFileHandle file = getCurrentOutput();
        checkopen(file);
        file.flush();
        return TRUE;
    }

    /**
     * {@code io.input([file]) -> file}
     * @throws IOException if this operation fails.
     */
    @LuaBoundFunction
    public Varargs input(Varargs args) throws IOException {
        if (args.isnil(1)) {
            // Do nothing, just return the current value
        } else if (args.isstring(1)) {
            setCurrentInput(doOpenFile(args.checkjstring(1), "r"));
        } else {
            setCurrentInput(checkfile(args.arg(1)));
        }
        return getCurrentInput();
    }

    /**
     * {@code io.output(filename) -> file}
     * @throws IOException if this operation fails.
     */
    @LuaBoundFunction
    public Varargs output(Varargs args) throws IOException {
        if (args.isnil(1)) {
            // Do nothing, just return the current value
        } else if (args.isstring(1)) {
            setCurrentOutput(doOpenFile(args.checkjstring(1), "w"));
        } else {
            setCurrentOutput(checkfile(args.arg(1)));
        }
        return getCurrentOutput();
    }

    /**
     * {@code io.lines(filename) -> iterator}
     * @throws IOException if this operation fails.
     */
    @LuaBoundFunction
    public Varargs lines(Varargs args) throws IOException {
        LuaFileHandle file;
        boolean shouldClose;
        if (args.isstring(1)) {
            file = doOpenFile(args.checkjstring(1), "r");
            shouldClose = true;
        } else {
            file = getCurrentInput();
            shouldClose = false;
        }
        checkopen(file);

        return new LinesIterFunction(file, shouldClose);
    }

    /**
     * {@code io.open(filename, [mode]) -> file | nil,err}
     */
    @LuaBoundFunction
    public Varargs open(Varargs args) {
        String filename = args.checkjstring(1);
        String mode = args.optjstring(2, "r");

        try {
            return doOpenFile(filename, mode);
        } catch (IOException e) {
            String message = "Error opening file (" + filename + "): " + e.toString();
            return varargsOf(NIL, valueOf(message), valueOf(0));
        }
    }

    /**
     * {@code io.popen(prog, [mode]) -> file}
     * @throws IOException if this operation fails.
     */
    @LuaBoundFunction
    public Varargs popen(Varargs args) throws IOException {
        String program = args.checkjstring(1);
        String mode = args.optjstring(2, "r");
        return impl.openProgram(program, mode);
    }

    /**
     * {@code io.read(...) -> (...)}
     * @throws IOException if this operation fails.
     */
    @LuaBoundFunction
    public Varargs read(Varargs args) throws IOException {
        LuaFileHandle file = getCurrentInput();
        checkopen(file);
        return doRead(file, args);
    }

    static Varargs doRead(LuaFileHandle f, Varargs args) throws IOException {
        // The behavior when called with no args is as if we're called with "*l"
        if (args.narg() == 0) {
            return freadline(f);
        }

        LuaValue[] v = new LuaValue[args.narg()];

        int i = 0;
        while (i < v.length) {
            LuaValue ai = args.arg(i + 1);

            LuaValue vi;
            switch (ai.type()) {
            case TNUMBER:
                vi = freadbytes(f, ai.toint());
                break;
            case TSTRING:
                LuaString fmt = ai.checkstring();
                if (fmt.length() > 0) {
                    int fmtCharIndex = 0;

                    // '*' prefix is optional
                    if (fmt.luaByte(0) == '*') {
                        fmtCharIndex++;
                    }

                    if (fmt.length() <= fmtCharIndex) {
                        throw argerror(i + 1, "(invalid format)");
                    }

                    switch (fmt.luaByte(fmtCharIndex)) {
                    case 'n':
                        vi = freadnumber(f);
                        break;
                    case 'l':
                        vi = freadline(f);
                        break;
                    case 'a':
                        vi = freadall(f);
                        // '*a' ignores end-of-file and always succeeds
                        if (vi.isnil()) {
                            vi = LuaConstants.EMPTYSTRING;
                        }
                        break;
                    default:
                        throw argerror(i + 1, "(invalid format)");
                    }
                } else {
                    throw argerror(i + 1, "(invalid format)");
                }
                break;
            default:
                throw argerror(i + 1, "(invalid format)");
            }

            v[i++] = vi;
        }
        return varargsOf(v, 0, i);
    }

    /**
     * {@code io.write(...) -> void}
     * @throws IOException if this operation fails.
     */
    @LuaBoundFunction
    public Varargs write(Varargs args) throws IOException {
        LuaFileHandle file = getCurrentOutput();
        checkopen(file);
        return doWrite(file, args);
    }

    static Varargs doWrite(LuaFileHandle f, Varargs args) throws IOException {
        for (int i = 1, n = args.narg(); i <= n; i++) {
            f.write(args.checkstring(i));
        }
        return TRUE;
    }

    /**
     * {@code io.tmpfile() -> file}
     * @param args Not used.
     * @throws IOException if this operation fails.
     */
    @LuaBoundFunction
    public Varargs tmpfile(Varargs args) throws IOException {
        return impl.createTempFile(fileTable);
    }

    /** {@code io.type(obj) -> "file" | "closed file" | nil} */
    @LuaBoundFunction
    public Varargs type(Varargs args) {
        LuaFileHandle f = optfile(args.arg(1));
        if (f == null) {
            return NIL;
        }

        return (f.isClosed() ? CLOSED_FILE : FILE);
    }

    static @Nullable LuaFileHandle optfile(LuaValue val) {
        return (val instanceof LuaFileHandle) ? (LuaFileHandle)val : null;
    }

    static LuaFileHandle checkfile(LuaValue val) {
        LuaFileHandle f = optfile(val);
        if (f == null) {
            throw argerror(1, "not a file handle");
        }
        checkopen(f);
        return f;
    }

    private static LuaFileHandle checkopen(LuaFileHandle file) {
        if (file == null || file.isClosed()) {
            throw new LuaException("attempt to use a closed file");
        }
        return file;
    }

    private LuaFileHandle doOpenFile(String filename, String mode) throws IOException {
        boolean isreadmode = mode.startsWith("r");
        if ("-".equals(filename)) {
            return isreadmode ? stdInHandle : stdOutHandle;
        }

        return impl.openFile(fileTable, filename, FileOpenMode.fromString(mode));
    }

    static Varargs doClose(LuaFileHandle f) throws IOException {
        if (f.isStdFile()) {
            return errorresult("cannot close standard file");
        } else {
            f.close();
            return successresult();
        }
    }

    private static Varargs successresult() {
        return TRUE;
    }

    private static Varargs errorresult(String errortext) {
        return varargsOf(NIL, valueOf(errortext));
    }

    static LuaValue freadbytes(LuaFileHandle f, int count) throws IOException {
        if (count == 0) {
            if (f.peek() == -1) {
                // End of file reached
                return NIL;
            } else {
                return LuaConstants.EMPTYSTRING;
            }
        }

        byte[] b = new byte[count];
        int r;
        if ((r = f.read(b, 0, b.length)) < 0) {
            return NIL;
        }
        return LuaString.valueOf(b, 0, r);
    }

    static LuaValue freaduntil(LuaFileHandle f, boolean lineonly) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int c;
        if (lineonly) {
            loop:
            while ((c = f.read()) > 0) {
                switch (c) {
                case '\r':
                    break;
                case '\n':
                    break loop;
                default:
                    baos.write(c);
                    break;
                }
            }
        } else {
            while ((c = f.read()) > 0) {
                baos.write(c);
            }
        }

        if (c < 0 && baos.size() == 0) {
            return NIL;
        } else {
            return LuaString.valueOf(baos.toByteArray());
        }
    }

    static LuaValue freadline(LuaFileHandle f) throws IOException {
        return freaduntil(f, true);
    }

    static LuaValue freadall(LuaFileHandle f) throws IOException {
        int n = f.remaining();
        if (n >= 0) {
            return freadbytes(f, n);
        } else {
            return freaduntil(f, false);
        }
    }

    static LuaValue freadnumber(LuaFileHandle f) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        freadchars(f, " \t\r\n", null);
        freadchars(f, "-+", baos);
        freadchars(f, "0", baos);
        freadchars(f, "xX", baos);
        freadchars(f, "0123456789", baos);
        freadchars(f, ".", baos);
        freadchars(f, "0123456789", baos);
        freadchars(f, "eEfFgG", baos);
        freadchars(f, "+-", baos);
        freadchars(f, "0123456789", baos);

        if (baos.size() == 0) {
            return NIL;
        }
        return valueOf(Double.parseDouble(baos.toString("ASCII")));
    }

    private static void freadchars(LuaFileHandle f, String chars, ByteArrayOutputStream baos)
            throws IOException {
        int c;
        while (true) {
            c = f.peek();
            if (chars.indexOf(c) < 0) {
                return;
            }
            f.read();
            if (baos != null) {
                baos.write(c);
            }
        }
    }

    @LuaSerializable
    private final class IoIndexFunction extends VarArgFunction {

        private static final long serialVersionUID = 1L;

        public IoIndexFunction() {
            name = "__index";
        }

        @Override
        public Varargs invoke(Varargs args) {
            LuaValue v = args.arg(2);
            if (v.equals(STDOUT)) {
                return stdOutHandle;
            } else if (v.equals(STDIN)) {
                return stdInHandle;
            } else if (v.equals(STDERR)) {
                return stdErrHandle;
            } else {
                return NIL;
            }
        }
    }
}
