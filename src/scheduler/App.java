package scheduler;

import java.util.Random;

import thread_pool_api.RequestToken;

public class App {
    static final Random rnd = new Random();

    public static void main(String[] args) {
        Scheduler scheduler = Scheduler.initialize(3, Scheduler.orderByPriority());
        var pausable = scheduler.<Void>requestFromPool(() -> {
            for (int i = 0; i < 20; i++) {
                RequestToken.checkForPause();
                for (int j = 0; j < 200; j++) {
                    System.out.println("line " + (i * j + j));
                }
            }
            return null;
        }, 0);
        var t1 = scheduler.<String>requestFromPool(App::msgDelay, 0);
        var t2 = scheduler.<String>requestFromNewThread(App::msgDelay, 1);
        var t3 = scheduler.<String>requestFromNewThread(App::msgDelay, 0);
        var t4 = scheduler.<String>requestFromPool(App::msgDelay, 2);
        var t5 = scheduler.<String>requestFromPool(App::msgDelay, 1);


        pausable.pauseTask();
        System.out.println(t1.receive());
        pausable.continueTask();
        System.out.println(t2.receive());
        System.out.println(t3.receive());
        System.out.println(t5.receive());
        System.out.println(t4.receive());
        pausable.receive();
    }

    static String msgDelay() {
        try {
            Thread.sleep((2 + rnd.nextInt(5)) * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
        return "friend id:" + Thread.currentThread().getId();
    }
}
