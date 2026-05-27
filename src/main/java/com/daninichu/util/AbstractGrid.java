package com.daninichu.util;

import java.util.Iterator;

public abstract class AbstractGrid<E> implements Grid<E> {
    @Override
    public void setAll(Grid<? extends E> grid){
        for(Cell<? extends E> cell : grid.cells())
            set(cell.x, cell.y, cell.value);
    }

    @Override
    public String toString(){
        Iterator<Cell<E>> it = cells().iterator();
        if(!it.hasNext()){
            return "[]";
        }
        StringBuilder sb = new StringBuilder().append('[');
        while(true){
            Cell<E> cell = it.next();
            if(cell.value == this){
                sb.append('(').append(cell.x).append(',').append(cell.y).append(")=(this Grid)");
            } else{
                sb.append(cell);
            }
            if(!it.hasNext()){
                return sb.append(']').toString();
            }
            sb.append(',').append(' ');
        }
    }
}