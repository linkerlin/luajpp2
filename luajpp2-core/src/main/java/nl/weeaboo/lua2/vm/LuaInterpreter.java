package nl.weeaboo.lua2.vm;

import static nl.weeaboo.lua2.internal.LuaArgsUtil.copyArgs;
import static nl.weeaboo.lua2.vm.LuaBoolean.FALSE;
import static nl.weeaboo.lua2.vm.LuaBoolean.TRUE;
import static nl.weeaboo.lua2.vm.LuaConstants.NONE;
import static nl.weeaboo.lua2.vm.LuaNil.NIL;
import static nl.weeaboo.lua2.vm.LuaValue.varargsOf;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.errorprone.annotations.CheckReturnValue;

import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.LuaRunState;
import nl.weeaboo.lua2.stdlib.DebugLib;
import nl.weeaboo.lua2.vm.StackFrame.Status;

final class LuaInterpreter {

    private static final Logger LOG = LoggerFactory.getLogger(LuaInterpreter.class);

    private static FrameState frameState = new FrameState();

    private LuaInterpreter() {
    }

    /**
     * @param thread The executing thread
     * @param callstackBase Don't touch stack frames with zero-based index &lt; callstackBase.
     */
    public static Varargs resume(LuaThread thread, int callstackBase) {
        if (callstackBase < 0) {
            throw new IllegalArgumentException("callstackBase must be >= 0");
        }

        Varargs result = NONE;
        while (thread.isRunning() && thread.callstackSize() > callstackBase) {
            StackFrame sf = thread.callstack;
            if (sf.status == Status.FRESH) {
                startCall(thread, sf);
                sf.status = Status.RUNNING;
            }

            try {
                result = resume(thread, sf);
            } finally {
                if (sf.status == Status.FINISHED) {
                    finishCall(thread, sf, result);
                }
            }
        }

        return result;
    }

    private static Varargs resume(LuaThread thread, StackFrame sf) {
        if (sf.status != Status.RUNNING) {
            throw new LuaException("StackFrame isn't running: status=" + sf.status + ", stackFrame=" + sf);
        }

        sf.status = Status.RUNNING;

        Varargs result;
        frameState.startRunning(thread, sf);
        try {
            result = frameState.run();
        } finally {
            frameState.finishRunning();
        }

        return result;
    }

    private static void startCall(LuaThread thread, StackFrame sf) {
        thread.preCall(sf);
    }

    private static void finishCall(LuaThread thread, StackFrame sf, Varargs retval) {
        // Pushes return values on parent's stack
        StackFrame parent = sf.parent;
        if (parent != null) {
            int a = sf.returnBase;
            int c = sf.returnCount + 1;

            if (c > 0) {
                while (--c > 0) {
                    parent.stack[a + c - 1] = retval.arg(c);
                }
                parent.top = a + retval.narg();
                parent.v = retval;
            } else {
                parent.top = a + retval.narg();
                parent.v = retval;
            }
        }

        if (thread.callstack == sf) {
            thread.popStackFrame();
        } else {
            LOG.error("Callstack was corrupted, finished={}, callstack={}",
                    sf, thread.callstack);
            sf.close();
        }
    }

    static void pushReturnValues(StackFrame sf, int a, int c) {
        pushReturnValues(sf, sf.v, a, c);
    }

    static void pushReturnValues(StackFrame sf, Varargs returnValues, int a, int c) {
        // Push return values on the stack
        if (c > 0) {
            while (--c > 0) {
                sf.stack[a + c - 1] = returnValues.arg(c);
            }
            sf.v = NONE;
        } else {
            sf.top = a + returnValues.narg();
            sf.v = returnValues;
        }
    }

    private static final class FrameState {

        private LuaRunState lrs;
        private LuaThread thread;
        private StackFrame stackFrame;

        private LuaClosure closure;
        private Prototype p;
        private int[] code;
        private LuaValue[] k;
        private UpValue[] upValues;

        private LuaValue[] stack;
        private UpValue[] openups;
        private Varargs varargs;

        private int top;
        private int pc;
        private Varargs v;

        public void startRunning(LuaThread thread, StackFrame sf) {
            this.lrs = LuaRunState.getCurrent();
            this.thread = thread;
            this.stackFrame = sf;

            closure = sf.func.checkclosure();
            p = closure.getPrototype();
            code = p.code;
            k = p.k;
            upValues = closure.getUpValues();

            stack = sf.stack;
            openups = sf.openups;
            varargs = sf.varargs;

            top = sf.top;
            pc = sf.pc;
            v = sf.v;
        }

