package nl.weeaboo.lua2.lib;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import nl.weeaboo.lua2.AbstractLuaTest;
import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.LuaRunState;

public class StdStreamsTest extends AbstractLuaTest {

    @Rule
    public StdStreamsRedirect redir = new StdStreamsRedirect();

    @Before
    public void before() throws LuaException {
        LuaRunState.newInstance();
    }

    @Test
    public void accessStdOut() throws LuaException, IOException {
        redir.writeToStdIn("stdin\n");

        loadScript("lib/io/stdfile.lua");

        runToCompletion();

        // Check that the Lua script wrote the expected values to our redirected streams
        String stdOut = redir.readStdOut();
        String stdErr = redir.readStdErr();
        assertContains(stdOut, "stdout");
        assertContains(stdErr, "input=stdin");
    }

    private void assertContains(String text, String pattern) {
        Assert.assertTrue("Text: " + text, text.contains(pattern));
    }

}
