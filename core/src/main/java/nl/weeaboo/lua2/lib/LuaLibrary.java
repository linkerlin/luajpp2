package nl.weeaboo.lua2.lib;

import nl.weeaboo.lua2.LuaRunState;
import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.vm.LuaError;
import nl.weeaboo.lua2.vm.LuaTable;
import nl.weeaboo.lua2.vm.LuaValue;
import nl.weeaboo.lua2.vm.Varargs;

@LuaSerializable
public abstract class LuaLibrary extends VarArgFunction {

	private static final long serialVersionUID = -46607383638064637L;

	public LuaLibrary() {
	}

	@Override
	public Varargs invoke(Varargs args) {
		throw new LuaError("Invalid function id: " + opcode);
	}

	protected abstract LuaLibrary newInstance();

	private void init(int opcode, String name, LuaValue env) {
		this.opcode = opcode;
		this.name = name;
		this.env = env;
	}

    protected Varargs initLibrary(String libName, String[] names, int opcodeOffset) {
		LuaTable t = new LuaTable();
		try {
            initLibrary(t, names, opcodeOffset);
		} catch (Exception e) {
            throw new LuaError("Bind failed: " + libName, e);
		}
        env.set(libName, t);
        LuaRunState.getCurrent().setIsLoaded(libName, t);
		return t;
	}

    protected void initLibrary(LuaTable t, String[] names, int opcodeOffset) {
        for (int i = 0, n = names.length; i < n; i++) {
            LuaLibrary f = newInstance();
            f.init(opcodeOffset + i, names[i], env);
            t.set(names[i], f);
        }
    }

}
