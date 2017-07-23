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
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;

import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.lib.LuaFileHandle;
import nl.weeaboo.lua2.lib.VarArgFunction;
import nl.weeaboo.lua2.lib2.LuaBoundFunction;
import nl.weeaboo.lua2.vm.LuaConstants;
import nl.weeaboo.lua2.vm.LuaError;
import nl.weeaboo.lua2.vm.LuaString;
import nl.weeaboo.lua2.vm.LuaTable;
import nl.weeaboo.lua2.vm.LuaValue;
import nl.weeaboo.lua2.vm.Varargs;

@LuaSerializable
public final class IoLib extends LuaModule {

    private static final long serialVersionUID = 1L;

    private static final LuaValue STDIN = valueOf("stdin");
    private static final LuaValue STDOUT = valueOf("stdout");
    private static final LuaValue STDERR = valueOf("stderr");
    private static final LuaValue FILE = valueOf("file");
    private static final LuaValue CLOSED_FILE = valueOf("closed file");

    private LuaFileHandle stdInHandle;
    private LuaFileHandle stdOutHandle;
    private LuaFileHandle stdErrHandle;

    private LuaFileHandle currentInput;
    private LuaFileHandle currentOutput;

    public IoLib() {
        super("io");
    }

    @Override
    protected void registerAdditional(LuaTable globals, LuaTable libTable) throws LuaException {
        super.registerAdditional(globals, libTable);

        libTable.setmetatable(tableOf(new LuaValue[] {
                LuaConstants.INDEX, new IoIndexFunction()
        }));

        new FileLib().register();

        // Initialize file handles
        LuaTable fileTable = globals.rawget("file").checktable();
        stdInHandle = new StdInFileHandle(fileTable);
        currentInput = stdInHandle;
        stdOutHandle = new StdOutFileHandle(fileTable, false);
        currentOutput = stdOutHandle;
        stdErrHandle = new StdOutFileHandle(fileTable, true);
    }

    /** io.close([file]) -> void */
    @LuaBoundFunction
    public Varargs close(Varargs args) throws IOException {
        LuaFileHandle f = args.isnil(1) ? stdOutHandle : checkfile(args.arg1());
        checkopen(f);
        return doClose(f);
    }

    /**
     * io.flush() -> bool
     * @param args Not used.
     */
    @LuaBoundFunction
    public Varargs flush(Varargs args) throws IOException {
        checkopen(currentOutput);
        currentOutput.flush();
        return TRUE;
    }

    /**
     * io.input([file]) -> file
     */
    @LuaBoundFunction
    public Varargs input(Varargs args) throws IOException {
        if (args.isnil(1)) {
            currentInput = stdInHandle;
        } else if (args.isstring(1)) {
            currentInput = doOpenFile(args.checkjstring(1), "r");
        } else {
            currentInput = checkfile(args.arg(1));
        }
        return currentInput;
    }

    /**
     * io.output(filename) -> file
     */
    @LuaBoundFunction
    public Varargs output(Varargs args) throws IOException {
        if (args.isnil(1)) {
            currentOutput = stdOutHandle;
        } else if (args.isstring(1)) {
            stdOutHandle = doOpenFile(args.checkjstring(1), "w");
        } else {
            stdOutHandle = checkfile(args.arg(1));
        }
        return stdOutHandle;
    }

    /**
     * io.lines(filename) -> iterator
     */
    @LuaBoundFunction
    public Varargs lines(Varargs args) throws IOException {
        LuaFileHandle file = (args.isstring(1) ? doOpenFile(args.checkjstring(1), "r") : currentInput);
        checkopen(file);
        return lines(file);
    }

    /**
     * io.open(filename, [mode]) -> file | nil,err
     */
    @LuaBoundFunction
    public Varargs open(Varargs args) throws IOException {
        String filename = args.checkjstring(1);
        String mode = args.optjstring(2, "r");

        return doOpenFile(filename, mode);
    }

    /**
     * io.popen(prog, [mode]) -> file
     */
    @LuaBoundFunction
    public Varargs popen(Varargs args) throws IOException {
        String program = args.checkjstring(1);
        String mode = args.optjstring(2, "r");
        return openProgram(program, mode);
    }

    /**
     * Start a new process and return a file for input or output
     *
     * @param prog the program to execute
     * @param mode "r" to read, "w" to write
     * @return File to read to or write from
     * @throws IOException if an i/o exception occurs
     */
    protected LuaFileHandle openProgram(String prog, String mode) throws IOException {
        throw new IOException("Unable to open program: " + prog);
    }

