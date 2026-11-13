package com.daninichu.util;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

public class Camera{
    private double x, y, zoom = 1;

    public Camera(){}

    public Camera(double x, double y){
        setX(x);
        setY(y);
    }

    public double toWorldX(int screenX){
        return x + screenX / zoom;
    }

    public double toWorldY(int screenY){
        return y + screenY / zoom;
    }

    public Point2D.Double toWorld(int screenX, int screenY){
        return new Point2D.Double(toWorldX(screenX), toWorldY(screenY));
    }

    public Point2D.Double toWorld(Point screenPosition){
        return new Point2D.Double(toWorldX(screenPosition.x), toWorldY(screenPosition.y));
    }

    public double toScreenX(double worldX){
        return (worldX - x) * zoom;
    }

    public double toScreenY(double worldY){
        return (worldY - y) * zoom;
    }

    public Point2D.Double toScreen(double worldX, double worldY){
        return new Point2D.Double(toScreenX(worldX), toScreenY(worldY));
    }

    public Point2D.Double toScreen(Point2D worldPosition){
        return new Point2D.Double(toScreenX(worldPosition.getX()), toScreenY(worldPosition.getY()));
    }

    public double getX(){
        return x;
    }

    public double getY(){
        return y;
    }

    public void setX(double x){
        if(!Double.isFinite(x))
            throw new IllegalArgumentException("x is " + x);
        this.x = x;
    }

    public void setY(double y){
        if(!Double.isFinite(y))
            throw new IllegalArgumentException("y is " + y);
        this.y = y;
    }

    public void setPosition(double x, double y){
        setX(x);
        setY(y);
    }

    public void setPosition(Point2D p){
        setX(p.getX());
        setY(p.getY());
    }

    public void setCenterX(double x, int screenW){
        setX(x - screenW / 2);
    }

    public void setCenterY(double y, int screenH){
        setX(y - screenH / 2);
    }

    public void setCenter(double x, double y, int screenW, int screenH){
        setX(x - screenW / 2);
        setY(y - screenH / 2);
    }

    public void setCenter(Point2D p, int screenW, int screenH){
        setX(p.getX() - screenW / 2);
        setY(p.getY() - screenH / 2);
    }

    public double getZoom(){
        return zoom;
    }

    public void setZoom(double zoom){
        if(!Double.isFinite(zoom) || zoom <= 0)
            throw new IllegalArgumentException("zoom is " + zoom);
        this.zoom = zoom;
    }

    public void scaleZoom(double scalar){
        setZoom(zoom * scalar);
    }

    public void apply(Graphics2D g){
        g.transform(createTransform());
    }

    public AffineTransform createTransform(){
        AffineTransform transform = new AffineTransform();
        transform.scale(zoom, zoom);
        transform.translate(-x, -y);
        return transform;
    }

    @Override
    public String toString(){
        return "Camera[" + "x=" + x + ", y=" + y + ", zoom=" + zoom + ']';
    }
}
