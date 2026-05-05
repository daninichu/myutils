package com.daninichu.util;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class QuadTree2<T> {
    private class Entry{
        T element;
        double x, y, width, height;

        Entry(T element, double x, double y, double width, double height){
            this.element = element;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    /** @noinspection unchecked*/
    public final QuadTree2<T>[] childTrees = new QuadTree2[4];
    private final Rectangle2D.Double[] childBounds = new Rectangle2D.Double[4];
    private final int depth, maxDepth;
    private final List<Entry> entries = new ArrayList<>();

    public QuadTree2(double x, double y, double width, double height) {
        this(new Rectangle2D.Double(x, y, width, height), 0, 8);
    }

    public QuadTree2(double x, double y, double width, double height, int maxDepth) {
        this(new Rectangle2D.Double(x, y, width, height), 0, maxDepth);
    }

    public QuadTree2(Rectangle2D bounds) {
        this(bounds, 0, 8);
    }

    public QuadTree2(Rectangle2D bounds, int maxDepth) {
        this(bounds, 0, maxDepth);
    }

    private QuadTree2(Rectangle2D bounds, int depth, int maxDepth) {
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

    public void add(T element, double x, double y, double width, double height) {
        if(depth != maxDepth){
            for(int i = 0; i < 4; i++){
                Rectangle2D.Double bound = childBounds[i];
                if(bound.contains(x, y, width, height)){
                    QuadTree2<T> tree = childTrees[i];
                    if(tree == null)
                        tree = childTrees[i] = new QuadTree2<>(bound, depth + 1, maxDepth);
                    tree.add(element, x, y, width, height);
                    return;
                }
            }
        }
        entries.add(new Entry(element, x, y, width, height));
    }

    public void add(T element, Rectangle2D bounds){
        add(element, bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
    }

    public ArrayList<T> search(Rectangle2D searchArea){
        ArrayList<T> result = new ArrayList<>();
        search(searchArea, result);
        return result;
    }

    public void search(double x, double y, double width, double height, Collection<T> result){
        for(Entry entry : entries){
            if(intersects(x, y, width, height, entry.x, entry.y, entry.width, entry.height))
                result.add(entry.element);
        }
        for(int i = 0; i < 4; i++){
            QuadTree2<T> tree = childTrees[i];
            if(tree != null){
                Rectangle2D.Double bound = childBounds[i];
                if(contains(x, y, width, height, bound.x, bound.y, bound.width, bound.height))
                    tree.copyElements(result);
                else if(intersects(x, y, width, height, bound.x, bound.y, bound.width, bound.height))
                    tree.search(x, y, width, height, result);
            }
        }
    }

    public void search(Rectangle2D searchArea, Collection<T> result){
        search(searchArea.getX(), searchArea.getY(), searchArea.getWidth(), searchArea.getHeight(), result);
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

    private static boolean intersects(
            double x1, double y1, double w1, double h1,
            double x2, double y2, double w2, double h2
    ){
        return x2 + w2 > x1 && y2 + h2 > y1 && x2 < x1 + w1 && y2 < y1 + h1;
    }

    private static boolean contains(
            double x1, double y1, double w1, double h1,
            double x2, double y2, double w2, double h2
    ){
        return x2 >= x1 && y2 >= y1 && x2 + w2 <= x1 + w1 && y2 + h2 <= y1 + h1;
    }
}