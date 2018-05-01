package nl.weeaboo.lua2.stdlib;

import java.io.IOException;

import nl.weeaboo.lua2.io.LuaSerializable;

@LuaSerializable
enum FileBufferMode {

    NO("no"),
    LINE("line"),
    FULL("full");

    private final String luaMode;

    private FileBufferMode(String luaMode) {
        this.luaMode = luaMode;
    }

    public static FileBufferMode fromString(String mode) throws IOException {
        for (FileBufferMode value : values()) {
            if (value.luaMode.equals(mode)) {
                return value;
            }
        }
        throw new IOException("Invalid file buffer mode: " + mode);
    }

}
