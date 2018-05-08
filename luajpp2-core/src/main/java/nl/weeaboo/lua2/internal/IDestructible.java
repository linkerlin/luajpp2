package nl.weeaboo.lua2.internal;

public interface IDestructible {

    /** Destroys the object. */
    void destroy();

    /** Returns {@code true} if the object was destroyed. */
    boolean isDestroyed();

}
