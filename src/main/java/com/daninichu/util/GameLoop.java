package com.daninichu.util;

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
            long nextFrame = last + frameTime;
            while(nextFrame - System.nanoTime() > 1_000_000){
                try{
                    Thread.sleep(0, 1000);
                } catch(InterruptedException e){
                    return;
                }
            }
            long now;
            while((now = System.nanoTime()) < nextFrame);

            double deltaTime = (now - last) / 1_000_000_000.0;
            last = now;
            update.accept(deltaTime);
        }
    }
}
