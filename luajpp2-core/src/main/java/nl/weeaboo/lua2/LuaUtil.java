package nl.weeaboo.lua2;

import static nl.weeaboo.lua2.vm.LuaConstants.NONE;
import static nl.weeaboo.lua2.vm.LuaNil.NIL;
import static nl.weeaboo.lua2.vm.LuaValue.valueOf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import nl.weeaboo.lua2.compiler.LoadState;
import nl.weeaboo.lua2.luajava.LuajavaLib;
import nl.weeaboo.lua2.stdlib.DebugTrace;
import nl.weeaboo.lua2.vm.LuaClosure;
import nl.weeaboo.lua2.vm.LuaConstants;
import nl.weeaboo.lua2.vm.LuaNil;
import nl.weeaboo.lua2.vm.LuaString;
import nl.weeaboo.lua2.vm.LuaTable;
import nl.weeaboo.lua2.vm.LuaThread;
import nl.weeaboo.lua2.vm.LuaValue;
import nl.weeaboo.lua2.vm.Varargs;

public final class LuaUtil {

    private static final LuaString NEW = valueOf("new");
    private static final int DEFAULT_STACK_LIMIT = 8;

    private LuaUtil() {
    }

    /**
     * Makes the given class available to Lua by registering a global table.
     *
     * @see #registerClass(LuaValue, Class, String)
     */
    public static void registerClass(LuaValue globals, Class<?> clazz) {
        registerClass(globals, clazz, clazz.getSimpleName());
    }

    /**
     * Makes the given class available to Lua by registering a global table.
     *
     * @param globals The global table to register the class table in.
     * @param clazz Java type to register.
     * @param tableName Name of the class table.
     */
    public static <T> void registerClass(LuaValue globals, Class<T> clazz, String tableName) {
        LuaTable table = new LuaTable();
        if (clazz.isEnum()) {
            for (T val : clazz.getEnumConstants()) {
                Enum<?> e = (Enum<?>)val;
                table.rawset(e.name(), LuajavaLib.toUserdata(e, clazz));
            }
        } else {
            table.rawset(NEW, LuajavaLib.getConstructor(clazz));
        }
        globals.rawset(tableName, table);
    }

    /**
     * Compiles and runs a piece of Lua code in the given thread.
     * @throws LuaException If an error occurs while trying to compare or run the code.
     */
    public static Varargs eval(LuaThread thread, String code) throws LuaException {
        LuaClosure function = compileForEval(code, thread.getfenv());
        return thread.callFunctionInThread(function, LuaConstants.NONE);
    }

    /**
     * Compiles a snippet of Lua code as a runnable closure.
     * @throws LuaException If an error occurs while trying to compile the code.
     */
    public static LuaClosure compileForEval(String code, LuaValue env) throws LuaException {
        final String chunkName = "(eval)";
        try {
            Varargs result = NONE;
            try {
                // Try to evaluate as an expression
                result = LoadState.load("return " + code, chunkName, env);
            } catch (LuaException err) {
                // Try to evaluate as a statement, no value to return
                result = LoadState.load(code, chunkName, env);
            }

            LuaValue f = result.arg1();
            if (!f.isclosure()) {
                throw new LuaException(result.arg(2).tojstring());
            }
            return f.checkclosure();
        } catch (RuntimeException e) {
            throw LuaException.wrap("Error compiling code", e);
        } catch (IOException e) {
            throw LuaException.wrap("Error compiling code", e);
        }
    }

    /**
     * @see #getEntryForPath(LuaValue, String)
     */
    public static LuaValue getEntryForPath(LuaThread thread, String path) {
        return getEntryForPath(thread.getfenv(), path);
    }

    /**
     * Fetches a Lua value from a table.
     *
     * @param path The name of the function to find. If the name contains any dots, the name is
     *        interpreted as a path. For example, a function name of 'a.b.c' first searches the initial table
     *        for an entry named 'a', then searches that entry for 'b', then that entry for 'c'.
     * @return The value at the given path, or {@code LuaNil#NIL} if not found.
     */
    public static LuaValue getEntryForPath(LuaValue table, String path) {
        // Resolve a.b.c.d, ends with table=c
        int index;
        while (table != null && !table.isnil() && (index = path.indexOf('.')) >= 0) {
            String part = path.substring(0, index);
            table = table.get(LuaString.valueOf(part));
            path = path.substring(index + 1);
        }

        LuaValue func = LuaNil.NIL;
        if (table != null && !table.isnil()) {
            func = table.get(LuaString.valueOf(path));
        }
        return func;
    }

