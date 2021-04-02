package nl.weeaboo.lua2.vm;

import javax.annotation.Nullable;

/**
 * Base class for regular entries.
 * <p>
 * If the key may be an integer, the {@link #arraykey(int)} method must be overridden to handle that case.
 */
abstract class Entry implements IStrongSlot {

    @Override
    public abstract LuaValue key();

    @Override
    public abstract LuaValue value();

    @Override
    public ISlot set(IStrongSlot target, LuaValue value) {
        return set(value);
    }

    abstract Entry set(LuaValue value);

    @Override
    public abstract boolean keyeq(LuaValue key);

    @Override
    public abstract int keyindex(int hashMask);

    @Override
    public int arraykey(int max) {
        LuaValue key = key();
        if (!key.isinttype()) {
            return 0;
        }

        int intKey = key.toint();
        return (intKey >= 1 && intKey <= max) ? intKey : 0;
    }

    @Override
    public Varargs toVarargs() {
        return LuaTable.varargsOf(key(), value());
    }

    @Override
    public IStrongSlot first() {
        return this;
    }

    @Override
    public @Nullable ISlot rest() {
        return null;
    }

    @Override
    public @Nullable IStrongSlot find(LuaValue key) {
        return keyeq(key) ? this : null;
    }

    @Override
    public ISlot add(ISlot entry) {
        return new LinkSlot(this, entry);
    }

    @Override
    public ISlot remove(IStrongSlot target) {
        return new DeadSlot(key(), null);
    }

    @Override
    public ISlot relink(ISlot rest) {
        return (rest != null) ? new LinkSlot(this, rest) : (ISlot)this;
    }
}