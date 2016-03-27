package org.luaj.vm2;

import static org.luaj.vm2.LuaConstants.NONE;

import java.io.Serializable;

import nl.weeaboo.lua2.io.LuaSerializable;

/**
 * Varargs implemenation backed by an array of LuaValues
 * <p>
 * This is an internal class not intended to be used directly. Instead use
 * the corresponding static methods on LuaValue.
 *
 * @see LuaValue#varargsOf(LuaValue[], int, int)
 * @see LuaValue#varargsOf(LuaValue[], int, int, Varargs)
 */
@LuaSerializable
final class ArrayPartVarargs extends Varargs implements Serializable {

	private static final long serialVersionUID = 5311707858805030821L;

	private final int offset;
	private final LuaValue[] v;
	private final int length;
	private final Varargs more;

	/**
	 * Construct a Varargs from an array of LuaValue.
	 * <p>
	 * This is an internal class not intended to be used directly. Instead
	 * use the corresponding static methods on LuaValue.
	 *
	 * @see LuaValue#varargsOf(LuaValue[], int, int)
	 */
	ArrayPartVarargs(LuaValue[] v, int offset, int length) {
		this.v = v;
		this.offset = offset;
		this.length = length;
		this.more = NONE;
	}

	/**
	 * Construct a Varargs from an array of LuaValue and additional
	 * arguments.
	 * <p>
	 * This is an internal class not intended to be used directly. Instead
	 * use the corresponding static method on LuaValue.
	 *
	 * @see LuaValue#varargsOf(LuaValue[], int, int, Varargs)
	 */
	public ArrayPartVarargs(LuaValue[] v, int offset, int length, Varargs more) {
		this.v = v;
		this.offset = offset;
		this.length = length;
		this.more = more;
	}

	@Override
	public LuaValue arg(int i) {
		return i >= 1 && i <= length ? v[i + offset - 1] : more.arg(i - length);
	}

	@Override
	public int narg() {
		return length + more.narg();
	}

	@Override
	public LuaValue arg1() {
		return length > 0 ? v[offset] : more.arg1();
	}
}