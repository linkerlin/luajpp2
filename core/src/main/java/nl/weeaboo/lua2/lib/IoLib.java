/*******************************************************************************
 * Copyright (c) 2009-2011 Luaj.org. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ******************************************************************************/

package nl.weeaboo.lua2.lib;

import static nl.weeaboo.lua2.vm.LuaBoolean.TRUE;
import static nl.weeaboo.lua2.vm.LuaConstants.NONE;
import static nl.weeaboo.lua2.vm.LuaConstants.TNUMBER;
import static nl.weeaboo.lua2.vm.LuaConstants.TSTRING;
import static nl.weeaboo.lua2.vm.LuaNil.NIL;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;

import nl.weeaboo.lua2.LuaRunState;
import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.vm.LuaString;
import nl.weeaboo.lua2.vm.LuaTable;
import nl.weeaboo.lua2.vm.LuaValue;
import nl.weeaboo.lua2.vm.Varargs;

/**
 * Abstract base class extending {@link LibFunction} which implements the core of the lua standard {@code io}
 * library.
 * <p>
 * It contains the implementation of the io library support that is common to the JSE and JME platforms. In
 * practice on of the concrete IOLib subclasses is chosen: JseIoLib for the JSE platform, and JmeIoLib for the
 * JME platform.
 * <p>
 * The JSE implementation conforms almost completely to the C-based lua library, while the JME implementation
 * follows closely except in the area of random-access files, which are difficult to support properly on JME.
 * <p>
 * To instantiate and use it directly, link it into your globals table via {@link LuaValue#load(LuaValue)}
 * using code such as:
 *
 * <pre>
 * {
 *     &#064;code
 *     LuaTable _G = new LuaTable();
 *     _G.load(new JseIoLib());
 *     LuaThread.setGlobals(_G);
 *     _G.load(new JseBaseLib());
 *     _G.load(new PackageLib());
 *     _G.load(new JseIoLib());
 *     _G.get(&quot;io&quot;).get(&quot;write&quot;).call(LuaValue.valueOf(&quot;hello, world\n&quot;));
 * }
 * </pre>
 *
 * Doing so will ensure the library is properly initialized and loaded into the globals table.
 * <p>
 * This has been implemented to match as closely as possible the behavior in the corresponding library in C.
 *
 * @see LibFunction
 * @see <a href="http://www.lua.org/manual/5.1/manual.html#5.7">http://www.lua.org/manual/5.1/manual.html#5.7
 *      </a>
 */
@LuaSerializable
public abstract class IoLib extends OneArgFunction {

    private static final long serialVersionUID = -5905012974365980539L;

    private LuaFileHandle stdInHandle = null;
    private LuaFileHandle stdOutHandle = null;
    private LuaFileHandle stdErrHandle = null;

    private static final LuaValue STDIN = valueOf("stdin");
    private static final LuaValue STDOUT = valueOf("stdout");
    private static final LuaValue STDERR = valueOf("stderr");
    private static final LuaValue FILE = valueOf("file");
    private static final LuaValue CLOSED_FILE = valueOf("closed file");

    private static final int IO_CLOSE = 0;
    private static final int IO_FLUSH = 1;
    private static final int IO_INPUT = 2;
    private static final int IO_LINES = 3;
    private static final int IO_OPEN = 4;
    private static final int IO_OUTPUT = 5;
    private static final int IO_POPEN = 6;
    private static final int IO_READ = 7;
    private static final int IO_TMPFILE = 8;
    private static final int IO_TYPE = 9;
    private static final int IO_WRITE = 10;

    private static final int FILE_CLOSE = 11;
    private static final int FILE_FLUSH = 12;
    private static final int FILE_LINES = 13;
    private static final int FILE_READ = 14;
    private static final int FILE_SEEK = 15;
    private static final int FILE_SETVBUF = 16;
    private static final int FILE_WRITE = 17;

