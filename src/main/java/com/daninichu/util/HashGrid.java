package com.daninichu.util;

import java.awt.Point;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
        if (grid instanceof HashGrid<E> hashGrid)
            data = new HashMap<>(hashGrid.data);
        else{
            data = new HashMap<>();
            for(Cell<E> cell : grid.cells())
                data.put(cell.point(), cell.value());
        }
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
    public void remove(int x, int y) {
        data.remove(new Point(x, y));
    }

    @Override
    public void remove(Point p) {
        remove(p.x, p.y);
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
                return new Cell<>(new Point(entry.getKey()), entry.getValue());
            }
        };
    }

	@Override
	public Iterator<E> iterator(){
		return data.values().iterator();
	}

    public Map<Point, E> toMap(){
        return new HashMap<>(data);
    }
}