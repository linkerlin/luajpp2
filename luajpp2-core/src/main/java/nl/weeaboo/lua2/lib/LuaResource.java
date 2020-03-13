package nl.weeaboo.lua2.lib;

import java.io.IOException;
import java.io.InputStream;

/**
 * Represents a named loadable resource.
 */
public abstract class LuaResource {

    private final String canonicalName;

    public LuaResource(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    /**
     * The name of the resource.
     */
    public String getCanonicalName() {
        return canonicalName;
    }

    /**
     * @return An inputstream for reading this resource.
     * @throws IOException If anything goes wrong trying to open the resource.
     */
    public abstract InputStream open() throws IOException;

    @Override
    public String toString() {
        return getCanonicalName();
    }

}