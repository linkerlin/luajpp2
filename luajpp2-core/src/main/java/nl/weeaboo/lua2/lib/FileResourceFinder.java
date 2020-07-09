package nl.weeaboo.lua2.lib;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Resource finder which loads resources from the local file system.
 */
public final class FileResourceFinder implements ILuaResourceFinder {

    private final File baseFolder;

    public FileResourceFinder() {
        this(new File("."));
    }

    public FileResourceFinder(File baseFolder) {
        this.baseFolder = baseFolder;
    }

    @Override
    public LuaResource findResource(String filename) {
        return new LuaResource(filename) {
            @Override
            public InputStream open() throws IOException {
                return new FileInputStream(new File(baseFolder, filename));
            }
        };
    }

}
