package com.daninichu.util;

public class GameLoop extends Thread {
    public final int fps;
    private final Runnable runnable;

    private boolean running;

    public GameLoop(int fps, Runnable runnable) {
        this.fps = fps;
        this.runnable = runnable;
    }

    @Override
    public void run(){
        double frameTime = 1_000_000_000.0 / fps;
        double delta = 0;
        long last = System.nanoTime();
        while(true){
            long now = System.nanoTime();
            delta += now - last;
            last = now;
            if(delta >= frameTime){
                delta -= frameTime;
                runnable.run();
            }
        }
    }

//    @Override
//    public void start(){
//        running = true;
//        super.start();
//    }
}