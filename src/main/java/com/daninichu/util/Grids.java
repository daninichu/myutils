package com.daninichu.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.UnaryOperator;

public final class Grids{
    private Grids(){}

    public static List<Grid.Point> getPoints(double x, double y, double w, double h, double cellSize){
        return getPoints(x, y, w, h, cellSize, cellSize);
    }

    public static List<Grid.Point> getPoints(
            double x, double y, double w, double h,
            double cellWidth, double cellHeight
    ){
        int x1 = (int) Math.floor(x / cellWidth);
        int y1 = (int) Math.floor(y / cellHeight);
        int x2 = (int) Math.ceil((x + w) / cellWidth);
        int y2 = (int) Math.ceil((y + h) / cellHeight);

        List<Grid.Point> points = new ArrayList<>((x2 - x1) * (y2 - y1));
        int startX = x1;
        while(y1 < y2){
            x1 = startX;
            while(x1 < x2){
                points.add(new Grid.Point(x1, y1));
                x1++;
            }
            y1++;
        }
        return points;
    }

    private static class BackedGrid<E> implements Grid<E>{
        final Grid<E> grid;

        public BackedGrid(Grid<E> grid){
            this.grid = grid;
        }

        public int size(){return grid.size();}
        public boolean isEmpty(){return grid.isEmpty();}

        public E get(int x, int y){return grid.get(x, y);}
        public E get(Point p){return grid.get(p);}

        public E getOrDefault(int x, int y, E defaultValue){return grid.getOrDefault(x, y, defaultValue);}
        public E getOrDefault(Point p, E defaultValue){return grid.getOrDefault(p, defaultValue);}

        public E set(int x, int y, E e){return grid.set(x, y, e);}
        public E set(Point p, E e){return grid.set(p, e);}

        public void setAll(Grid<? extends E> grid){this.grid.setAll(grid);}

        public E removePoint(int x, int y){return grid.removePoint(x, y);}
        public E removePoint(Point p){return grid.removePoint(p);}

        public boolean removeValue(E e){return grid.removeValue(e);}

        public boolean containsPoint(int x, int y){return grid.containsPoint(x, y);}
        public boolean containsPoint(Point p){return grid.containsPoint(p);}

        public boolean containsValue(E e){return grid.containsValue(e);}

        public Point pointOf(E e){return grid.pointOf(e);}

        public void clear(){grid.clear();}

        public Iterable<Point> points(){return grid.points();}
        public Iterable<Cell<E>> cells(){return grid.cells();}
        public Iterator<E> iterator(){return grid.iterator();}

        public void compute(int x, int y, UnaryOperator<E> operator){grid.compute(x, y, operator);}
    }

    private static final class TranslatedGrid<E> extends BackedGrid<E>{
        private int originX, originY;

        public TranslatedGrid(Grid<E> grid, int originX, int originY){
            super(grid);
            this.originX = originX;
            this.originY = originY;
        }

        public E get(int x, int y){
            return grid.get(x - originX, y - originY);
        }
        public E get(Point p){
            return grid.get(p.x - originX, p.y - originY);
        }
        public E getOrDefault(int x, int y, E defaultValue){
            return grid.getOrDefault(x - originX, y - originY, defaultValue);
        }
        public E getOrDefault(Point p, E defaultValue){
            return grid.getOrDefault(p.x - originX, p.y - originY, defaultValue);
        }
        public E set(int x, int y, E e){
            return grid.set(x - originX, y - originY, e);
        }
        public E set(Point p, E e){
            return grid.set(p.x - originX, p.y - originY, e);
        }
        public E removePoint(int x, int y){
            return grid.removePoint(x - originX, y - originY);
        }
        public E removePoint(Point p){
            return grid.removePoint(p.x - originX, p.y - originY);
        }
        public boolean containsPoint(int x, int y){
            return grid.containsPoint(x - originX, y - originY);
        }
        public boolean containsPoint(Point p){
            return grid.containsPoint(p.x - originX, p.y - originY);
        }

        public Iterable<Point> points(){
            return () -> new Iterator<>(){
                final Iterator<Point> it = grid.points().iterator();
                public boolean hasNext(){
                    return it.hasNext();
                }
                public Point next(){
                    Point p = it.next();
                    return new Point(p.x + originX, p.y + originY);
                }
            };
        }

        public void compute(int x, int y, UnaryOperator<E> operator){
            grid.compute(x - originX, y - originY, operator);
        }
    }

    public static <E> Grid<E> translatedGrid(Grid<E> grid, int originX, int originY){
        if(grid instanceof TranslatedGrid<E> translatedGrid){
            originX += translatedGrid.originX;
            originY += translatedGrid.originY;
            grid = translatedGrid.grid;
        }
        return new TranslatedGrid<>(grid, originX, originY);
    }

    static final class UnmodifiableGrid<E> extends BackedGrid<E>{
        public UnmodifiableGrid(Grid<E> grid){
            super(grid);
        }

        public E set(int x, int y, E e){throw new UnsupportedOperationException();}
        public E set(Point p, E e){throw new UnsupportedOperationException();}
        public void setAll(Grid<? extends E> grid){throw new UnsupportedOperationException();}
        public E removePoint(int x, int y){throw new UnsupportedOperationException();}
        public E removePoint(Point p){throw new UnsupportedOperationException();}
        public boolean removeValue(E e){throw new UnsupportedOperationException();}
        public void clear(){throw new UnsupportedOperationException();}
        public void compute(int x, int y, UnaryOperator<E> operator){throw new UnsupportedOperationException();}
    }

    public static <E> Grid<E> unmodifiableGrid(Grid<E> grid){
        return grid instanceof UnmodifiableGrid<E>? grid : new UnmodifiableGrid<>(grid);
    }
}