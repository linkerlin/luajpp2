package nl.weeaboo.lua2.vm;

import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.stdlib.DebugTrace;

@LuaSerializable
public final class LuaError extends RuntimeException {

    private static final long serialVersionUID = 1657137595545123245L;

    private static final LuaString DEFAULT_MESSAGE = LuaString.valueOf("Lua error");
    private static final int MAX_LEVELS = 8;

    private final LuaValue message;
    private final Throwable cause;

    public LuaError() {
        this(DEFAULT_MESSAGE, null, 0);
    }

    public LuaError(String message) {
        this(LuaString.valueOf(message));
    }

    public LuaError(LuaValue message) {
        this(message, null, 0);
    }

    public LuaError(Throwable c) {
        this(DEFAULT_MESSAGE, c, 0);
    }

    public LuaError(String message, Throwable c) {
        this(LuaString.valueOf(message), c);
    }

    public LuaError(LuaValue message, Throwable c) {
        this(message, c, 0);
    }

    public LuaError(LuaValue message, Throwable c, int level) {
        super();

        if (c instanceof LuaError) {
            // Wrap existing exception
            this.message = ((LuaError)c).getMessageObject();
            this.cause = null;
            setStackTrace(c.getStackTrace());
        } else {
            this.message = (message != null ? message : LuaNil.NIL);
            this.cause = c;

            if (level >= 0) {
                StackTraceElement[] stack = DebugTrace.getStackTrace(LuaThread.getRunning(), level, MAX_LEVELS);
                if (c != null) {
                    stack = prefixLuaStackTrace(c, stack);
                } else {
                    stack = prefixLuaStackTrace(this, stack);
                }
                setStackTrace(stack);
            }
        }
    }

    //Functions
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
