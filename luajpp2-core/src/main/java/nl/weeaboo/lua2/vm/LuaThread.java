/*******************************************************************************
 * Copyright (c) 2007 LuaJ. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ******************************************************************************/

package nl.weeaboo.lua2.vm;

import static nl.weeaboo.lua2.vm.LuaConstants.NONE;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.weeaboo.lua2.LuaRunState;
import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.stdlib.DebugLib;

@LuaSerializable
public final class LuaThread extends LuaValue implements Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(LuaThread.class);
    private static final long serialVersionUID = 2L;

    public static final int MAX_CALLSTACK = 512;
    public static LuaValue s_metatable;

    private LuaRunState luaRunState;
    private LuaValue env;
    private LuaThreadStatus status = LuaThreadStatus.INITIAL;
    private int callstackMin;
    private boolean isMainThread;
    private int sleep;

    StackFrame callstack;
    public Object debugState;

    /**
     * Do not use. Required for efficient serialization.
     */
    @Deprecated
    public LuaThread() {
    }

    public LuaThread(LuaRunState lrs, LuaValue environment) {
        luaRunState = lrs;
        env = environment;

        callstack = null;
    }

    /**
     * Create a LuaThread around a function and environment
     *
     * @param function The function to execute
     */
    public LuaThread(LuaThread parent, LuaClosure function) {
        this(parent.luaRunState, parent.getfenv());

        callstack = StackFrame.newInstance(function, NONE, null, 0, 0);
    }

    public static LuaThread createMainThread(LuaRunState lrs, LuaValue env) {
        LuaThread thread = new LuaThread(lrs, env);
        thread.isMainThread = true;
        return thread;
    }

    public void reset() {
        StackFrame.releaseCallstack(callstack);

        status = LuaThreadStatus.INITIAL;
        callstackMin = 0;
        callstack = null;
        debugState = null;
    }

    @Override
    public String toString() {
        return (isMainThread ? "main" : "") + super.toString();
    }

    @Override
    public int type() {
        return LuaConstants.TTHREAD;
    }

    @Override
    public String typename() {
        return "thread";
    }

    @Override
    public boolean isthread() {
        return true;
    }

    @Override
    public LuaThread optthread(LuaThread defval) {
        return this;
    }

    @Override
    public LuaThread checkthread() {
        return this;
    }

    @Override
    public LuaValue getmetatable() {
        return s_metatable;
    }

    @Override
    public LuaValue getfenv() {
        return env;
    }

    @Override
    public void setfenv(LuaValue env) {
        this.env = env;
    }

    public LuaThreadStatus getStatus() {
        return status;
    }

    public boolean isRunning() {
        return status == LuaThreadStatus.RUNNING;
    }

    public boolean isFinished() {
        return isDead() || callstack == null;
    }

    public boolean isDead() {
        return status == LuaThreadStatus.DEAD;
    }

    public boolean isEndCall() {
        return status == LuaThreadStatus.END_CALL;
    }

    public boolean isSuspended() {
        return status == LuaThreadStatus.SUSPENDED;
    }

    public void destroy() {
        status = LuaThreadStatus.DEAD;
    }

    public int callstackSize() {
        return (callstack != null ? callstack.size() : 0);
    }

    public void preCall(StackFrame sf) {
        if (DebugLib.isDebugEnabled()) {
            DebugLib.debugSetupCall(this, sf.args, sf.stack);
            DebugLib.debugOnCall(this, sf.func);

            LOG.trace(">>({}) {}", sf.size(), sf);
        }
    }

    /**
     * @param sf The stack frame that was just popped from the callstack.
     */
    public void postReturn(StackFrame sf) {
        if (DebugLib.isDebugEnabled()) {
            LOG.trace("<<({}) {}", sf.size(), sf);

            DebugLib.debugOnReturn(this);
        }

        if (callstack == null) {
            LOG.info("Final stack frame popped: {}", sf);
        }
    }

    public void pushPending(LuaClosure func, Varargs args) {
        pushPending(func, args, -1, 0);
    }

    public void pushPending(LuaClosure func, Varargs args, int returnBase, int returnCount) {
        callstack = StackFrame.newInstance(func, args, callstack, returnBase, returnCount);
    }

    public LuaFunction getCallstackFunction(int level) {
        if (callstack == null) {
            return null;
        }
        return callstack.getCallstackFunction(level);
    }

    public static LuaThread getRunning() {
        LuaRunState lrs = LuaRunState.getCurrent();
        if (lrs == null) {
            throw new RuntimeException("No LuaRunState valid on current thread: " + Thread.currentThread());
        }
        return lrs.getRunningThread();
    }

    public boolean isMainThread() {
        return isMainThread;
    }

    /**
     * Yield this thread with arguments.
     */
    public Varargs yield(Varargs args) {
        if (!isRunning()) {
            error(this + " not running");
        }

        status = LuaThreadStatus.SUSPENDED;
        return args;
    }

    public Varargs endCall(Varargs args) {
        args = yield(args);
        status = LuaThreadStatus.END_CALL;
        return args;
    }

    public Varargs resume(int maxDepth) {
        if (isDead()) {
            return valueOf("cannot resume dead thread");
        }

        if (sleep != 0) {
            if (sleep > 0) {
                sleep--;
            }
            return NONE;
        }

        final int oldCallstackMin = callstackMin;
        final LuaThread prior = luaRunState.getRunningThread();
        Varargs result;
        try {
            if (prior.status == LuaThreadStatus.RUNNING) {
                prior.status = LuaThreadStatus.SUSPENDED;
            }
            status = LuaThreadStatus.RUNNING;
            luaRunState.setRunningThread(this);

            callstackMin = Math.max(callstackMin, (maxDepth < 0 ? 0 : callstackSize() - maxDepth));
            result = LuaInterpreter.resume(this, callstackMin);
        } catch (LuaError e) {
            popStackFrames();
            throw e;
        } catch (Exception e) {
            popStackFrames();
            throw new LuaError("Runtime error :: " + e, e);
        } finally {
            callstackMin = oldCallstackMin;
            luaRunState.setRunningThread(prior);

            if (!isMainThread && callstack == null) {
                status = LuaThreadStatus.DEAD;
            } else if (status == LuaThreadStatus.RUNNING) {
                status = LuaThreadStatus.SUSPENDED;
            }

            if (prior.status == LuaThreadStatus.SUSPENDED) {
                prior.status = LuaThreadStatus.RUNNING;
            }
        }

        return result;
    }

    private void popStackFrames() {
        // Note: maxDepth may be negative
        while (callstack != null && callstackSize() > callstackMin) {
            popStackFrame();
        }
    }

    void popStackFrame() {
        final StackFrame sf = callstack;

        // Pop from call stack
        callstack = callstack.parent;

        // Close stack frame
        sf.close();

        // Notify debuglib that we've returned from our current call
        postReturn(sf);
    }

    public static Varargs execute(LuaClosure c, Varargs args) {
        LuaThread running = getRunning();
        running.pushPending(c, args);
        return running.resume(1);
    }

    public LuaValue getCallEnv() {
        if (callstack != null) {
            return callstack.getCallstackFunction(1).getfenv();
        }
        return getfenv();
    }

    /** Jumps to the given closure, ending the current call. */
    public void jump(LuaClosure closure, LuaValue args) {
        reset();
        pushPending(closure, args);
    }

    public void setSleep(int frames) {
        sleep = frames;
    }

    public int getSleep() {
        return sleep;
    }

    public void setArgs(Varargs args) {
        callstack.setArgs(args);
    }

    public Varargs getArgs() {
        // TODO: Append varargs
        return callstack.args;
    }

    public void setReturnedValues(Varargs args) {
        callstack.setReturnedValues(args);
    }

}
