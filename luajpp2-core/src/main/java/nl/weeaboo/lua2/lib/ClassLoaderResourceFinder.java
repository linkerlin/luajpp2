package nl.weeaboo.lua2.lib;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;

import javax.annotation.Nullable;

import nl.weeaboo.lua2.io.LuaSerializable;

@LuaSerializable
public class ClassLoaderResourceFinder implements ILuaResourceFinder, Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public @Nullable LuaResource findResource(String filename) {
        if (!filename.startsWith("/")) {
            filename = "/" + filename;
        }

        URL url = getClass().getResource(filename);
        if (url == null) {
            return null;
        }
        return new ClassLoaderResource(filename);
    }

    private static class ClassLoaderResource extends LuaResource {

        public ClassLoaderResource(String canonicalName) {
            super(canonicalName);
        }

        @Override
        public InputStream open() throws IOException {
            InputStream in = getClass().getResourceAsStream(getCanonicalName());
            if (in == null) {
                throw new FileNotFoundException(getCanonicalName());
            }
            return in;
        }

    }

}
