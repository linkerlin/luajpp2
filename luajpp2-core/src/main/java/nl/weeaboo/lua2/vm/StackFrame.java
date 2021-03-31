package nl.weeaboo.lua2.vm;

import static nl.weeaboo.lua2.vm.LuaConstants.NONE;
import static nl.weeaboo.lua2.vm.LuaNil.NIL;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

import javax.annotation.Nullable;

import nl.weeaboo.lua2.io.LuaSerializable;

@LuaSerializable
final class StackFrame implements Externalizable {

    enum Status {
        FRESH, RUNNING, FINISHED, CLOSED
    }

    // --- Uses manual serialization, don't add variables ---
    Status status;
    LuaFunction func;  //The function that's being called
    String functionName; // The name of 'func' at the place where it's being called from
    Varargs args;      //The args given
    Varargs varargs;   //The varargs part of the arguments given

    StackFrame parent; //Link to calling context
    int parentCount;   //Number of parents
    int returnBase;    //Stack offset in parent to write return values to
    int returnCount;   //Number of return values to write in parent stack

    LuaValue[] stack = LuaConstants.NOVALS;
    UpValue[] openups = UpValue.NOUPVALUES;
    Varargs v;
    int top;
    int pc;
    // --- Uses manual serialization, don't add variables ---

    @Deprecated
    public StackFrame() {
    }

    static StackFrame newInstance(LuaFunction func, Varargs args, String functionName,
            StackFrame parent, int returnBase, int returnCount) {

        StackFrame frame = new StackFrame();
        frame.prepareCall(func, args, functionName, parent, returnBase, returnCount);
        return frame;
    }

    /** Closes every frame in the callstack. */
    static void releaseCallstack(StackFrame frame) {
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
        out.writeUTF(functionName);
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
        functionName = in.readUTF();
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
        status = Status.CLOSED;

        closeUpValues();

        stack = LuaConstants.NOVALS;
    }

    public void closeUpValues() {
        for (int u = openups.length; --u >= 0;) {
            if (openups[u] != null) {
                openups[u].close();
                openups[u] = null;
            }
        }
    }

    private void resetExecutionState(int minStackSize) {
        if (stack.length < minStackSize) {
            stack = new LuaValue[minStackSize];
        }
        Arrays.fill(stack, NIL);

        // (re)size upValue array
        if (openups.length < stack.length) {
            openups = new UpValue[stack.length];
        } else {
            Arrays.fill(openups, null);
        }

        v = NONE;
        top = 0;
        pc = 0;
    }

    public int size() {
        return parentCount + 1;
    }

    public @Nullable LuaFunction getCallstackFunction(int level) {
        StackFrame sf = getStackFrame(level);
        if (sf == null) {
            return null;
        }
        return sf.func;
    }

    public @Nullable StackFrame getStackFrame(int level) {
        StackFrame sf = this;
        while (--level >= 1) {
            sf = sf.parent;
            if (sf == null) {
                return null;
            }
        }
        return sf;
    }

    private static @Nullable Prototype getPrototype(LuaFunction func) {
        if (func.isclosure()) {
            return func.checkclosure().getPrototype();
        } else {
            return null;
        }
    }

    public final void prepareCall(LuaFunction func, Varargs args, String functionName,
            StackFrame parent, int returnBase, int returnCount) {

        final Prototype p = getPrototype(func);

        this.status = Status.FRESH;
        this.func = func;
        this.functionName = functionName;

        this.parent = parent;
        this.parentCount = (parent != null ? parent.size() : 0);
        this.returnBase = returnBase;
        this.returnCount = returnCount;

        if (p == null) {
            resetExecutionState(0);
        } else {
            resetExecutionState(p.maxstacksize);
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

    public final void prepareTailcall(LuaFunction func, Varargs args, String functionName) {
        closeUpValues(); // We're clobbering the stack, save the upvalues first

        final Prototype p = getPrototype(func);

        // Don't change status

        this.func = func;
        this.functionName = functionName;
        this.args = args;
        this.varargs = extractVarargs(p, args);

        // Don't change parent

        if (p == null) {
            resetExecutionState(0);
        } else {
            resetExecutionState(p.maxstacksize);

            // Push params on stack
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
        if (pc <= 0) {
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

    public LuaValue getLocalValue(int index) {
        return stack[index - 1];
    }

    public void setLocalValue(int index, LuaValue value) {
        stack[index - 1] = value;
    }

}
