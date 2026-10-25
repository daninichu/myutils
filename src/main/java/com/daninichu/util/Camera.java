package com.daninichu.util;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

public class Camera{
    public double x, y;
    private double zoom = 1;
    private int screenWidth, screenHeight;

    public Camera(){
        this(0, 0);
    }

    public Camera(double x, double y){
        this(x, y, 0, 0);
    }

    public Camera(double x, double y, int screenWidth, int screenHeight){
        this.x = x;
        this.y = y;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    public double toWorldX(int screenX){
        return x + screenX / zoom;
    }

    public double toWorldY(int screenY){
        return y + screenY / zoom;
    }

    public int toScreenX(double worldX){
        return (int) Math.round((worldX - x) * zoom);
    }

    public int toScreenY(double worldY){
        return (int) Math.round((worldY - y) * zoom);
    }

    public void setPosition(double x, double y){
        this.x = x;
        this.y = y;
    }

    public void setCenter(double x, double y){
        this.x = x - screenWidth / 2;
        this.y = y - screenHeight / 2;
    }

    public double getX(){
        return x;
    }

    public double getY(){
        return y;
    }

    public double getCenterX(){
        return x + screenWidth / 2;
    }

    public double getCenterY(){
        return y + screenHeight / 2;
    }

    public void setZoom(double zoom){
        if(zoom <= 0)
            throw new IllegalArgumentException("zoom must be positive");
        this.zoom = zoom;
    }

    public void scaleZoom(double scalar){
        if(scalar <= 0)
            throw new IllegalArgumentException("scalar must be positive");
        zoom *= scalar;
    }

    public double getZoom(){
        return zoom;
    }

    public void setScreenWidth(int screenWidth){
        this.screenWidth = screenWidth;
    }

    public void setScreenHeight(int screenHeight){
        this.screenHeight = screenHeight;
    }

    public void setScreenSize(int screenWidth, int screenHeight){
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    public int getScreenWidth(){
        return screenWidth;
    }

    public int getScreenHeight(){
        return screenHeight;
    }

    public void apply(Graphics2D g){
        g.transform(createTransform(screenWidth, screenHeight));
    }

    public void apply(Graphics2D g, int screenWidth, int screenHeight){
        g.transform(createTransform(screenWidth, screenHeight));
    }

    public AffineTransform createTransform(){
        return createTransform(screenWidth, screenHeight);
    }

    public AffineTransform createTransform(int screenWidth, int screenHeight){
        AffineTransform transform = new AffineTransform();
        transform.translate(screenWidth / 2.0, screenHeight / 2.0);
        transform.scale(zoom, zoom);
        transform.translate(-x, -y);
        return transform;
    }
}
