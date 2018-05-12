package nl.weeaboo.lua2.link;

import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.vm.LuaClosure;
import nl.weeaboo.lua2.vm.LuaConstants;
import nl.weeaboo.lua2.vm.Varargs;

public class LuaLinkStub extends AbstractLuaLink {

    private static final long serialVersionUID = 1L;

    private int instructionsLeft;
    private int callCount;
    private boolean destroyed;

    private int wait;

    public LuaLinkStub() {
        this(1);
    }

    public LuaLinkStub(int instructionsLeft) {
        this.instructionsLeft = instructionsLeft;
    }

    @Override
    public boolean update() throws LuaException {
        callCount++;

        if (getWait() != 0) {
            decreaseWait(1);
            return false;
        }

        if (instructionsLeft > 0) {
            instructionsLeft--;
            return true;
        } else {
            return false;
        }
    }

    /** Reads and resets the internal call counter. */
    public int consumeCallCount() {
        int result = callCount;
        callCount = 0;
        return result;
    }

    @Override
    public boolean isFinished() {
        return instructionsLeft <= 0;
    }

    public void destroy() {
        destroyed = true;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    @Override
    public void jump(LuaClosure func, Varargs args) {
        instructionsLeft = 1;
        callCount = 0;
    }

    @Override
    protected LuaClosure findFunction(String funcName) {
        return null;
    }

    @Override
    public Varargs call(LuaClosure func, Object... args) throws LuaException {
        return LuaConstants.NONE;
    }

    @Override
    public int getWait() {
        return wait;
    }

    @Override
    public void setWait(int wait) {
        this.wait = wait;
    }

}