        public Varargs run() {
            // Only check this flag once, and not between every instruction
            final boolean debugEnabled = lrs.isDebugEnabled();

            int b;
            int c;
            LuaValue o;
            while (thread.isRunning()) {
                // Pull out instruction
                int i;
                try {
                    i = code[pc];
                } catch (ArrayIndexOutOfBoundsException e) {
                    throw new LuaException("Program Counter outside code range: " + pc + " for " + closure);
                }

                if (debugEnabled) {
                    lrs.onInstruction(pc);
                    DebugLib.debugBytecode(thread, pc, varargs, top);
                }

                pc++;

                // process the op code
                int a = ((i >> 6) & 0xff);
                switch (i & 0x3f) {

                case Lua.OP_MOVE:/* A B R(A):= R(B) */
                    stack[a] = stack[i >>> 23];
                    continue;

                case Lua.OP_LOADK:/* A Bx R(A):= Kst(Bx) */
                    stack[a] = k[i >>> 14];
                    continue;

                case Lua.OP_LOADBOOL:/* A B C R(A):= (Bool)B: if (C) pc++ */
                    stack[a] = (i >>> 23 != 0) ? TRUE : FALSE;
                    if ((i & (0x1ff << 14)) != 0) {
                        pc++; // Skip next instruction (if C)
                    }
                    continue;

                case Lua.OP_LOADNIL: /* A B R(A):= ...:= R(B):= nil */
                    for (b = i >>> 23; a <= b;) {
                        stack[a++] = NIL;
                    }
                    continue;

                case Lua.OP_GETUPVAL: /* A B R(A):= UpValue[B] */
                    stack[a] = upValues[i >>> 23].getValue();
                    continue;

                case Lua.OP_GETGLOBAL: /* A Bx R(A):= Gbl[Kst(Bx)] */
                    stack[a] = closure.getfenv().get(k[i >>> 14]);
                    continue;

                case Lua.OP_GETTABLE: /* A B C R(A):= R(B)[RK(C)] */
                    stack[a] = stack[i >>> 23].get((c = (i >> 14) & 0x1ff) > 0xff ? k[c & 0x0ff] : stack[c]);
                    continue;

                case Lua.OP_SETGLOBAL: /* A Bx Gbl[Kst(Bx)]:= R(A) */
                    closure.getfenv().set(k[i >>> 14], stack[a]);
                    continue;

                case Lua.OP_SETUPVAL: /* A B UpValue[B]:= R(A) */
                    upValues[i >>> 23].setValue(stack[a]);
                    continue;

                case Lua.OP_SETTABLE: /* A B C R(A)[RK(B)]:= RK(C) */
                    stack[a].set(((b = i >>> 23) > 0xff ? k[b & 0x0ff] : stack[b]),
                            (c = (i >> 14) & 0x1ff) > 0xff ? k[c & 0x0ff] : stack[c]);
                    continue;

                case Lua.OP_NEWTABLE: /* A B C R(A):= {} (size = B,C) */
                    stack[a] = new LuaTable(i >>> 23, (i >> 14) & 0x1ff);
                    continue;

                case Lua.OP_SELF:
                    opSelf(i, a);
                    continue;

                case Lua.OP_ADD:
                case Lua.OP_SUB:
                case Lua.OP_MUL:
                case Lua.OP_DIV:
                case Lua.OP_MOD:
                case Lua.OP_POW:
                    stack[a] = binaryArithmeticOp(i);
                    continue;

                case Lua.OP_UNM: /* A B R(A):= -R(B) */
                    stack[a] = stack[i >>> 23].neg();
                    continue;

                case Lua.OP_NOT: /* A B R(A):= not R(B) */
                    stack[a] = stack[i >>> 23].not();
                    continue;

                case Lua.OP_LEN: /* A B R(A):= length of R(B) */
                    stack[a] = stack[i >>> 23].len();
                    continue;

                case Lua.OP_CONCAT:
                    opConcat(i, a);
                    continue;

                case Lua.OP_JMP: /* sBx pc+=sBx */
                    pc += (i >>> 14) - 0x1ffff;
                    continue;

                case Lua.OP_EQ:
                    opCompare(i, a);
                    continue;

                case Lua.OP_TEST: /* A C if not (R(A) <=> C) then pc++ */
                    if (stack[a].toboolean() != ((i & (0x1ff << 14)) != 0)) {
                        ++pc;
                    }
                    continue;

                case Lua.OP_TESTSET: /*
                                      * A B C if (R(B) <=> C) then R(A):= R(B) else pc++
                                      */
                    /* note: doc appears to be reversed */
                    if ((o = stack[i >>> 23]).toboolean() != ((i & (0x1ff << 14)) != 0)) {
                        ++pc;
                    } else {
                        stack[a] = o; // TODO: should be sBx?
                    }
                    continue;

                case Lua.OP_CALL: {
                    Varargs result = opCall(i, a);
                    if (result != null) {
                        return result;
                    }
                    continue;
                }

                case Lua.OP_TAILCALL: {
                    Varargs result = opTailCall(i, a);
                    if (result != null) {
                        return result;
                    }
                    continue;
                }

                case Lua.OP_RETURN:
                    return opReturn(i, a);

                case Lua.OP_FORLOOP:
                    opForLoop(i, a);
                    continue;

                case Lua.OP_FORPREP:
                    opForPrep(i, a);
                    continue;

                case Lua.OP_TFORLOOP:
                    opTForLoop(i, a);
                    continue;

                case Lua.OP_SETLIST:
                    opSetList(i, a);
                    continue;

                case Lua.OP_CLOSE: /*
                                    * A close all variables in the stack up to (>=) R(A)
                                    */
                    for (b = openups.length; --b >= a;) {
                        if (openups[b] != null) {
                            openups[b].close();
                            openups[b] = null;
                        }
                    }
                    continue;

                case Lua.OP_CLOSURE:
                    opClosure(i, a);
                    continue;

                case Lua.OP_VARARG:
                    opVararg(i, a);
                    continue;
                }
            }

            return NONE;
        }

