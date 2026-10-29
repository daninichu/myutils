package com.daninichu.math;

public class Rectangle implements Shape{
    public double x, y, width, height;

    public Rectangle(){}

    public Rectangle(double x, double y, double width, double height){
        set(x, y, width, height);
    }

    public Rectangle(Rectangle rectangle){
        set(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
    }

    public void set(double x, double y, double width, double height){
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public double getX(){
        return x;
    }

    public double getY(){
        return y;
    }

    public double getWidth(){
        return width;
    }

    public double getHeight(){
        return height;
    }

    @Override
    public boolean contains(double x, double y){
        double x0 = this.x;
        double y0 = this.y;
        return x >= x0 && y >= y0 && x < x0 + width && y < y0 + height;
    }

    @Override
    public boolean contains(Shape shape){
        return containsRectangle(this);
    }

    @Override
    public boolean containsRectangle(double x, double y, double w, double h){
        double x0 = this.x;
        double y0 = this.y;
        return x >= x0 && y >= y0 && x + w <= x0 + width && y + h <= y0 + height;
    }

    @Override
    public boolean containsCircle(double x, double y, double radius){
        double x0 = this.x;
        double y0 = this.y;
        return x - radius >= x0 && y - radius >= y0 && x + radius < x0 + width && y + radius < y0 + height;
    }

    @Override
    public boolean containsPolygon(Polygon p){
        return false;
    }

    @Override
    public boolean intersects(Shape shape){
        return shape.intersectsRectangle(x, y, width, height);
    }

    @Override
    public boolean intersectsRectangle(double x, double y, double w, double h){
        double x0 = this.x;
        double y0 = this.y;
        return x + w > x0 && y + h > y0 && x < x0 + width && y < y0 + height;
    }

    @Override
    public boolean intersectsCircle(double x, double y, double radius){
        double clampX;
        double clampY;
        double x0 = this.x;
        double y0 = this.y;

        if(x < x0)
            clampX = x0;
        else if(x > x0 + width)
            clampX = x0 + width;
        else
            clampX = x;
        if(y < y0)
            clampY = y0;
        else if(y > y0 + height)
            clampY = y0 + height;
        else
            clampY = y;

        double dx = x - clampX;
        double dy = y - clampY;
        return dx * dx + dy * dy <= radius * radius;
    }

    @Override
    public boolean intersectsPolygon(Polygon p){
        return false;
    }

    @Override
    public String toString(){
        return "Rectangle[" + x + ", " + y + ", " + width + ", " + height + "]";
    }
}
