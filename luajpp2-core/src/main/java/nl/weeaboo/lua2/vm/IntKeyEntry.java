package nl.weeaboo.lua2.vm;

final class IntKeyEntry extends Entry {

    private final int key;
    private LuaValue value;

    IntKeyEntry(int key, LuaValue value) {
        this.key = key;
        this.value = value;
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