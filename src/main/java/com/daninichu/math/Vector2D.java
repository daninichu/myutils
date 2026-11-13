package com.daninichu.math;

import java.awt.geom.Point2D;

public abstract class Vector2D{
    public static class Float extends Vector2D{
        public float x, y;

        public Float(){}

        public Float(float x, float y){
            this.x = x;
            this.y = y;
        }

        @Override
        public Point2D.Float toPoint2D(){
            return new Point2D.Float(x, y);
        }

        @Override
        public Vector2D.Float copy(){
            return new Float(x, y);
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
    }

    public static class Double extends Vector2D{
        public double x, y;

        public Double(){}

        public Double(double x, double y){
            this.x = x;
            this.y = y;
        }

        @Override
        public Point2D.Double toPoint2D(){
            return new Point2D.Double(x, y);
        }

        @Override
        public Vector2D.Double copy(){
            return new Double(x, y);
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

    public abstract Point2D toPoint2D();

    public abstract Vector2D copy();

    public abstract double getX();

    public abstract double getY();

    public abstract void setX(double x);

    public abstract void setY(double y);

    public void set(double x, double y){
        setX(x);
        setY(y);
    }

    public void setZero(){
        setX(0);
        setY(0);
    }

    public void addX(double dx){
        setX(getX() + dx);
    }

    public void addY(double dy){
        setY(getY() + dy);
    }

    public void add(double dx, double dy){
        setX(getX() + dx);
        setY(getY() + dy);
    }

    public void add(Vector2D vector){
        add(vector.getX(), vector.getY());
    }

    public void subX(double dx){
        setX(getX() - dx);
    }

    public void subY(double dy){
        setY(getY() - dy);
    }

    public void sub(double dx, double dy){
        setX(getX() - dx);
        setY(getY() - dy);
    }

    public void sub(Vector2D vector){
        sub(vector.getX(), vector.getY());
    }

    public void scaleX(double scalar){
        setX(getX() * scalar);
    }

    public void scaleY(double scalar){
        setY(getY() * scalar);
    }

    public void scale(double scalar){
        setX(getX() * scalar);
        setY(getY() * scalar);
    }

    public void scale(double scalarX, double scalarY){
        setX(getX() * scalarX);
        setY(getY() * scalarY);
    }

    public void scale(Vector2D vector){
        setX(getX() * vector.getX());
        setY(getY() * vector.getY());
    }

    public void divX(double divisor){
        setX(getX() / divisor);
    }

    public void divY(double divisor){
        setY(getY() / divisor);
    }

    public void div(double divisor){
        setX(getX() / divisor);
        setY(getY() / divisor);
    }

    public void div(double divisorX, double divisorY){
        setX(getX() / divisorX);
        setY(getY() / divisorY);
    }

    public void div(Vector2D vector){
        setX(getX() / vector.getX());
        setY(getY() / vector.getY());
    }

    public void nor(){
        double len = len(getX(), getY());
        setX(getX() / len);
        setY(getY() / len);
    }

    public void setLen(double len){
        double scalar = len / len(getX(), getY());
        setX(getX() * scalar);
        setY(getY() * scalar);
    }

    public void clampLenMin(double min){
        if(!(min >= 0))
            throw new IllegalArgumentException("Illegal min: " + min);
        double lenSq = lenSq(getX(), getY());
        if(lenSq < min * min){
            min /= Math.sqrt(lenSq);
            setX(getX() * min);
            setY(getY() * min);
        }
    }

    public void clampLenMax(double max){
        if(!(max >= 0))
            throw new IllegalArgumentException("Illegal max: " + max);
        double lenSq = lenSq(getX(), getY());
        if(lenSq > max * max){
            max /= Math.sqrt(lenSq);
            setX(getX() * max);
            setY(getY() * max);
        }
    }

    public void clampLen(double min, double max){
        if(!(min >= 0))
            throw new IllegalArgumentException("Illegal min: " + min);
        if(!(min <= max))
            throw new IllegalArgumentException("min = " + min + " > " + max + " = max");
        double lenSq = lenSq(getX(), getY());
        if(lenSq < min * min){
            min /= Math.sqrt(lenSq);
            setX(getX() * min);
            setY(getY() * min);
        } else if(lenSq > max * max){
            max /= Math.sqrt(lenSq);
            setX(getX() * max);
            setY(getY() * max);
        }
    }

    public void moveTowards(double targetX, double targetY, double maxDelta){
        double dx = targetX - getX();
        double dy = targetY - getY();
        double distSq = dx*dx + dy*dy;
        if(maxDelta >= 0 && distSq <= maxDelta * maxDelta){
            setX(targetX);
            setY(targetY);
        } else{
            double scalar = maxDelta / Math.sqrt(distSq);
            setX(getX() + dx * scalar);
            setY(getY() + dy * scalar);
        }
    }

    public void moveTowards(Vector2D target, double maxDelta){
        moveTowards(target.getX(), target.getY(), maxDelta);
    }

    public void lerp(double targetX, double targetY, double alpha){
        double inverseAlpha = 1 - alpha;
        setX(getX() * inverseAlpha + targetX * alpha);
        setY(getY() * inverseAlpha + targetY * alpha);
    }

    public void lerp(Vector2D target, double alpha){
        lerp(target.getX(), target.getY(), alpha);
    }

    public void slerp(Vector2D target, double alpha){
        double len1 = len();
        double len2 = target.len();

        if(len1 == 0 || len2 == 0){
            setX(getX() + (target.getX() - getX()) * alpha);
            setY(getY() + (target.getY() - getY()) * alpha);
            return;
        }

        double angle = Math.atan2(cross(target), dot(target));

        double newAngle = angle * alpha;
        double newLen = len1 + (len2 - len1) * alpha;

        double cos = Math.cos(newAngle);
        double sin = Math.sin(newAngle);

        double x = getX() * cos - getY() * sin;
        double y = getX() * sin + getY() * cos;

        set(x * newLen / len1, y * newLen / len1);
    }

    public void reverseX(){
        setX(-getX());
    }

    public void reverseY(){
        setY(-getY());
    }

    public void reverse(){
        setX(-getX());
        setY(-getY());
    }

    public void rotate90(){
        rotate90(true);
    }

    public void rotate90(boolean positiveDir){
        double x = getX();
        if(positiveDir){
            setX(-getY());
            setY(x);
        } else{
            setX(getY());
            setY(-x);
        }
    }

    public void rotateRad(double radians){
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        double newX = getX() * cos - getY() * sin;
        double newY = getX() * sin + getY() * cos;
        setX(newX);
        setY(newY);
    }

    public void rotateDeg(double degrees){
        rotateRad(Math.toRadians(degrees));
    }

    public void setAngleRad(double radians){
        setX(len());
        setY(0);
        rotateRad(radians);
    }

    public void setAngleDeg(double degrees){
        setAngleRad(Math.toRadians(degrees));
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

    public static double angleRad(double x, double y){
        return Math.atan2(y, x);
    }

    public double angleRad(){
        return Math.atan2(getY(), getX());
    }

    public static double angleDeg(double x, double y){
        return Math.toDegrees(Math.atan2(y, x));
    }

    public double angleDeg(){
        return Math.toDegrees(Math.atan2(getY(), getX()));
    }

    public static double angleBetweenRad(double x1, double y1, double x2, double y2){
        return Math.acos(dot(x1, y1, x2, y2) / (len(x1, y1) * len(x2, y2)));
    }

    public double angleBetweenRad(double x, double y){
        return Math.acos(dot(x, y) / (len() * len(x, y)));
    }

    public double angleBetweenRad(Vector2D vector){
        return Math.acos(dot(vector) / (len() * vector.len()));
    }

    public static double angleBetweenDeg(double x1, double y1, double x2, double y2){
        return Math.toDegrees(angleBetweenRad(x1, y1, x2, y2));
    }

    public double angleBetweenDeg(double x, double y){
        return Math.toDegrees(angleBetweenRad(x, y));
    }

    public double angleBetweenDeg(Vector2D vector){
        return Math.toDegrees(angleBetweenRad(vector.getX(), vector.getY()));
    }

    public static double dot(double x1, double y1, double x2, double y2){
        return x1*x2 + y1*y2;
    }

    public double dot(double x, double y){
        return getX() * x + getY() * y;
    }

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

    public boolean isZero(double epsilon){
        if (!(epsilon >= 0))
            throw new IllegalArgumentException("Illegal epsilon: " + epsilon);
        return lenSq(getX(), getY()) <= epsilon * epsilon;
    }

    public boolean isUnit(){
        return lenSq(getX(), getY()) == 1;
    }

    public boolean isUnit(double epsilon){
        if (!(epsilon >= 0))
            throw new IllegalArgumentException("Illegal epsilon: " + epsilon);
        double lenSq = lenSq(getX(), getY());
        double lowerBound = Math.max(0, 1 - epsilon);
        double upperBound = 1 + epsilon;
        return lowerBound * lowerBound <= lenSq && lenSq <= upperBound * upperBound;
    }

    public boolean isFinite(){
        return java.lang.Double.isFinite(getX()) && java.lang.Double.isFinite(getY());
    }

    public static boolean equals(Vector2D a, Vector2D b){
        if(a == null)
            return b == null;
        if(b == null)
            return false;
        return a.getX() == b.getX() && a.getY() == b.getY();
    }

    public static boolean equals(Vector2D a, Vector2D b, double epsilon){
        if (!(epsilon >= 0))
            throw new IllegalArgumentException("Illegal epsilon: " + epsilon);
        if(a == null)
            return b == null;
        if(b == null)
            return false;
        return distSq(a.getX(), a.getY(), b.getX(), b.getY()) <= epsilon * epsilon;
    }

    public boolean equals(Vector2D vector, double epsilon){
        if (!(epsilon >= 0))
            throw new IllegalArgumentException("Illegal epsilon: " + epsilon);
        if(vector == null)
            return false;
        return distSq(getX(), getY(), vector.getX(), vector.getY()) <= epsilon * epsilon;
    }

    @Override
    public boolean equals(Object obj){
        return obj instanceof Vector2D v && getX() == v.getX() && getY() == v.getY();
    }

    @Override
    public int hashCode(){
        double x = getX();
        double y = getY();
        long xBits = java.lang.Double.doubleToLongBits(x == -0.0? 0 : x);
        long yBits = java.lang.Double.doubleToLongBits(y == -0.0? 0 : y);
        return 31 * (int) (xBits ^ (xBits >>> 32)) + (int) (yBits ^ (yBits >>> 32));
    }

    public static String toString(double x, double y){
        return "(" + x + ", " + y + ')';
    }

    @Override
    public String toString(){
        return "(" + getX() + ", " + getY() + ')';
    }
}
