package nl.weeaboo.lua2;

import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.stdlib.DebugTrace;
import nl.weeaboo.lua2.vm.LuaNil;
import nl.weeaboo.lua2.vm.LuaString;
import nl.weeaboo.lua2.vm.LuaThread;
import nl.weeaboo.lua2.vm.LuaValue;

@LuaSerializable
public final class LuaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private static final int MAX_LEVELS = 8;

    private final LuaValue message;
    private final Throwable cause;

    public LuaException(String message) {
        this(LuaString.valueOf(message));
    }

    public LuaException(LuaValue message) {
        this(message, null, 0);
    }

    public LuaException(LuaValue message, Throwable cause, int level) {
        super();

        this.message = (message != null ? message : LuaNil.NIL);
        this.cause = cause;

        if (level >= 0) {
            initStackTrace(cause, level);
        }
    }

    private void initStackTrace(Throwable cause, int level) {
        @SuppressWarnings("deprecation")
        StackTraceElement[] stack = DebugTrace.getStackTrace(LuaThread.getRunning(), level, MAX_LEVELS);
        if (cause != null) {
            stack = prefixLuaStackTrace(cause, stack);
        } else {
            stack = prefixLuaStackTrace(this, stack);
        }
        setStackTrace(stack);
    }

    /**
     * Converts an arbitrary exception to an equivalent {@link LuaException}.
     */
    public static LuaException wrap(String message, Throwable ex) {
        LuaException le;
        if (ex instanceof LuaException) {
            if (ex.getMessage() != null) {
                message += ": " + ex.toString();
            }
            le = new LuaException(message);

            // Copy stacktrace and cause from the existing exception
            le.setStackTrace(ex.getStackTrace());
            le.initCause(ex.getCause());
        } else {
            // For non-Lua exceptions, add the original exception as a cause
            le = new LuaException(message);
            le.initCause(ex);
        }

        return le;
    }

    private static StackTraceElement[] prefixLuaStackTrace(Throwable t, StackTraceElement[] luaStack) {
        StackTraceElement[] javaStack = t.getStackTrace();
        StackTraceElement[] newStack = new StackTraceElement[luaStack.length + javaStack.length];

        int joff = 0;
        for (int n = 0; n < javaStack.length; n++) {
            StackTraceElement ste = javaStack[n];
            if (ste.getClassName().contains("LuaInterpreter") && ste.getMethodName().equals("resume")) {
                joff = n;
                break;
            }
        }
        System.arraycopy(javaStack, 0, newStack, 0, joff);
        System.arraycopy(luaStack, 0, newStack, joff, luaStack.length);
        System.arraycopy(javaStack, joff, newStack, joff + luaStack.length, javaStack.length - joff);
        return newStack;
    }

    @Override
    public String getMessage() {
        return String.valueOf(message);
    }

    /** Retuns the error message for this exception as a Lua object. */
    public LuaValue getMessageObject() {
        return message;
    }

    @Override
    public Throwable getCause() {
        return cause;
    }

}
