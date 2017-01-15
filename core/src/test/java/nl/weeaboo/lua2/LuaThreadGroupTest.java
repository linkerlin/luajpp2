package nl.weeaboo.lua2;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import nl.weeaboo.lua2.link.LuaFunctionLink;
import nl.weeaboo.lua2.link.LuaLinkStub;

public final class LuaThreadGroupTest {

    private LuaThreadGroup threadGroup;

    @Before
    public void before() {
        LuaRunState lrs = new LuaRunState();

        threadGroup = new LuaThreadGroup(lrs);
    }

    @Test
    public void newThread() {
        assertThreadCount(0);

        // Create a new thread
        LuaFunctionLink thread = threadGroup.newThread("test");
        assertThreadCount(1);

        // Destroying a thread removes it from the thread group
        thread.destroy();
        assertThreadCount(0);
    }

    /**
     * Test suspend/resume behavior of the thread group.
     */
    @Test
    public void suspendResume() throws LuaException {
        LuaLinkStub link = new LuaLinkStub();
        threadGroup.add(link);

        threadGroup.suspend();
        Assert.assertEquals(true, threadGroup.isSuspended());

        // Calling update while suspended doesn't update the threads in the group
        Assert.assertEquals(false, threadGroup.update());
        assertCalled(link, 0);

        threadGroup.resume();
        Assert.assertEquals(false, threadGroup.isSuspended());

        // No longer suspended, threads are updated
        Assert.assertEquals(true, threadGroup.update());
        assertCalled(link, 1);
    }

    /**
     * Check that finished threads are automatically removed from the thread group.
     */
    @Test
    public void finishedThreadsRemoved() throws LuaException {
        LuaLinkStub link = new LuaLinkStub(1);
        threadGroup.add(link);
        Assert.assertEquals(true, threadGroup.update());
        assertThreadCount(0);
    }

    /**
     * Destroying the thread group also destroys all attached threads.
     */
    @Test
    public void destroyGroup() throws LuaException {
        @SuppressWarnings("serial")
        LuaLinkStub link = new LuaLinkStub() {
            @Override
            public boolean update() throws LuaException {
                threadGroup.destroy();

                return super.update();
            }
        };

        threadGroup.add(link);

        Assert.assertEquals(true, threadGroup.update());
        assertCalled(link, 1);
        Assert.assertEquals(true, link.isDestroyed());
        assertThreadCount(0);
    }

    private void assertCalled(LuaLinkStub link, int expectedUpdateCount) {
        Assert.assertEquals(expectedUpdateCount, link.consumeCallCount());
    }

    private void assertThreadCount(int expected) {
        Assert.assertEquals(expected, threadGroup.getThreads().size());
    }

}