    private static final int IO_INDEX = 18;
    private static final int LINES_ITER = 19;

    public static final String[] IO_NAMES = { "close", "flush", "input", "lines", "open", "output", "popen",
            "read", "tmpfile", "type", "write", };

    public static final String[] FILE_NAMES = { "close", "flush", "lines", "read", "seek", "setvbuf",
            "write", };

    LuaTable fileMethods;

    public IoLib() {
    }

    @Override
    public LuaValue call(LuaValue arg) {
        // io lib functions
        LuaTable t = new LuaTable();
        bind(t, IoLibV.class, IO_NAMES);

        // create file methods table
        fileMethods = new LuaTable();
        bind(fileMethods, IoLibV.class, FILE_NAMES, FILE_CLOSE);

        // set up file metatable
        LuaTable mt = new LuaTable();
        bind(mt, IoLibV.class, new String[] { "__index" }, IO_INDEX);
        t.setmetatable(mt);

        // all functions link to library instance
        setLibInstance(t);
        setLibInstance(fileMethods);
        setLibInstance(mt);

        // return the table
        env.set("io", t);
        LuaRunState.getCurrent().setIsLoaded("io", t);
        return t;
    }

    private void setLibInstance(LuaTable t) {
        LuaValue[] k = t.keys();
        for (int i = 0, n = k.length; i < n; i++) {
            ((IoLibV)t.get(k[i])).iolib = this;
        }
    }

    /**
     * Wrap the standard input stream.
     */
    protected abstract LuaFileHandle wrapStdIn();

    /**
     * Wrap the standard output stream.
     */
    protected abstract LuaFileHandle wrapStdOut();

    /**
     * Wrap the standard error stream.
     */
    protected abstract LuaFileHandle wrapStdErr();

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
    protected abstract LuaFileHandle openFile(String filename, boolean readMode, boolean appendMode,
            boolean updateMode, boolean binaryMode) throws IOException;

    /**
     * Open a temporary file.
     *
     * @return File object if successful
     * @throws IOException if could not be opened
     */
    protected abstract LuaFileHandle tmpFile() throws IOException;

    /**
     * Start a new process and return a file for input or output
     *
     * @param prog the program to execute
     * @param mode "r" to read, "w" to write
     * @return File to read to or write from
     * @throws IOException if an i/o exception occurs
     */
    protected abstract LuaFileHandle openProgram(String prog, String mode) throws IOException;

    @LuaSerializable
    static final class IoLibV extends VarArgFunction {

        private static final long serialVersionUID = -8397648200399479786L;

        public IoLib iolib;

        /** Public no-arg constructor required for {@link LibFunction#bind(LuaValue, Class, String[])} */
        public IoLibV() {
        }

        public IoLibV(LuaValue env, String name, int opcode, IoLib iolib) {
            super();
            this.env = env;
            this.name = name;
            this.opcode = opcode;
            this.iolib = iolib;
        }

        @Override
        public Varargs invoke(Varargs args) {
            try {
                switch (opcode) {
                case IO_FLUSH:
                    return iolib._io_flush();
                case IO_TMPFILE:
                    return iolib._io_tmpfile();
                case IO_CLOSE:
                    return iolib._io_close(args.arg1());
                case IO_INPUT:
                    return iolib._io_input(args.arg1());
                case IO_OUTPUT:
                    return iolib._io_output(args.arg1());
                case IO_TYPE:
                    return iolib._io_type(args.arg1());
                case IO_POPEN:
                    return iolib._io_popen(args.checkjstring(1), args.optjstring(2, "r"));
                case IO_OPEN:
                    return iolib._io_open(args.checkjstring(1), args.optjstring(2, "r"));
                case IO_LINES:
                    return iolib._io_lines(args.isvalue(1) ? args.checkjstring(1) : null);
                case IO_READ:
                    return iolib._io_read(args);
                case IO_WRITE:
                    return iolib._io_write(args);

                case FILE_CLOSE:
                    return iolib._file_close(args.arg1());
                case FILE_FLUSH:
                    return iolib._file_flush(args.arg1());
                case FILE_SETVBUF:
                    return iolib._file_setvbuf(args.arg1(), args.checkjstring(2), args.optint(3, 1024));
                case FILE_LINES:
                    return iolib._file_lines(args.arg1());
                case FILE_READ:
                    return iolib._file_read(args.arg1(), args.subargs(2));
                case FILE_SEEK:
                    return iolib._file_seek(args.arg1(), args.optjstring(2, "cur"), args.optint(3, 0));
                case FILE_WRITE:
                    return iolib._file_write(args.arg1(), args.subargs(2));

                case IO_INDEX:
                    return iolib._io_index(args.arg(2));
                case LINES_ITER:
                    return iolib._lines_iter(env);
                }
            } catch (IOException ioe) {
                return errorresult(ioe);
            }
            return NONE;
        }
    }

