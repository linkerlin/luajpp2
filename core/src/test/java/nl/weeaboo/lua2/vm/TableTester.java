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

    public static void collectGarbage() {
        // Attempt to force a full GC
        for (int n = 0; n < 10; n++) {
            System.gc();
            System.runFinalization();
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // Ignore
            }
        }
    }

}
