package com.daninichu.math;

public class Vector {
    public double x, y;

    public Vector(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Vector(Vector vector) {
        this.x = vector.x;
        this.y = vector.y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public Vector setX(double x) {
        this.x = x;
        return this;
    }

    public Vector setY(double y) {
        this.y = y;
        return this;
    }

    public Vector set(double x, double y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public static Vector add(Vector a, Vector b) {
        return new Vector(a.x + b.x, a.y + b.y);
    }

    public Vector add(double dx, double dy) {
        x += dx;
        y += dy;
        return this;
    }

    public Vector add(Vector vector) {
        this.x += vector.x;
        this.y += vector.y;
        return this;
    }

    public static Vector sub(Vector a, Vector b) {
        return new Vector(a.x - b.x, a.y - b.y);
    }

    public Vector sub(double x, double y) {
        this.x -= x;
        this.y -= y;
        return this;
    }

    public Vector sub(Vector vector) {
        this.x -= vector.x;
        this.y -= vector.y;
        return this;
    }

    public static Vector scale(Vector vector, double scalar) {
        return new Vector(vector.x * scalar, vector.y * scalar);
    }

    public Vector scale(double scalar) {
        this.x *= scalar;
        this.y *= scalar;
        return this;
    }

    public static Vector div(Vector vector, double divisor) {
        return new Vector(vector.x / divisor, vector.y / divisor);
    }

    public Vector div(double divisor) {
        this.x /= divisor;
        this.y /= divisor;
        return this;
    }

    public static Vector nor(Vector vector) {
        double len = Math.sqrt(vector.x * vector.x + vector.y * vector.y);
        return new Vector(vector.x / len, vector.y / len);
    }

    public Vector nor(){
        double len = Math.sqrt(x*x + y*y);
        this.x /= len;
        this.y /= len;
        return this;
    }

    public Vector reverse(){
        x *= -1;
        y *= -1;
        return this;
    }

    public static double len(double x, double y) {
        return Math.sqrt(x*x + y*y);
    }

    public double len() {
        return Math.sqrt(x*x + y*y);
    }

    public static double len2(double x, double y) {
        return x*x + y*y;
    }

    public double len2() {
        return x*x + y*y;
    }

    public static double distance(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return Math.sqrt(dx*dx + dy*dy);
    }

    public double distance(Vector vector) {
        double dx = vector.x - x;
        double dy = vector.y - y;
        return Math.sqrt(dx*dx + dy*dy);
    }

    public double distance2(Vector vector) {
        double dx = vector.x - x;
        double dy = vector.y - y;
        return dx*dx + dy*dy;
    }

    public static double dot(double x1, double y1, double x2, double y2) {
        return x1*x2 + y1*y2;
    }

    /**
     * @return A positive number if the angle between {@code this} and the argument is less than 90 degrees. <br>
     * A negative number if the angle between {@code this} and the argument is more than 90 degrees. <br>
     * 0 if the angle between {@code this} and the argument is exactly 90 degrees. <br>
     */
    public double dot(double x, double y) {
        return this.x*x + this.y*y;
    }

    /**
     * @return A positive number if the angle between {@code this} and the argument is less than 90 degrees. <br>
     * A negative number if the angle between {@code this} and the argument is more than 90 degrees. <br>
     * 0 if the angle between {@code this} and the argument is exactly 90 degrees. <br>
     */
    public double dot(Vector vector) {
        return x*vector.x + y*vector.y;
    }

    public boolean isZero(){
        return x == 0 && y == 0;
    }

    @Override
    public boolean equals(Object obj){
        return obj instanceof Vector vector && x == vector.x && y == vector.y;
    }

    @Override
    public String toString(){
        return "("+x+", "+y+")";
    }
}