    // io.flush() -> bool
    public Varargs _io_flush() throws IOException {
        checkopen(getStdOut());
        stdOutHandle.flush();
        return TRUE;
    }

    // io.tmpfile() -> file
    public Varargs _io_tmpfile() throws IOException {
        return tmpFile();
    }

    // io.close([file]) -> void
    public Varargs _io_close(LuaValue file) throws IOException {
        LuaFileHandle f = file.isnil() ? getStdOut() : checkfile(file);
        checkopen(f);
        return ioclose(f);
    }

    // io.input([file]) -> file
    public Varargs _io_input(LuaValue file) {
        if (file.isnil()) {
            stdInHandle = getStdIn();
        } else if (file.isstring()) {
            stdInHandle = ioopenfile(file.checkjstring(), "r");
        } else {
            stdInHandle = checkfile(file);
        }
        return stdInHandle;
    }

    // io.output(filename) -> file
    public Varargs _io_output(LuaValue filename) {
        if (filename.isnil()) {
            stdOutHandle = getStdOut();
        } else if (filename.isstring()) {
            stdOutHandle = ioopenfile(filename.checkjstring(), "w");
        } else {
            stdOutHandle = checkfile(filename);
        }
        return stdOutHandle;
    }

    // io.type(obj) -> "file" | "closed file" | nil
    public Varargs _io_type(LuaValue obj) {
        LuaFileHandle f = optfile(obj);
        return f != null ? f.isclosed() ? CLOSED_FILE : FILE : NIL;
    }

    // io.popen(prog, [mode]) -> file
    public Varargs _io_popen(String prog, String mode) throws IOException {
        return openProgram(prog, mode);
    }

    // io.open(filename, [mode]) -> file | nil,err
    public Varargs _io_open(String filename, String mode) throws IOException {
        return rawopenfile(filename, mode);
    }

    // io.lines(filename) -> iterator
    public Varargs _io_lines(String filename) {
        stdInHandle = filename == null ? getStdIn() : ioopenfile(filename, "r");
        checkopen(stdInHandle);
        return lines(stdInHandle);
    }

    // io.read(...) -> (...)
    public Varargs _io_read(Varargs args) throws IOException {
        checkopen(getStdIn());
        return ioread(stdInHandle, args);
    }

    // io.write(...) -> void
    public Varargs _io_write(Varargs args) throws IOException {
        checkopen(getStdOut());
        return iowrite(stdOutHandle, args);
    }

    // file:close() -> void
    public Varargs _file_close(LuaValue file) throws IOException {
        return ioclose(checkfile(file));
    }

    // file:flush() -> void
    public Varargs _file_flush(LuaValue file) throws IOException {
        checkfile(file).flush();
        return TRUE;
    }

    // file:setvbuf(mode,[size]) -> void
    public Varargs _file_setvbuf(LuaValue file, String mode, int size) {
        checkfile(file).setvbuf(mode, size);
        return TRUE;
    }

