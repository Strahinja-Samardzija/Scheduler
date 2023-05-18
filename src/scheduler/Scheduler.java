package scheduler;

import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;

import thread_pool_api.FunctionalRunnable;
import thread_pool_api.FunctionalTaskSAM;
import thread_pool_api.RequestToken;
import thread_pool_api.ThreadPool;

public class Scheduler extends Thread {
    final int maxTasks;
    final Semaphore freeSpots;

    // TCB and tcb are acronyms for TaskControlBlock
    private BlockingQueue<TaskControlBlockSlim> tcbs = new LinkedBlockingQueue<>();

    private HashMap<RequestToken, TaskControlBlockSlim> cancelled = new HashMap<>();

    public static BlockingQueue<TaskControlBlockSlim> orderByPriority() {
        return new PriorityBlockingQueue<>(11,
                (t1, t2) -> t1.getPriority() - t2.getPriority());
    }

    public static BlockingQueue<TaskControlBlockSlim> order() {
        return new PriorityBlockingQueue<>(11,
                (t1, t2) -> t1.getPriority() - t2.getPriority());
    }

    private static Scheduler instance = null;

    private Scheduler(int maxTasks) {
        this.setDaemon(true);
        this.maxTasks = maxTasks;
        this.freeSpots = new Semaphore(maxTasks);
    }

    public static Scheduler initialize(int maxTasks) {
        if (instance == null) {
            instance = new Scheduler(maxTasks);
            instance.start();
        } else
            throw new Error("Many initializations instead of one.");
        return instance;
    }

    public static Scheduler initialize(int maxTasks, BlockingQueue<TaskControlBlockSlim> orderQueue) {
        if (instance == null) {
            instance = new Scheduler(maxTasks);
            ThreadPool.getInstance().setMaxThreads(maxTasks);
            instance.tcbs = orderQueue;
            instance.start();
        } else
            throw new Error("Many initializations instead of one.");
        return instance;
    }

    public static Scheduler getInstance() {
        if (instance == null) {
            instance = new Scheduler(3);
            instance.start();
        }
        return instance;
    }

    public <T> RequestToken<T> requestFromPool(FunctionalTaskSAM<T> task) {
        return createTCB(task, true, 0);
    }

    public <T> RequestToken<T> requestFromPool(FunctionalTaskSAM<T> task, int priority) {
        return createTCB(task, true, priority);
    }

    public <T> RequestToken<T> requestFromNewThread(FunctionalTaskSAM<T> task) {
        return createTCB(task, false, 0);
    }

    public <T> RequestToken<T> requestFromNewThread(FunctionalTaskSAM<T> task, int priority) {
        return createTCB(task, false, priority);
    }

    private <T> RequestToken<T> createTCB(FunctionalTaskSAM<T> task, boolean poolTask, int priority) {
        RequestToken<T> token = new RequestToken<>(this);
        final var runnable = new FunctionalRunnable<T>(token, task);
        TaskControlBlockSlim tcb = new TaskControlBlockSlim(token, runnable, poolTask, freeSpots, priority);
        try {
            tcbs.put(tcb);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
        return token;
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                var tcb = tcbs.take();
                synchronized (tcb.getToken()) {
                    if (tcb.getToken().getDontStart()) {
                        cancelled.put(tcb.getToken(), tcb);
                        continue;
                    } else
                        tcb.getToken().start();
                }
                freeSpots.acquire();
                if (tcb.isPoolTask()) {
                    ThreadPool.getInstance().submit(tcb);

                } else {
                    new Thread(tcb).start();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }
    }

    public void restart(RequestToken requestToken) throws InterruptedException {
        synchronized (cancelled) {
            var tcb = cancelled.get(requestToken);
            if (tcb != null) {
                tcbs.put(tcb);
            }
        }
    }

}
