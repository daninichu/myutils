package com.daninichu.util;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class QuadTreeContainer<T> {
    private class Entry{
        T element;
        Rectangle2D bounds;

        Entry(T element, Rectangle2D bounds){
            this.element = element;
            this.bounds = bounds;
        }
    }

    /** @noinspection unchecked*/
    public QuadTree<Integer> childTrees;
    private final List<T> entries = new ArrayList<>();

//    public QuadTreeContainer(double x, double y, double width, double height) {
//        this(new Rectangle2D.Double(x, y, width, height), 0, 8);
//    }
//
//    public QuadTreeContainer(double x, double y, double width, double height, int maxDepth) {
//        this(new Rectangle2D.Double(x, y, width, height), 0, maxDepth);
//    }

    public QuadTreeContainer(Rectangle2D bounds) {
        childTrees = new QuadTree<>(bounds);
    }

//    public QuadTreeContainer(Rectangle2D bounds, int maxDepth) {
//        this(bounds, 0, maxDepth);
//    }
//
//    private QuadTreeContainer(Rectangle2D bounds, int depth, int maxDepth) {
//        if(maxDepth < 0){
//            throw new IllegalArgumentException("maxDepth can't be negative");
//        }
//        this.bounds = new Rectangle2D.Double();
//        this.depth = depth;
//        this.maxDepth = maxDepth;
//
//        for(int i = 0; i < 4; i++){
//            childBounds[i] = new Rectangle2D.Double();
//        }
//        resize(bounds);
//    }

    public void add(T element, Rectangle2D bounds){
        childTrees.add(entries.size(), bounds);
        entries.add(element);
    }

    public ArrayList<T> search(Rectangle2D searchArea){
        ArrayList<T> result = new ArrayList<>();
        for(int i : childTrees.search(searchArea)){
            result.add(entries.get(i));
        }
        return result;
    }

    public void search(Rectangle2D searchArea, Collection<T> result){
        for(int i : childTrees.search(searchArea)){
            result.add(entries.get(i));
        }
    }


//    public void resize(Rectangle2D bounds){
//        clear();
//        this.bounds.setRect(bounds);
//
//        double x = bounds.getX();
//        double y = bounds.getY();
//        double w = bounds.getWidth() / 2.0;
//        double h = bounds.getHeight() / 2.0;
//        childBounds[0].setRect(x, y, w, h);
//        childBounds[1].setRect(x + w, y, w, h);
//        childBounds[2].setRect(x, y + h, w, h);
//        childBounds[3].setRect(x + w, y + h, w, h);
//    }
//
//    public void clear(){
//        entries.clear();
//        for(int i = 0; i < 4; i++){
//            childTrees[i] = null;
//        }
//    }

//    public int size(){
//        int size = entries.size();
//        for(int i = 0; i < 4; i++){
//            if(childTrees[i] != null){
//                size += childTrees[i].size();
//            }
//        }
//        return size;
//    }
//
//    public Rectangle2D getBounds() {
//        return bounds.getBounds2D();
//    }
}