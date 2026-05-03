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
    private final QuadTree<T>[] childTrees = new QuadTree[4];
    private final Rectangle2D bounds;
    private final Rectangle2D[] childBounds = new Rectangle2D[4];
    private final int depth, maxDepth;
    private final List<Entry> entries = new ArrayList<>();

    public QuadTree(Rectangle2D bounds) {
        this(bounds, 0, 8);
    }

    public QuadTree(Rectangle2D bounds, int maxDepth) {
        this(bounds, 0, maxDepth);
    }

    private QuadTree(Rectangle2D bounds, int depth, int maxDepth) {
        this.bounds = new Rectangle2D.Double();
        this.depth = depth;
        this.maxDepth = maxDepth;
        for(int i = 0; i < 4; i++){
            childBounds[i] = new Rectangle2D.Double();
        }
        resize(bounds);
    }

    public void add(T element, Rectangle2D bounds){
        for(int i = 0; i < 4; i++){
            if(childBounds[i].contains(bounds)){
                if(depth != maxDepth){
                    if(childTrees[i] == null){
                        childTrees[i] = new QuadTree<>(childBounds[i], depth + 1);
                    }
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
            if(entry.bounds.intersects(searchArea)){
                result.add(entry.element);
            }
        }
        for(int i = 0; i < 4; i++){
            if(childTrees[i] != null){
                if(searchArea.contains(childBounds[i])){
                    childTrees[i].copyElements(result);
                } else if(searchArea.intersects(childBounds[i])){
                    childTrees[i].search(searchArea, result);
                }
            }
        }
    }

    private void copyElements(Collection<T> result){
        for(Entry entry : entries){
            result.add(entry.element);
        }
        for(int i = 0; i < 4; i++){
            if(childTrees[i] != null){
                childTrees[i].copyElements(result);
            }
        }
    }

    public void resize(Rectangle2D bounds){
        clear();
        this.bounds.setRect(bounds);

        double x = bounds.getX();
        double y = bounds.getY();
        double w = bounds.getWidth() / 2.0;
        double h = bounds.getHeight() / 2.0;
        childBounds[0].setRect(x, y, w, h);
        childBounds[1].setRect(x + w, y, w, h);
        childBounds[2].setRect(x, y + h, w, h);
        childBounds[3].setRect(x + w, y + h, w, h);
    }

    public void clear(){
        entries.clear();
        for(int i = 0; i < 4; i++){
            if(childTrees[i] != null){
                childTrees[i].clear();
            }
            childTrees[i] = null;
        }
    }

    public int size(){
        int size = entries.size();
        for(int i = 0; i < 4; i++){
            if(childTrees[i] != null){
                size += childTrees[i].size();
            }
        }
        return size;
    }

    public Rectangle2D getBounds() {
        return bounds.getBounds2D();
    }
}