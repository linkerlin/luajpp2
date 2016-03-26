package nl.weeaboo.lua2.link;

import java.io.Serializable;

import org.luaj.vm2.LuaClosure;
import org.luaj.vm2.Varargs;

import nl.weeaboo.lua2.IDestructible;
import nl.weeaboo.lua2.LuaException;

public interface ILuaLink extends Serializable, IDestructible {

    /**
     * @return {@code true} if the thread was updated.
     */
    boolean update() throws LuaException;

    /**
     * @return {@code true} if the thread has finished running.
     */
    boolean isFinished();

    /**
     * While {@code wait > 0}, each call to {@link #update()} decreases the wait by one instead of running the
     * thread. If {@code wait < 0}, the wait is infinite.
     */
    void setWait(int w);

    /**
     * Increases the wait time by {@code dt}. If the wait is already infinite, doesn't do anything.
     *
     * @param dt The amount to increase the wait by. May not be negative.
     * @return {@code true} if the wait time changed as a result of calling this method.
     */
    boolean addWait(int dt);

    /**
     * Decreases the wait time by {@code dt}. If the wait is already infinite, doesn't do anything.
     *
     * @param dt The amount to decrease the wait by. May not be negative.
     * @return {@code true} if the wait time changed as a result of calling this method.
     */
    boolean decreaseWait(int dt);

    /**
     * Sets the wait time to {@code w}, but only if that would increase the wait time.
     */
    void setMinimumWait(int w);

    /**
     * Clears the current callstack, then pushes the given funtion.
     */
    void jump(LuaClosure func, Varargs args);

}
