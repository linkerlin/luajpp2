package nl.weeaboo.lua2;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.lib.BaseLib;
import nl.weeaboo.lua2.lib.CoroutineLib;
import nl.weeaboo.lua2.lib.DebugLib;
import nl.weeaboo.lua2.lib.MathLib;
import nl.weeaboo.lua2.lib.OsLib;
import nl.weeaboo.lua2.lib.PackageLib;
import nl.weeaboo.lua2.lib.SerializableIoLib;
import nl.weeaboo.lua2.lib.StringLib;
import nl.weeaboo.lua2.lib.TableLib;
import nl.weeaboo.lua2.lib.ThreadLib;
import nl.weeaboo.lua2.link.ILuaLink;
import nl.weeaboo.lua2.link.LuaFunctionLink;
import nl.weeaboo.lua2.luajava.LuajavaLib;
import nl.weeaboo.lua2.vm.LuaClosure;
import nl.weeaboo.lua2.vm.LuaError;
import nl.weeaboo.lua2.vm.LuaTable;
import nl.weeaboo.lua2.vm.LuaThread;
import nl.weeaboo.lua2.vm.Varargs;

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

        globalEnvironment = registerStandardLibs(this);
		mainThread = LuaThread.createMainThread(this, globalEnvironment);
        threadGroups = new DestructibleElemList<LuaThreadGroup>();

		newThreadGroup();
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		registerOnThread();

		in.defaultReadObject();
	}

    private static LuaTable registerStandardLibs(LuaRunState lrs) {
        PackageLib packageLib = new PackageLib();
        lrs.setPackageLib(packageLib);

        LuaTable _G = new LuaTable();
        _G.load(new BaseLib());
        _G.load(packageLib);
        _G.load(new TableLib());
        _G.load(new StringLib());
        _G.load(new CoroutineLib());
        _G.load(MathLib.getInstance());
        _G.load(new SerializableIoLib());
        _G.load(new OsLib());
        _G.load(new LuajavaLib());
        _G.load(new ThreadLib());
        _G.load(new DebugLib());
        return _G;
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
