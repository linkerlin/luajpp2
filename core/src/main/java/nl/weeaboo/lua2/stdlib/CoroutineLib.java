package nl.weeaboo.lua2.stdlib;

import static nl.weeaboo.lua2.vm.LuaNil.NIL;
import static nl.weeaboo.lua2.vm.LuaValue.valueOf;
import static nl.weeaboo.lua2.vm.LuaValue.varargsOf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.lib.VarArgFunction;
import nl.weeaboo.lua2.lib2.LuaBoundFunction;
import nl.weeaboo.lua2.vm.LuaBoolean;
import nl.weeaboo.lua2.vm.LuaClosure;
import nl.weeaboo.lua2.vm.LuaError;
import nl.weeaboo.lua2.vm.LuaThread;
import nl.weeaboo.lua2.vm.LuaThreadStatus;
import nl.weeaboo.lua2.vm.Varargs;

@LuaSerializable
public final class CoroutineLib extends LuaModule {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(CoroutineLib.class);

    CoroutineLib() {
        super("coroutine");
    }

    @LuaBoundFunction
    public Varargs create(Varargs args) {
        final LuaClosure func = args.checkclosure(1);
        final LuaThread running = LuaThread.getRunning();

        LuaThread thread = new LuaThread(running, func);
        thread.setfenv(func.getfenv());
        return thread;
    }

    @LuaBoundFunction
    public Varargs resume(Varargs args) {
        final LuaThread t = args.checkthread(1);

        try {
            Varargs result = resume(t, args.subargs(2));

            return varargsOf(LuaBoolean.TRUE, result);
        } catch (LuaError e) {
            LOG.trace("Unable to resume coroutine: {}", t, e);
            return varargsOf(LuaBoolean.FALSE, valueOf(e.getMessage()));
        }
    }

    /**
     * @param args Not used.
     */
    @LuaBoundFunction
    public Varargs running(Varargs args) {
        LuaThread running = LuaThread.getRunning();
        if (running.isMainThread()) {
            return NIL;
        }
        return running;
    }

    @LuaBoundFunction
    public Varargs status(Varargs args) {
        LuaThread thread = args.checkthread(1);
        return valueOf(getCoroutineStatus(thread));
    }

    private static String getCoroutineStatus(LuaThread thread) {
        switch (thread.getStatus()) {
        case INITIAL:
        case SUSPENDED:
            return "suspended";
        case RUNNING:
            return "running";
        case DEAD:
            return "dead";
        default:
            return "normal";
        }
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
        thread.setSleep(-1);
        thread.setfenv(func.getfenv());

        return new WrappedFunction(thread);
    }

    private static Varargs resume(LuaThread thread, Varargs args) {
        LuaThreadStatus status = thread.getStatus();

        if (status == LuaThreadStatus.INITIAL) {
            thread.setArgs(args);
            thread.setSleep(0);
        } else if (status == LuaThreadStatus.SUSPENDED) {
            // Resume coroutine
            // Place args on the thread's stack as though it was returned from the call that yielded
            thread.setReturnedValues(args);
        } else {
            throw new LuaError("Unable to resume coroutine: " + thread + ", status=" + getCoroutineStatus(thread));
        }

        return thread.resume(-1);
    }

    @LuaSerializable
    private static final class WrappedFunction extends VarArgFunction {

        private static final long serialVersionUID = 2L;

        public WrappedFunction(LuaThread thread) {
            setfenv(thread);
            name = "wrapped";
        }

        @Override
        public Varargs invoke(Varargs args) {
            LuaThread thread = getfenv().checkthread();
            return resume(thread, args);
        }
    }

}
