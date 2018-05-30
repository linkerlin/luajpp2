package nl.weeaboo.lua2.vm;

import static nl.weeaboo.lua2.internal.LuaArgsUtil.copyArgs;
import static nl.weeaboo.lua2.vm.LuaBoolean.FALSE;
import static nl.weeaboo.lua2.vm.LuaBoolean.TRUE;
import static nl.weeaboo.lua2.vm.LuaConstants.NONE;
import static nl.weeaboo.lua2.vm.LuaNil.NIL;
import static nl.weeaboo.lua2.vm.LuaValue.varargsOf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.LuaRunState;
import nl.weeaboo.lua2.stdlib.DebugLib;
import nl.weeaboo.lua2.vm.StackFrame.Status;

final class LuaInterpreter {

    private static final Logger LOG = LoggerFactory.getLogger(LuaInterpreter.class);

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

        final LuaClosure closure = sf.func.checkclosure();
        final Prototype p = closure.getPrototype();
        final int[] code = p.code;
        final LuaValue[] k = p.k;
        final UpValue[] upValues = closure.getUpValues();

        final LuaValue[] stack = sf.stack;
        final UpValue[] openups = sf.openups;
        final Varargs varargs = sf.varargs;
        int top = sf.top;
        int pc = sf.pc;
        Varargs v = sf.v;

        LuaRunState lrs = LuaRunState.getCurrent();
        sf.status = Status.RUNNING;

