package com.daninichu.util;

import java.awt.Point;
import java.util.Iterator;
import java.util.function.Function;

public abstract class AbstractGrid<E> implements Grid<E>{
    @Override
    public E get(Point p){
        return get(p.x, p.y);
    }

    @Override
    public E getOrDefault(int x, int y, E defaultValue){
        E e = get(x, y);
        return e == null? defaultValue : e;
    }

    @Override
    public E getOrDefault(Point p, E defaultValue){
        E e = get(p.x, p.y);
        return e == null? defaultValue : e;
    }

    @Override
    public E set(Point p, E e){
        return set(p.x, p.y, e);
    }

    @Override
    public E getOrSet(Point p, E e){
        return getOrSet(p.x, p.y, e);
    }

    @Override
    public E removePoint(Point p){
        return removePoint(p.x, p.y);
    }

    @Override
    public boolean containsPoint(Point p){
        return containsPoint(p.x, p.y);
    }

    @Override
    public E compute(Point p, Function<? super E, ? extends E> function){
        return compute(p.x, p.y, function);
    }

    @Override
    public void setAll(Grid<? extends E> grid){
        for(Cell<? extends E> cell : grid.cells())
            set(cell.x, cell.y, cell.value);
    }

    @Override
    public String toString(){
        Iterator<Cell<E>> it = cells().iterator();
        if(!it.hasNext()){
            return "{}";
        }
        StringBuilder sb = new StringBuilder().append('{');
        while(true){
            Cell<E> cell = it.next();
            if(cell.value == this){
                sb.append('(').append(cell.x).append(',').append(cell.y).append(")=(this Grid)");
            } else{
                sb.append(cell);
            }
            if(!it.hasNext()){
                return sb.append('}').toString();
            }
            sb.append(',').append(' ');
        }
    }
}