        public void finishRunning() {
            if (thread.isDead() || thread.getStatus() == LuaThreadStatus.END_CALL) {
                stackFrame.status = Status.FINISHED;
            }

            stackFrame.top = top;
            stackFrame.pc = pc;
            stackFrame.v = v;
        }

        /**
         * A B C R(A), ... ,R(A+C-2):= R(A)(R(A+1), ... ,R(A+B-1))
         *
         * @return A non-null value if execution should terminate with the given result.
         */
        private @Nullable Varargs opCall(int i, int a) {
            int b = i >>> 23;
            int c = (i >> 14) & 0x1ff;
            if (b > 0) {
                v = varargsOf(stack, a + 1, b - 1); // exact arg count
            } else {
                v = varargsOf(stack, a + 1, top - v.narg() - (a + 1), v); // from prev top
            }

            LuaValue f = stack[a];
            if (f.isclosure()) {
                thread.pushPending(f.checkclosure(), v, a, c - 1);
                return NONE;
            }

            // Call immediately
            invoke(f, v);

            if (thread.getStatus() == LuaThreadStatus.SUSPENDED) {
                return v; // Yield
            }

            pushReturnValues(stackFrame, v, a, c);
            top = stackFrame.top;
            v = stackFrame.v;
            return null;
        }

        /**
         * A B C return R(A)(R(A+1), ... ,R(A+B-1))
         *
         * @return A non-null value if execution should terminate with the given result.
         */
        private Varargs opTailCall(int i, int a) {
            int b = i >>> 23;
            if (b > 0) {
                v = copyArgs(stack, a + 1, b - 1); // Important: copies args
            } else {
                v = copyArgs(stack, a + 1, top - v.narg() - (a + 1), v); // Important: copies args
            }

            LuaValue f = stack[a];
            if (f.isclosure()) {
                thread.postReturn(stackFrame);

                stackFrame.prepareTailcall(f.checkclosure(), v);
                top = stackFrame.top;
                pc = stackFrame.pc;
                v = stackFrame.v;

                startCall(thread, stackFrame);
                return NONE;
            }

            stackFrame.top = top;
            stackFrame.pc = pc;
            stackFrame.v = v;

            // Call hooks as if we returned from the current function, then jump into the tail-called function
            thread.postReturn(stackFrame);
            startCall(thread, stackFrame);

            // Hack to make recursive calls have the correct callstack size when I remove stackFrame later
            stackFrame.parentCount--;
            v = f.invoke(v);

            if (stackFrame != thread.callstack) {
                // Remove stackFrame from callstack
                stackFrame.close();
                StackFrame cur = thread.callstack;
                while (cur != null) {
                    if (cur.parent == stackFrame) {
                        cur.parent = stackFrame.parent;
                        break;
                    }
                    cur = cur.parent;
                }
                return NONE;
            }

            // Java function didn't do anything to the callstack, recover.
            stackFrame.parentCount++;

            pc = stackFrame.pc;

            if (thread.getStatus() == LuaThreadStatus.SUSPENDED) {
                return v; // Yield
            }

            int c = (i >> 14) & 0x1ff;
            pushReturnValues(stackFrame, v, a, c);
            top = stackFrame.top;
            v = stackFrame.v;
            return null;
        }

