package nl.weeaboo.lua2.link;

import static org.luaj.vm2.LuaValue.NONE;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.luaj.vm2.LuaClosure;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaThread;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.LuaRunState;
import nl.weeaboo.lua2.LuaUtil;
import nl.weeaboo.lua2.io.DelayedReader;
import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.io.LuaSerializer;
import nl.weeaboo.lua2.lib.CoerceJavaToLua;

@LuaSerializable
public class LuaLink extends AbstractLuaLink {

    private static final long serialVersionUID = 1L;

	protected LuaRunState luaRunState;
	protected transient LuaThread thread;

	private boolean inited;

    /** @see #setPersistent(boolean) */
	private boolean persistent;

	public LuaLink(LuaRunState lrs) {
		luaRunState = lrs;

		thread = new LuaThread(luaRunState, luaRunState.getGlobalEnvironment());
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();

		LuaSerializer ls = LuaSerializer.getThreadLocal();
		if (ls == null) {
			out.writeObject(thread);
		} else {
			ls.writeDelayed(thread);
		}
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();

		LuaSerializer ls = LuaSerializer.getThreadLocal();
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

    protected final LuaClosure getFunction(String funcName) throws LuaException {
        LuaClosure function = findFunction(funcName);
        if (function != null) {
            return function;
        }
        throw new LuaException(String.format("function \"%s\" not found", funcName));
	}

    public final boolean hasFunction(String funcName) {
        return findFunction(funcName) != null;
    }

    protected LuaClosure findFunction(String funcName) {
        LuaValue table = thread.getfenv();

		//Resolve a.b.c.d, ends with table=c
		int index;
		while (table != null && !table.isnil() && (index = funcName.indexOf('.')) >= 0) {
			String part = funcName.substring(0, index);
			table = table.get(LuaString.valueOf(part));
			funcName = funcName.substring(index+1);
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
		return LuaValue.NONE;
	}

	public void pushCall(String funcName, Object... args) throws LuaException {
        Varargs mergedArgs = LuaUtil.concatArgs(getImplicitArgs(), CoerceJavaToLua.coerceArgs(args));
        pushCall(getFunction(funcName), mergedArgs);
	}

	public void pushCall(LuaClosure func, Varargs args) throws LuaException {
        pushCall(func, LuaUtil.concatArgs(getImplicitArgs(), args));
    }

    private void doPushCall(LuaClosure func, Varargs args) {
        thread.pushPending(func, args);
	}

    /** @see #call(LuaClosure, Object...) */
    public Varargs call(String funcName, Object... args) throws LuaException {
        return call(getFunction(funcName), args);
    }

    /**
     * Calls a Lua function and returns its result.
     */
	public Varargs call(LuaClosure func, Object... args) throws LuaException {
        Varargs mergedArgs = LuaUtil.concatArgs(getImplicitArgs(), CoerceJavaToLua.coerceArgs(args));
        return doCall(func, mergedArgs);
	}

    private Varargs doCall(LuaClosure func, Varargs args) throws LuaException {
        Varargs result = NONE;

        ILuaLink oldLink = luaRunState.getCurrentLink();
		luaRunState.setCurrentLink(this);
		try {
            doPushCall(func, args);
            result = thread.resume(1);
		} catch (RuntimeException e) {
            handleThreadException("Error calling function: " + func, e);
		} finally {
			luaRunState.setCurrentLink(oldLink);
		}

        return result;
	}

    /**
     * @throws LuaException
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

        decreaseWait(1);
        if (getWait() != 0) {
            return changed;
		}

        ILuaLink oldLink = luaRunState.getCurrentLink();
		luaRunState.setCurrentLink(this);
		try {
			changed = true;
			thread.resume(-1);
		} catch (RuntimeException e) {
            handleThreadException("Error running thread", e);
		} finally {
			luaRunState.setCurrentLink(oldLink);
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

	public boolean isRunnable() {
	    if (!inited) return true;
	    if (thread == null) return false;
	    return !thread.isFinished();
	}

    @Override
    public final boolean isFinished() {
		if (!inited) return false;
		if (thread == null) return true;
		return (persistent ? thread.isDead() : thread.isFinished());
	}

    public LuaThread getThread() {
        return thread;
    }

	/**
	 * A persistent LuaLink will not destroy itself when its thread finishes.
	 */
	public void setPersistent(boolean p) {
		persistent = p;
	}

}
