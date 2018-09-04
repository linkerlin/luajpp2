package nl.weeaboo.lua2.stdlib;

import static nl.weeaboo.lua2.vm.LuaNil.NIL;
import static nl.weeaboo.lua2.vm.LuaValue.valueOf;
import static nl.weeaboo.lua2.vm.LuaValue.varargsOf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.lib.LuaBoundFunction;
import nl.weeaboo.lua2.lib.VarArgFunction;
import nl.weeaboo.lua2.vm.LuaBoolean;
import nl.weeaboo.lua2.vm.LuaClosure;
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

    /**
     * Creates a new coroutine from the given Lua function.
     *
     * @param args
     *        <ol>
     *        <li>function
     *        </ol>
     * @return A new coroutine (a thread).
     */
    @LuaBoundFunction
    public Varargs create(Varargs args) {
        final LuaClosure func = args.checkclosure(1);
        final LuaThread running = LuaThread.getRunning();

        LuaThread thread = new LuaThread(running, func);
        thread.setfenv(func.getfenv());
        return thread;
    }

    /**
     * Starts or resumes execution of the given couroutine. When this function is called for the first time,
     * any additional arguments are passed as function parameters to the coroutine's function. Further calls
     * to this function pass additional args as return values of a call to {@code yield} inside the coroutine.
     *
     * @param args
     *        <ol>
     *        <li>coroutine
     *        <li>args...
     *        </ol>
     * @return {@code true, returnValues} if the coroutine ran without errors, where {@code returnValues} are
     *         the values passed to {@code yield} inside the coroutine. If there was an error,
     *         {@code false, errorMessage} is returned instead.
     */
    @LuaBoundFunction
    public Varargs resume(Varargs args) {
        final LuaThread t = args.checkthread(1);

        try {
            Varargs result = t.resume(args.subargs(2));

            return varargsOf(LuaBoolean.TRUE, result);
        } catch (LuaException e) {
            LOG.trace("Unable to resume coroutine: {}", t, e);
            return varargsOf(LuaBoolean.FALSE, e.getMessageObject());
        }
    }

    /**
     * @param args Not used.
     * @return The currently running coroutine, or {@code nil} when called by the main thread.
     */
    @LuaBoundFunction
    public Varargs running(Varargs args) {
        LuaThread running = LuaThread.getRunning();
        if (running.isMainThread()) {
            return NIL;
        }
        return running;
    }

    /**
     * Returns the status of the given coroutine as a string. The possible values are:
     * <ul>
     * <li>"running": The coroutine is currently running, see {@link #running(Varargs)}.
     * <li>"suspended": The coroutine is suspended in a call to {@code yield} or hasn't started yet.
     * <li>"normal": If the coroutine is active but not running (it has resumed another coroutine).
     * <li>"dead": The coroutine has finished, either because the end of its function was reached or because
     * of an error.
     * </ul>
     *
     * @param args
     *        <ol>
     *        <li>coroutine
     *        </ol>
     * @return The status of the coroutine.
     */
    @LuaBoundFunction
    public Varargs status(Varargs args) {
        LuaThread thread = args.checkthread(1);
        return valueOf(getCoroutineStatus(thread.getStatus()));
    }

    /**
     * @return The Lua coroutine status value (a string) corresponding to the internal thread status.
     */
    public static String getCoroutineStatus(LuaThreadStatus threadStatus) {
        switch (threadStatus) {
        case INITIAL:
        case SUSPENDED:
        case END_CALL:
            return "suspended";
        case RUNNING:
            return "running";
        case DEAD:
            return "dead";
        default:
            return "normal";
        }
    }

    /**
     * Suspends execution of the current coroutine. Any additional arguments are passed as return values for
     * the call to {@link #resume(Varargs)} that caused this coroutine to run.
     *
     * @param args
     *        <ol>
     *        <li>args...
     *        </ol>
     * @return Any arguments that were passed to {@link #resume(Varargs)} in the call that resumes execution.
     */
    @LuaBoundFunction
    public Varargs yield(Varargs args) {
        final LuaThread running = LuaThread.getRunning();

        return running.yield(args);
    }

    /**
     * Variant of {@link #create(Varargs)} that creates a coroutine from the given Lua function, then returns
     * a function which calls {@link #resume(Varargs)} on the coroutine every time it's called. Arguments
     * passed to the function are forwarded to {@link #resume(Varargs)} and return values from
     * {@link #resume(Varargs)} are passed through as return values of the function (except the first boolen).
     *
     * @param args
     *        <ol>
     *        <li>args...
     *        </ol>
     * @return Return values from {@link #yield}.
     */
    @LuaBoundFunction
    public Varargs wrap(Varargs args) {
        final LuaClosure func = args.checkclosure(1);
        final LuaThread running = LuaThread.getRunning();
        final LuaThread thread = new LuaThread(running, func);
        thread.setSleep(-1);
        thread.setfenv(func.getfenv());

        return new WrappedFunction(thread);
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
            return thread.resume(args);
        }
    }

}