    // file:lines() -> iterator
    public Varargs _file_lines(LuaValue file) {
        return lines(checkfile(file));
    }

    // file:read(...) -> (...)
    public Varargs _file_read(LuaValue file, Varargs subargs) throws IOException {
        return ioread(checkfile(file), subargs);
    }

    // file:seek([whence][,offset]) -> pos | nil,error
    public Varargs _file_seek(LuaValue file, String whence, int offset) throws IOException {
        return valueOf(checkfile(file).seek(whence, offset));
    }

    // file:write(...) -> void
    public Varargs _file_write(LuaValue file, Varargs subargs) throws IOException {
        return iowrite(checkfile(file), subargs);
    }

    // __index, returns a field
    public Varargs _io_index(LuaValue v) {
        if (v.equals(STDOUT)) {
            return getStdOut();
        } else if (v.equals(STDIN)) {
            return getStdIn();
        } else if (v.equals(STDERR)) {
            return getStdErr();
        } else {
            return NIL;
        }
    }

    // lines iterator(s,var) -> var'
    public Varargs _lines_iter(LuaValue file) throws IOException {
        return freadline(checkfile(file));
    }

    private LuaFileHandle getStdIn() {
        if (stdInHandle == null) {
            stdInHandle = wrapStdIn();
        }
        return stdInHandle;
    }

    private LuaFileHandle getStdOut() {
        if (stdOutHandle == null) {
            stdOutHandle = wrapStdOut();
        }
        return stdOutHandle;
    }

    private LuaFileHandle getStdErr() {
        if (stdErrHandle == null) {
            stdErrHandle = wrapStdErr();
        }
        return stdErrHandle;
    }

    private LuaFileHandle ioopenfile(String filename, String mode) {
        try {
            return rawopenfile(filename, mode);
        } catch (Exception e) {
            error("io error: " + e.getMessage());
            return null;
        }
    }

    private static Varargs ioclose(LuaFileHandle f) throws IOException {
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

    private static Varargs errorresult(Exception ioe) {
        String s = ioe.getMessage();
        return errorresult("io error: " + (s != null ? s : ioe.toString()));
    }

    private static Varargs errorresult(String errortext) {
        return varargsOf(NIL, valueOf(errortext));
    }

    private Varargs lines(final LuaFileHandle f) {
        try {
            return new IoLibV(f, "lnext", LINES_ITER, this);
        } catch (Exception e) {
            return error("lines: " + e);
        }
    }

    private static Varargs iowrite(LuaFileHandle f, Varargs args) throws IOException {
        for (int i = 1, n = args.narg(); i <= n; i++) {
            f.write(args.checkstring(i));
        }
        return TRUE;
    }

    private static Varargs ioread(LuaFileHandle f, Varargs args) throws IOException {
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

    private static LuaFileHandle checkfile(LuaValue val) {
        LuaFileHandle f = optfile(val);
        if (f == null) {
            argerror(1, "not a file handle");
        }
        checkopen(f);
        return f;
    }

    private static LuaFileHandle optfile(LuaValue val) {
        return (val instanceof LuaFileHandle) ? (LuaFileHandle)val : null;
    }

    private static LuaFileHandle checkopen(LuaFileHandle file) {
        if (file.isclosed()) {
            error("attempt to use a closed file");
        }
        return file;
    }

    private LuaFileHandle rawopenfile(String filename, String mode) throws IOException {
        boolean isreadmode = mode.startsWith("r");
        if ("-".equals(filename)) {
            return isreadmode ? wrapStdIn() : wrapStdOut();
        }

        boolean isappend = mode.startsWith("a");
        boolean isupdate = mode.indexOf("+") > 0;
        boolean isbinary = mode.endsWith("b");
        return openFile(filename, isreadmode, isappend, isupdate, isbinary);
    }

    // ------------- file reading utilitied ------------------

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

}
