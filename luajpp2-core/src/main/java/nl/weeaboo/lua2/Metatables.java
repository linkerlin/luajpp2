package nl.weeaboo.lua2;

import static nl.weeaboo.lua2.vm.LuaNil.NIL;

import nl.weeaboo.lua2.vm.LuaValue;

public final class Metatables {

    private LuaValue nilMetatable = NIL;
    private LuaValue numberMetatable = NIL;
    private LuaValue booleanMetatable = NIL;
    private LuaValue stringMetatable = NIL;
    private LuaValue functionMetatable = NIL;
    private LuaValue threadMetatable = NIL;

    public LuaValue getNilMetatable() {
        return nilMetatable;
    }

    public void setNilMetatable(LuaValue nilMetatable) {
        this.nilMetatable = nilMetatable;
    }

    public LuaValue getNumberMetatable() {
        return numberMetatable;
    }

    public void setNumberMetatable(LuaValue numberMetatable) {
        this.numberMetatable = numberMetatable;
    }

    public LuaValue getBooleanMetatable() {
        return booleanMetatable;
    }

    public void setBooleanMetatable(LuaValue booleanMetatable) {
        this.booleanMetatable = booleanMetatable;
    }

    public LuaValue getStringMetatable() {
        return stringMetatable;
    }

    public void setStringMetatable(LuaValue stringMetatable) {
        this.stringMetatable = stringMetatable;
    }

    public LuaValue getFunctionMetatable() {
        return functionMetatable;
    }

    public void setFunctionMetatable(LuaValue functionMetatable) {
        this.functionMetatable = functionMetatable;
    }

    public LuaValue getThreadMetatable() {
        return threadMetatable;
    }

    public void setThreadMetatable(LuaValue val) {
        threadMetatable = val;
    }

}
