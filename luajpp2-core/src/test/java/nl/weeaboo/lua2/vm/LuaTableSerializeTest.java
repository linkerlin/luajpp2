package nl.weeaboo.lua2.vm;

import java.io.Serializable;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.testing.SerializableTester;

import nl.weeaboo.lua2.AbstractLuaTest;

public final class LuaTableSerializeTest extends AbstractLuaTest {

    /**
     * {@link LuaUserdata} objects can have a different hash code after loading.
     */
    @Test
    public void testUserdataKeys() {
        LuaTable table = new LuaTable();
        for (int n = 0; n < 1000; n++) {
            table.set(newUserdata(), LuaInteger.valueOf(n));
        }
        checkHashSlots(table);

        table = SerializableTester.reserialize(table);
        checkHashSlots(table);
    }

    private static LuaUserdata newUserdata() {
        return LuaUserdata.userdataOf(new Serializable() {
            private static final long serialVersionUID = 1L;
        });
    }

    private void checkHashSlots(LuaTable table) {
        ISlot[] slots = table.hash;
        for (int n = 0; n < slots.length; n++) {
            ISlot slot = slots[n];
            while (slot != null) {
                LuaValue key = slot.first().key();
                int expectedSlot = LuaTable.hashSlot(key, slots.length - 1);
                Assert.assertEquals(expectedSlot, n);
                slot = slot.rest();
            }
        }
    }
}
