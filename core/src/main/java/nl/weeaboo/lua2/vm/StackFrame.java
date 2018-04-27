package nl.weeaboo.lua2.vm;

import static nl.weeaboo.lua2.vm.LuaConstants.NONE;
import static nl.weeaboo.lua2.vm.LuaNil.NIL;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

import nl.weeaboo.lua2.io.LuaSerializable;

@LuaSerializable
final class StackFrame implements Externalizable {

    enum Status {
        FRESH, RUNNING, DEAD
    }

    // --- Uses manual serialization, don't add variables ---
    Status status;
    LuaFunction func;  //The function that's being called
    Varargs args;      //The args given
    Varargs varargs;   //The varargs part of the arguments given

    StackFrame parent; //Link to calling context
    int parentCount;   //Number of parents
    int returnBase;    //Stack offset in parent to write return values to
    int returnCount;   //Number of return values to write in parent stack

    LuaValue[] stack;
    UpValue[] openups;
    Varargs v;
    int top;
    int pc;
    // --- Uses manual serialization, don't add variables ---

    @Deprecated
    public StackFrame() {
    }

    public static StackFrame newInstance(LuaFunction func, Varargs args, StackFrame parent, int returnBase,
            int returnCount) {

        StackFrame frame = new StackFrame();
        frame.prepareCall(func, args, parent, returnBase, returnCount);
        return frame;
    }

    /** Closes every frame in the callstack. */
    public static void releaseCallstack(StackFrame frame) {
        while (frame != null) {
            StackFrame parent = frame.parent;
            frame.close();
            frame = parent;
        }
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(status);
        out.writeObject(func);
        out.writeObject(args);
        out.writeObject(varargs);

        out.writeObject(stack);
        out.writeObject(openups);
        out.writeObject(v);
        out.writeInt(top);
        out.writeInt(pc);

        out.writeObject(parent);
        out.writeInt(parentCount);
        out.writeInt(returnBase);
        out.writeInt(returnCount);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        status = (Status)in.readObject();
        func = (LuaClosure)in.readObject();
        args = (Varargs)in.readObject();
        varargs = (Varargs)in.readObject();

        stack = (LuaValue[])in.readObject();
        openups = (UpValue[])in.readObject();
        v = (Varargs)in.readObject();
        top = in.readInt();
        pc = in.readInt();

        parent = (StackFrame)in.readObject();
        parentCount = in.readInt();
        returnBase = in.readInt();
        returnCount = in.readInt();
    }

    public void close() {
        status = Status.DEAD;

        closeUpValues();

        stack = null;
    }

    public void closeUpValues() {
        for (int u = openups.length; --u >= 0;) {
            if (openups[u] != null) {
                openups[u].close();
                openups[u] = null;
            }
        }
    }

    private void resetExecutionState(int minStackSize, int subFunctionCount) {
        if (stack == null || stack.length != minStackSize) {
            stack = new LuaValue[minStackSize];
        }
        Arrays.fill(stack, NIL);

        if (subFunctionCount == 0) {
            openups = UpValue.NOUPVALUES;
        } else {
            openups = new UpValue[minStackSize];
        }

        v = NONE;
        top = 0;
        pc = 0;
    }

    public int size() {
        return parentCount + 1; //(parent != null ? parentCount + 1 : 1);
    }

    public LuaFunction getCallstackFunction(int level) {
        StackFrame sf = this;
        while (--level >= 1) {
            sf = sf.parent;
            if (sf == null) {
                return null;
            }
        }
        return sf.func;
    }

    private static Prototype getPrototype(LuaFunction func) {
        if (func.isclosure()) {
            return func.checkclosure().getPrototype();
        } else {
            return null;
        }
    }

    public final void prepareCall(LuaFunction func, Varargs args, StackFrame parent, int returnBase,
            int returnCount) {

        final Prototype p = getPrototype(func);

        this.status = Status.FRESH;
        this.func = func;

        this.parent = parent;
        this.parentCount = (parent != null ? parent.size() : 0);
        this.returnBase = returnBase;
        this.returnCount = returnCount;

        if (p == null) {
            resetExecutionState(0, 0);
        } else {
            resetExecutionState(p.maxstacksize, p.p.length);
        }

        setArgs(args);
    }

    public void setArgs(Varargs args) {
        final Prototype p = getPrototype(func);

        this.args = args;
        this.varargs = extractVarargs(p, args);

        if (p != null) {
            //Push params on stack
            for (int i = 0; i < p.numparams; i++) {
                stack[i] = args.arg(i + 1);
            }
            if (p.isVararg >= Lua.VARARG_NEEDSARG) {
                stack[p.numparams] = new LuaTable(args.subargs(p.numparams + 1));
            }
        }
    }

    public final void prepareTailcall(LuaFunction func, Varargs args) {
        closeUpValues(); //We're clobbering the stack, save the upvalues first

        final Prototype p = getPrototype(func);

        //Don't change status

        this.func = func;
        this.args = args;
        this.varargs = extractVarargs(p, args);

        //Don't change parent

        if (p == null) {
            resetExecutionState(0, 0);
        } else {
            resetExecutionState(p.maxstacksize, p.p.length);

            //Push params on stack
            for (int i = 0; i < p.numparams; i++) {
                stack[top + i] = args.arg(i + 1);
            }
            if (p.isVararg >= Lua.VARARG_NEEDSARG) {
                stack[p.numparams] = new LuaTable(args.subargs(p.numparams + 1));
            }
        }
    }

    private static Varargs extractVarargs(Prototype p, Varargs args) {
        if (p == null || p.isVararg == 0) {
            return NONE;
        }
        return args.subargs(p.numparams + 1);
    }

    @Override
    public String toString() {
        return "StackFrame[" + func + ", args=" + args + "]";
    }

    public void setReturnedValues(Varargs args) {
        // Push args on the stack
        LuaClosure closure = func.checkclosure();
        Prototype p = closure.getPrototype();
        if (pc < 0) {
            v = args;
            return;
        }

        int i = p.code[pc - 1];
        int opcode = (i & 0x3f);
        if (opcode != Lua.OP_CALL && opcode != Lua.OP_TAILCALL) {
            v = args;
            return;
        }

        // Yielded from a Lua function call -- push return values on the stack
        int a = ((i >> 6) & 0xff);
        int c = (i >> 14) & 0x1ff;
        LuaInterpreter.pushReturnValues(this, args, a, c);
    }

}
