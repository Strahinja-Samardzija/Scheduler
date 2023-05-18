package thread_pool_api;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import scheduler.Scheduler;

public class RequestToken<T> {

    final Scheduler scheduler;
    static HashMap<Long, RequestToken<?>> lookup = new HashMap<>();
    AtomicBoolean isRunning = new AtomicBoolean(false);
    AtomicBoolean dontStart = new AtomicBoolean(false);
    volatile boolean started = false;
    volatile boolean done = false;
    T result;

    public RequestToken(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public boolean getDontStart() {
        return dontStart.get();
    }

    public synchronized void register() {
        if (!started || done) {
            throw new Error("Register outside a dedicated thread.");
        }
        synchronized (lookup) {
            lookup.put(Thread.currentThread().getId(), this);
        }
    }

    public synchronized void unregister() {
        if (!done) {
            throw new Error("Unregister before done.");
        }
        synchronized (lookup) {
            lookup.remove(Thread.currentThread().getId());
        }
    }

    public synchronized void pauseTask() {
        if (done) return;
        if (!started) {
            dontStart.set(true);
        }
        isRunning.set(false);
    }

    public synchronized void continueTask() {
        if (done) return;
        if (!started){
            dontStart.set(false);
            try {
                scheduler.restart(this);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
            return;
        }
        isRunning.set(true);
        notifyAll();
    }

    public T receive() {
        synchronized (this) {
            while (!done) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
            }
        }
        return result;
    }

    private static long identify() {
        return Thread.currentThread().getId();
    }

    public static void checkForPause() {
        long id = identify();
        RequestToken token = null;
        synchronized (lookup) {
            if (lookup.containsKey(id)) {
                token = lookup.get(id);
            } else {
                // todo not registered
                throw new Error("Check pause on an unregistered token.");
            }
        }
        synchronized (token) {
            while (!token.isRunning.get()) {
                try {
                    token.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public synchronized void start() {
        started = true;
    }
}
