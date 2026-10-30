package com.daninichu.math;

public abstract class Vector2D{
    public static class Float extends Vector2D{
        public float x, y;

        public Float(){}

        public Float(float x, float y){
            this.x = x;
            this.y = y;
        }

        @Override
        public double getX(){
            return x;
        }

        @Override
        public double getY(){
            return y;
        }

        @Override
        public void setX(double x){
            this.x = (float) x;
        }

        @Override
        public void setY(double y){
            this.y = (float) y;
        }

        public void set(float x, float y){
            this.x = x;
            this.y = y;
        }
    }

    public static class Double extends Vector2D{
        public double x, y;

        public Double(){}

        public Double(double x, double y){
            this.x = x;
            this.y = y;
        }

        @Override
        public double getX(){
            return x;
        }

        @Override
        public double getY(){
            return y;
        }

        @Override
        public void setX(double x){
            this.x = x;
        }

        @Override
        public void setY(double y){
            this.y = y;
        }
    }

    public abstract double getX();

    public abstract double getY();

    public abstract void setX(double x);

    public abstract void setY(double y);

    public void set(double x, double y){
        setX(x);
        setY(y);
    }

    public void add(double dx, double dy){
        setX(getX() + dx);
        setY(getY() + dy);
    }

    public void add(Vector2D vector){
        add(vector.getX(), vector.getY());
    }

    public void sub(double dx, double dy){
        setX(getX() - dx);
        setY(getY() - dy);
    }

    public void sub(Vector2D vector){
        sub(vector.getX(), vector.getY());
    }

    public void scale(double scalar){
        setX(getX() * scalar);
        setY(getY() * scalar);
    }

    public void div(double divisor){
        setX(getX() / divisor);
        setY(getY() / divisor);
    }

    public void nor(){
        double len = len(getX(), getY());
        setX(getX() / len);
        setY(getY() / len);
    }

    public void reverse(){
        setX(-getX());
        setY(-getY());
    }

    public static double len(double x, double y){
        return Math.sqrt(x*x + y*y);
    }

    public double len(){
        return len(getX(), getY());
    }

    public static double lenSq(double x, double y){
        return x*x + y*y;
    }

    public double lenSq(){
        return lenSq(getX(), getY());
    }

    public static double dist(double x1, double y1, double x2, double y2){
        x1 -= x2;
        y1 -= y2;
        return Math.sqrt(x1*x1 + y1*y1);
    }

    public double dist(double x, double y){
        x -= getX();
        y -= getY();
        return Math.sqrt(x*x + y*y);
    }

    public double dist(Vector2D vector){
        double dx = vector.getX() - getX();
        double dy = vector.getY() - getY();
        return Math.sqrt(dx*dx + dy*dy);
    }

    public static double distSq(double x1, double y1, double x2, double y2){
        x1 -= x2;
        y1 -= y2;
        return x1*x1 + y1*y1;
    }

    public double distSq(double x, double y){
        x -= getX();
        y -= getY();
        return x*x + y*y;
    }

    public double distSq(Vector2D vector){
        double dx = vector.getX() - getX();
        double dy = vector.getY() - getY();
        return dx*dx + dy*dy;
    }

    public double angleRad(){
        return Math.atan2(getY(), getX());
    }

    public double angleDeg(){
        return Math.toDegrees(Math.atan2(getY(), getX()));
    }

    public static double dot(double x1, double y1, double x2, double y2){
        return x1*x2 + y1*y2;
    }

    /**
     * @return A positive number if the angle between {@code this} and the argument is less than 90 degrees. <br>
     * A negative number if the angle between {@code this} and the argument is more than 90 degrees. <br>
     * 0 if the angle between {@code this} and the argument is exactly 90 degrees. <br>
     */
    public double dot(double x, double y){
        return getX() * x + getY() * y;
    }

    /**
     * @return A positive number if the angle between {@code this} and the argument is less than 90 degrees. <br>
     * A negative number if the angle between {@code this} and the argument is more than 90 degrees. <br>
     * 0 if the angle between {@code this} and the argument is exactly 90 degrees. <br>
     */
    public double dot(Vector2D vector){
        return getX() * vector.getX() + getY() * vector.getY();
    }

    public static double cross(double x1, double y1, double x2, double y2){
        return x1*y2 - y1*x2;
    }

    public double cross(double x, double y){
        return getX() * y - getY() * x;
    }

    public double cross(Vector2D vector){
        return getX() * vector.getY() - getY() * vector.getX();
    }

    public boolean isZero(){
        return getX() == 0 && getY() == 0;
    }

    public boolean equalsEpsilon(Vector2D vector, double epsilon){
        return distSq(vector) <= epsilon;
    }

    @Override
    public boolean equals(Object obj){
        return obj instanceof Vector2D v && getX() == v.getX() && getY() == v.getY();
    }

    @Override
    public String toString(){
        return "(" + getX() + ", " + getY() + ")";
    }
}