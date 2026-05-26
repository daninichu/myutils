package com.daninichu.util;

import java.awt.Point;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.UnaryOperator;

public class HashGrid2<E> {
    public record Point(int x, int y) {
        @Override public int hashCode(){
            return 31 * x + y;
        }
    }
	private final HashMap<Point, E> data;

	public HashGrid2() {
        data = new HashMap<>();
    }

    public HashGrid2(int initialCapacity) {
        data = new HashMap<>(initialCapacity);
    }
//
//    public HashGrid2(int initialCapacity, float loadFactor) {
//        data = new HashMap<>(initialCapacity, loadFactor);
//    }

//	public HashGrid2(Grid<? extends E> grid) {
//        data = new HashMap<>();
//        setAll(grid);
//	}


    long key(int x, int y) {
        return ((long) x << 32) | (y & 0xFFFFFFFFL);
    }

    public E get(int x, int y) {
        return data.get(key(x, y));
    }


//	public E get(Point p){
//		return get(p.x, p.y);
//	}


    public void set(int x, int y, E e) {
        if(e == null)
            data.remove(key(x, y));
        else{
//            data2.put(new Point(x, y), e);
            data.put(new Point(x, y), e);
        }
    }

//
	public void set(Point p, E e){
        data.put(p, e);
	}


//    public void setAll(Grid<? extends E> grid){
//        if (grid instanceof HashGrid2<? extends E> hashGrid)
//            data.putAll(hashGrid.data);
//        else
//            for(Cell<? extends E> cell : grid.cells())
//                data.put(new Point(cell.x(), cell.y()), cell.value());
//    }


//    public void removePoint(int x, int y) {
//        data.remove(new Point(x, y));
//    }
//
//
//    public void removePoint(Point p) {
//        removePoint(p.x, p.y);
//    }
//
//
//    public void removeValue(E e){
//        data.values().remove(e);
//    }
//
//
//    public boolean containsPoint(int x, int y) {
//        return data.containsKey(new Point(x, y));
//    }
//
//
//    public boolean containsPoint(Point p) {
//        return containsPoint(p.x, p.y);
//    }
//
//
//    public boolean containsValue(E e){
//        return data.containsValue(e);
//    }
//
//
//	public void clear(){
//		data.clear();
//	}


//	public Iterable<Point> points(){
//        return () -> new Iterator<>() {
//            final Iterator<Point> it = data.keySet().iterator();
//
//
//            public boolean hasNext() {
//                return it.hasNext();
//            }
//
//
//            public Point next() {
//                return new Point(it.next());
//            }
//        };
//    }
//
//
//    public Iterable<Cell<E>> cells() {
//        return () -> new Iterator<>() {
//            final Iterator<Map.Entry<Point, E>> it = data.entrySet().iterator();
//
//
//            public boolean hasNext() {
//                return it.hasNext();
//            }
//
//
//            public Cell<E> next(){
//                Map.Entry<Point, E> entry = it.next();
//                return new Cell<>(entry.getKey().x, entry.getKey().y, entry.getValue());
//            }
//        };
//    }
//
//
//	public Iterator<E> iterator(){
//		return data.values().iterator();
//	}

//
//    public void compute(int x, int y, UnaryOperator<E> operator){
//        data.compute(new Point(x, y), (k, e) -> operator.apply(e));
//    }
//
//    public Map<Point, E> toMap(){
//        return new HashMap<>(data);
//    }
//
//
//    public String toString(){
//        StringBuilder sb = new StringBuilder("[");
//        for(Cell<E> cell : cells()){
//            sb.append(cell.toString()).append(", ");
//        }
//        return sb.delete(sb.length() - 2, sb.length()).append(']').toString();
//    }
}