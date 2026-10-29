package com.daninichu.math;

import java.awt.Point;
import java.util.Arrays;
import java.util.List;

public class Polygon implements Shape{
    private double[] vertices;
    private boolean isConvex;

    public void set(Polygon polygon){
        vertices = Arrays.copyOf(polygon.vertices, polygon.vertices.length);
        isConvex = polygon.isConvex;
    }

    public Polygon(double[] vertices){
        set(vertices);
    }

    public Polygon(double[] xs, double[] ys){
        set(xs, ys);
    }

    public Polygon(Point[] points){
        set(points);
    }

    public Polygon(List<Point> points){
        set(points);
    }

    public Polygon(Polygon polygon){
        set(polygon);
    }

    public void set(double[] vertices){
        int n = vertices.length;
        if(n < 6)
            throw new IllegalArgumentException("Need at least 3 points");
        if((n & 1) != 0)
            throw new IllegalArgumentException("vertices must have even size");

        double[] vs = this.vertices = new double[n];
        for(int i = 0; i < n; i += 2){
            double x1 = vertices[i];
            double y1 = vertices[i+1];
            double x2 = vertices[(i+2) % n];
            double y2 = vertices[(i+3) % n];

            if(x1 == x2 && y1 == y2){
                throw new IllegalArgumentException("Vertices " + i +" and "+ (i+1) +" are the same");
            }
            vs[i] = x1;
            vs[i+1] = y1;
        }
    }

    public void set(double[] xs, double[] ys){
        if(xs.length != ys.length)
            throw new IllegalArgumentException("xs.length != ys.length");
        if(xs.length < 3)
            throw new IllegalArgumentException("Need at least 3 points");
        construct(xs, ys);
    }

    public void set(Point[] points){
        if(points.length < 3)
            throw new IllegalArgumentException("Need at least 3 points");
        construct(points);
    }

    public void set(List<Point> points){
        if(points.size() < 3)
            throw new IllegalArgumentException("Need at least 3 points");
        construct(points);
    }

    private void construct(double[] xs, double[] ys){
        int n = xs.length;
        double[] vs = this.vertices = new double[2*n];
        for(int i = 0; i < n; i++){
            double x = xs[i];
            double y = ys[i];
            vs[i] = x;
            vs[i+1] = y;
        }
    }

    private void construct(Point[] points){
        int n = points.length;
        double[] vs = this.vertices = new double[2*n];
        for(int i = 0; i < n; i++){
            Point p = points[i];
            vs[i] = p.x;
            vs[i+1] = p.y;
        }
    }

    private void construct(List<Point> points){
        int n = points.size();
        double[] vs = this.vertices = new double[2*n];
        for(int i = 0; i < n; i++){
            Point p = points.get(i);
            vs[i] = p.x;
            vs[i+1] = p.y;
        }
    }

    public boolean isConvex(){
        return isConvex;
    }

    public boolean isSelfIntersecting(){
        return false;
    }

    public boolean isDegenerate(){
        return false;
    }

    public double[] getVertices(){
        return Arrays.copyOf(vertices, vertices.length);
    }

    private static double cross(double x1, double y1, double x2, double y2){
        return x1 * y2 - y1 * x2;
    }

    @Override
    public boolean contains(double x, double y){
        boolean inside = false;

        int n = vertices.length;
        int j = n - 2;
        for(int i = 0; i < n; j = i, i += 2){
            double x1 = vertices[i];
            double y1 = vertices[i+1];
            double x2 = vertices[j];
            double y2 = vertices[j+1];

            if(y1 > y != y2 > y && x < (x2-x1) * (y-y1) / (y2 - y1) + x1){
                inside = !inside;
            }
        }

        return inside;
    }

    @Override
    public boolean contains(Shape shape){
        return false;
    }

    @Override
    public boolean containsRectangle(double x, double y, double w, double h){
        return false;
    }

    @Override
    public boolean containsCircle(double x, double y, double radius){
        return false;
    }

    @Override
    public boolean containsPolygon(Polygon p){
        return false;
    }

    @Override
    public boolean intersects(Shape shape){
        return shape.intersectsPolygon(this);
    }

    @Override
    public boolean intersectsRectangle(double x, double y, double w, double h){
        return false;
    }

    @Override
    public boolean intersectsCircle(double x, double y, double radius){
        return false;
    }

    @Override
    public boolean intersectsPolygon(Polygon p){
        if(isConvex && p.isConvex){
            return separatingAxisTheorem(p) && p.separatingAxisTheorem(this);
        }
        return false;
    }

    private boolean separatingAxisTheorem(Polygon p){
        return true;
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder("Polygon[");
        double[] vs = this.vertices;
        int n = vs.length;
        for(int i = 0; i < n; i += 2){
            sb.append("(").append(vs[i]).append(", ").append(vs[i+1]).append(")");
            if(i < n-2)
                sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }
}
