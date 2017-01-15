package nl.weeaboo.lua2.link;

import nl.weeaboo.lua2.LuaRunState;
import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.vm.LuaClosure;
import nl.weeaboo.lua2.vm.LuaString;
import nl.weeaboo.lua2.vm.LuaUserdata;
import nl.weeaboo.lua2.vm.LuaValue;
import nl.weeaboo.lua2.vm.Varargs;

@LuaSerializable
public class LuaMethodLink extends LuaFunctionLink {

    private static final long serialVersionUID = 1L;

    public final LuaUserdata self;

    public LuaMethodLink(LuaRunState lrs, LuaUserdata self, String methodName, Object... args) {
        this(lrs, lrs.getGlobalEnvironment(), self, methodName, args);
    }

    public LuaMethodLink(LuaRunState lrs, LuaValue environment, LuaUserdata self, String methodName,
            Object... args) {

        super(lrs, environment, methodName, args);

        this.self = self;
    }

    public LuaMethodLink(LuaRunState lrs, LuaUserdata self, LuaClosure func, Varargs args) {
        this(lrs, lrs.getGlobalEnvironment(), self, func, args);
    }

    public LuaMethodLink(LuaRunState lrs, LuaValue environment, LuaUserdata self, LuaClosure func,
            Varargs args) {

        super(lrs, environment, func, args);

        this.self = self;
    }

    @Override
    protected Varargs getImplicitArgs() {
        return self;
    }

    @Override
    protected LuaClosure findFunction(String methodName) {
        LuaValue val = self.get(LuaString.valueOf(methodName));
        return (val instanceof LuaClosure ? (LuaClosure)val : null);
    }

}
