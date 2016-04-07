package nl.weeaboo.lua2.io;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObjectSerializer extends ObjectOutputStream {

    public enum ErrorLevel {
        NONE, WARNING, ERROR;
    }

    private static final Logger LOG = LoggerFactory.getLogger(ObjectSerializer.class);

    private final Environment env; // Null if empty or not used
    private final Set<String> validPackages = new HashSet<String>();
    private final Set<Class<?>> validClasses = new HashSet<Class<?>>();
    private final ExecutorService executor;

    private final List<String> errors = new ArrayList<String>();
    private final List<String> warnings = new ArrayList<String>();
    private final Map<Class<?>, Stats> classCounter = new IdentityHashMap<Class<?>, Stats>();

    private ErrorLevel packageErrorLevel = ErrorLevel.ERROR;
    private boolean collectStats = true;
    private boolean checkTypes;

    protected ObjectSerializer(OutputStream out, Environment e) throws IOException {
        super(out);

        env = (e.size() != 0 ? e : null);
        executor = new DelayedIoExecutor("LuaObjectSerializer");

        resetValidClasses();

        onPackageLimitChanged();
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            executor.shutdown();
        }
    }

    private static String toErrorString(String[] errors) {
        StringBuilder sb = new StringBuilder();
        sb.append(errors.length).append(" error(s) occurred while writing objects:");

        int t = 1;
        for (String err : errors) {
            sb.append('\n');
            sb.append(t);
            sb.append(": ");
            sb.append(err);
            t++;
        }
        return sb.toString();
    }

    /**
     * Clears the list and returns its former contents.
     */
    private static String[] consume(Collection<String> list) {
        String[] result = list.toArray(new String[list.size()]);
        list.clear();
        return result;
    }

    /**
     * @return An array containing all warnings encountered during serialization.
     * @throws IOException if any fatal errors were encountered.
     */
    public String[] checkErrors() throws IOException {
        String[] errors = consume(this.errors);
        String[] warnings = consume(this.warnings);

        if (errors.length > 0) {
            throw new RuntimeException(toErrorString(errors));
        }

        if (collectStats) {
            Entry<Class<?>, Stats> entries[] = classCounter.entrySet().toArray(new Entry[0]);
            Arrays.sort(entries, new Comparator<Entry<Class<?>, Stats>>() {
                @Override
                public int compare(Entry<Class<?>, Stats> e1, Entry<Class<?>, Stats> e2) {
                    return -e1.getValue().compareTo(e2.getValue());
                }
            });
            for (Entry<Class<?>, Stats> entry : entries) {
                LOG.debug("[stats] {}: {}", entry.getKey().getName(), entry.getValue());
            }
        }

        return warnings;
    }

    @Override
    protected Object replaceObject(Object obj) {
        Class<?> clazz = obj.getClass();

        // Updating stats
        if (collectStats) {
            Stats stats = classCounter.get(clazz);
            if (stats == null) {
                stats = new Stats();
                classCounter.put(clazz, stats);
            }
            stats.count++;
        }

        // Environment
        if (env != null) {
            String id = env.getId(obj);
            if (id != null) {
                return new RefEnvironment(id);
            }
        }

        if (checkTypes) {
            // Whitelisted types
            if (clazz.getAnnotation(LuaSerializable.class) != null) {
                return obj; // Whitelist types with the LuaSerializable annotation
            } else if (clazz.isArray()) {
                return obj; // Whitelist array types
            } else if (clazz.isEnum()) {
                return obj; // Whitelist enum types
            }

            if (packageErrorLevel != ErrorLevel.NONE) {
                if (!isValidClass(clazz)) {

                    String message = "Class outside valid packages: " + clazz.getName() + " :: " + obj;
                    if (packageErrorLevel == ErrorLevel.ERROR) {
                        errors.add(message);
                        return null; // Don't serialize object in case of error
                    } else if (packageErrorLevel == ErrorLevel.WARNING) {
                        warnings.add(message);
                    }
                }
            }
        }

        return obj;
    }

    private boolean isValidClass(Class<?> clazz) {
        if (validClasses.contains(clazz)) {
            return true;
        }

        // Check if this package is a valid package (or a sub-package of a valid package)
        String packageName = clazz.getPackage().getName();
        if (validPackages.contains(packageName)) {
            return true;
        }

        return false;
    }

    public void writeObjectOnNewThread(final Object obj) throws IOException {
        Future<?> future = executor.submit(createAsyncWriteTask(obj));
        try {
            future.get();
        } catch (InterruptedException e) {
            throw new IOException("Async write interrupted: " + e);
        } catch (ExecutionException e) {
            throw new IOException("Error during async write", e.getCause());
        }
    }

    protected Callable<Void> createAsyncWriteTask(final Object obj) {
        return new Callable<Void>() {
            @Override
            public Void call() throws IOException {
                writeObject(obj);
                return null;
            }
        };
    }

    private void resetValidPackages() {
        validPackages.clear();

        validPackages.add("java.util");
    }

    private void resetValidClasses() {
        validClasses.clear();

        Collections.<Class<?>> addAll(validClasses,
            Boolean.class, Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class,
            String.class, Class.class, Random.class, BitSet.class
        );
    }

    private void onPackageLimitChanged() {
        checkTypes = (packageErrorLevel != ErrorLevel.NONE);

        updateEnableReplace();
    }

    private void updateEnableReplace() {
        boolean replace = (env != null || checkTypes || collectStats);
        try {
            enableReplaceObject(replace);
        } catch (SecurityException se) {
            LOG.error("Error calling 'enableReplaceObject'", se);
        }
    }

    public ErrorLevel getPackageErrorLevel() {
        return packageErrorLevel;
    }

    public void setPackageErrorLevel(ErrorLevel el) {
        if (packageErrorLevel != el) {
            packageErrorLevel = el;

            onPackageLimitChanged();
        }
    }

    public void setAllowedPackages(Collection<String> packages) {
        resetValidPackages();

        validPackages.addAll(packages);
    }

    public void setAllowedClasses(Collection<Class<?>> classes) {
        resetValidClasses();

        validClasses.addAll(classes);
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

    // Inner Classes
    private static class Stats implements Comparable<Stats> {

        public int count;

        @Override
        public int compareTo(Stats s) {
            return (count < s.count ? -1 : (count == s.count ? 0 : 1));
        }

        @Override
        public String toString() {
            return String.format("%d", count);
        }

    }

}
