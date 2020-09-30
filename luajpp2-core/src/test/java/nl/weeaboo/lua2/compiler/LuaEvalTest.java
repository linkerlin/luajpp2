package nl.weeaboo.lua2.compiler;

import org.junit.Assert;
import org.junit.Test;

import nl.weeaboo.lua2.AbstractLuaTest;
import nl.weeaboo.lua2.vm.LuaConstants;
import nl.weeaboo.lua2.vm.LuaInteger;
import nl.weeaboo.lua2.vm.LuaThread;

public final class LuaEvalTest extends AbstractLuaTest {

    /**
     * The code being eval'd should have access to local variables.
     */
    @Test
    public void testAccessLocals() {
        LuaThread thread = loadScript("compiler/eval-locals.lua");
        thread.resume(LuaConstants.NONE);

        // Read locals
        Assert.assertEquals(LuaInteger.valueOf(4), LuaEval.eval(thread, "y"));
        // Read upvalues
        Assert.assertEquals(LuaInteger.valueOf(1), LuaEval.eval(thread, "x"));

        // Write locals
        Assert.assertEquals(LuaInteger.valueOf(20), LuaEval.eval(thread, "y = 20\nreturn y"));
        // Write upvalues
        Assert.assertEquals(LuaInteger.valueOf(10), LuaEval.eval(thread, "x = 10\nreturn x"));
    }

}
