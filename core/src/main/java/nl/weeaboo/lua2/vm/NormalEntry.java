package nl.weeaboo.lua2.vm;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import nl.weeaboo.lua2.io.LuaSerializable;

@LuaSerializable
final class NormalEntry extends Entry {

    // --- Warning: uses manual serialization ---
    private LuaValue key;
    private LuaValue value;
    // --- Warning: uses manual serialization ---

    /** Exists for serialization */
    @Deprecated
    public NormalEntry() {
    }

    NormalEntry(LuaValue key, LuaValue value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(key);
        out.writeObject(value);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        key = (LuaValue)in.readObject();
        value = (LuaValue)in.readObject();
    }

    @Override
    public LuaValue key() {
        return key;
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
    public Varargs toVarargs() {
        return this;
    }

    @Override
    public int keyindex(int hashMask) {
        return LuaTable.hashSlot(key, hashMask);
    }

    @Override
    public boolean keyeq(LuaValue key) {
        return key.raweq(this.key);
    }
}