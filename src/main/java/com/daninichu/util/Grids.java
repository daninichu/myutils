package com.daninichu.util;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public final class Grids{
    private Grids(){}

    public static List<Point> getPoints(double x, double y, double w, double h, double cellSize){
        return getPoints(x, y, w, h, cellSize, cellSize);
    }

    public static List<Point> getPoints(
            double x, double y, double w, double h,
            double cellWidth, double cellHeight
    ){
        int x1 = (int) Math.floor(x / cellWidth);
        int y1 = (int) Math.floor(y / cellHeight);
        int x2 = (int) Math.ceil((x + w) / cellWidth);
        int y2 = (int) Math.ceil((y + h) / cellHeight);

        List<Point> points = new ArrayList<>((x2 - x1) * (y2 - y1));
        int startX = x1;
        while(y1 < y2){
            x1 = startX;
            while(x1 < x2){
                points.add(new Point(x1, y1));
                x1++;
            }
            y1++;
        }
        return points;
    }

    private static class BackedGrid<E> extends AbstractGrid<E>{
        final Grid<E> grid;

        public BackedGrid(Grid<E> grid){
            this.grid = grid;
        }

        public E get(int x, int y){return grid.get(x, y);}
        public E set(int x, int y, E e){return grid.set(x, y, e);}
        public E getOrSet(int x, int y, E e){return grid.getOrSet(x, y, e);}
        public E compute(int x, int y, Function<? super E, ? extends E> function){return grid.compute(x, y, function);}
        public E computeIfAbsent(int x, int y, Supplier<? extends E> function){return grid.computeIfAbsent(x, y, function);}
        public E removePoint(int x, int y){return grid.removePoint(x, y);}
        public boolean removeValue(E e){return grid.removeValue(e);}
        public boolean containsPoint(int x, int y){return grid.containsPoint(x, y);}
        public boolean containsValue(E e){return grid.containsValue(e);}
        public Point pointOf(E e){return grid.pointOf(e);}
        public void clear(){grid.clear();}
        public Iterable<Point> points(){return grid.points();}
        public Iterable<Cell<E>> cells(){return grid.cells();}
        public Iterator<E> iterator(){return grid.iterator();}
    }

    private static final class TranslatedGrid<E> extends BackedGrid<E>{
        private int dx, dy;

        public TranslatedGrid(Grid<E> grid, int dx, int dy){
            super(grid);
            this.dx = dx;
            this.dy = dy;
        }

        public E get(int x, int y){return grid.get(x - dx, y - dy);}
        public E set(int x, int y, E e){return grid.set(x - dx, y - dy, e);}
        public E getOrSet(int x, int y, E e){return grid.getOrSet(x - dx, y - dy, e);}
        public E removePoint(int x, int y){return grid.removePoint(x - dx, y - dy);}
        public E compute(int x, int y, Function<? super E, ? extends E> function){return grid.compute(x - dx, y - dy, function);}
        public E computeIfAbsent(int x, int y, Supplier<? extends E> function){return grid.computeIfAbsent(x - dx, y - dy, function);}
        public boolean containsPoint(int x, int y){return grid.containsPoint(x - dx, y - dy);}
        public Point pointOf(E e){
            Point p = grid.pointOf(e);
            p.translate(dx, dy);
            return p;
        }
        public Iterable<Point> points(){
            return () -> new Iterator<>(){
                final Iterator<Point> it = grid.points().iterator();
                public boolean hasNext(){
                    return it.hasNext();
                }
                public Point next(){
                    Point p = it.next();
                    p.translate(dx, dy);
                    return  p;
                }
            };
        }
        public Iterable<Cell<E>> cells(){
            return () -> new Iterator<>(){
                final Iterator<Cell<E>> it = grid.cells().iterator();
                public boolean hasNext(){
                    return it.hasNext();
                }
                public Cell<E> next(){
                    Cell<E> c = it.next();
                    return new Cell<>(c.x + dx, c.y + dy, c.value);
                }
            };
        }
    }

    public static <E> Grid<E> translatedGrid(Grid<E> grid, int dx, int dy){
        if(grid instanceof TranslatedGrid<E> translatedGrid){
            dx += translatedGrid.dx;
            dy += translatedGrid.dy;
            grid = translatedGrid.grid;
        }
        return new TranslatedGrid<>(grid, dx, dy);
    }

    private static final class UnmodifiableGrid<E> extends BackedGrid<E>{
        public UnmodifiableGrid(Grid<E> grid){
            super(grid);
        }

        public E set(int x, int y, E e){throw new UnsupportedOperationException();}
        public E getOrSet(int x, int y, E e){throw new UnsupportedOperationException();}
        public E compute(int x, int y, Function<? super E, ? extends E> function){throw new UnsupportedOperationException();}
        public E computeIfAbsent(int x, int y, Supplier<? extends E> function){throw new UnsupportedOperationException();}
        public E removePoint(int x, int y){throw new UnsupportedOperationException();}
        public boolean removeValue(E e){throw new UnsupportedOperationException();}
        public void setAll(Grid<? extends E> grid){throw new UnsupportedOperationException();}
        public void clear(){throw new UnsupportedOperationException();}
    }

    public static <E> Grid<E> unmodifiableGrid(Grid<E> grid){
        return grid instanceof UnmodifiableGrid<E>? grid : new UnmodifiableGrid<>(grid);
    }
}
