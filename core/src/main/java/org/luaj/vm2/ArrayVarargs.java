package org.luaj.vm2;

import java.io.Serializable;

import nl.weeaboo.lua2.io.LuaSerializable;

/**
 * Varargs implemenation backed by an array of LuaValues
 * <p>
 * This is an internal class not intended to be used directly. Instead use
 * the corresponding static methods on LuaValue.
 *
 * @see LuaValue#varargsOf(LuaValue[])
 * @see LuaValue#varargsOf(LuaValue[], Varargs)
 */
@LuaSerializable
final class ArrayVarargs extends Varargs implements Serializable {

	private static final long serialVersionUID = -7227599572095569753L;

	private final LuaValue[] v;
	private final Varargs r;

	/**
	 * Construct a Varargs from an array of LuaValue.
	 * <p>
	 * This is an internal class not intended to be used directly. Instead
	 * use the corresponding static methods on LuaValue.
	 *
	 * @see LuaValue#varargsOf(LuaValue[])
	 * @see LuaValue#varargsOf(LuaValue[], Varargs)
	 */
	ArrayVarargs(LuaValue[] v, Varargs r) {
		this.v = v;
		this.r = r;
	}

	@Override
	public LuaValue arg(int i) {
		return i >= 1 && i <= v.length ? v[i - 1] : r.arg(i - v.length);
	}

	@Override
	public int narg() {
		return v.length + r.narg();
	}

	@Override
	public LuaValue arg1() {
		return v.length > 0 ? v[0] : r.arg1();
	}
}