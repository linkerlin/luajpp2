package nl.weeaboo.lua2.stdlib;

import static nl.weeaboo.lua2.vm.LuaConstants.NONE;

import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.LuaRunState;
import nl.weeaboo.lua2.compiler.ScriptLoader;
import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.lib.LuaBoundFunction;
import nl.weeaboo.lua2.vm.LuaClosure;
import nl.weeaboo.lua2.vm.LuaThread;
import nl.weeaboo.lua2.vm.Varargs;

@LuaSerializable
public final class ThreadLib extends LuaModule {

    private static final long serialVersionUID = 1L;

    ThreadLib() {
        super("Thread");
    }

    /**
     * Creates a new thread in the default thread group.
     *
     * @param args
     *        <ol>
     *        <li>Lua closure to call in a new thread
     *        <li>Vararg of arguments to pass to the closure
     *        </ol>
     */
    @LuaBoundFunction(luaName = "new")
    public Varargs new_(Varargs args) {
        LuaRunState lrs = LuaRunState.getCurrent();
        LuaClosure func = args.arg1().checkclosure();
        return lrs.newThread(func, args.subargs(2));
    }

    /**
     * Stops executing the current thread.
     *
     * @param args
     *        <ol>
     *        <li>Number of frames to yield. If negative, the wait is infinite.
     *        </ol>
     */
    @LuaBoundFunction
    public Varargs yield(Varargs args) {
        final LuaThread thread = LuaThread.getRunning();

        if (!args.isnil(1)) {
            int w = args.toint(1);
            thread.setSleep(w <= 0 ? w : w - 1);
        }

        return thread.yield(args);
    }

    /**
     * Ends the current call stack frame, then yields the current thread. This is effectively a return,
     * immediately followed by a {@link #yield(Varargs)}.
     *
     * @param args
     *        <ol>
     *        <li>Number of frames to yield. If negative, the wait is infinite.
     *        </ol>
     */
    @LuaBoundFunction
    public Varargs endCall(Varargs args) {
        final LuaThread thread = LuaThread.getRunning();

        if (!args.isnil(1)) {
            int w = args.toint(1);
            thread.setSleep(w <= 0 ? w : w - 1);
        }

        return thread.endCall(args);
    }

    /**
     * Goto a different script file
     *
     * @param args
     *        <ol>
     *        <li>Filename of the script to jump to.
     *        </ol>
     */
    @LuaBoundFunction
    public Varargs jump(Varargs args) {
        Varargs v = ScriptLoader.loadFile(args.checkjstring(1));
        if (v.isnil(1)) {
            throw new LuaException(v.tojstring(2));
        }

        // We can only jump to closures
        LuaClosure closure = v.checkclosure(1);

        LuaThread thread = LuaThread.getRunning();
        thread.jump(closure, NONE);
        return NONE;
    }

}
