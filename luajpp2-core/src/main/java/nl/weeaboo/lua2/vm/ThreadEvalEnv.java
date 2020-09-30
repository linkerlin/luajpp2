package nl.weeaboo.lua2.vm;

import javax.annotation.Nullable;

import nl.weeaboo.lua2.compiler.LuaEval;
import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.lib.VarArgFunction;

/**
 * Variable lookup for {@link LuaEval} to access local variables inside a running thread.
 */
@LuaSerializable
public final class ThreadEvalEnv extends VarArgFunction {

    private static final long serialVersionUID = 1L;

    private final LuaThread thread;
    private final @Nullable StackFrame stackFrame;
    private final boolean isGetter;

    private ThreadEvalEnv(LuaThread thread, boolean isGetter) {
        this.thread = thread;
        stackFrame = thread.getStackFrame(1);
        this.isGetter = isGetter;
    }

    public static VarArgFunction getter(LuaThread thread) {
        return new ThreadEvalEnv(thread, true);
    }

    public static LuaValue setter(LuaThread thread) {
        return new ThreadEvalEnv(thread, false);
    }

    @Override
    public Varargs invoke(Varargs args) {
        String name = args.tojstring(2);
        LuaValue value = args.arg(3);

        LuaValue parentEnv = thread.getfenv();
        StackFrame sf = stackFrame;
        if (sf != null) {
            parentEnv = sf.func.getfenv();

            if (sf.func instanceof LuaClosure) {
                LuaClosure closure = (LuaClosure)sf.func;
                Prototype p = closure.getPrototype();

                // Try to resolve name as a local value
                for (int n = 0; n < p.locvars.length; n++) {
                    if (p.locvars[n].varname.tojstring().equals(name)) {
                        if (isGetter) {
                            return sf.getLocalValue(1 + n);
                        } else {
                            sf.setLocalValue(1 + n, value);
                            return LuaConstants.NONE;
                        }
                    }
                }

                // Try to resolve name as an upvalue
                for (int n = 0; n < p.upvalues.length; n++) {
                    if (p.upvalues[n].tojstring().equals(name)) {
                        UpValue upValue = closure.getUpValue(n);
                        if (isGetter) {
                            return upValue.getValue();
                        } else {
                            upValue.setValue(value);
                            return LuaConstants.NONE;
                        }
                    }
                }
            }
        }

        if (isGetter) {
            return parentEnv.get(name);
        } else {
            parentEnv.set(name, value);
            return LuaConstants.NONE;
        }
    }

}