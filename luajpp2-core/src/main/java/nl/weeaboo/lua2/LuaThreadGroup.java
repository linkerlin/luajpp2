package nl.weeaboo.lua2;

import static nl.weeaboo.lua2.vm.LuaConstants.NONE;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.vm.LuaThread;
import nl.weeaboo.lua2.vm.LuaValue;

@LuaSerializable
final class LuaThreadGroup implements Serializable {

    private static final long serialVersionUID = 3L;
    private static final Logger LOG = LoggerFactory.getLogger(LuaThreadGroup.class);

    private final LuaRunState luaRunState;
    private final LuaValue environment;

    // This list is treated as immutable; it will never be modified once assigned
    private List<LuaThread> threads = Collections.emptyList();
    private ILuaExceptionHandler exceptionHandler = new DefaultLuaExceptionHandler();
    private boolean destroyed;

    public LuaThreadGroup(LuaRunState lrs) {
        this(lrs, lrs.getGlobalEnvironment());
    }

    public LuaThreadGroup(LuaRunState lrs, LuaValue environment) {
        this.luaRunState = lrs;
        this.environment = environment;
    }

    private void checkDestroyed() {
        if (destroyed) {
            throw new IllegalStateException("Attempted to change a disposed thread group");
        }
    }

    public boolean isFinished() {
        for (LuaThread thread : threads) {
            if (thread.isRunnable()) {
                return false;
            }
        }
        return true;
    }

    public void destroy() {
        destroyed = true;

        for (LuaThread thread : threads) {
            thread.destroy();
        }
    }

    public LuaThread newThread() {
        checkDestroyed();

        LuaThread thread = new LuaThread(luaRunState, environment);
        add(thread);
        return thread;
    }

    /** Adds a thread to this thread group */
    void add(LuaThread thread) {
        checkDestroyed();

        List<LuaThread> newThreads = new ArrayList<>(threads);
        newThreads.add(thread);
        threads = newThreads;
    }

    /** Runs all threads in this thread group */
    public void update() {
        checkDestroyed();

        for (LuaThread thread : threads) {
            if (!thread.isDead()) {
                try {
                    thread.resume(NONE);
                } catch (RuntimeException e) {
                    exceptionHandler.onScriptException(thread, e);
                }
            }

            if (destroyed) {
                break;
            }
        }

        removeDeadThreads();
    }

    /**
     * Returns a snapshots of the active threads currently attached to this thread group.
     */
    public Collection<LuaThread> getThreads() {
        removeDeadThreads();
        return Collections.unmodifiableList(threads);
    }

    private void removeDeadThreads() {
        List<LuaThread> toRemove = null;
        for (LuaThread thread : threads) {
            if (thread.isDead()) {
                LOG.debug("Removing dead thread: {}", thread);
                if (toRemove == null) {
                    toRemove = new ArrayList<>();
                }
                toRemove.add(thread);
            }
        }

        if (toRemove != null) {
            List<LuaThread> newThreads = new ArrayList<>(threads);
            newThreads.removeAll(toRemove);
            threads = newThreads;
        }
    }

    public void setExceptionHandler(ILuaExceptionHandler handler) {
        this.exceptionHandler = handler;
    }

}
