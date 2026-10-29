package com.daninichu.math;

public class Circle implements Shape{
    public double x, y, radius;

    public Circle(){}

    public Circle(double x, double y, double radius){
        set(x, y, radius);
    }

    public Circle(Circle circle){
        set(circle.x, circle.y, circle.radius);
    }

    public void set(double x, double y, double radius){
        this.x = x;
        this.y = y;
        this.radius = radius;
    }

    public double getX(){
        return x;
    }

    public double getY(){
        return y;
    }

    public double getRadius(){
        return radius;
    }

    @Override
    public boolean contains(double x, double y){
        double dx = this.x - x;
        double dy = this.y - y;
        double r0 = this.radius;
        return dx*dx + dy*dy <= r0*r0;
    }

    @Override
    public boolean contains(Shape shape){
        return false;
    }

    @Override
    public boolean containsRectangle(double x, double y, double w, double h){
        return contains(x, y) && contains(x+w, y) && contains(x, y+h) && contains(x+w, y+h);
    }

    @Override
    public boolean containsCircle(double x, double y, double radius){
        double r0 = this.radius;
        if(r0 < radius)
            return false;
        double dx = x - this.x;
        double dy = y - this.y;
        return dx*dx + dy*dy <= r0*r0 - radius*radius;
    }

    @Override
    public boolean containsPolygon(Polygon p){
        return false;
    }

    @Override
    public boolean intersects(Shape shape){
        return shape.intersectsCircle(x, y, radius);
    }

    @Override
    public boolean intersectsRectangle(double x, double y, double w, double h){
        return new Rectangle(x, y, w, h).intersectsCircle(this);
    }

    @Override
    public boolean intersectsCircle(double x, double y, double radius){
        double dx = this.x - x;
        double dy = this.y - y;
        double rSum = this.radius + radius;
        return dx*dx + dy*dy <= rSum*rSum;
    }

    @Override
    public boolean intersectsPolygon(Polygon p){
        return false;
    }

    @Override
    public String toString(){
        return "Circle[" + x + ", " + y + ", " + radius + "]";
    }
}
