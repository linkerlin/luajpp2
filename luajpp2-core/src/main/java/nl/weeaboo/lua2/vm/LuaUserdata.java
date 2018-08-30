/*******************************************************************************
 * Copyright (c) 2009 Luaj.org. All rights reserved.
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

import static nl.weeaboo.lua2.vm.LuaNil.NIL;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import javax.annotation.Nullable;

import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.io.LuaSerializable;

@LuaSerializable
public class LuaUserdata extends LuaValue implements Serializable {

    private static final long serialVersionUID = -2825288508171353992L;

    private final Object userdata;
    private LuaValue metatable;

    public LuaUserdata(Object obj) {
        this(obj, NIL);
    }

    public LuaUserdata(Object obj, LuaValue metatable) {
        if (obj == null) {
            throw new LuaException("Attempt to create userdata from null object");
        }
        if (metatable == null) {
            throw new LuaException("Attempt to create userdata with null metatable");
        }

        this.userdata = obj;
        this.metatable = metatable;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    @Override
    public String tojstring() {
        return String.valueOf(userdata);
    }

    @Override
    public int type() {
        return LuaConstants.TUSERDATA;
    }

    @Override
    public String typename() {
        return "userdata";
    }

    @Override
    public int hashCode() {
        return userdata.hashCode();
    }

    /** The wrapped Java object */
    public Object userdata() {
        return userdata;
    }

    @Override
    public boolean isuserdata() {
        return true;
    }

    @Override
    public boolean isuserdata(Class<?> c) {
        return c.isAssignableFrom(userdata.getClass());
    }

    @Override
    public Object touserdata() {
        return userdata;
    }

    @Override
    public @Nullable <T> T touserdata(Class<T> c) {
        return c.isAssignableFrom(userdata.getClass()) ? c.cast(userdata) : null;
    }

    @Override
    public Object optuserdata(Object defval) {
        return userdata;
    }

    @Override
    public <T> T optuserdata(Class<T> c, T defval) {
        if (!c.isAssignableFrom(userdata.getClass())) {
            throw typerror(c.getName());
        }
        return c.cast(userdata);
    }

    @Override
    public LuaValue getmetatable() {
        return metatable;
    }

    @Override
    public LuaValue setmetatable(LuaValue metatable) {
        this.metatable = metatable;
        return this;
    }

    @Override
    public Object checkuserdata() {
        return userdata;
    }

    @Override
    public <T> T checkuserdata(Class<T> c) {
        if (!c.isAssignableFrom(userdata.getClass())) {
            throw typerror(c.getName());
        }
        return c.cast(userdata);
    }

    @Override
    public LuaValue get(LuaValue key) {
        return metatable != null ? gettable(this, key) : NIL;
    }

    @Override
    public void set(LuaValue key, LuaValue value) {
        if (metatable == null || !settable(this, key, value)) {
            error("cannot set " + key + " for userdata");
        }
    }

    @Override
    public boolean equals(Object val) {
        if (this == val) {
            return true;
        }
        if (!(val instanceof LuaUserdata)) {
            return false;
        }
        LuaUserdata u = (LuaUserdata) val;
        return userdata.equals(u.userdata);
    }

    // equality w/ metatable processing
    @Override
    public boolean eq_b(LuaValue val) {
        if (val.raweq(this)) {
            return true;
        }
        if (metatable == null || !val.isuserdata()) {
            return false;
        }
        LuaValue valmt = val.getmetatable();
        return valmt != null && LuaValue.eqmtcall(this, metatable, val, valmt);
    }

    // equality w/o metatable processing
    @Override
    public boolean raweq(LuaValue val) {
        return val.raweq(this);
    }

    /*
     * Suppress ErrorProne:ReferenceEquality: we use a reference equality comparison as a fast path to avoid potentially
     * more expensive checks
     */
    @SuppressWarnings("ReferenceEquality")
    @Override
    public boolean raweq(LuaUserdata val) {
        if (this == val) {
            return true;
        }
        return (metatable == val.metatable || (metatable != null && metatable.equals(val.metatable)))
                && userdata.equals(val.userdata);
    }
}
