package nl.weeaboo.lua2.stdlib;

import static nl.weeaboo.lua2.vm.LuaConstants.EMPTYSTRING;
import static nl.weeaboo.lua2.vm.LuaConstants.NONE;
import static nl.weeaboo.lua2.vm.LuaNil.NIL;
import static nl.weeaboo.lua2.vm.LuaValue.valueOf;

import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.lib2.LuaBoundFunction;
import nl.weeaboo.lua2.vm.LuaString;
import nl.weeaboo.lua2.vm.LuaTable;
import nl.weeaboo.lua2.vm.LuaValue;
import nl.weeaboo.lua2.vm.Varargs;

@LuaSerializable
public final class TableLib extends LuaModule {

    private static final long serialVersionUID = 1L;

    public TableLib() {
        super("table");
    }

    /**
     * "getn" (table) -> number
     */
    @LuaBoundFunction
    public Varargs getn(Varargs args) {
        return args.checktable(1).getn();
    }

    /**
     * "maxn" (table) -> number
     */
    @LuaBoundFunction
    public Varargs maxn(Varargs args) {
        return valueOf(args.checktable(1).maxn());
    }

    /**
     * "remove" (table [, pos]) -> removed-ele
     */
    @LuaBoundFunction
    public Varargs remove(Varargs args) {
        LuaTable table = args.checktable(1);
        int pos = args.narg() > 1 ? args.checkint(2) : 0;
        return table.remove(pos);
    }

    /**
     * "concat" (table [, sep [, i [, j]]]) -> string
     */
    @LuaBoundFunction
    public Varargs concat(Varargs args) {
        LuaTable table = args.checktable(1);
        LuaString sep = args.optstring(2, EMPTYSTRING);
        int firstIndex = args.optint(3, 1);
        int lastIndex = args.isvalue(4) ? args.checkint(4) : table.length();
        return table.concat(sep, firstIndex, lastIndex);
    }

    /**
     * "insert" (table, [pos,] value) -> prev-ele
     */
    @LuaBoundFunction
    public Varargs insert(Varargs args) {
        final LuaTable table = args.checktable(1);
        final int pos = args.narg() > 2 ? args.checkint(2) : 0;
        final LuaValue value = args.arg(args.narg() > 2 ? 3 : 2);
        table.insert(pos, value);
        return NONE;
    }

    /**
     * "sort" (table [, comp]) -> void
     */
    @LuaBoundFunction
    public Varargs sort(Varargs args) {
        LuaTable table = args.checktable(1);
        LuaValue compare = (args.isnoneornil(2) ? NIL : args.checkfunction(2));
        table.sort(compare);
        return NONE;
    }

    /**
     * "foreach" (table, func) -> void
     */
    @LuaBoundFunction
    public Varargs foreach(Varargs args) {
        return args.checktable(1).foreach(args.checkfunction(2));
    }

    /**
     * "foreachi" (table, func) -> void
     */
    @LuaBoundFunction
    public Varargs foreachi(Varargs args) {
        return args.checktable(1).foreachi(args.checkfunction(2));
    }

}
