package thread_pool_api;

public class FunctionalRunnable<T> implements Runnable {
    FunctionalTaskSAM<T> task;
    private RequestToken<T> token;

    public FunctionalRunnable(RequestToken<T> token, FunctionalTaskSAM<T> task){
        this.token = token;
        this.task = task;
    }
    
    @Override
    public void run() {
        synchronized(token){
            token.isRunning.set(true);
            token.register();
        }
        token.result = task.calculate();
        synchronized(token){
            token.done = true;
            token.isRunning.set(false);
            token.unregister();
            token.notifyAll();
        }
    }

}
