package nl.weeaboo.lua2.test;

import org.junit.Test;

import nl.weeaboo.lua2.AbstractLuaTest;
import nl.weeaboo.lua2.LuaException;

public final class Lua51TestSuiteTest extends AbstractLuaTest {

    @Test
    public void testApi() {
        runScript("api.lua");
    }

    @Test
    public void testAttrib() {
        runScript("attrib.lua");
    }

    @Test
    public void testBig() {
        runScript("big.lua");
    }

    @Test
    public void testCalls() {
        runScript("calls.lua");
    }

    @Test
    public void testChecktable() {
        runScript("checktable.lua");
    }

    @Test
    public void testClosure() {
        runScript("closure.lua");
    }

    @Test
    public void testCode() {
        runScript("code.lua");
    }

    @Test
    public void testConstructs() {
        runScript("constructs.lua");
    }

    @Test
    public void testDb() {
        runScript("db.lua");
    }

    @Test
    public void testErrors() {
        runScript("errors.lua");
    }

    @Test
    public void testEvents() {
        runScript("events.lua");
    }

    @Test
    public void testFiles() {
        runScript("files.lua");
    }

    @Test
    public void testGc() {
        runScript("gc.lua");
    }

    @Test
    public void testLiterals() {
        runScript("literals.lua");
    }

    @Test
    public void testLocals() {
        runScript("locals.lua");
    }

    @Test
    public void testMain() {
        runScript("main.lua");
    }

    @Test
    public void testMath() {
        runScript("math.lua");
    }

    @Test
    public void testNextvar() {
        runScript("nextvar.lua");
    }

    @Test
    public void testPm() {
        runScript("pm.lua");
    }

    @Test
    public void testSort() {
        runScript("sort.lua");
    }

    @Test
    public void testStrings() {
        runScript("strings.lua");
    }

    @Test
    public void testVararg() {
        runScript("vararg.lua");
    }

    @Test
    public void testVerybig() {
        runScript("verybig.lua");
    }

    private void runScript(String filename) {
        loadScript("lua51tests/" + filename);
        try {
            runToCompletion();
        } catch (LuaException e) {
            throw new AssertionError(e);
        }
    }

}
