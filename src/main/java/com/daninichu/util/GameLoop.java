package com.daninichu.util;

import java.util.Objects;
import java.util.function.DoubleConsumer;

public class GameLoop implements Runnable{
    private final double frameTime;
    private final DoubleConsumer runnable;
    private Thread thread;
    private volatile boolean running;

    public GameLoop(int fps, DoubleConsumer update){
        if(fps <= 0)
            throw new IllegalArgumentException("fps must be positive");
        frameTime = 1_000_000_000.0 / fps;
        this.runnable = Objects.requireNonNull(update);
    }

    public synchronized void start(){
        if(thread != null)
            throw new IllegalStateException("GameLoop already started");
        running = true;
        thread = new Thread(this, "GameLoop");
        thread.start();
    }

    @Override
    public void run(){
        long last = System.nanoTime();
        long targetNextFrame = last;
        while(running){
            int i=0;
            long now;
            while((now = System.nanoTime()) < targetNextFrame){
                Thread.onSpinWait();
                i++;
            }
            System.out.println(i);
            double deltaTime = (now - last) / 1_000_000_000.0;
            last = now;

            runnable.accept(deltaTime);

            targetNextFrame = last + (long) frameTime;
            while(targetNextFrame - System.nanoTime() > 2_000_000){
                try{
                    Thread.sleep(1);
                } catch(InterruptedException e){
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    public static void main(String[] args){
        new GameLoop(60, dt -> {
            System.out.println("dt = "+dt);
            System.out.println("fps = " + 1/dt);
            System.out.println();
        }).start();
    }
}