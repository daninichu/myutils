package com.daninichu.util;

import java.awt.Point;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.UnaryOperator;

public class HashGrid<E> implements Grid<E> {
	private final HashMap<Point, E> data;

	public HashGrid() {
        data = new HashMap<>();
    }

    public HashGrid(int initialCapacity) {
        data = new HashMap<>(initialCapacity);
    }

    public HashGrid(int initialCapacity, float loadFactor) {
        data = new HashMap<>(initialCapacity, loadFactor);
    }

	public HashGrid(Grid<E> grid) {
        data = new HashMap<>();
        setAll(grid);
	}

    @Override
    public E get(int x, int y) {
        return data.get(new Point(x, y));
    }

	@Override
	public E get(Point p){
		return get(p.x, p.y);
	}

    @Override
    public void set(int x, int y, E e) {
        if(e == null)
            data.remove(new Point(x, y));
        else
            data.put(new Point(x, y), e);
    }

	@Override
	public void set(Point p, E e){
        set(p.x, p.y, e);
	}

    @Override
    public void setAll(Grid<E> grid){
        if (grid instanceof HashGrid<E> hashGrid)
            data.putAll(hashGrid.data);
        else
            for(Cell<E> cell : grid.cells())
                data.put(new Point(cell.x(), cell.y()), cell.value());
    }

    @Override
    public void removePoint(int x, int y) {
        data.remove(new Point(x, y));
    }

    @Override
    public void removePoint(Point p) {
        removePoint(p.x, p.y);
    }

    @Override
    public void removeValue(E e){
        data.values().remove(e);
    }

    @Override
    public boolean containsPoint(int x, int y) {
        return data.containsKey(new Point(x, y));
    }

    @Override
    public boolean containsPoint(Point p) {
        return containsPoint(p.x, p.y);
    }

    @Override
    public boolean containsValue(E e){
        return data.containsValue(e);
    }

    @Override
	public void clear(){
		data.clear();
	}

	@Override
	public Iterable<Point> points(){
        return () -> new Iterator<>() {
            final Iterator<Point> it = data.keySet().iterator();

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public Point next() {
                return new Point(it.next());
            }
        };
    }

    @Override
    public Iterable<Cell<E>> cells() {
        return () -> new Iterator<>() {
            final Iterator<Map.Entry<Point, E>> it = data.entrySet().iterator();

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public Cell<E> next(){
                Map.Entry<Point, E> entry = it.next();
                return new Cell<>(entry.getKey().x, entry.getKey().y, entry.getValue());
            }
        };
    }

	@Override
	public Iterator<E> iterator(){
		return data.values().iterator();
	}

    @Override
    public void compute(int x, int y, UnaryOperator<E> operator){
        data.compute(new Point(x, y), (k, e) -> operator.apply(e));
    }

    public Map<Point, E> toMap(){
        return new HashMap<>(data);
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder("[");
        for(Cell<E> cell : cells()){
            sb.append(cell.toString()).append(", ");
        }
        return sb.delete(sb.length() - 2, sb.length()).append(']').toString();
    }
}