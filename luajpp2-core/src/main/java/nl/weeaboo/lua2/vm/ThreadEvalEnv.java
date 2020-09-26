package nl.weeaboo.lua2.vm;

import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.lib.VarArgFunction;

@LuaSerializable
public final class ThreadEvalEnv extends VarArgFunction {

    private static final long serialVersionUID = 1L;

    private final StackFrame stackFrame;

    public ThreadEvalEnv(LuaThread thread) {
        this.stackFrame = thread.getStackFrame(1);
    }

    @Override
    public Varargs invoke(Varargs args) {
        LuaValue parentEnv = stackFrame.func.getfenv();

        String name = args.tojstring(2);
        if (stackFrame.func instanceof LuaClosure) {
            LuaClosure closure = (LuaClosure)stackFrame.func;
            Prototype p = closure.getPrototype();

            int n = 0;
            for (LocVars locVar : p.locvars) {
                if (locVar.varname.tojstring().equals(name)) {
                    return stackFrame.getLocalValue(1 + n);
                }
                n++;
            }
        }

        return parentEnv.get(name);
    }

}