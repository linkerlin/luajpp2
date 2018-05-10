package nl.weeaboo.lua2.test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import nl.weeaboo.lua2.AbstractLuaTest;
import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.LuaRunState;
import nl.weeaboo.lua2.lib.ClassLoaderResourceFinder;
import nl.weeaboo.lua2.lib.LuaResource;
import nl.weeaboo.lua2.stdlib.StandardLibrary;

public final class Lua51TestSuiteTest extends AbstractLuaTest {

    @Rule
    public Timeout timeout = new Timeout(600, TimeUnit.SECONDS);

    @SuppressWarnings("serial")
    @Override
    public void initLuaRunState() throws LuaException {
        StandardLibrary stdlib = new StandardLibrary();
        stdlib.setAllowUnsafeIO(true); // Unsafe I/O functions are required for this test
        luaRunState = LuaRunState.create(stdlib);
        luaRunState.setLuaPath("lua51tests/?.lua");
        luaRunState.setResourceFinder(new ClassLoaderResourceFinder() {
            @Override
            public LuaResource findResource(final String filename) {
                LuaResource resource = super.findResource(filename);
                if (resource == null) {
                    // Load from file if not found as a class resource
                    resource = new LuaResource(filename) {
                        @Override
                        public InputStream open() throws IOException {
                            return new FileInputStream(filename);
                        }
                    };
                }
                return resource;
            }
        });
    }

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

    @Ignore("Doesn't work yet")
    @Test
    public void testDb() {
        runScript("db.lua");
    }

    @Ignore("Doesn't work yet")
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
        runToCompletion();
    }

}