        /**
         * OP_ADD :: A B C R(A):= RK(B) + RK(C)
         * OP_SUB :: A B C R(A):= RK(B) - RK(C)
         * OP_MUL :: A B C R(A):= RK(B) * RK(C)
         * OP_DIV :: A B C R(A):= RK(B) / RK(C)
         * OP_MOD :: A B C R(A):= RK(B) % RK(C)
         * OP_POW :: A B C R(A):= RK(B) ^ RK(C)
         */
        @CheckReturnValue
        private LuaValue binaryArithmeticOp(int i) {
            int b = i >>> 23;
            int c = (i >> 14) & 0x1ff;

            LuaValue left = (b > 0xff ? k[b & 0x0ff] : stack[b]);
            LuaValue right = (c > 0xff ? k[c & 0x0ff] : stack[c]);

            final int opcode = (i & 0x3f);
            switch (opcode) {
            case Lua.OP_ADD:
                return left.add(right);
            case Lua.OP_SUB:
                return left.sub(right);
            case Lua.OP_MUL:
                return left.mul(right);
            case Lua.OP_DIV:
                return left.div(right);
            case Lua.OP_MOD:
                return left.mod(right);
            case Lua.OP_POW:
                return left.pow(right);
            default:
                throw new LuaException("Unsupported opcode: " + opcode);
            }
        }

        /**
         * OP_EQ :: A B C if ((RK(B) == RK(C)) ~= A) then pc++
         * OP_LT :: A B C if ((RK(B) < RK(C)) ~= A) then pc++
         * OP_LE :: A B C if ((RK(B) <= RK(C)) ~= A) then pc++
         */
        private void opCompare(int i, int a) {
            boolean aBool = (a != 0);
            int b = i >>> 23;
            int c = (i >> 14) & 0x1ff;

            LuaValue left = (b > 0xff ? k[b & 0x0ff] : stack[b]);
            LuaValue right = (c > 0xff ? k[c & 0x0ff] : stack[c]);

            final int opcode = (i & 0x3f);
            switch (opcode) {
            case Lua.OP_EQ:
                if (left.eq_b(right) != aBool) {
                    pc++;
                }
                break;
            case Lua.OP_LT:
                if (left.lt_b(right) != aBool) {
                    pc++;
                }
                break;
            case Lua.OP_LE:
                if (left.lteq_b(right) != aBool) {
                    pc++;
                }
                break;
            default:
                throw new LuaException("Unsupported opcode: " + opcode);
            }
        }

        /** A Bx R(A):= closure(KPROTO[Bx], R(A), ... ,R(A+n)) */
        private void opClosure(int i, int a) {
            Prototype newp = p.p[i >>> 14];
            LuaClosure newcl = new LuaClosure(newp, closure.getfenv());
            UpValue[] newUpValues = newcl.getUpValues();
            for (int j = 0, nup = newp.nups; j < nup; ++j) {
                i = code[pc++];

                int b = i >>> 23;
                if ((i & 4) != 0) {
                    newUpValues[j] = upValues[b];
                } else {
                    if (openups[b] == null) {
                        openups[b] = new UpValue(stack, b);
                    }
                    newUpValues[j] = openups[b];
                }
            }
            stack[a] = newcl;
        }