    /**
     * io.read(...) -> (...)
     */
    @LuaBoundFunction
    public Varargs read(Varargs args) throws IOException {
        checkopen(currentInput);
        return doRead(currentInput, args);
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
                if (fmt.length() == 2 && fmt.luaByte(0) == '*') {
                    switch (fmt.luaByte(1)) {
                    case 'n':
                        vi = freadnumber(f);
                        break;
                    case 'l':
                        vi = freadline(f);
                        break;
                    case 'a':
                        vi = freadall(f);
                        break;
                    }
                }
                return argerror(i + 1, "(invalid format)");
            default:
                return argerror(i + 1, "(invalid format)");
            }

            v[i++] = vi;
        }
        return varargsOf(v, 0, i);
    }

    /**
     * io.write(...) -> void
     */
    @LuaBoundFunction
    public Varargs write(Varargs args) throws IOException {
        checkopen(currentOutput);
        return doWrite(currentOutput, args);
    }

    static Varargs doWrite(LuaFileHandle f, Varargs args) throws IOException {
        for (int i = 1, n = args.narg(); i <= n; i++) {
            f.write(args.checkstring(i));
        }
        return TRUE;
    }

    /**
     * io.tmpfile() -> file
     * @param args Not used.
     */
    @LuaBoundFunction
    public Varargs tmpfile(Varargs args) throws IOException {
        return createTempFile();
    }

    /**
     * Open a temporary file.
     *
     * @return File object if successful
     * @throws IOException if could not be opened
     */
    protected LuaFileHandle createTempFile() throws IOException {
        throw new IOException("Unable to create temp file");
    }

    /** io.type(obj) -> "file" | "closed file" | nil */
    @LuaBoundFunction
    public Varargs type(Varargs args) {
        LuaFileHandle f = optfile(args.arg(1));
        if (f == null) {
            return NIL;
        }

        return (f.isclosed() ? CLOSED_FILE : FILE);
    }

    private static LuaFileHandle optfile(LuaValue val) {
        return (val instanceof LuaFileHandle) ? (LuaFileHandle)val : null;
    }

    static LuaFileHandle checkfile(LuaValue val) {
        LuaFileHandle f = optfile(val);
        if (f == null) {
            argerror(1, "not a file handle");
        }
        checkopen(f);
        return f;
    }

    private static LuaFileHandle checkopen(LuaFileHandle file) {
        if (file.isclosed()) {
            throw new LuaError("attempt to use a closed file");
        }
        return file;
    }

    private LuaFileHandle doOpenFile(String filename, String mode) throws IOException {
        boolean isreadmode = mode.startsWith("r");
        if ("-".equals(filename)) {
            return isreadmode ? stdInHandle : stdOutHandle;
        }

        boolean isappend = mode.startsWith("a");
        boolean isupdate = mode.indexOf("+") > 0;
        boolean isbinary = mode.endsWith("b");
        return openFile(filename, isreadmode, isappend, isupdate, isbinary);
    }

    /**
     * Open a file in a particular mode.
     *
     * @param readMode true if opening in read mode
     * @param appendMode true if opening in append mode
     * @param updateMode true if opening in update mode
     * @param binaryMode true if opening in binary mode
     * @return File object if successful
     * @throws IOException if could not be opened
     */
    protected LuaFileHandle openFile(String filename, boolean readMode, boolean appendMode,
            boolean updateMode, boolean binaryMode) throws IOException {

        throw new FileNotFoundException(filename);
    }

    static Varargs doClose(LuaFileHandle f) throws IOException {
        if (f.isstdfile()) {
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


    public static LuaValue freadbytes(LuaFileHandle f, int count) throws IOException {
        byte[] b = new byte[count];
        int r;
        if ((r = f.read(b, 0, b.length)) < 0) {
            return NIL;
        }
        return LuaString.valueOf(b, 0, r);
    }

    public static LuaValue freaduntil(LuaFileHandle f, boolean lineonly) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int c;
        try {
            if (lineonly) {
                loop: while ((c = f.read()) > 0) {
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
        } catch (EOFException e) {
            c = -1;
        }


        if (c < 0 && baos.size() == 0) {
            return NIL;
        } else {
            return LuaString.valueOf(baos.toByteArray());
        }
    }

    public static LuaValue freadline(LuaFileHandle f) throws IOException {
        return freaduntil(f, true);
    }

    public static LuaValue freadall(LuaFileHandle f) throws IOException {
        int n = f.remaining();
        if (n >= 0) {
            return freadbytes(f, n);
        } else {
            return freaduntil(f, false);
        }
    }

    public static LuaValue freadnumber(LuaFileHandle f) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        freadchars(f, " \t\r\n", null);
        freadchars(f, "-+", baos);
        // freadchars(f,"0",baos);
        // freadchars(f,"xX",baos);
        freadchars(f, "0123456789", baos);
        freadchars(f, ".", baos);
        freadchars(f, "0123456789", baos);
        // freadchars(f,"eEfFgG",baos);
        // freadchars(f,"+-",baos);
        // freadchars(f,"0123456789",baos);
        String s = baos.toString();
        return s.length() > 0 ? valueOf(Double.parseDouble(s)) : NIL;
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
