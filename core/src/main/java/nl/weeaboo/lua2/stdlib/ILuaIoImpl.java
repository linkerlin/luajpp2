package nl.weeaboo.lua2.stdlib;

import java.io.IOException;
import java.io.Serializable;

import nl.weeaboo.lua2.lib.LuaFileHandle;

interface ILuaIoImpl extends Serializable {

    /**
     * Start a new process and return a file for input or output
     *
     * @param prog the program to execute
     * @param mode "r" to read, "w" to write
     * @return File to read to or write from
     * @throws IOException if an i/o exception occurs
     */
    LuaFileHandle openProgram(String prog, String mode) throws IOException;

    /**
     * Open a temporary file.
     *
     * @return File object if successful
     * @throws IOException if could not be opened
     */
    LuaFileHandle createTempFile() throws IOException;

    /**
     * Open a file in a particular mode.
     *
     * @return File object if successful
     * @throws IOException if could not be opened
     */
    LuaFileHandle openFile(String filename, FileOpenMode mode) throws IOException;

    /**
     * Deletes a file
     */
    void deleteFile(String filename) throws IOException;

    /**
     * Renames a file.
     */
    void renameFile(String oldFilename, String newFilename) throws IOException;

}
