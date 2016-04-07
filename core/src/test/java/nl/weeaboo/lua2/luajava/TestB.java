package nl.weeaboo.lua2.luajava;

import nl.weeaboo.lua2.io.LuaSerializable;

@LuaSerializable
class TestB extends TestA {

    private static final long serialVersionUID = 1L;

    public int value;

    public TestB(int id) {
        super(id);
    }

    /**
     * Overrides {@link TestA#override(int)}, but with a more specific return type.
     */
    @Override
    public TestB override(int id) {
        return new TestB(id);
    }

    /**
     * Method that only exists in this subclass, and not in the parent class.
     */
    public void testB() {
    }

}
