package nl.weeaboo.lua2.link;

import java.io.Serializable;

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

}
