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
    private final LuaThread mainThread;

    private boolean destroyed;
    private boolean debugEnabled = true;
    private int instructionCountLimit = 1000000;

    private ILuaResourceFinder resourceFinder = new ClassLoaderResourceFinder();

    private transient LuaThread currentThread;
    private transient int instructionCount;

    private LuaRunState() {
        threadGroups = new DestructibleElemList<LuaThreadGroup>();
        mainThread = LuaThread.createMainThread(this, globals);
        newThreadGroup();
    }

    public static LuaRunState create() throws LuaException {
        return create(new StandardLibrary());
    }

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

    public LuaFunctionLink newThread(LuaClosure func, Varargs args) {
        LuaThreadGroup group = getDefaultThreadGroup();
        if (group == null) {
            throw new IllegalStateException("Attempted to spawn a new thread, but all thread groups are destroyed");
        }
        return group.newThread(func, args);
    }

    public LuaFunctionLink newThread(String func, Object... args) {
        LuaThreadGroup group = getDefaultThreadGroup();
        if (group == null) {
            throw new IllegalStateException("Attempted to spawn a new thread, but all thread groups are destroyed");
        }
        return group.newThread(func, args);
    }

    public LuaThreadGroup newThreadGroup() {
        LuaThreadGroup tg = new LuaThreadGroup(this);
        threadGroups.add(tg);
        return tg;
    }

    public boolean update() throws LuaException {
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
     */
    public void onInstruction(int pc) throws LuaError {
        instructionCount++;
        if (currentThread != null && instructionCount > instructionCountLimit) {
            throw new LuaError("Lua thread instruction limit exceeded (is there an infinite loop somewhere)?");
        }
    }

    public static LuaRunState getCurrent() {
        return threadInstance.get();
    }

    @Override
    public boolean isDestroyed() {
        return destroyed;
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public LuaThreadGroup getDefaultThreadGroup() {
        return findFirstThreadGroup();
    }

    public LuaThread getRunningThread() {
        return (currentThread != null ? currentThread : mainThread);
    }

    public LuaTable getGlobalEnvironment() {
        return globals;
    }

    public LuaTable getRegistry() {
        return registry;
    }

    public int getInstructionCountLimit() {
        return instructionCountLimit;
    }

    public void setInstructionCountLimit(int lim) {
        instructionCountLimit = lim;
    }

    public void setRunningThread(LuaThread t) {
        if (currentThread == t) {
            return;
        }

        currentThread = t;
        instructionCount = 0;
        LOG.trace("Set running thread: {}", t);
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
