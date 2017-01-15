package nl.weeaboo.lua2.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class LuaSerializer {

    private static final ThreadLocal<LuaSerializer> CURRENT = new ThreadLocal<LuaSerializer>();
    private final Environment env;

    private final List<Object> writeDelayed = new ArrayList<Object>();
    private final List<DelayedReader> readDelayed = new ArrayList<DelayedReader>();

    public LuaSerializer() {
        env = new Environment();
    }

    /**
     * @return The previously active {@link LuaSerializer}
     */
    protected LuaSerializer makeCurrent() {
        LuaSerializer previous = CURRENT.get();
        CURRENT.set(this);
        return previous;
    }

    public static LuaSerializer getCurrent() {
        return CURRENT.get();
    }

    public void writeDelayed(Object obj) {
        writeDelayed.add(obj);
    }

    public void readDelayed(DelayedReader reader) {
        readDelayed.add(reader);
    }

    public ObjectSerializer openSerializer(OutputStream out) throws IOException {
        final LuaSerializer previous = makeCurrent();

        ObjectSerializer oout = new ObjectSerializer(out, env) {

            int delayedWritten = 0;

            private void writeDelayed() throws IOException {
                while (delayedWritten < writeDelayed.size()) {
                    writeObject(writeDelayed.get(delayedWritten++));
                }
            }

            @Override
            protected Callable<Void> createAsyncWriteTask(Object obj) {
                final Callable<Void> inner = super.createAsyncWriteTask(obj);

                return new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        makeCurrent();
                        inner.call();
                        writeDelayed();
                        return null;
                    }
                };
            }

            @Override
            public void close() throws IOException {
                try {
                    writeDelayed();
                } finally {
                    try {
                        super.close();
                    } finally {
                        writeDelayed.clear();
                        CURRENT.set(previous);
                    }
                }

                checkErrors();
            }
        };
        return oout;
    }

    public ObjectDeserializer openDeserializer(InputStream in) throws IOException {
        final LuaSerializer previous = makeCurrent();

        ObjectDeserializer oin = new ObjectDeserializer(in, env) {

            int delayedRead = 0;

            private void readDelayed() throws IOException {
                try {
                    while (delayedRead < readDelayed.size()) {
                        DelayedReader reader = readDelayed.get(delayedRead++);
                        reader.onRead(readObject());
                    }
                } catch (ClassNotFoundException cnfe) {
                    throw new IOException(cnfe.toString());
                }
            }

            @Override
            protected Callable<Object> createAsyncReadTask() {
                final Callable<Object> inner = super.createAsyncReadTask();

                return new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        makeCurrent();
                        Object result = inner.call();
                        readDelayed();
                        return result;
                    }
                };
            }

            @Override
            public void close() throws IOException {
                try {
                    readDelayed();
                } finally {
                    try {
                        super.close();
                    } finally {
                        readDelayed.clear();
                        CURRENT.set(previous);
                    }
                }
            }
        };
        return oin;
    }

    public Environment getEnvironment() {
        return env;
    }

}
