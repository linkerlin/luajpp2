package nl.weeaboo.lua2.vm;

/**
 * Entry class used with numeric values, but only when the key is not an integer.
 */
final class NumberValueEntry extends Entry {

    private final LuaValue key;
    private double value;

    NumberValueEntry(LuaValue key, double value) {
        this.key = key;
        this.value = value;
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