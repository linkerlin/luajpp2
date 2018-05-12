package nl.weeaboo.lua2;

import nl.weeaboo.lua2.io.LuaSerializable;

@Deprecated
@LuaSerializable
public class LuaException extends Exception {

    private static final long serialVersionUID = 1L;

    public LuaException(String message) {
        super(message);
    }

    /**
     * Converts an arbitrary exception to an equivalent {@link LuaException}. This is used to prevent
     * excessively long caused-by chains.
     */
    public static LuaException wrap(String message, Exception ex) {
        if (ex.getMessage() != null) {
            message += ": " + ex.getMessage();
        }

        LuaException le = new LuaException(message);
        le.setStackTrace(ex.getStackTrace());
        le.initCause(ex.getCause());
        return le;
    }

}
