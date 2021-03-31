package nl.weeaboo.lua2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.vm.LuaThread;

/**
 * Default exception handler which logs uncaught exceptions.
 */
@LuaSerializable
public final class DefaultLuaExceptionHandler implements ILuaExceptionHandler {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(DefaultLuaExceptionHandler.class);

    @Override
    public void onScriptException(LuaThread thread, Exception exception) {
        LOG.warn("Error running thread: {}", thread, exception);
    }

}
