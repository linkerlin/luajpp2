package nl.weeaboo.lua2.vm;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import nl.weeaboo.lua2.io.LuaSerializable;

/**
 * Entry class used with numeric values, but only when the key is not an integer.
 */
@LuaSerializable
final class NumberValueEntry extends Entry {

    // --- Warning: uses manual serialization ---
    private LuaValue key;
    private double value;
    // --- Warning: uses manual serialization ---

    /** Exists for serialization */
    @Deprecated
    public NumberValueEntry() {
    }

    NumberValueEntry(LuaValue key, double value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(key);
        out.writeDouble(value);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        key = (LuaValue)in.readObject();
        value = in.readDouble();
    }

    @Override
    public LuaValue key() {
        return key;
    }

    @Override
    public LuaValue value() {
        return LuaTable.valueOf(value);
    }

    @Override
    public Entry set(LuaValue value) {
        LuaValue n = value.tonumber();
        if (!n.isnil()) {
            this.value = n.todouble();
            return this;
        } else {
            return new NormalEntry(this.key, value);
        }
    }

    @Override
    public int keyindex(int mask) {
        return LuaTable.hashSlot(key, mask);
    }

    @Override
    public boolean keyeq(LuaValue key) {
        return key.raweq(this.key);
    }
}