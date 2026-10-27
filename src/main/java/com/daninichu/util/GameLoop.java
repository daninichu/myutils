package com.daninichu.util;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.DoubleConsumer;

/**
 * Runs a game update callback at a target frequency,
 * providing the elapsed time between consecutive updates in seconds.
 */
public class GameLoop{
    private final Thread thread;
    private volatile boolean running = true;

    /**
     * Creates a game loop with the specified target update frequency.
     * Upon construction, the game loop immediately starts on a dedicated thread.
     * @param fps target number of updates per second.
     * @param update callback executed on each update,
     *        receiving the elapsed time in seconds since the previous update.
     * @throws IllegalArgumentException if {@code fps} is not positive.
     * @throws NullPointerException if {@code update} is {@code null}.
     */
    public GameLoop(int fps, DoubleConsumer update){
        if(fps <= 0)
            throw new IllegalArgumentException("fps must be positive");
        if(update == null)
            throw new NullPointerException("update is null");

        int frameTime = 1_000_000_000 / fps;
        this.thread = new Thread(() -> run(frameTime, update), "GameLoop");
        this.thread.start();
    }

    /**
     * Stops the game loop and terminates this object's thread.
     * If this method is called during an update,
     * the update will finish before the thread gets terminated.
     * <p>
     * Additional calls to this method will have no effect.
     * @throws IllegalStateException if this method is called from this game loop's thread,
     *         which only happens if this method is called from inside the update callback.
     */
    public void stop(){
        running = false;
        thread.interrupt();

        if(Thread.currentThread() == thread)
            throw new IllegalStateException("Cannot stop GameLoop from its own thread");
        while(true){
            try{
                thread.join();
                return;
            } catch(InterruptedException e){}
        }
    }

    private void run(int frameTime, DoubleConsumer update){
        long last = System.nanoTime();
        while(running){
            int cycles = 0;
            long nextFrame = last + frameTime;
            while(nextFrame - System.nanoTime() > 1_000_000){
                cycles++;
                try{
                    Thread.sleep(0, 1000);
                } catch(InterruptedException e){
                    return;
                }
            }
            System.out.println("sleep cycles: " + cycles);
            System.out.printf("excess time: %fms%n", (nextFrame-System.nanoTime())/1_000_000.);
            cycles = 0;
            long now;
            while((now = System.nanoTime()) < nextFrame){
                cycles++;
            }
            System.out.println("100% cycles: " + cycles);
            System.out.println();

            double deltaTime = (now - last) / 1_000_000_000.0;
            last = now;
            update.accept(deltaTime);
        }
    }

    public static void main(String[] args) throws InterruptedException{
        new GameLoop(60, System.out::println);
//        measureOversleep(100000,0,1000);
    }

    private static void measureOversleep(int n, int ms, int ns) throws InterruptedException{
        var oversleep = new double[n];

        for(int i = 0; i < n; i++){
            long start = System.nanoTime();
            Thread.sleep(ms, ns);
            oversleep[i] = (System.nanoTime() - start - ms*1000000 - ns)/1000000.0;
            if(oversleep[i] >= 1){
                System.out.printf("i = %d, overslept by %sms%n", i, oversleep[i]);
            }
        }

        Map<String, Integer> oversleepMap = new TreeMap<>();
        for(int i = 0; i < oversleep.length; i++){
            oversleepMap.merge("%.2fms".formatted(oversleep[i]), 1, Integer::sum);
        }
        System.out.println(oversleepMap);
    }
}