    /**
     * Returns the current call stack of the active Lua thread, or an empty list if no thread is active.
     * @see #getLuaStack()
     */
    public static List<String> getLuaStack() {
        return getLuaStack(LuaThread.getRunning());
    }

    /**
     * Returns the call stack of the given thread.
     */
    public static List<String> getLuaStack(LuaThread thread) {
        if (thread == null) {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<String>();
        for (int level = 1; level <= DEFAULT_STACK_LIMIT; level++) {
            String line = DebugTrace.fileline(thread, level);
            if (line == null) {
                break;
            }
            result.add(line);
        }
        return Collections.unmodifiableList(result);
    }


    /**
     * Turns the given string containing Lua source code declaring a simple
     * constant of type boolean, number or string into the proper LuaValue
     * subclass.
     */
    public static LuaValue parseLuaLiteral(String code) {
        if ("true".equals(code)) {
            return valueOf(true);
        } else if ("false".equals(code)) {
            return valueOf(false);
        } else  if (code.length() >= 2 && code.startsWith("\"") && code.endsWith("\"")) {
            String withoutQuotes = code.substring(1, code.length() - 1);
            return valueOf(unescape(withoutQuotes));
        } else if (code.length() >= 2 && code.startsWith("'") && code.endsWith("'")) {
            String withoutQuotes = code.substring(1, code.length() - 1);
            return valueOf(unescape(withoutQuotes));
        }

        try {
            if (code.startsWith("0x")) {
                return valueOf(Long.parseLong(code.substring(2), 16));
            } else {
                return valueOf(Double.parseDouble(code));
            }
        } catch (NumberFormatException nfe) {
            //Can't be parsed as a number
        }

        return NIL;
    }

    /**
     * Returns an independent copy of a {@link Varargs}.
     */
    public static Varargs copyArgs(Varargs in) {
        if (in == null) {
            return null;
        }
        final int inL = in.narg();
        if (inL == 0) {
            return NONE;
        }

        LuaValue[] array = new LuaValue[inL];
        for (int n = 0; n < array.length; n++) {
            array[n] = in.arg(1 + n);
        }
        return LuaValue.varargsOf(array);
    }

    private static final char[] escapeList = {
        '\"', '\"',
        '\'', '\'',
        '\\', '\\',
        'n', '\n',
        'r', '\r',
        't', '\t',
        'f', '\f',
        'a', '\u0007', // Bell character
        'b', '\b',
        'v', '\u000B',
    };

    /**
     * Escapes special characters (such as '\n') like they would be in a Lua string.
     * @return A string with the special characters replaced.
     * @see #escape(StringBuilder, String)
     */
    public static String escape(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        escape(sb, s);
        return sb.toString();
    }

    private static void escape(StringBuilder out, String s) {
        for (int n = 0; n < s.length(); n++) {
            char c = s.charAt(n);

            int t;
            for (t = 0; t < escapeList.length; t += 2) {
                if (c == escapeList[t + 1]) {
                    out.append('\\');
                    out.append(escapeList[t]);
                    break;
                }
            }
            if (t >= escapeList.length) {
                out.append(c);
            }
        }
    }

    /**
     * Performs the inverse operation of {@link #escape(String)}, replacing escaped special characters with
     * their non-escaped counterparts.
     *
     * @see #unescape(char)
     */
    public static String unescape(String s) {
        char[] chars = new char[s.length()];
        s.getChars(0, chars.length, chars, 0);

        int t = 0;
        for (int n = 0; n < chars.length; n++) {
            if (chars[n] == '\\') {
                n++;
                chars[t] = unescape(chars[n]);
            } else {
                chars[t] = chars[n];
            }
            t++;
        }
        return new String(chars, 0, t);
    }

    /**
     * Performs the inverse operation of {@link #escape(String)}, replacing an escaped special character with
     * their non-escaped counterparts.
     *
     * @param c The character following the '\\' in the escaped sequence.
     * @see #unescape(String)
     */
    public static char unescape(char c) {
        for (int n = 0; n < escapeList.length; n += 2) {
            if (c == escapeList[n]) {
                return escapeList[n + 1];
            }
        }
        return c;
    }

}
