package nl.weeaboo.lua2.vm;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import javax.annotation.Nullable;

import nl.weeaboo.lua2.io.LuaSerializable;

@LuaSerializable
final class LinkSlot implements IStrongSlot, Externalizable {

    private static final long serialVersionUID = 1L;

    // --- Uses manual serialization ---
    private Entry entry;
    private ISlot next;
    // --- Uses manual serialization ---

    /** Exists for serialization */
    @Deprecated
    public LinkSlot() {
    }

    public LinkSlot(Entry entry, ISlot next) {
        this.entry = entry;
        setNext(next);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(entry);
        out.writeObject(next);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        entry = (Entry)in.readObject();
        next = (ISlot)in.readObject();
    }

    @Override
    public LuaValue key() {
        return entry.key();
    }

    @Override
    public int keyindex(int hashMask) {
        return entry.keyindex(hashMask);
    }

    @Override
    public LuaValue value() {
        return entry.value();
    }

    @Override
    public Varargs toVarargs() {
        return entry.toVarargs();
    }

    @Override
    public IStrongSlot first() {
        return entry;
    }

    @Override
    public @Nullable IStrongSlot find(LuaValue key) {
        return entry.keyeq(key) ? this : null;
    }

    @Override
    public boolean keyeq(LuaValue key) {
        return entry.keyeq(key);
    }

    @Override
    public ISlot rest() {
        return next;
    }

    @Override
    public int arraykey(int max) {
        return entry.arraykey(max);
    }

    @Override
    public ISlot set(IStrongSlot target, LuaValue value) {
        if (target == this) {
            entry = entry.set(value);
            return this;
        } else {
            return setNext(next.set(target, value));
        }
    }

    @Override
    public ISlot add(ISlot entry) {
        return setNext(next.add(entry));
    }

    @Override
    public ISlot remove(IStrongSlot target) {
        if (this == target) {
            return new DeadSlot(key(), next);
        } else {
            setNext(next.remove(target));
        }
        return this;
    }

    @Override
    public ISlot relink(ISlot rest) {
        // This method is (only) called during rehash, so it must not change this.next.
        return (rest != null) ? new LinkSlot(entry, rest) : entry;
    }

    // this method ensures that this.next is never set to null.
    private ISlot setNext(ISlot next) {
        if (next == this) {
            throw new IllegalArgumentException("Attempt to link a slot to itself: " + next);
        }

        if (next != null) {
            this.next = next;
            return this;
        } else {
            return entry;
        }
    }

    @Override
    public String toString() {
        return entry + "; " + next;
    }

}