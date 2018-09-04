package nl.weeaboo.lua2.luajava;

import java.io.Serializable;

import nl.weeaboo.lua2.io.LuaSerializable;

@LuaSerializable
class TestA implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;

    public TestA(int id) {
        this.id = id;
    }

    public TestA override(int id) {
        return new TestA(id);
    }

    public int getId() {
        return id;
    }

}
