package nl.weeaboo.lua2;

import static nl.weeaboo.lua2.vm.LuaConstants.NONE;
import static nl.weeaboo.lua2.vm.LuaNil.NIL;
import static nl.weeaboo.lua2.vm.LuaValue.valueOf;
import static nl.weeaboo.lua2.vm.LuaValue.varargsOf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import nl.weeaboo.lua2.compiler.LoadState;
import nl.weeaboo.lua2.lib.DebugLib;
import nl.weeaboo.lua2.link.LuaLink;
import nl.weeaboo.lua2.luajava.LuajavaLib;
import nl.weeaboo.lua2.vm.LuaClosure;
import nl.weeaboo.lua2.vm.LuaError;
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
    public static void registerClass(LuaValue globals, Class<?> clazz, String tableName) {
        LuaTable table = new LuaTable();
        if (clazz.isEnum()) {
            for (Object val : clazz.getEnumConstants()) {
                Enum<?> e = (Enum<?>)val;
                table.rawset(e.name(), LuajavaLib.toUserdata(e, clazz));
            }
        } else {
            table.rawset(NEW, new LuajavaLib.ConstrFunction(clazz));
        }
        globals.rawset(tableName, table);
    }

    /** Compiles and runs a piece of Lua code in the given thread */
    public static Varargs eval(LuaLink thread, String code) throws LuaException {
        return thread.call(compileForEval(code, thread.getThread().getCallEnv()));
    }

    public static LuaClosure compileForEval(String code, LuaValue env) throws LuaException {
        final String chunkName = "(eval)";
        try {
            Varargs result = NONE;
            try {
                // Try to evaluate as an expression
                result = LoadState.load("return " + code, chunkName, env);
            } catch (LuaError err) {
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

    /** @return The current call stack of the active Lua thread, or an empty list if no thread is active. */
    public static List<String> getLuaStack() {
        return getLuaStack(LuaThread.getRunning());
    }

    public static List<String> getLuaStack(LuaThread thread) {
        if (thread == null) {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<String>();
        for (int level = 1; level <= DEFAULT_STACK_LIMIT; level++) {
            String line = DebugLib.fileline(thread, level);
            if (line == null) {
                break;
            }
            result.add(line);
        }
        return Collections.unmodifiableList(result);
    }

    public static Varargs concatVarargs(Varargs firstArgs, Varargs secondArgs) {
        if (firstArgs == null || firstArgs.narg() == 0) {
            return secondArgs;
        }
        if (secondArgs == null || secondArgs.narg() == 0) {
            return firstArgs;
        }

        int firstCount = firstArgs.narg();
        int secondCount = secondArgs.narg();

        LuaValue[] merged = new LuaValue[firstCount + secondCount];
        for (int n = 0; n < firstCount; n++) {
            merged[n] = firstArgs.arg(1 + n);
        }
        for (int n = 0; n < secondCount; n++) {
            merged[firstCount + n] = secondArgs.arg(1 + n);
        }
        return LuaValue.varargsOf(merged);
    }

    public static Varargs copyArgs(LuaValue[] stack, int offset, int length) {
        if (length <= 0) {
            return NONE;
        }

        LuaValue[] array = new LuaValue[length];
        for (int n = 0; n < length; n++) {
            array[n] = stack[offset+n];
        }
        return varargsOf(array);
    }

    public static Varargs copyArgs(LuaValue[] stack, int offset, int length, Varargs extra) {
        if (length <= 0) {
            return copyArgs(extra);
        }

        if (extra == null) {
            extra = NONE;
        }
        int extraL = extra.narg();

        LuaValue[] array = new LuaValue[length + extraL];
        for (int n = 0; n < length; n++) {
            array[n] = stack[offset+n];
        }
        for (int n = 0; n < extraL; n++) {
            array[length + n] = extra.arg(1 + n);
        }
        return varargsOf(array);
    }

    public static Varargs copyArgs(Varargs in) {
        if (in == null) return null;
        final int inL = in.narg();
        if (inL == 0) return NONE;

        LuaValue[] array = new LuaValue[inL];
        for (int n = 0; n < array.length; n++) {
            array[n] = in.arg(1+n);
        }
        return LuaValue.varargsOf(array);
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
        } else if (code.length() >= 2 && code.startsWith("\"") && code.endsWith("\"")) {
            return valueOf(unescape(code.substring(1, code.length()-1)));
        } else if (code.length() >= 2 && code.startsWith("'") && code.endsWith("'")) {
            return valueOf(unescape(code.substring(1, code.length()-1)));
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

    private static final char escapeList[] = {
        '\"', '\"',
        '\'', '\'',
        '\\', '\\',
        'n', '\n',
        'r', '\r',
        't', '\t',
        'f', '\f',
        'a', '\u0007',
        'b', '\b',
        'v', '\u000B',
    };

    public static String escape(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        escape(sb, s);
        return sb.toString();
    }
    public static void escape(StringBuilder out, String s) {
        for (int n = 0; n < s.length(); n++) {
            char c = s.charAt(n);

            int t;
            for (t = 0; t < escapeList.length; t+=2) {
                if (c == escapeList[t+1]) {
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

    public static String unescape(String s) {
        char chars[] = new char[s.length()];
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

    public static char unescape(char c) {
        for (int n = 0; n < escapeList.length; n+=2) {
            if (c == escapeList[n]) {
                return escapeList[n+1];
            }
        }
        return c;
    }

}
