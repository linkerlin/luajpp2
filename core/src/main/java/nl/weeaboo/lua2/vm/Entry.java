package nl.weeaboo.lua2.vm;

import static nl.weeaboo.lua2.vm.LuaConstants.NONE;
import static nl.weeaboo.lua2.vm.LuaNil.NIL;

import java.io.Externalizable;

/**
 * Base class for regular entries.
 * <p>
 * If the key may be an integer, the {@link #arraykey(int)} method must be overridden to handle that case.
 */
abstract class Entry extends Varargs implements StrongSlot, Externalizable {

    private static final long serialVersionUID = 1L;

    @Override
    public abstract LuaValue key();

    @Override
    public abstract LuaValue value();

    abstract Entry set(LuaValue value);

    @Override
    public abstract boolean keyeq(LuaValue key);

    @Override
    public abstract int keyindex(int hashMask);

    @Override
    public int arraykey(int max) {
        return 0;
    }

    @Override
    public LuaValue arg(int i) {
        switch (i) {
        case 1:
            return key();
        case 2:
            return value();
        }
        return NIL;
    }

    @Override
    public int narg() {
        return 2;
    }

    /**
     * Subclasses should redefine as "return this;" whenever possible.
     */
    @Override
    public Varargs toVarargs() {
        return LuaTable.varargsOf(key(), value());
    }

    @Override
    public LuaValue arg1() {
        return key();
    }

    @Override
    public Varargs subargs(int start) {
        switch (start) {
        case 1:
            return this;
        case 2:
            return value();
        }
        return NONE;
    }

    @Override
    public StrongSlot first() {
        return this;
    }

    @Override
    public Slot rest() {
        return null;
    }

    @Override
    public StrongSlot find(LuaValue key) {
        return keyeq(key) ? this : null;
    }

    @Override
    public Slot set(StrongSlot target, LuaValue value) {
        return set(value);
    }

    @Override
    public Slot add(Slot entry) {
        return new LinkSlot(this, entry);
    }

    @Override
    public Slot remove(StrongSlot target) {
        return new DeadSlot(key(), null);
    }

    @Override
    public Slot relink(Slot rest) {
        return (rest != null) ? new LinkSlot(this, rest) : (Slot)this;
    }
}