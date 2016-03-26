package nl.weeaboo.lua2;

import nl.weeaboo.lua2.io.LuaSerializable;

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
        LuaException le = new LuaException(message + ": " + ex.getMessage());
        le.setStackTrace(ex.getStackTrace());
        le.initCause(ex.getCause());
        return le;
	}

}
