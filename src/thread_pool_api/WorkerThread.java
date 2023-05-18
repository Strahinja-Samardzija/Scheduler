package thread_pool_api;

import java.util.concurrent.*;

class WorkerThread extends Thread {
    private Dispatcher dispatcher;
    private LinkedBlockingQueue<Runnable> signal = new LinkedBlockingQueue<>();

    public WorkerThread(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                Runnable currentTask = signal.take();
                currentTask.run();
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
            waitNext();
        }
    }

    public void signal(Runnable task) {
        try {
            signal.put(task);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    private void waitNext(){
        try {
            dispatcher.threads.put(this);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }
}
