package nl.weeaboo.lua2.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObjectDeserializer extends ObjectInputStream {

    private static final Logger LOG = LoggerFactory.getLogger(ObjectDeserializer.class);
    private static final int STACK_DEPTH_WARN_LIMIT = 100;

    private final Environment env;
    private final ExecutorService executor;

    private boolean collectStats = true;
    private int maxDepth = 0;

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
    protected Object resolveObject(Object obj) throws IOException {
        Class<?> clazz = obj.getClass();

        if (collectStats) {
            RuntimeException dummy = new RuntimeException();
            StackTraceElement[] stackTrace = dummy.getStackTrace();

            int depth = stackTrace.length;
            maxDepth = Math.max(maxDepth, depth);
            if (depth >= STACK_DEPTH_WARN_LIMIT) {
                LOG.warn("Max stack depth exceeded ({}) while reading {}", depth, clazz.getName(), dummy);
            } else {
                LOG.trace("Stack depth ({}) while reading {}", depth, clazz);
            }
        }

        if (clazz == RefEnvironment.class) {
            return ((RefEnvironment)obj).resolve(env);
        }

        return obj;
    }

    public boolean getCollectStats() {
        return collectStats;
    }

    public void setCollectStats(boolean enable) {
        if (collectStats != enable) {
            collectStats = enable;
            updateEnableReplace();
        }
    }

}
