package nl.weeaboo.lua2.vm;

public final class TableTester {

    public static LuaTable newWeakTable(boolean weakKeys, boolean weakValues) {
        return WeakTable.make(weakKeys, weakValues);
    }

    public static int getArrayLength(LuaTable table) {
        return table.getArrayLength();
    }

    public static int getHashLength(LuaTable table) {
        return table.getHashLength();
    }

    public static int getHashEntries(LuaTable table) {
        return table.hashEntries;
    }

}