        int i;
        int a;
        int b;
        int c;
        LuaValue o;
        try {
            while (thread.isRunning()) {
                if (pc < 0 || pc >= code.length) {
                    throw new LuaException("Program Counter outside code range: " + pc + " for " + closure);
                }

                if (DebugLib.isDebugEnabled()) {
                    lrs.onInstruction(pc);
                    DebugLib.debugBytecode(thread, pc, varargs, top);
                }

                // pull out instruction
                i = code[pc++];
                a = ((i >> 6) & 0xff);

                // process the op code
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
                        pc++; /*
                                                             * skip next instruction (if C)
                                                             */
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

                case Lua.OP_SELF: /* A B C R(A+1):= R(B): R(A):= R(B)[RK(C)] */
                    stack[a + 1] = (o = stack[i >>> 23]);
                    stack[a] = o.get((c = (i >> 14) & 0x1ff) > 0xff ? k[c & 0x0ff] : stack[c]);
                    continue;

                case Lua.OP_ADD: /* A B C R(A):= RK(B) + RK(C) */
                    stack[a] = ((b = i >>> 23) > 0xff ? k[b & 0x0ff] : stack[b])
                            .add((c = (i >> 14) & 0x1ff) > 0xff ? k[c & 0x0ff] : stack[c]);
                    continue;

                case Lua.OP_SUB: /* A B C R(A):= RK(B) - RK(C) */
                    stack[a] = ((b = i >>> 23) > 0xff ? k[b & 0x0ff] : stack[b])
                            .sub((c = (i >> 14) & 0x1ff) > 0xff ? k[c & 0x0ff] : stack[c]);
                    continue;

                case Lua.OP_MUL: /* A B C R(A):= RK(B) * RK(C) */
                    stack[a] = ((b = i >>> 23) > 0xff ? k[b & 0x0ff] : stack[b])
                            .mul((c = (i >> 14) & 0x1ff) > 0xff ? k[c & 0x0ff] : stack[c]);
                    continue;

                case Lua.OP_DIV: /* A B C R(A):= RK(B) / RK(C) */
                    stack[a] = ((b = i >>> 23) > 0xff ? k[b & 0x0ff] : stack[b])
                            .div((c = (i >> 14) & 0x1ff) > 0xff ? k[c & 0x0ff] : stack[c]);
                    continue;

                case Lua.OP_MOD: /* A B C R(A):= RK(B) % RK(C) */
                    stack[a] = ((b = i >>> 23) > 0xff ? k[b & 0x0ff] : stack[b])
                            .mod((c = (i >> 14) & 0x1ff) > 0xff ? k[c & 0x0ff] : stack[c]);
                    continue;

                case Lua.OP_POW: /* A B C R(A):= RK(B) ^ RK(C) */
                    stack[a] = ((b = i >>> 23) > 0xff ? k[b & 0x0ff] : stack[b])
                            .pow((c = (i >> 14) & 0x1ff) > 0xff ? k[c & 0x0ff] : stack[c]);
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

                case Lua.OP_CONCAT: /* A B C R(A):= R(B).. ... ..R(C) */
                    b = i >>> 23;
                    c = (i >> 14) & 0x1ff;
                    if (c > b + 1) {
                        Buffer buffer = stack[c].buffer();
                        while (--c >= b) {
                            buffer = stack[c].concat(buffer);
                        }
                        stack[a] = buffer.value();
                    } else {
                        stack[a] = stack[c - 1].concat(stack[c]);
                    }
                    continue;

                case Lua.OP_JMP: /* sBx pc+=sBx */
                    pc += (i >>> 14) - 0x1ffff;
                    continue;

                case Lua.OP_EQ: /* A B C if ((RK(B) == RK(C)) ~= A) then pc++ */
                    if (((b = i >>> 23) > 0xff ? k[b & 0x0ff] : stack[b])
                            .eq_b((c = (i >> 14) & 0x1ff) > 0xff ? k[c & 0x0ff] : stack[c]) != (a != 0)) {
                        ++pc;
                    }
                    continue;

                case Lua.OP_LT: /* A B C if ((RK(B) < RK(C)) ~= A) then pc++ */
                    if (((b = i >>> 23) > 0xff ? k[b & 0x0ff] : stack[b])
                            .lt_b((c = (i >> 14) & 0x1ff) > 0xff ? k[c & 0x0ff] : stack[c]) != (a != 0)) {
                        ++pc;
                    }
                    continue;

                case Lua.OP_LE: /* A B C if ((RK(B) <= RK(C)) ~= A) then pc++ */
                    if (((b = i >>> 23) > 0xff ? k[b & 0x0ff] : stack[b])
                            .lteq_b((c = (i >> 14) & 0x1ff) > 0xff ? k[c & 0x0ff] : stack[c]) != (a != 0)) {
                        ++pc;
                    }
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

                case Lua.OP_CALL: /*
                                   * A B C R(A), ... ,R(A+C-2):= R(A)(R(A+1), ... ,R(A+B-1))
                                   */
                {
                    b = i >>> 23;
                    c = (i >> 14) & 0x1ff;
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
                    sf.top = top;
                    sf.pc = pc;
                    sf.v = v;
                    v = f.invoke(v);
                    top = sf.top;
                    pc = sf.pc;

                    if (thread.getStatus() == LuaThreadStatus.SUSPENDED) {
                        return v; // Yield
                    }

                    pushReturnValues(sf, v, a, c);
                    top = sf.top;
                    v = sf.v;
                    continue;
                }

                case Lua.OP_TAILCALL: /* A B C return R(A)(R(A+1), ... ,R(A+B-1)) */
                {
                    b = i >>> 23;
                    c = (i >> 14) & 0x1ff;
                    if (b > 0) {
                        v = copyArgs(stack, a + 1, b - 1); // Important: copies args
                    } else {
                        v = copyArgs(stack, a + 1, top - v.narg() - (a + 1), v); // Important: copies args
                    }

                    LuaValue f = stack[a];
                    if (f.isclosure()) {
                        thread.postReturn(sf);

                        sf.prepareTailcall(f.checkclosure(), v);
                        top = sf.top;
                        pc = sf.pc;
                        v = sf.v;

                        startCall(thread, sf);
                        return NONE;
                    }

                    sf.top = top;
                    sf.pc = pc;
                    sf.v = v;

                    // Call hooks as if we returned from the current function, then jump into the tail-called function
                    thread.postReturn(sf);
                    startCall(thread, sf);

                    // Hack to make recursive calls have the correct callstack size when I remove sf later
                    sf.parentCount--;
                    v = f.invoke(v);

                    if (sf != thread.callstack) {
                        // Remove sf from callstack
                        sf.close();
                        StackFrame cur = thread.callstack;
                        while (cur != null) {
                            if (cur.parent == sf) {
                                cur.parent = sf.parent;
                                break;
                            }
                            cur = cur.parent;
                        }
                        return NONE;
                    } else {
                        // Java function didn't do anything to the callstack, recover.
                        sf.parentCount++;

                        pc = sf.pc;

                        if (thread.getStatus() == LuaThreadStatus.SUSPENDED) {
                            return v; // Yield
                        }

                        pushReturnValues(sf, v, a, c);
                        top = sf.top;
                        v = sf.v;
                    }
                    continue;
                }

                case Lua.OP_RETURN: /* A B return R(A), ... ,R(A+B-2) (see note) */
                    b = i >>> 23;

                    sf.status = Status.FINISHED;
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

                case Lua.OP_FORLOOP: /*
                                      * A sBx R(A)+=R(A+2): if R(A) <?= R(A+1) then { pc+=sBx: R(A+3)=R(A) }
                                      */
                {
                    LuaValue limit = stack[a + 1];
                    LuaValue step = stack[a + 2];
                    LuaValue idx = step.add(stack[a]);
                    if (step.gt_b(0) ? idx.lteq_b(limit) : idx.gteq_b(limit)) {
                        stack[a] = idx;
                        stack[a + 3] = idx;
                        pc += (i >>> 14) - 0x1ffff;
                    }

                }
                    continue;

                case Lua.OP_FORPREP: /* A sBx R(A)-=R(A+2): pc+=sBx */
                {
                    LuaValue init = stack[a].checknumber("'for' initial value must be a number");
                    LuaValue limit = stack[a + 1].checknumber("'for' limit must be a number");
                    LuaValue step = stack[a + 2].checknumber("'for' step must be a number");
                    stack[a] = init.sub(step);
                    stack[a + 1] = limit;
                    stack[a + 2] = step;
                    pc += (i >>> 14) - 0x1ffff;
                }
                    continue;

                case Lua.OP_TFORLOOP: /*
                                       * A C R(A+3), ... ,R(A+2+C):= R(A)(R(A+1), R(A+2)): if R(A+3) ~= nil
                                       * then R(A+2)=R(A+3) else pc++
                                       */
                    sf.top = top;
                    sf.pc = pc;
                    sf.v = v;
                    v = stack[a].invoke(varargsOf(stack[a + 1], stack[a + 2]));
                    top = sf.top;
                    pc = sf.pc;

                    if ((o = v.arg1()).isnil()) {
                        ++pc;
                    } else {
                        stack[a + 2] = stack[a + 3] = o;
                        for (c = (i >> 14) & 0x1ff; c > 1; --c) {
                            stack[a + 2 + c] = v.arg(c);
                        }
                        v = NONE; // todo: necessary?
                    }
                    continue;

                case Lua.OP_SETLIST: /*
                                      * A B C R(A)[(C-1)*FPF+i]:= R(A+i), 1 <= i <= B
                                      */
                {
                    if ((c = (i >> 14) & 0x1ff) == 0) {
                        c = code[pc++];
                    }
                    int offset = (c - 1) * Lua.LFIELDS_PER_FLUSH;
                    o = stack[a];
                    if ((b = i >>> 23) == 0) {
                        b = top - a - 1;
                        int m = b - v.narg();
                        int j = 1;
                        for (; j <= m; j++) {
                            o.set(offset + j, stack[a + j]);
                        }
                        for (; j <= b; j++) {
                            o.set(offset + j, v.arg(j - m));
                        }
                    } else {
                        o.presize(offset + b);
                        for (int j = 1; j <= b; j++) {
                            o.set(offset + j, stack[a + j]);
                        }
                    }
                }
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

                case Lua.OP_CLOSURE: /*
                                      * A Bx R(A):= closure(KPROTO[Bx], R(A), ... ,R(A+n))
                                      */
                {
                    Prototype newp = p.p[i >>> 14];
                    LuaClosure newcl = new LuaClosure(newp, closure.getfenv());
                    UpValue[] newUpValues = newcl.getUpValues();
                    for (int j = 0, nup = newp.nups; j < nup; ++j) {
                        i = code[pc++];
                        b = i >>> 23;
                        newUpValues[j] = (i & 4) != 0 ? upValues[b]
                                : openups[b] != null ? openups[b] : (openups[b] = new UpValue(stack, b));
                    }
                    stack[a] = newcl;
                }
                    continue;

                case Lua.OP_VARARG: /* A B R(A), R(A+1), ..., R(A+B-1) = vararg */
                    b = i >>> 23;
                    if (b == 0) {
                        top = a + (b = varargs.narg());
                        v = varargs;
                    } else {
                        for (int j = 1; j < b; ++j) {
                            stack[a + j - 1] = varargs.arg(j);
                        }
                    }
                    continue;
                }
            }

            // Yield
            if (thread.isDead() || thread.getStatus() == LuaThreadStatus.END_CALL) {
                sf.status = Status.FINISHED;
            }
            return NONE;
        } finally {
            sf.top = top;
            sf.pc = pc;
            sf.v = v;
        }
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

}
