package nl.weeaboo.lua2;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import org.luaj.vm2.LuaClosure;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaThread;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.PackageLib;

import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.lib.J2sePlatform;
import nl.weeaboo.lua2.link.ILuaLink;
import nl.weeaboo.lua2.link.LuaFunctionLink;

@LuaSerializable
public final class LuaRunState implements Serializable, IDestructible {

    private static final long serialVersionUID = 1L;

	private static ThreadLocal<LuaRunState> threadInstance = new ThreadLocal<LuaRunState>();

    private LuaTable globalEnvironment;
    private LuaThread mainThread;
    private DestructibleElemList<LuaThreadGroup> threadGroups;

    private boolean destroyed;
    private int instructionCountLimit = 1000000;
    private PackageLib packageLib;

    private transient ILuaLink current;
	private transient LuaThread currentThread;
	private transient int instructionCount;

	public LuaRunState() {
		registerOnThread();

        globalEnvironment = J2sePlatform.registerStandardLibs(this);
		mainThread = LuaThread.createMainThread(this, globalEnvironment);
        threadGroups = new DestructibleElemList<LuaThreadGroup>();

		newThreadGroup();
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

		destroyed = true;
        threadGroups.destroyAll();

		current = null;
		currentThread = null;

		if (threadInstance.get() == this) {
			threadInstance.set(null);
		}
	}

	public void registerOnThread() {
		threadInstance.set(this);
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
		return globalEnvironment;
	}
	public int getInstructionCountLimit() {
		return instructionCountLimit;
	}

	public void setInstructionCountLimit(int lim) {
		instructionCountLimit = lim;
	}

    public void setCurrentLink(ILuaLink cur) {
		current = cur;
	}

	public void setRunningThread(LuaThread t) {
		if (currentThread == t) {
			return;
		}

		currentThread = t;
		instructionCount = 0;
	}

    public void setPackageLib(PackageLib plib) {
		packageLib = plib;
	}

}
