package nl.weeaboo.lua2.stdlib;

import static nl.weeaboo.lua2.vm.LuaNil.NIL;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.vm.LuaClosure;
import nl.weeaboo.lua2.vm.LuaString;
import nl.weeaboo.lua2.vm.LuaValue;
import nl.weeaboo.lua2.vm.Varargs;

/**
 * ------------------------ Debug Info management --------------------------
 * <p>
 * when DEBUG_ENABLED is set to true, these functions will be called
 * by Closure instances as they process bytecodes.
 * <p>
 * Each thread will get a DebugState attached to it by the debug library
 * which will track function calls, hook functions, etc.
 */
@LuaSerializable
final class DebugInfo implements Externalizable {

    // --- Uses manual serialization, don't add variables ---
    LuaValue func;
    LuaClosure closure;
    LuaValue[] stack;
    Varargs varargs;
    Varargs extras;
    int pc;
    int top;
    // --- Uses manual serialization, don't add variables ---

    public DebugInfo() {
        func = NIL;
    }

    DebugInfo(LuaValue func) {
        pc = -1;
        setfunction(func);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(func);
        out.writeObject(closure);
        out.writeObject(stack);
        out.writeObject(varargs);
        out.writeObject(extras);
        out.writeInt(pc);
        out.writeInt(top);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        func = (LuaValue)in.readObject();
        closure = (LuaClosure)in.readObject();
        stack = (LuaValue[])in.readObject();
        varargs = (Varargs)in.readObject();
        extras = (Varargs)in.readObject();
        pc = in.readInt();
        top = in.readInt();
    }

    void setargs(Varargs varargs, LuaValue[] stack) {
        this.varargs = varargs;
        this.stack = stack;
    }

    void setfunction(LuaValue func) {
        this.func = func;
        this.closure = (func instanceof LuaClosure ? (LuaClosure)func : null);
    }

    void clear() {
        func = NIL;
        closure = null;
        stack = null;
        varargs = extras = null;
        pc = top = 0;
    }

    public LuaString[] getfunckind() {
        int stackpos = getStackPos();
        if (stackpos < 0) {
            return null;
        }
        return DebugTrace.getobjname(this, stackpos);
    }

    int getStackPos() {
        if (closure == null || pc < 0) {
            return -1;
        }
        int[] code = closure.getPrototype().code;
        if (pc >= code.length) {
            return -1;
        }
        return (code[pc] >> 6) & 0xff;
    }

    public String source() {
        if (closure == null) {
            return (func == null || func.isnil() ? "???" : func.tojstring());
        }
        String s = closure.getPrototype().source.tojstring();
        return (s.startsWith("@") || s.startsWith("=") ? s.substring(1) : s);
    }

    public int currentline() {
        if (closure == null) {
            return 0;
        }
        int[] li = closure.getPrototype().lineinfo;
        return li == null || pc < 0 || pc >= li.length ? -1 : li[pc];
    }

    public String sourceline() {
        return source() + ":" + currentline();
    }

    public String tracename() {
        // if ( func != null )
        // return func.tojstring();

        LuaString[] kind = getfunckind();
        if (kind == null || kind.length <= 0) {
            return "function ?";
        }
        return "function " + kind[0].tojstring();
    }

    public LuaString getlocalname(int index) {
        if (closure == null) {
            return null;
        }
        return closure.getPrototype().getlocalname(index, pc);
    }

    public String tojstring() {
        return tracename() + " " + sourceline();
    }

    @Override
    public String toString() {
        return tojstring();
    }

}