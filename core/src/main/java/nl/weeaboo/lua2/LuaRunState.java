package nl.weeaboo.lua2;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.weeaboo.lua2.interpreter.LuaInterpreter;
import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.lib.ClassLoaderResourceFinder;
import nl.weeaboo.lua2.lib.CoroutineLib;
import nl.weeaboo.lua2.lib.DebugLib;
import nl.weeaboo.lua2.lib.LuaResource;
import nl.weeaboo.lua2.lib.LuaResourceFinder;
import nl.weeaboo.lua2.lib.MathLib;
import nl.weeaboo.lua2.lib.OsLib;
import nl.weeaboo.lua2.lib.PackageLib;
import nl.weeaboo.lua2.lib.SerializableIoLib;
import nl.weeaboo.lua2.lib.ThreadLib;
import nl.weeaboo.lua2.link.ILuaLink;
import nl.weeaboo.lua2.link.LuaFunctionLink;
import nl.weeaboo.lua2.luajava.LuajavaLib;
import nl.weeaboo.lua2.stdlib.BaseLib;
import nl.weeaboo.lua2.stdlib.StringLib;
import nl.weeaboo.lua2.stdlib.TableLib;
import nl.weeaboo.lua2.vm.LuaClosure;
import nl.weeaboo.lua2.vm.LuaError;
import nl.weeaboo.lua2.vm.LuaTable;
import nl.weeaboo.lua2.vm.LuaThread;
import nl.weeaboo.lua2.vm.Varargs;

@LuaSerializable
public final class LuaRunState implements Serializable, IDestructible, LuaResourceFinder {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(DestructibleElemList.class);

    private static ThreadLocal<LuaRunState> threadInstance = new ThreadLocal<LuaRunState>();

    private final LuaTable globals;
    private final DestructibleElemList<LuaThreadGroup> threadGroups;
    private final LuaThread mainThread;

    private PackageLib packageLib;

    private boolean destroyed;
    private int instructionCountLimit = 1000000;

    private LuaResourceFinder resourceFinder = new ClassLoaderResourceFinder();

    private transient ILuaLink current;
    private transient LuaThread currentThread;
    private transient int instructionCount;

    private LuaRunState() {
        globals = new LuaTable();
        threadGroups = new DestructibleElemList<LuaThreadGroup>();
        mainThread = LuaThread.createMainThread(this, globals);
        newThreadGroup();
    }

    public static LuaRunState newInstance() throws LuaException {
        LuaRunState runState = new LuaRunState();
        runState.loadStandardLib();

        return runState;
    }

    private void loadStandardLib() throws LuaException {
        registerOnThread();

        packageLib = new PackageLib();

        new BaseLib().register();
        globals.load(packageLib);
        new TableLib().register();
        new StringLib().register();
        globals.load(new CoroutineLib());
        globals.load(new MathLib());
        globals.load(new SerializableIoLib());
        globals.load(new OsLib());
        globals.load(new LuajavaLib());
        globals.load(new ThreadLib());
        globals.load(new DebugLib());

        // Set Thread.yield() as a global yield function
        globals.rawset("yield", globals.rawget("Thread").rawget("yield"));
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

        current = null;
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
     * Called by {@link LuaInterpreter} on every instruction (if {@link DebugLib#DEBUG_ENABLED}).
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

    public ILuaLink getCurrentLink() {
        return current;
    }

    public LuaThreadGroup getDefaultThreadGroup() {
        return findFirstThreadGroup();
    }

    public LuaThread getRunningThread() {
        return (currentThread != null ? currentThread : mainThread);
    }

    public PackageLib getPackageLib() {
        return packageLib;
    }

    public LuaTable getGlobalEnvironment() {
        return globals;
    }

    public int getInstructionCountLimit() {
        return instructionCountLimit;
    }

    public void setInstructionCountLimit(int lim) {
        instructionCountLimit = lim;
    }

    public void setCurrentLink(ILuaLink cur) {
        if (current == cur) {
            return;
        }

        current = cur;
        LOG.trace("Set current link: {}", cur);
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
     * Allow packages to mark themselves as loaded.
     *
     * @see PackageLib#setIsLoaded(String, LuaTable)
     */
    public void setIsLoaded(String name, LuaTable value) {
        packageLib.setIsLoaded(name, value);
    }

    @Override
    public LuaResource findResource(String filename) {
        return resourceFinder.findResource(filename);
    }

    public void setResourceFinder(LuaResourceFinder resourceFinder) {
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
