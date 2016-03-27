/*******************************************************************************
 * Copyright (c) 2009-2011 Luaj.org. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ******************************************************************************/
package nl.weeaboo.lua2.vm;

import java.io.Serializable;

import nl.weeaboo.lua2.io.LuaSerializable;

/**
 * Extension of {@link LuaValue} which can hold a Java boolean as its value.
 * <p>
 * These instance are not instantiated directly by clients. Instead, there are exactly two instances of this
 * class, {@link #TRUE} and {@link #FALSE} representing the lua values {@code true} and {@code
 * false}. The function {@link LuaValue#valueOf(boolean)} will always return one of these two values.
 * <p>
 * Any {@link LuaValue} can be converted to its equivalent boolean representation using
 * {@link LuaValue#toboolean()}
 */
@LuaSerializable
public final class LuaBoolean extends LuaValue implements Serializable {

    private static final long serialVersionUID = 1L;

    /** The singleton instance representing lua {@code true} */
    public static final LuaBoolean TRUE = new LuaBoolean(true);

    /** The singleton instance representing lua {@code false} */
    public static final LuaBoolean FALSE = new LuaBoolean(false);

    /** Shared static metatable for boolean values represented in lua. */
    public static LuaValue s_metatable;

    /** The value of the boolean */
    public final boolean bool;

    private LuaBoolean(boolean b) {
        this.bool = b;
    }

    public static LuaBoolean valueOf(boolean b) {
        return b ? TRUE : FALSE;
    }

    protected Object readResolve() {
        // Special serialization returning the cached values
        return valueOf(bool);
    }

    @Override
    public int type() {
        return LuaConstants.TBOOLEAN;
    }

    @Override
    public String typename() {
        return "boolean";
    }

    @Override
    public boolean isboolean() {
        return true;
    }

    @Override
    public LuaValue not() {
        return bool ? FALSE : TRUE;
    }

    @Override
    public boolean toboolean() {
        return bool;
    }

    @Override
    public String tojstring() {
        return bool ? "true" : "false";
    }

    @Override
    public boolean optboolean(boolean defval) {
        return bool;
    }

    @Override
    public boolean checkboolean() {
        return bool;
    }

    @Override
    public LuaValue getmetatable() {
        return s_metatable;
    }

}
