package thread_pool_api;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Dispatcher extends Thread {

    // this long because the tasks are usually long
    private static final long TIMEOUT = 500;

    private ThreadPool threadPool;

    LinkedBlockingQueue<Runnable> tasks = new LinkedBlockingQueue<>();
    LinkedBlockingQueue<WorkerThread> threads = new LinkedBlockingQueue<>();

    public Dispatcher(ThreadPool threadPool) {
        this.setDaemon(true);
        this.threadPool = threadPool;
    }

    public void run() {
        spawnThread();
        while (!Thread.interrupted()) {
            try {
                if (threads.size() > 0.4 * threadPool.getMaxThreads()) {
                    free();
                }
                Runnable task = tasks.take();
                WorkerThread thread = threads.poll(10, TimeUnit.MICROSECONDS);
                if (thread != null) {
                    thread.signal(task);
                    continue;
                }
                // if minThreads is not reached a thread spawns sooner
                if (threadPool.getCurrentThreadCount() < threadPool.getMinThreads()) {
                    spawnThread().signal(task);
                } else {
                    thread = threads.poll(TIMEOUT, TimeUnit.MILLISECONDS);
                    if (thread != null) {
                        thread.signal(task);
                        continue;
                    }
                    if (threadPool.getCurrentThreadCount() < threadPool.getMaxThreads()) {
                        spawnThread().signal(task);
                    } else {
                        threads.take().signal(task);
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void free() throws InterruptedException {
        int numberToKill = threadPool.getCurrentThreadCount()
                - (int) Math.max(threadPool.getMinThreads(), 0.1 * threadPool.getMaxThreads());
        for (int i = 0; i < numberToKill; i++) {
            threads.take().interrupt();
            threadPool.removeFromPool();
        }
    }

    private WorkerThread spawnThread() {
        WorkerThread newThread = new WorkerThread(this);
        newThread.start();
        try {
            threads.put(newThread);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
        threadPool.addToPool();
        return newThread;
    }
}
