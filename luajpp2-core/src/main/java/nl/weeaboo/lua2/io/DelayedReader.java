package nl.weeaboo.lua2.io;

/**
 * @see LuaSerializer#readDelayed(DelayedReader)
 */
public interface DelayedReader {

    /**
     * This method is called when a 'delayed' object is read.
     *
     * @see LuaSerializer#writeDelayed(Object)
     */
    void onRead(Object obj);

}
