package nl.weeaboo.lua2.link;

import static nl.weeaboo.lua2.vm.LuaConstants.NONE;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.LuaRunState;
import nl.weeaboo.lua2.internal.LuaArgsUtil;
import nl.weeaboo.lua2.io.DelayedReader;
import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.io.LuaSerializer;
import nl.weeaboo.lua2.luajava.CoerceJavaToLua;
import nl.weeaboo.lua2.vm.LuaClosure;
import nl.weeaboo.lua2.vm.LuaString;
import nl.weeaboo.lua2.vm.LuaThread;
import nl.weeaboo.lua2.vm.LuaValue;
import nl.weeaboo.lua2.vm.Varargs;

@LuaSerializable
public class LuaLink extends AbstractLuaLink {

    private static final long serialVersionUID = 1L;

    protected LuaRunState luaRunState;
    protected transient LuaThread thread;

    private boolean inited;

    /**
     * @see #setPersistent(boolean)
     */
    private boolean persistent;

    public LuaLink(LuaRunState lrs) {
        this(lrs, lrs.getGlobalEnvironment());
    }

    public LuaLink(LuaRunState lrs, LuaValue environment) {
        this(lrs, new LuaThread(lrs, environment));
    }

    public LuaLink(LuaRunState lrs, LuaThread thread) {
        this.luaRunState = lrs;
        this.thread = thread;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();

        LuaSerializer ls = LuaSerializer.getCurrent();
        if (ls == null) {
            out.writeObject(thread);
        } else {
            ls.writeDelayed(thread);
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        LuaSerializer ls = LuaSerializer.getCurrent();
        if (ls == null) {
            thread = (LuaThread)in.readObject();
        } else {
            ls.readDelayed(new DelayedReader() {
                @Override
                public void onRead(Object obj) {
                    thread = (LuaThread)obj;
                }
            });
        }
    }

    @Override
    public boolean isDestroyed() {
        return thread.isDead();
    }

    @Override
    public void destroy() {
        persistent = false;
        thread.destroy();
    }

    @Override
    protected LuaClosure findFunction(String funcName) {
        LuaValue table = thread.getfenv();

        //Resolve a.b.c.d, ends with table=c
        int index;
        while (table != null && !table.isnil() && (index = funcName.indexOf('.')) >= 0) {
            String part = funcName.substring(0, index);
            table = table.get(LuaString.valueOf(part));
            funcName = funcName.substring(index + 1);
        }

        LuaValue func = null;
        if (table != null && !table.isnil()) {
            func = table.get(LuaString.valueOf(funcName));
        }

        if (func instanceof LuaClosure) {
            return (LuaClosure)func;
        }

        // Function not found
        return null;
    }

    protected Varargs getImplicitArgs() {
        return NONE;
    }

    /**
     * @see #pushCall(LuaClosure, Varargs)
     * @throws LuaException If an error occurs while trying to set up the function call.
     */
    public void pushCall(String funcName, Object... args) throws LuaException {
        Varargs mergedArgs = LuaArgsUtil.concatVarargs(getImplicitArgs(), CoerceJavaToLua.coerceArgs(args));
        doPushCall(getFunction(funcName), mergedArgs);
    }

    /**
     * Pushes a function call on the thread's call stack.
     */
    public void pushCall(LuaClosure func, Varargs args) {
        doPushCall(func, LuaArgsUtil.concatVarargs(getImplicitArgs(), args));
    }

    private void doPushCall(LuaClosure func, Varargs args) {
        thread.pushPending(func, args);
    }

    /**
     * Calls a Lua function and returns its result.
     */
    @Override
    public Varargs call(LuaClosure func, Object... args) throws LuaException {
        Varargs mergedArgs = LuaArgsUtil.concatVarargs(getImplicitArgs(), CoerceJavaToLua.coerceArgs(args));
        return doCall(func, mergedArgs);
    }

    private Varargs doCall(LuaClosure func, Varargs args) throws LuaException {
        Varargs result = NONE;

        int oldSleep = thread.getSleep();
        try {
            doPushCall(func, args);

            // Run only the supplied function (and ignore sleep)
            thread.setSleep(0);
            result = thread.resume(1);
        } catch (RuntimeException e) {
            handleThreadException("Error calling function: " + func, e);
        } finally {
            thread.setSleep(oldSleep);
        }

        return result;
    }

    /**
     * @throws LuaException If initialization fails.
     */
    protected void init() throws LuaException {
    }

    @Override
    public boolean update() throws LuaException {
        boolean changed = false;

        if (!inited) {
            inited = true;
            changed = true;
            init();
        }

        if (isFinished()) {
            return changed;
        }

        if (getWait() != 0) {
            decreaseWait(1);
            return changed;
        }

        try {
            changed = true;
            thread.resume(-1);
        } catch (RuntimeException e) {
            handleThreadException("Error running thread", e);
        }

        return changed;
    }

    private void handleThreadException(String message, Exception e) throws LuaException {
        if (e.getCause() instanceof NoSuchMethodException) {
            throw new LuaException(message + ": " + e.getCause().getMessage());
        } else {
            throw LuaException.wrap(message, e);
        }
    }

    @Override
    public void jump(LuaClosure func, Varargs args) {
        thread.reset();
        doPushCall(func, args);
    }

    /**
     * @return {@code true} if this thread can still be used to run code.
     */
    public boolean isRunnable() {
        if (!inited) {
            return true;
        }
        if (thread == null) {
            return false;
        }
        return !thread.isFinished();
    }

    @Override
    public final boolean isFinished() {
        if (!inited) {
            return false;
        }
        if (thread == null) {
            return true;
        }
        return (persistent ? thread.isDead() : thread.isFinished());
    }

    /**
     * Returns the thread wrapped by this {@link LuaLink}.
     */
    public LuaThread getThread() {
        return thread;
    }

    /**
     * A persistent LuaLink will not destroy itself when its thread finishes.
     */
    public void setPersistent(boolean p) {
        persistent = p;
    }

    @Override
    public void setWait(int w) {
        thread.setSleep(w);
    }

    @Override
    public int getWait() {
        return thread.getSleep();
    }

}
