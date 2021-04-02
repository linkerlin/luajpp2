package nl.weeaboo.lua2.vm;

final class NormalEntry extends Entry {

    private final LuaValue key;
    private LuaValue value;

    NormalEntry(LuaValue key, LuaValue value) {
        this.key = key;
        this.value = value;
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
    public int keyindex(int hashMask) {
        return LuaTable.hashSlot(key, hashMask);
    }

    @Override
    public boolean keyeq(LuaValue key) {
        return key.raweq(this.key);
    }
}