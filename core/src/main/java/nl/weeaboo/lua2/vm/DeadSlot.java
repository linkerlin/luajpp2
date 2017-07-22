package nl.weeaboo.lua2.vm;

import java.lang.ref.WeakReference;

import nl.weeaboo.lua2.io.LuaSerializable;

/**
 * A Slot whose value has been set to nil. The key is kept in a weak reference so that it can be found by
 * next().
 */
@LuaSerializable
final class DeadSlot implements ISlot {

    private static final long serialVersionUID = 1L;

    private final Object key;
    private ISlot next;

    DeadSlot(LuaValue key, ISlot next) {
        this.key = LuaTable.isLargeKey(key) ? new WeakReference<LuaValue>(key) : (Object)key;
        this.next = next;
    }

    private LuaValue key() {
        if (key instanceof WeakReference<?>) {
            return (LuaValue)((WeakReference<?>)key).get();
        } else {
            return (LuaValue)key;
        }
    }

    @Override
    public int keyindex(int hashMask) {
        // Not needed: this entry will be dropped during rehash.
        return 0;
    }

    @Override
    public IStrongSlot first() {
        return null;
    }

    @Override
    public IStrongSlot find(LuaValue key) {
        return null;
    }

    @Override
    public boolean keyeq(LuaValue key) {
        LuaValue k = key();
        return k != null && key.raweq(k);
    }

    @Override
    public ISlot rest() {
        return next;
    }

    @Override
    public int arraykey(int max) {
        return -1;
    }

    @Override
    public ISlot set(IStrongSlot target, LuaValue value) {
        ISlot next = (this.next != null) ? this.next.set(target, value) : null;
        if (key() != null) {
            // if key hasn't been garbage collected, it is still potentially a valid argument
            // to next(), so we can't drop this entry yet.
            this.next = next;
            return this;
        } else {
            return next;
        }
    }

    @Override
    public ISlot add(ISlot newEntry) {
        return (next != null) ? next.add(newEntry) : newEntry;
    }

    @Override
    public ISlot remove(IStrongSlot target) {
        if (key() != null) {
            next = next.remove(target);
            return this;
        } else {
            return next;
        }
    }

    @Override
    public ISlot relink(ISlot rest) {
        return rest;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("<dead");
        LuaValue k = key();
        if (k != null) {
            buf.append(": ");
            buf.append(k.toString());
        }
        buf.append('>');
        if (next != null) {
            buf.append("; ");
            buf.append(next.toString());
        }
        return buf.toString();
    }
}