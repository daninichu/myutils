package com.daninichu.util;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

public class Camera{
    public double x, y;
    private double zoom = 1;

    public Camera(){}

    public Camera(double x, double y){
        this.x = x;
        this.y = y;
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

    public Point2D.Double toWorld(Point point){
        return new Point2D.Double(point.x, point.y);
    }

    public int toScreenX(double worldX){
        return (int) Math.floor((worldX - x) * zoom);
    }

    public int toScreenY(double worldY){
        return (int) Math.floor((worldY - y) * zoom);
    }

    public Point toScreen(double worldX, double worldY){
        return new Point(toScreenX(worldX), toScreenY(worldY));
    }

    public Point toScreen(Point2D point){
        return new Point(toScreenX(point.getX()), toScreenY(point.getY()));
    }

    public double getX(){
        return x;
    }

    public double getY(){
        return y;
    }

    public void setX(double x){
        this.x = x;
    }

    public void setY(double y){
        this.y = y;
    }

    public void setPosition(double x, double y){
        this.x = x;
        this.y = y;
    }

    public void setPosition(Point2D point){
        this.x = point.getX();
        this.y = point.getY();
    }

    public double getZoom(){
        return zoom;
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

    public void apply(Graphics2D g){
        g.transform(createTransform());
    }

    public AffineTransform createTransform(){
        AffineTransform transform = new AffineTransform();
        transform.scale(zoom, zoom);
        transform.translate(-x, -y);
        return transform;
    }
}
