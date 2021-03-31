package nl.weeaboo.lua2.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads Lua objects from a binary stream.
 *
 * @see LuaSerializer
 */
public class ObjectDeserializer extends ObjectInputStream {

    private static final Logger LOG = LoggerFactory.getLogger(ObjectDeserializer.class);

    private final @Nullable Environment env;
    private final ExecutorService executor;

    private boolean collectStats = true;
    private int depthWarnLimit = 100;

    protected ObjectDeserializer(InputStream in, Environment e) throws IOException {
        super(in);

        env = (e.size() > 0 ? e : null);
        executor = new DelayedIoExecutor("LuaObjectDeserializer");

        updateEnableReplace();
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            executor.shutdown();
        }
    }

    private void updateEnableReplace() {
        boolean replace = (env != null || collectStats);

        try {
            enableResolveObject(replace);
        } catch (SecurityException se) {
            LOG.error("Error calling 'enableReplaceObject'", se);
        }
    }

    /**
     * Calls {@link ObjectInputStream#readObject()} on a new thread and returns the result.
     * <p>
     * This method can be used to avoid stack space issues when deserializing large object graphs.
     *
     * @throws IOException If the thread throws an exception, or if the wait for the thread's result is interrupted.
     */
    public Object readObjectOnNewThread() throws IOException {
        Future<Object> future = executor.submit(createAsyncReadTask());
        try {
            return future.get();
        } catch (InterruptedException e) {
            throw new IOException("Async read interrupted: " + e);
        } catch (ExecutionException e) {
            throw new IOException("Error during async read", e.getCause());
        }
    }

    protected Callable<Object> createAsyncReadTask() {
        return new Callable<Object>() {
            @Override
            public Object call() throws IOException, ClassNotFoundException {
                return readObject();
            }
        };
    }

    @Override
    protected Object resolveObject(Object obj) {
        Class<?> clazz = obj.getClass();

        if (collectStats) {
            RuntimeException dummy = new RuntimeException();
            StackTraceElement[] stackTrace = dummy.getStackTrace();

            int depth = stackTrace.length;
            if (depth >= depthWarnLimit) {
                LOG.warn("Max stack depth exceeded ({}/{}) while reading {}", depth, depthWarnLimit,
                        clazz.getName(), dummy);
            } else {
                LOG.trace("Stack depth ({}) while reading {}", depth, clazz);
            }
        }

        if (clazz == RefEnvironment.class) {
            return ((RefEnvironment)obj).resolve(env);
        }

        return obj;
    }

    /**
     * If {@code true}, tracks various statistics during use and warns if certain values (primarily stack
     * depth) become dangerously large.
     */
    public boolean getCollectStats() {
        return collectStats;
    }

    /**
     * @see #getCollectStats()
     */
    public void setCollectStats(boolean enable) {
        if (collectStats != enable) {
            collectStats = enable;
            updateEnableReplace();
        }
    }

    /**
     * The stack depth limit at which to generate warnings.
     * @see #getCollectStats()
     */
    public int getDepthWarnLimit() {
        return depthWarnLimit;
    }

    /**
     * @see #getDepthWarnLimit()
     */
    public void setDepthWarnLimit(int limit) {
        depthWarnLimit = limit;
    }

}