        /** A B C R(A):= R(B).. ... ..R(C) */
        private void opConcat(int i, int a) {
            int b = i >>> 23;
            int c = (i >> 14) & 0x1ff;

            if (c > b + 1) {
                Buffer buffer = stack[c].buffer();
                while (--c >= b) {
                    buffer = stack[c].concat(buffer);
                }
                stack[a] = buffer.value();
            } else {
                stack[a] = stack[c - 1].concat(stack[c]);
            }
        }

        /** A sBx R(A)+=R(A+2): if R(A) <?= R(A+1) then { pc+=sBx: R(A+3)=R(A) } */
        private void opForLoop(int i, int a) {
            LuaValue limit = stack[a + 1];
            LuaValue step = stack[a + 2];
            LuaValue idx = step.add(stack[a]);
            if (step.gt_b(0) ? idx.lteq_b(limit) : idx.gteq_b(limit)) {
                stack[a] = idx;
                stack[a + 3] = idx;
                pc += (i >>> 14) - 0x1ffff;
            }
        }

        /** A sBx R(A)-=R(A+2): pc+=sBx */
        private void opForPrep(int i, int a) {
            LuaValue init = stack[a].checknumber("'for' initial value must be a number");
            LuaValue limit = stack[a + 1].checknumber("'for' limit must be a number");
            LuaValue step = stack[a + 2].checknumber("'for' step must be a number");
            stack[a] = init.sub(step);
            stack[a + 1] = limit;
            stack[a + 2] = step;
            pc += (i >>> 14) - 0x1ffff;
        }

        /**
         * A C R(A+3), ... ,R(A+2+C):= R(A)(R(A+1), R(A+2)):
         * if R(A+3) ~= nil then R(A+2)=R(A+3) else pc++
         */
        private void opTForLoop(int i, int a) {
            invoke(stack[a], varargsOf(stack[a + 1], stack[a + 2]));

            LuaValue object = v.arg1();
            if (object.isnil()) {
                pc++;
            } else {
                stack[a + 2] = stack[a + 3] = object;

                for (int c = (i >> 14) & 0x1ff; c > 1; --c) {
                    stack[a + 2 + c] = v.arg(c);
                }
                v = NONE; // todo: necessary?
            }
        }

        /** A B C R(A+1):= R(B): R(A):= R(B)[RK(C)] */
        private void opSelf(int i, int a) {
            LuaValue object = stack[i >>> 23];
            stack[a + 1] = object;

            int c = (i >> 14) & 0x1ff;
            stack[a] = object.get(c > 0xff ? k[c & 0x0ff] : stack[c]);
        }

        /** A B C R(A)[(C-1)*FPF+i]:= R(A+i), 1 <= i <= B */
        private void opSetList(int i, int a) {
            int b = (i >>> 23);
            int c = (i >> 14) & 0x1ff;
            if (c == 0) {
                c = code[pc++];
            }

            LuaValue object = stack[a];
            int offset = (c - 1) * Lua.LFIELDS_PER_FLUSH;

            if (b == 0) {
                b = top - a - 1;
                int m = b - v.narg();
                int j = 1;
                for (; j <= m; j++) {
                    object.set(offset + j, stack[a + j]);
                }
                for (; j <= b; j++) {
                    object.set(offset + j, v.arg(j - m));
                }
            } else {
                object.presize(offset + b);
                for (int j = 1; j <= b; j++) {
                    object.set(offset + j, stack[a + j]);
                }
            }
        }

        /** A B return R(A), ... ,R(A+B-2) (see note) */
        private Varargs opReturn(int i, int a) {
            int b = i >>> 23;

            stackFrame.status = Status.FINISHED;

            switch (b) {
            case 0:
                return copyArgs(stack, a, top - v.narg() - a, v); // Important: copies args
            case 1:
                return NONE;
            case 2:
                return stack[a];
            default:
                return copyArgs(stack, a, b - 1); // Important: copies args
            }
        }

        /** A B R(A), R(A+1), ..., R(A+B-1) = vararg */
        private void opVararg(int i, int a) {
            int b = i >>> 23;
            if (b == 0) {
                top = a + varargs.narg();
                v = varargs;
            } else {
                for (int j = 1; j < b; ++j) {
                    stack[a + j - 1] = varargs.arg(j);
                }
            }
        }

        private void invoke(LuaValue function, Varargs args) {
            stackFrame.top = top;
            stackFrame.pc = pc;
            stackFrame.v = v;

            v = function.invoke(args);

            top = stackFrame.top;
            pc = stackFrame.pc;
        }
    }
}
