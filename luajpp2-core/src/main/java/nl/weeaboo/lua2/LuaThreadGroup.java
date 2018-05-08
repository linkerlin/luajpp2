package nl.weeaboo.lua2;

import java.io.Serializable;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.weeaboo.lua2.internal.DestructibleElemList;
import nl.weeaboo.lua2.internal.IDestructible;
import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.link.ILuaLink;
import nl.weeaboo.lua2.link.LuaFunctionLink;
import nl.weeaboo.lua2.vm.LuaClosure;
import nl.weeaboo.lua2.vm.LuaValue;
import nl.weeaboo.lua2.vm.Varargs;

@LuaSerializable
public final class LuaThreadGroup implements Serializable, IDestructible {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(DestructibleElemList.class);

    private final LuaRunState luaRunState;
    private final LuaValue environment;
    private boolean destroyed;
    private boolean suspended;

    private final DestructibleElemList<ILuaLink> threads;

    public LuaThreadGroup(LuaRunState lrs) {
        this(lrs, lrs.getGlobalEnvironment());
    }

    public LuaThreadGroup(LuaRunState lrs, LuaValue environment) {
        this.luaRunState = lrs;
        this.environment = environment;

        threads = new DestructibleElemList<ILuaLink>();
    }

    private void checkDestroyed() {
        if (isDestroyed()) {
            throw new IllegalStateException("Attempted to change a disposed thread group");
        }
    }

    @Override
    public void destroy() {
        if (destroyed) {
            return;
        }

        destroyed = true;

        threads.destroyAll();
    }

    /** Calls the given function (with the supplied arguments) in a new thread */
    public LuaFunctionLink newThread(LuaClosure func, Varargs args) {
        checkDestroyed();

        LuaFunctionLink thread = new LuaFunctionLink(luaRunState, environment, func, args);
        add(thread);
        return thread;
    }

    /** Calls the given function (with the supplied arguments) in a new thread */
    public LuaFunctionLink newThread(String func, Object... args) {
        checkDestroyed();

        LuaFunctionLink thread = new LuaFunctionLink(luaRunState, environment, func, args);
        add(thread);
        return thread;
    }

    /** Adds a thread to this thread group */
    public void add(ILuaLink link) {
        checkDestroyed();

        threads.add(link);
    }

    /** Runs all threads in this thread group */
    public boolean update() {
        checkDestroyed();

        boolean changed = false;
        for (ILuaLink thread : getThreads()) {
            if (!suspended) {
                try {
                    changed |= thread.update();
                } catch (LuaException e) {
                    LOG.warn("Error running thread: {}", thread, e);
                }
            }
            if (isDestroyed()) {
                break;
            }
        }
        return changed;
    }

    @Override
    public boolean isDestroyed() {
        return destroyed;
    }

    /**
     * Returns a snapshots of the non-finished threads currently attached to this thread group.
     */
    public Collection<ILuaLink> getThreads() {
        for (ILuaLink thread : threads) {
            if (thread.isFinished()) {
                LOG.debug("Removing finished thread: {}", thread);
                threads.remove(thread);
            }
        }
        return threads.getSnapshot();
    }

    /**
     * Suspends this thread group.
     * @see #setSuspended(boolean)
     */
    public void suspend() {
        setSuspended(true);
    }

    /**
     * Resume this thread group.
     * @see #setSuspended(boolean)
     */
    public void resume() {
        setSuspended(false);
    }

    /**
     * @see #setSuspended(boolean)
     */
    public boolean isSuspended() {
        return suspended;
    }

    /**
     * Sets the suspend-state for this thread group. A suspended thread group doesn't run its threads even
     * when {@link #update()} is called.
     */
    public void setSuspended(boolean s) {
        checkDestroyed();

        suspended = s;
    }

}
