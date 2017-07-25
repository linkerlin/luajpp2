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

    public LuaFunctionLink newThread(LuaClosure func, Varargs args) {
        checkDestroyed();

        LuaFunctionLink thread = new LuaFunctionLink(luaRunState, environment, func, args);
        add(thread);
        return thread;
    }

    public LuaFunctionLink newThread(String func, Object... args) {
        checkDestroyed();

        LuaFunctionLink thread = new LuaFunctionLink(luaRunState, environment, func, args);
        add(thread);
        return thread;
    }

    public void addAll(LuaThreadGroup tg) {
        for (ILuaLink thread : tg.getThreads()) {
            add(thread);
        }
    }

    public void add(ILuaLink link) {
        checkDestroyed();

        threads.add(link);
    }

    public boolean update() throws LuaException {
        checkDestroyed();

        boolean changed = false;
        for (ILuaLink thread : getThreads()) {
            if (!suspended) {
                changed |= thread.update();
            }
            if (isDestroyed()) {
                break;
            }
        }
        return changed;
    }

    public boolean isSuspended() {
        return suspended;
    }

    @Override
    public boolean isDestroyed() {
        return destroyed;
    }

    public Collection<ILuaLink> getThreads() {
        for (ILuaLink thread : threads) {
            if (thread.isFinished()) {
                LOG.debug("Removing finished thread: {}", thread);
                threads.remove(thread);
            }
        }
        return threads.getSnapshot();
    }

    public void suspend() {
        setSuspended(true);
    }

    public void resume() {
        setSuspended(false);
    }

    public void setSuspended(boolean s) {
        checkDestroyed();

        suspended = s;
    }

}
