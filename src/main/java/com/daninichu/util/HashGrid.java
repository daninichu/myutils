package com.daninichu.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;

public class HashGrid<E> extends AbstractGrid<E> implements Grid<E> {
	private final HashMap<Point, E> data;

	public HashGrid(){
        data = new HashMap<>();
    }

    public HashGrid(int initialCapacity){
        data = new HashMap<>(initialCapacity);
    }

    public HashGrid(int initialCapacity, float loadFactor){
        data = new HashMap<>(initialCapacity, loadFactor);
    }

	public HashGrid(Grid<? extends E> grid){
        data = new HashMap<>();
        setAll(grid);
	}

    @Override
    public E get(int x, int y){
        return data.get(new Point(x, y));
    }

	@Override
	public E get(Point p){
		return data.get(Objects.requireNonNull(p));
	}

    @Override
    public E set(int x, int y, E e){
        return data.put(new Point(x, y), e);
    }

	@Override
	public E set(Point p, E e){
        return data.put(Objects.requireNonNull(p), e);
	}

    @Override
    public void setAll(Grid<? extends E> grid){
        if(grid instanceof HashGrid<? extends E> hashGrid)
            data.putAll(hashGrid.data);
        else
            for(Cell<? extends E> cell : grid.cells())
                data.put(new Point(cell.x, cell.y), cell.value);
    }

    @Override
    public boolean removePoint(int x, int y){
        Point p = new Point(x, y);
        if(data.containsKey(p)){
            data.remove(p);
            return true;
        }
        return false;
    }

    @Override
    public boolean removePoint(Point p){
        if(data.containsKey(Objects.requireNonNull(p))){
            data.remove(p);
            return true;
        }
        return false;
    }

    @Override
    public boolean removeValue(E e){
        return data.values().remove(e);
    }

    @Override
    public boolean containsPoint(int x, int y){
        return data.containsKey(new Point(x, y));
    }

    @Override
    public boolean containsPoint(Point p){
        return data.containsKey(Objects.requireNonNull(p));
    }

    @Override
    public boolean containsValue(E e){
        return data.containsValue(e);
    }

    @Override
    public Point pointOf(E e){
        for(Map.Entry<Point, E> entry : data.entrySet()){
            if(entry.getValue().equals(e))
                return entry.getKey();
        }
        return null;
    }

    @Override
	public void clear(){
		data.clear();
	}

	@Override
	public Iterable<Point> points(){
        return data.keySet();
    }

    @Override
    public Iterable<Cell<E>> cells(){
        return () -> new Iterator<>(){
            final Iterator<Map.Entry<Point, E>> it = data.entrySet().iterator();

            @Override
            public boolean hasNext(){
                return it.hasNext();
            }

            @Override
            public Cell<E> next(){
                Map.Entry<Point, E> entry = it.next();
                return new Cell<>(entry.getKey(), entry.getValue());
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

    public HashMap<Point, E> toMap(){
        return new HashMap<>(data);
    }
}