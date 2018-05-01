package nl.weeaboo.lua2.vm;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import nl.weeaboo.lua2.io.LuaSerializable;

@LuaSerializable
final class IntKeyEntry extends Entry {

    // --- Warning: uses manual serialization ---
    private int key;
    private LuaValue value;
    // --- Warning: uses manual serialization ---

    /** Exists for serialization */
    @Deprecated
    public IntKeyEntry() {
    }

    IntKeyEntry(int key, LuaValue value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(key);
        out.writeObject(value);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        key = in.readInt();
        value = (LuaValue)in.readObject();
    }

    @Override
    public LuaValue key() {
        return LuaInteger.valueOf(key);
    }

    @Override
    public int arraykey(int max) {
        return (key >= 1 && key <= max) ? key : 0;
    }

    @Override
    public LuaValue value() {
        return value;
    }

    @Override
    public Entry set(LuaValue value) {
        this.value = value;
        return this;
    }

    @Override
    public int keyindex(int mask) {
        return LuaTable.hashmod(LuaInteger.hashCode(key), mask);
    }

    @Override
    public boolean keyeq(LuaValue key) {
        return key.raweq(this.key);
    }

}