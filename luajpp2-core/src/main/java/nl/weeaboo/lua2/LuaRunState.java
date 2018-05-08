package nl.weeaboo.lua2;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.weeaboo.lua2.internal.DestructibleElemList;
import nl.weeaboo.lua2.internal.IDestructible;
import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.lib.ClassLoaderResourceFinder;
import nl.weeaboo.lua2.lib.ILuaResourceFinder;
import nl.weeaboo.lua2.lib.LuaResource;
import nl.weeaboo.lua2.link.LuaFunctionLink;
import nl.weeaboo.lua2.link.LuaLink;
import nl.weeaboo.lua2.stdlib.DebugLib;
import nl.weeaboo.lua2.stdlib.StandardLibrary;
import nl.weeaboo.lua2.vm.LuaClosure;
import nl.weeaboo.lua2.vm.LuaError;
import nl.weeaboo.lua2.vm.LuaTable;
import nl.weeaboo.lua2.vm.LuaThread;
import nl.weeaboo.lua2.vm.Varargs;

@LuaSerializable
public final class LuaRunState implements Serializable, IDestructible, ILuaResourceFinder {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(DestructibleElemList.class);

    private static ThreadLocal<LuaRunState> threadInstance = new ThreadLocal<LuaRunState>();

    private final LuaTable globals = new LuaTable();
    private final LuaTable registry = new LuaTable();
    private final DestructibleElemList<LuaThreadGroup> threadGroups;
    private final LuaLink mainThread;

    private boolean destroyed;
    private boolean debugEnabled = true;
    private int instructionCountLimit = 10 * 1000 * 1000;

    private ILuaResourceFinder resourceFinder = new ClassLoaderResourceFinder();

    private transient LuaThread currentThread;
    private transient int instructionCount;

    private LuaRunState() {
        threadGroups = new DestructibleElemList<LuaThreadGroup>();

        LuaThreadGroup mainGroup = newThreadGroup();
        mainThread = new LuaLink(this, LuaThread.createMainThread(this, globals));
        mainGroup.add(mainThread);
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

    @Override
    public void destroy() {
        if (destroyed) {
            return;
        }

        LOG.debug("Destroying LuaRunState: {}", this);

        destroyed = true;
        threadGroups.destroyAll();

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

    private LuaThreadGroup findFirstThreadGroup() {
        for (LuaThreadGroup group : threadGroups) {
            if (!group.isDestroyed()) {
                return group;
            }
        }
        return null;
    }

    /**
     * Creates a new Lua thread running the supplied function called with the supplied arguments.
     * @see LuaThreadGroup#newThread(LuaClosure, Varargs)
     */
    public LuaFunctionLink newThread(LuaClosure func, Varargs args) {
        LuaThreadGroup group = getDefaultThreadGroup();
        if (group == null) {
            throw new IllegalStateException("Attempted to spawn a new thread, but all thread groups are destroyed");
        }
        return group.newThread(func, args);
    }

    /**
     * Creates a new Lua thread running the supplied function called with the supplied arguments.
     * @see LuaThreadGroup#newThread(String, Object...)
     */
    public LuaFunctionLink newThread(String func, Object... args) {
        LuaThreadGroup group = getDefaultThreadGroup();
        if (group == null) {
            throw new IllegalStateException("Attempted to spawn a new thread, but all thread groups are destroyed");
        }
        return group.newThread(func, args);
    }

    /**
     * Creates a new Lua thread group.
     */
    public LuaThreadGroup newThreadGroup() {
        LuaThreadGroup tg = new LuaThreadGroup(this);
        threadGroups.add(tg);
        return tg;
    }

    /**
     * Runs all threads.
     */
    public boolean update() {
        if (destroyed) {
            LOG.debug("Attempted to update a destroyed LuaRunState");
            return false;
        }

        registerOnThread();

        boolean changed = false;
        for (LuaThreadGroup tg : threadGroups) {
            changed |= tg.update();
        }
        return changed;
    }

    /**
     * Called by the interpreter on every instruction (if {@link LuaRunState#isDebugEnabled()}).
     *
     * @param pc The current program counter
     * @throws LuaError If an internal assertion fails.
     */
    public void onInstruction(int pc) throws LuaError {
        instructionCount++;
        if (currentThread != null && instructionCount > instructionCountLimit) {
            throw new LuaError("Lua thread instruction limit exceeded (is there an infinite loop somewhere)?");
        }
    }

    /**
     * Returns the {@link LuaRunState} that's registered on the current thread, or {@code null} if none is registered.
     */
    public static LuaRunState getCurrent() {
        return threadInstance.get();
    }

    @Override
    public boolean isDestroyed() {
        return destroyed;
    }

    /**
     * Returns {@code true} if debug mode is enabled (switches on some assertions as well as the {@link DebugLib}).
     */
    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    /**
     * Returns the default thread group used for new threads.
     */
    public LuaThreadGroup getDefaultThreadGroup() {
        return findFirstThreadGroup();
    }

    /**
     * Returns the main thread for this Lua context.
     */
    public LuaLink getMainThread() {
        return mainThread;
    }

    /**
     * Returns the currently running thread, or if no thread is running, the main thread.
     */
    public LuaThread getRunningThread() {
        return (currentThread != null ? currentThread : mainThread.getThread());
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
    public LuaResource findResource(String filename) {
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
        for (LuaThreadGroup group : threadGroups) {
            if (!group.getThreads().isEmpty()) {
                return false;
            }
        }
        return true;
    }

}
