package org.luaj.vm2;

import java.io.Serializable;

import nl.weeaboo.lua2.io.LuaSerializable;

/**
 * Varargs implemenation backed by two values.
 * <p>
 * This is an internal class not intended to be used directly. Instead use
 * the corresponding static method on LuaValue.
 *
 * @see LuaValue#varargsOf(LuaValue, Varargs)
 */
@LuaSerializable
final class PairVarargs extends Varargs implements Serializable {

	private static final long serialVersionUID = 6823725326763743125L;

	private final LuaValue v1;
	private final Varargs v2;

	/**
	 * Construct a Varargs from an two LuaValue.
	 * <p>
	 * This is an internal class not intended to be used directly. Instead
	 * use the corresponding static method on LuaValue.
	 *
	 * @see LuaValue#varargsOf(LuaValue, Varargs)
	 */
	PairVarargs(LuaValue v1, Varargs v2) {
		this.v1 = v1;
		this.v2 = v2;
	}

	@Override
	public LuaValue arg(int i) {
		return i == 1 ? v1 : v2.arg(i - 1);
	}

	@Override
	public int narg() {
		return 1 + v2.narg();
	}

	@Override
	public LuaValue arg1() {
		return v1;
	}
}