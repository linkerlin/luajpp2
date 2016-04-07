package nl.weeaboo.lua2.io;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

final class DelayedIoExecutor extends ThreadPoolExecutor {

    public DelayedIoExecutor(String name) {
        super(0, 1, 1000L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(),
                new DelayedThreadFactory(name));
    }

    private static final class DelayedThreadFactory implements ThreadFactory {

        private final String name;
        private final AtomicInteger counter = new AtomicInteger();

        public DelayedThreadFactory(String name) {
            this.name = name;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, name + "-" + counter.incrementAndGet());
            return thread;
        }

    }

}
