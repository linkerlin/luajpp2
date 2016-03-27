package nl.weeaboo.lua2.lib;

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

	protected Varargs initLibrary(String tableName, String[] names, int opcodeOffset) {
		LuaTable t = new LuaTable();
		try {
			for (int i = 0, n = names.length; i < n; i++) {
				LuaLibrary f = newInstance();
				f.init(opcodeOffset + i, names[i], env);
				t.set(names[i], f);
			}
		} catch (Exception e) {
			throw new LuaError("bind failed: " + e);
		}
		env.set(tableName, t);
		return t;
	}

}
