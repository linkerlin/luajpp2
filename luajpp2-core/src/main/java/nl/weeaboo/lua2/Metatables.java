package nl.weeaboo.lua2;

import static nl.weeaboo.lua2.vm.LuaNil.NIL;

import java.io.Serializable;

import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.vm.LuaValue;

@LuaSerializable
public final class Metatables implements Serializable {

    private static final long serialVersionUID = 1L;

    private LuaValue nilMetatable = NIL;
    private LuaValue numberMetatable = NIL;
    private LuaValue booleanMetatable = NIL;
    private LuaValue stringMetatable = NIL;
    private LuaValue functionMetatable = NIL;
    private LuaValue threadMetatable = NIL;

    /**
     * The metatable for nil values.
     */
    public LuaValue getNilMetatable() {
        return nilMetatable;
    }

    /**
     * @see #getNilMetatable()
     */
    public void setNilMetatable(LuaValue nilMetatable) {
        this.nilMetatable = nilMetatable;
    }

    /**
     * The metatable for number values.
     */
    public LuaValue getNumberMetatable() {
        return numberMetatable;
    }

    /**
     * @see #getNumberMetatable()
     */
    public void setNumberMetatable(LuaValue numberMetatable) {
        this.numberMetatable = numberMetatable;
    }

    /**
     * The metatable for boolean values.
     */
    public LuaValue getBooleanMetatable() {
        return booleanMetatable;
    }

    /**
     * @see #getBooleanMetatable()
     */
    public void setBooleanMetatable(LuaValue booleanMetatable) {
        this.booleanMetatable = booleanMetatable;
    }

    /**
     * The metatable for string values.
     */
    public LuaValue getStringMetatable() {
        return stringMetatable;
    }

    /**
     * @see #getStringMetatable()
     */
    public void setStringMetatable(LuaValue stringMetatable) {
        this.stringMetatable = stringMetatable;
    }

    /**
     * The metatable for function values.
     */
    public LuaValue getFunctionMetatable() {
        return functionMetatable;
    }

    /**
     * @see #getFunctionMetatable()
     */
    public void setFunctionMetatable(LuaValue functionMetatable) {
        this.functionMetatable = functionMetatable;
    }

    /**
     * The metatable for thread values.
     */
    public LuaValue getThreadMetatable() {
        return threadMetatable;
    }

    /**
     * @see #getThreadMetatable()
     */
    public void setThreadMetatable(LuaValue val) {
        threadMetatable = val;
    }

}
