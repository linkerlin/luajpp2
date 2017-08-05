package nl.weeaboo.lua2.stdlib;

import static nl.weeaboo.lua2.vm.LuaValue.valueOf;

import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.lib.VarArgFunction;
import nl.weeaboo.lua2.lib2.LuaBoundFunction;
import nl.weeaboo.lua2.vm.LuaClosure;
import nl.weeaboo.lua2.vm.LuaError;
import nl.weeaboo.lua2.vm.LuaThread;
import nl.weeaboo.lua2.vm.Varargs;

@LuaSerializable
public final class CoroutineLib extends LuaModule {

    private static final long serialVersionUID = 1L;

    CoroutineLib() {
        super("coroutine");
    }

    @LuaBoundFunction
    public Varargs create(Varargs args) {
        final LuaClosure func = args.checkclosure(1);
        final LuaThread running = LuaThread.getRunning();
        return new LuaThread(running, func);
    }

    @LuaBoundFunction
    public Varargs resume(Varargs args) {
        final LuaThread t = args.checkthread(1);
        return t.resume(-1);
    }

    /**
     * @param args Not used.
     */
    @LuaBoundFunction
    public Varargs running(Varargs args) {
        return LuaThread.getRunning();
    }

    @LuaBoundFunction
    public Varargs status(Varargs args) {
        return valueOf(args.checkthread(1).getStatus());
    }

    @LuaBoundFunction
    public Varargs yield(Varargs args) {
        final LuaThread running = LuaThread.getRunning();
        return running.yield(args);
    }

    @LuaBoundFunction
    public Varargs wrap(Varargs args) {
        final LuaClosure func = args.checkclosure(1);
        final LuaThread running = LuaThread.getRunning();
        final LuaThread thread = new LuaThread(running, func);
        thread.setfenv(func.getfenv());

        return new WrappedFunction(thread);
    }

    @LuaSerializable
    private static final class WrappedFunction extends VarArgFunction {

        private static final long serialVersionUID = 1L;

        public WrappedFunction(LuaThread thread) {
            setfenv(thread);
            name = "wrapped";
        }

        @Override
        public Varargs invoke(Varargs args) {
            LuaThread thread = getfenv().checkthread();
            final Varargs result = thread.resume(-1);
            if (thread.isDead()) {
                throw new LuaError(result.arg1().tojstring());
            }

            return result;
        }
    }

}
