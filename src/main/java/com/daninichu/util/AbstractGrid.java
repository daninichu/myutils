package com.daninichu.util;

import java.util.Iterator;
import java.util.function.UnaryOperator;

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
    public void setAll(Grid<? extends E> grid){
        for(Cell<? extends E> cell : grid.cells())
            set(cell.x, cell.y, cell.value);
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
    public Point pointOf(E e){
        if(e == null){
            for(Cell<E> cell : cells()){
                if(cell.value == null)
                    return new Point(cell.x, cell.y);
            }
        } else{
            for(Cell<E> cell : cells()){
                if(e.equals(cell.value))
                    return new Point(cell.x, cell.y);
            }
        }
        return null;
    }

    @Override
    public void compute(int x, int y, UnaryOperator<E> operator){
        set(x, y, operator.apply(get(x, y)));
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

    public static void main(String[] args){
        Grid grid = new ArrayGrid(2, 2);
        grid.set(1, 0, 1);
        grid.set(2, 0, 0);
        grid.set(1, 1, 3);
        grid.set(0, 1, 2);
        System.out.println(grid);
    }
}