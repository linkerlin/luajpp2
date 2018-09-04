package nl.weeaboo.lua2;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.lib.ClassLoaderResourceFinder;
import nl.weeaboo.lua2.lib.ILuaResourceFinder;
import nl.weeaboo.lua2.lib.LuaResource;
import nl.weeaboo.lua2.stdlib.DebugLib;
import nl.weeaboo.lua2.stdlib.StandardLibrary;
import nl.weeaboo.lua2.vm.LuaClosure;
import nl.weeaboo.lua2.vm.LuaTable;
import nl.weeaboo.lua2.vm.LuaThread;
import nl.weeaboo.lua2.vm.Varargs;

@LuaSerializable
public final class LuaRunState implements Serializable, ILuaResourceFinder {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(LuaRunState.class);

    private static ThreadLocal<LuaRunState> threadInstance = new ThreadLocal<>();

    private final LuaTable globals = new LuaTable();
    private final LuaTable registry = new LuaTable();
    private final Metatables metatables = new Metatables();
    private final LuaThreadGroup threadGroup;
    private final LuaThread mainThread;

    private boolean destroyed;
    private boolean debugEnabled = true;
    private int instructionCountLimit = 10 * 1000 * 1000;

    private ILuaResourceFinder resourceFinder = new ClassLoaderResourceFinder();

    private transient @Nullable LuaThread currentThread;
    private transient int instructionCount;

    @SuppressWarnings("deprecation")
    private LuaRunState() {
        threadGroup = new LuaThreadGroup(this);

        mainThread = LuaThread.createMainThread(this, globals);
        threadGroup.add(mainThread);
    }

    /**
     * Creates a new instance using the stoch standard library.
     *
     * @throws LuaException If no run state could be created.
     * @see #create(StandardLibrary)
     */
    public static LuaRunState create() throws LuaException {
        return create(new StandardLibrary());
    }

    /**
     * Creates a new instance using the supplied standard library.
     *
     * @throws LuaException If no run state could be created.
     */
    public static LuaRunState create(StandardLibrary stdlib) throws LuaException {
        LuaRunState runState = new LuaRunState();
        runState.registerOnThread();
        stdlib.register();
        return runState;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        registerOnThread();

        in.defaultReadObject();
    }

    /**
     * Destroys this Lua context. This destroys all threads and attempts to close any open resources. After
     * calling this method, any Lua resources created by this context should no longer be used.
     */
    public void destroy() {
        if (destroyed) {
            return;
        }

        LOG.debug("Destroying LuaRunState: {}", this);

        destroyed = true;
        threadGroup.destroy();

        currentThread = null;

        if (threadInstance.get() == this) {
            threadInstance.set(null);
        }
    }

    /**
     * Sets this {@link LuaRunState} as the active Lua context for the current thread.
     */
    public void registerOnThread() {
        if (threadInstance.get() != this) {
            threadInstance.set(this);
            LOG.debug("Registered LuaRunState \"{}\" on thread \"{}\"", this, Thread.currentThread());
        }
    }

    /**
     * Creates a new thread with an empty call stack.
     *
     * @see #newThread(LuaClosure, Varargs)
     */
    public LuaThread newThread() {
        return threadGroup.newThread();
    }

    /**
     * Creates a new Lua thread running the supplied function called with the supplied arguments.
     */
    public LuaThread newThread(LuaClosure func, Varargs args) {
        LuaThread thread = newThread();
        thread.pushPending(func, args);
        return thread;
    }

    /**
     * Runs all threads.
     */
    public void update() {
        if (destroyed) {
            throw new IllegalStateException("Attempted to update a destroyed LuaRunState");
        }

        registerOnThread();
        threadGroup.update();
    }

    /**
     * Called by the interpreter on every instruction (if {@link LuaRunState#isDebugEnabled()}).
     *
     * @param pc The current program counter
     * @throws LuaException If an internal assertion fails.
     */
    public void onInstruction(int pc) throws LuaException {
        instructionCount++;
        if (currentThread != null && instructionCount > instructionCountLimit) {
            throw new LuaException("Lua thread instruction limit exceeded (is there an infinite loop somewhere)?");
        }
    }

    /**
     * Returns the {@link LuaRunState} that's registered on the current thread, or {@code null} if none is registered.
     */
    public static LuaRunState getCurrent() {
        return threadInstance.get();
    }

    /**
     * Returns {@code true} if debug mode is enabled (switches on some assertions as well as the {@link DebugLib}).
     */
    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    /**
     * Returns the main thread for this Lua context.
     */
    public LuaThread getMainThread() {
        return mainThread;
    }

    /**
     * Returns the currently running thread, or if no thread is running, the main thread.
     */
    public LuaThread getRunningThread() {
        LuaThread result = currentThread;
        if (result == null) {
            result = mainThread;
        }
        return result;
    }

    /**
     * The Lua globals table {@code _G}.
     */
    public LuaTable getGlobalEnvironment() {
        return globals;
    }

    /**
     * The Lua registry table.
     */
    public LuaTable getRegistry() {
        return registry;
    }

    /**
     * Returns the single-invocation instruction limit. If a threads runs more than this number of instructions
     * without yielding, an error is thrown.
     */
    public int getInstructionCountLimit() {
        return instructionCountLimit;
    }

    /**
     * @see #getInstructionCountLimit()
     */
    public void setInstructionCountLimit(int lim) {
        instructionCountLimit = lim;
    }

    /**
     * @deprecated Meant for internal use only.
     */
    @Deprecated
    public void setRunningThread(LuaThread t) {
        if (currentThread == t) {
            return;
        }

        currentThread = t;
        instructionCount = 0;
        LOG.trace("Set running thread: {}", t);
    }

    /**
     * The load path for Lua's 'require' function.
     * @return The load path, or an empty string if none is set.
     */
    public String getLuaPath() {
        return globals.get("package").get("path").optjstring("");
    }

    /**
     * Sets the load path for Lua's 'require' function.
     */
    public void setLuaPath(String path) {
        globals.get("package").set("path", path);
    }

    /**
     * Allow packages to mark themselves as loaded.
     */
    public void setPackageLoaded(String name, LuaTable value) {
        globals.get("package").get("loaded").set(name, value);
    }

    @Override
    public @Nullable LuaResource findResource(String filename) {
        return resourceFinder.findResource(filename);
    }

    /**
     * Sets the {@link ILuaResourceFinder} used to load script files.
     */
    public void setResourceFinder(ILuaResourceFinder resourceFinder) {
        this.resourceFinder = resourceFinder;
    }

    /**
     * @return {@code true} if there are still running threads.
     */
    public boolean isFinished() {
        return threadGroup.isFinished();
    }

    /**
     * The global metatables for basic types (numbers, strings, etc.)
     */
    public Metatables getMetatables() {
        return metatables;
    }

}
