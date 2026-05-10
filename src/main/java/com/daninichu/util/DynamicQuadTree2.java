package com.daninichu.util;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class DynamicQuadTree2<T> {
    public static class Entry<T>{
        public final T element;
        DynamicQuadTree2<T> tree;
        ArrayList<DynamicQuadTree2.Entry<T>> entries;
        double x, y, width, height;

        Entry(T element, DynamicQuadTree2<T> tree, double x, double y, double width, double height){
            this.element = element;
            this.tree = tree;
            entries=tree.entries;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    /** @noinspection unchecked*/
    public final DynamicQuadTree2<T>[] childTrees = new DynamicQuadTree2[4];
    private DynamicQuadTree2<T> parent;
    private double originX, originY, childW, childH;
    private final int depth, maxDepth;
    private final ArrayList<Entry<T>> entries = new ArrayList<>();

    public DynamicQuadTree2(Rectangle2D bounds) {
        this(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(), 8);
    }

    public DynamicQuadTree2(Rectangle2D bounds, int maxDepth) {
        this(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(), maxDepth);
    }

    public DynamicQuadTree2(double x, double y, double width, double height) {
        this(x, y, width, height, 8);
    }

    public DynamicQuadTree2(double x, double y, double width, double height, int maxDepth) {
        this(x, y, width, height, 0, maxDepth);
        if(maxDepth < 0)
            throw new IllegalArgumentException("maxDepth cannot be negative");
    }

    private DynamicQuadTree2(double x, double y, double width, double height, int depth, int maxDepth) {
        this.depth = depth;
        this.maxDepth = maxDepth;
        resize(x, y, width, height);
    }

    public Entry<T> add(T element, double x, double y, double width, double height) {
        if(depth != maxDepth){
            for(int i = 0; i < 4; i++){
                double childX = originX + (i % 2) * childW;
                double childY = originY + (i / 2) * childH;

                if(contains(childX, childY, childW, childH, x, y, width, height)){
                    DynamicQuadTree2<T> tree = childTrees[i];
                    if(tree == null){
                        childTrees[i] = tree = new DynamicQuadTree2<>(childX, childY, childW, childH, depth + 1, maxDepth);
                        tree.parent = this;
                    }
                    return tree.add(element, x, y, width, height);
                }
            }
        }
        Entry<T> entry = new Entry<>(element, this, x, y, width, height);
        entries.add(entry);
        return entry;
    }

    public Entry<T> add(T element, Rectangle2D bounds){
        return add(element, bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
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
            DynamicQuadTree2<T> tree = childTrees[i];
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
            DynamicQuadTree2<T> tree = childTrees[i];
            if(tree != null)
                tree.copyElements(result);
        }
    }

    public boolean remove(T element){
        for(int i = 0, n = entries.size(); i < n; i++){
            if(entries.get(i).element.equals(element)){
                Collections.swap(entries, i, n-1);
                entries.remove(n-1);
                return true;
            }
        }
        for(int i = 0; i < 4; i++){
            DynamicQuadTree2<T> tree = childTrees[i];
            if(tree != null && tree.remove(element)){
                return true;
            }
        }
        return false;
    }

    public boolean removeAndCollapse(T element){
        for(int i = 0, n = entries.size(); i < n; i++){
            if(entries.get(i).element.equals(element)){
                Collections.swap(entries, i, n-1);
                entries.remove(n-1);
                return true;
            }
        }
        for(int i = 0; i < 4; i++){
            DynamicQuadTree2<T> tree = childTrees[i];
            if(tree != null && tree.removeAndCollapse(element)){
                if(tree.entries.isEmpty()){
                    for(int j = 0; j < 4; j++){
                        if(tree.childTrees[j] != null){
                            return true;
                        }
                    }
                    childTrees[i] = null;
                }
                return true;
            }
        }
        return false;
    }

    public boolean remove(Entry<T> entry){
        return entry.tree.entries.remove(entry);
    }

    public boolean removeAndCollapse(Entry<T> entry){
        DynamicQuadTree2<T> tree = entry.tree;
        if(!tree.entries.remove(entry)){
            return false;
        }
        while(tree != this && tree.entries.isEmpty()){
            for(int i = 0; i < 4; i++){
                if(tree.childTrees[i] != null){
                    return true;
                }
            }
            DynamicQuadTree2<T> parent = tree.parent;
            DynamicQuadTree2<T>[] parentChildTrees = parent.childTrees;
            for(int i = 0; i < 4; i++){
                if(parentChildTrees[i] == tree){
                    parentChildTrees[i] = null;
                    tree = parent;
                    break;
                }
            }
        }
        return true;
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
            DynamicQuadTree2<T> tree = childTrees[i];
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