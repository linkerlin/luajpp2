package nl.weeaboo.lua2.vm;

/**
 * Test helper functions related to {@link LuaTable}.
 */
public final class TableTester {

    /** Creates a new weak table. */
    public static LuaTable newWeakTable(boolean weakKeys, boolean weakValues) {
        return WeakTable.make(weakKeys, weakValues);
    }

    /** Returns the number of array slots in the table. Some slots may be empty. */
    public static int getArrayLength(LuaTable table) {
        return table.getArrayLength();
    }

    /** Returns the number of hash table slots in the table. Some slots may be empty. */
    public static int getHashLength(LuaTable table) {
        return table.getHashLength();
    }

    /** Returns the number of values stored in hash table slots. */
    public static int getHashEntries(LuaTable table) {
        return table.hashEntries;
    }

    /** Attempts to force a full garbage collection cycle. */
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
