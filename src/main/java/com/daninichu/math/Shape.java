package com.daninichu.math;

public interface Shape{
    boolean contains(double x, double y);
    boolean contains(Shape shape);
    boolean containsRectangle(double x, double y, double w, double h);
    boolean containsCircle(double x, double y, double radius);
    boolean containsPolygon(Polygon p);

    default boolean containsRectangle(Rectangle r){
        return containsRectangle(r.x, r.y, r.width, r.height);
    }
    default boolean containsCircle(Circle c){
        return containsCircle(c.x, c.y, c.radius);
    }

    boolean intersects(Shape shape);
    boolean intersectsRectangle(double x, double y, double w, double h);
    boolean intersectsCircle(double x, double y, double radius);
    boolean intersectsPolygon(Polygon p);

    default boolean intersectsRectangle(Rectangle r){
        return intersectsRectangle(r.x, r.y, r.width, r.height);
    }
    default boolean intersectsCircle(Circle c){
        return intersectsCircle(c.x, c.y, c.radius);
    }
}
