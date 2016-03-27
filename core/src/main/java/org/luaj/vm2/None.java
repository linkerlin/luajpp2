package org.luaj.vm2;

import nl.weeaboo.lua2.io.LuaSerializable;

/**
 * Varargs implemenation with no values.
 * <p>
 * This is an internal class not intended to be used directly. Instead use the predefined constant
 * {@link #NONE}
 */
@LuaSerializable
final class None extends LuaNil {

    private static final long serialVersionUID = 1L;

    static final None NONE = new None();

    private None() {
    }

	@Override
    public LuaValue arg(int i) {
		return NIL;
	}

	@Override
    public Object readResolve() {
		// Special serialization returning the singleton
        return NONE;
	}

	@Override
	public int narg() {
		return 0;
	}

	@Override
	public LuaValue arg1() {
		return NIL;
	}

	@Override
	public String tojstring() {
		return "none";
	}

}