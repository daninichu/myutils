package com.daninichu.util;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class QuadTree<T> {
    private class Entry{
        T element;
        Rectangle2D bounds;

        Entry(T element, Rectangle2D bounds){
            this.element = element;
            this.bounds = bounds;
        }
    }

    /** @noinspection unchecked*/
    public final QuadTree<T>[] childTrees = new QuadTree[4];
    private final Rectangle2D.Double[] childBounds = new Rectangle2D.Double[4];
    private final int depth, maxDepth;
    private final List<Entry> entries = new ArrayList<>();

    public QuadTree(double x, double y, double width, double height) {
        this(new Rectangle2D.Double(x, y, width, height), 0, 8);
    }

    public QuadTree(double x, double y, double width, double height, int maxDepth) {
        this(new Rectangle2D.Double(x, y, width, height), 0, maxDepth);
    }

    public QuadTree(Rectangle2D bounds) {
        this(bounds, 0, 8);
    }

    public QuadTree(Rectangle2D bounds, int maxDepth) {
        this(bounds, 0, maxDepth);
    }

    private QuadTree(Rectangle2D bounds, int depth, int maxDepth) {
        if(maxDepth < 0){
            throw new IllegalArgumentException("maxDepth can't be negative");
        }
        this.depth = depth;
        this.maxDepth = maxDepth;

        for(int i = 0; i < 4; i++){
            childBounds[i] = new Rectangle2D.Double();
        }
        resize(bounds);
    }

    public void add(T element, Rectangle2D bounds){
        if(depth != maxDepth){
            for(int i = 0; i < 4; i++){
                if(childBounds[i].contains(bounds)){
                    if(childTrees[i] == null)
                        childTrees[i] = new QuadTree<>(childBounds[i], depth + 1, maxDepth);
                    childTrees[i].add(element, bounds);
                    return;
                }
            }
        }
        entries.add(new Entry(element, bounds));
    }

    public ArrayList<T> search(Rectangle2D searchArea){
        ArrayList<T> result = new ArrayList<>();
        search(searchArea, result);
        return result;
    }

    public void search(Rectangle2D searchArea, Collection<T> result){
        for(Entry entry : entries){
            if(entry.bounds.intersects(searchArea))
                result.add(entry.element);
        }
        for(int i = 0; i < 4; i++){
            if(childTrees[i] != null){
                if(searchArea.contains(childBounds[i]))
                    childTrees[i].copyElements(result);
                else if(searchArea.intersects(childBounds[i]))
                    childTrees[i].search(searchArea, result);
            }
        }
    }

    private void copyElements(Collection<T> result){
        for(Entry entry : entries){
            result.add(entry.element);
        }
        for(int i = 0; i < 4; i++){
            if(childTrees[i] != null)
                childTrees[i].copyElements(result);
        }
    }

    public void resize(double x, double y, double width, double height){
        clear();

        double w = width / 2.0;
        double h = height / 2.0;
        childBounds[0].setRect(x, y, w, h);
        childBounds[1].setRect(x + w, y, w, h);
        childBounds[2].setRect(x, y + h, w, h);
        childBounds[3].setRect(x + w, y + h, w, h);
    }

    public void resize(Rectangle2D bounds){
        resize(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
    }

    public void clear(){
        entries.clear();
        for(int i = 0; i < 4; i++){
            childTrees[i] = null;
        }
    }

    public int size(){
        int size = entries.size();
        for(int i = 0; i < 4; i++){
            if(childTrees[i] != null)
                size += childTrees[i].size();
        }
        return size;
    }

    public Rectangle2D.Double getBounds() {
        Rectangle2D.Double childBound = childBounds[0];
        double x = childBound.x;
        double y = childBound.y;
        double w = childBound.width * 2;
        double h = childBound.height * 2;
        return new Rectangle2D.Double(x, y, w, h);
    }
}