package thread_pool_api;

public final class ThreadPool {
    private int maxThreads = 300;

    public int getMaxThreads() {
        return maxThreads;
    }

    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    private int minThreads = 3;

    public int getMinThreads() {
        return minThreads;
    }
    
    public void setMinThreads(int minThreads) {
        this.minThreads = minThreads;
    }
    
    private static ThreadPool instance = null;

    public static ThreadPool getInstance() {
        if (instance == null) {
            instance = new ThreadPool();
            instance.dispatcher.start();
        }
        return instance;
    }

    private final Dispatcher dispatcher = new Dispatcher(this);
    private int currentThreadCount = 0;
    
    public int getCurrentThreadCount() {
        return currentThreadCount;
    }
    
    void addToPool() {
        currentThreadCount++;
    }

    void removeFromPool() {
        if (currentThreadCount > 0)
            currentThreadCount--;
    }
    
    public <T> RequestToken<T> request(FunctionalTaskSAM<T> task) {
        RequestToken<T> token = new RequestToken<>(null);
        final var runnable = new FunctionalRunnable<T>(token, task);
        try {
            dispatcher.tasks.put(runnable);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
        return token;
    }

    public void submit(Runnable task) {
        try {
            dispatcher.tasks.put(task);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }
}
