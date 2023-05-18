package scheduler;

import java.util.concurrent.Semaphore;

import thread_pool_api.RequestToken;

public class TaskControlBlockSlim implements Runnable {
    private Semaphore spot;
    private Runnable inner;
    private boolean poolTask;
    private int priority;
    private RequestToken token;

    public RequestToken getToken() {
        return token;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isPoolTask() {
        return poolTask;
    }

    public TaskControlBlockSlim(RequestToken token, Runnable inner, boolean poolTask, Semaphore freeSpots, int priority) {
        this.token = token;
        this.inner = inner;
        this.poolTask = poolTask;
        this.priority = priority;
        this.spot = freeSpots;
    }

    @Override
    public void run() {
        inner.run();
        spot.release();
    }

}
