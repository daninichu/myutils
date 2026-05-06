package com.daninichu.util;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;

public class DynamicQuadTree<T> {
    public static class Entry<T>{
        public final T element;
        ArrayList<Entry<T>> entries;
        double x, y, width, height;

        Entry(T element, ArrayList<Entry<T>> entries, double x, double y, double width, double height){
            this.element = element;
            this.entries = entries;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    /** @noinspection unchecked*/
    public final DynamicQuadTree<T>[] childTrees = new DynamicQuadTree[4];
    private double originX, originY, childW, childH;
    private final int depth, maxDepth;
    private final ArrayList<Entry<T>> entries = new ArrayList<>();

    public DynamicQuadTree(Rectangle2D bounds) {
        this(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(), 8);
    }

    public DynamicQuadTree(Rectangle2D bounds, int maxDepth) {
        this(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(), maxDepth);
    }

    public DynamicQuadTree(double x, double y, double width, double height) {
        this(x, y, width, height, 8);
    }

    public DynamicQuadTree(double x, double y, double width, double height, int maxDepth) {
        this(x, y, width, height, 0, maxDepth);
        if(width < 0 || height < 0)
            throw new IllegalArgumentException("width or height cannot be negative");
        if(maxDepth < 0)
            throw new IllegalArgumentException("maxDepth cannot be negative");
    }

    private DynamicQuadTree(double x, double y, double width, double height, int depth, int maxDepth) {
        this.depth = depth;
        this.maxDepth = maxDepth;
        resize(x, y, width, height);
    }

    public void add(T element, double x, double y, double width, double height) {
        if(depth != maxDepth){
            for(int i = 0; i < 4; i++){
                double childX = originX + (i % 2) * childW;
                double childY = originY + (i / 2) * childH;

                if(contains(childX, childY, childW, childH, x, y, width, height)){
                    DynamicQuadTree<T> tree = childTrees[i];
                    if(tree == null)
                        childTrees[i] = tree = new DynamicQuadTree<>(childX, childY, childW, childH, depth + 1, maxDepth);
                    tree.add(element, x, y, width, height);
                    return;
                }
            }
        }
        entries.add(new Entry<>(element, entries, x, y, width, height));
    }

    public void add(T element, Rectangle2D bounds){
        add(element, bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
    }

    public ArrayList<Entry<T>> search(double x, double y, double width, double height){
        ArrayList<Entry<T>> result = new ArrayList<>();
        search(x, y, width, height, result);
        return result;
    }

    public ArrayList<Entry<T>> search(Rectangle2D searchArea){
        ArrayList<Entry<T>> result = new ArrayList<>();
        search(searchArea.getX(), searchArea.getY(), searchArea.getWidth(), searchArea.getHeight(), result);
        return result;
    }

    public void search(double x, double y, double width, double height, Collection<Entry<T>> result){
        for(Entry<T> entry : entries){
            if(intersects(x, y, width, height, entry.x, entry.y, entry.width, entry.height))
                result.add(entry);
        }
        for(int i = 0; i < 4; i++){
            DynamicQuadTree<T> tree = childTrees[i];
            if(tree != null){
                double childX = originX + (i % 2) * childW;
                double childY = originY + (i / 2) * childH;

                if(contains(x, y, width, height, childX, childY, childW, childH))
                    tree.copyElements(result);
                else if(intersects(x, y, width, height, childX, childY, childW, childH))
                    tree.search(x, y, width, height, result);
            }
        }
    }

    public void search(Rectangle2D searchArea, Collection<Entry<T>> result){
        search(searchArea.getX(), searchArea.getY(), searchArea.getWidth(), searchArea.getHeight(), result);
    }

    private void copyElements(Collection<Entry<T>> result){
        result.addAll(entries);
        for(int i = 0; i < 4; i++){
            DynamicQuadTree<T> tree = childTrees[i];
            if(tree != null)
                tree.copyElements(result);
        }
    }

    public void remove(Entry<T> entry){
        entry.entries.remove(entry);
    }

    public void resize(double x, double y, double width, double height){
        if(width < 0 || height < 0)
            throw new IllegalArgumentException("width or height cannot be negative");
        clear();

        this.originX = x;
        this.originY = y;
        this.childW = width / 2;
        this.childH = height / 2;
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
            DynamicQuadTree<T> tree = childTrees[i];
            if(tree != null)
                size += tree.size();
        }
        return size;
    }

    public Rectangle2D.Double getBounds() {
        return new Rectangle2D.Double(originX, originY, childW * 2, childH * 2);
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