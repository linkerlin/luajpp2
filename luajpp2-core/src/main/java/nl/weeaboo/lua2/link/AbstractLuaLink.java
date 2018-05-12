package nl.weeaboo.lua2.link;

import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.vm.LuaClosure;
import nl.weeaboo.lua2.vm.Varargs;

@Deprecated
abstract class AbstractLuaLink implements ILuaLink {

    private static final long serialVersionUID = 1L;

    public abstract int getWait();

    @Override
    public boolean addWait(int dt) {
        if (dt < 0) {
            throw new IllegalArgumentException("Can't call addWait with a negative number: " + dt);
        }

        final int oldWait = getWait();
        if (oldWait >= 0 && dt > 0) {
            setWait(oldWait + dt);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean decreaseWait(int dt) {
        if (dt < 0) {
            throw new IllegalArgumentException("Can't call decreaseWait with a negative number: " + dt);
        }

        final int oldWait = getWait();
        if (oldWait > 0 && dt > 0) {
            setWait(Math.max(0, oldWait - dt));
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void setMinimumWait(int w) {
        final int oldWait = getWait();
        if (oldWait >= 0 && (w < 0 || w > oldWait)) {
            setWait(w);
        }
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

    protected abstract LuaClosure findFunction(String funcName);

    @Override
    public Varargs call(String funcName, Object... args) throws LuaException {
        return call(getFunction(funcName), args);
    }

}
