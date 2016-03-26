package nl.weeaboo.lua2.link;

import org.luaj.vm2.LuaClosure;
import org.luaj.vm2.Varargs;

import nl.weeaboo.lua2.LuaException;

public class LuaLinkStub extends AbstractLuaLink {

    private static final long serialVersionUID = 1L;

    private int instructionsLeft;
    private int callCount;
    private boolean destroyed;

    public LuaLinkStub() {
        this(1);
    }

    public LuaLinkStub(int instructionsLeft) {
        this.instructionsLeft = instructionsLeft;
    }

    @Override
    public boolean update() throws LuaException {
        callCount++;
        if (instructionsLeft > 0) {
            instructionsLeft--;
            return true;
        } else {
            return false;
        }
    }

    public int consumeCallCount() {
        int result = callCount;
        callCount = 0;
        return result;
    }

    @Override
    public boolean isFinished() {
        return instructionsLeft <= 0;
    }

    @Override
    public void destroy() {
        destroyed = true;
    }

    @Override
    public boolean isDestroyed() {
        return destroyed;
    }

    @Override
    public void jump(LuaClosure func, Varargs args) {
        instructionsLeft = 1;
        callCount = 0;
    }

}
