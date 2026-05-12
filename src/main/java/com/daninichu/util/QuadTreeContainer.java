package com.daninichu.util;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class QuadTreeContainer<T> implements Iterable<T> {
    private Quadrant<T> root;
    private int size;
    private double boundX, boundY, boundWidth, boundHeight;

    public QuadTreeContainer(Rectangle2D bounds) {
        this(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(), 8);
    }

    public QuadTreeContainer(Rectangle2D bounds, int maxDepth) {
        this(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(), maxDepth);
    }

    public QuadTreeContainer(double x, double y, double width, double height) {
        this(x, y, width, height, 8);
    }

    public QuadTreeContainer(double x, double y, double width, double height, int maxDepth) {
        checkNonNegativity(width, height);
        if(maxDepth < 0){
            throw new IllegalArgumentException("maxDepth cannot be negative");
        }
        boundX = x;
        boundY = y;
        boundWidth = width;
        boundHeight = height;
        root = new Quadrant<>(x, y, width, height, 0, maxDepth);
    }

    private static void checkNonNegativity(double width, double height){
        if(width < 0 || height < 0)
            throw new IllegalArgumentException("width or height cannot be negative");
    }

    public Entry<T> add(T value, double x, double y, double width, double height){
        checkNonNegativity(width, height);
        size++;
        return root.add(value, x, y, width, height);
    }

    public Entry<T> add(T value, Rectangle2D bounds){
        return add(value, bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
    }

    public ArrayList<Entry<T>> search(double x, double y, double width, double height){
        checkNonNegativity(width, height);
        ArrayList<Entry<T>> result = new ArrayList<>();
        root.search(x, y, width, height, result);
        return result;
    }

    public ArrayList<Entry<T>> search(Rectangle2D searchArea){
        return search(searchArea.getX(), searchArea.getY(), searchArea.getWidth(), searchArea.getHeight());
    }

    public void search(double x, double y, double width, double height, Collection<Entry<T>> result){
        checkNonNegativity(width, height);
        root.search(x, y, width, height, result);
    }

    public void search(Rectangle2D searchArea, Collection<Entry<T>> result){
        search(searchArea.getX(), searchArea.getY(), searchArea.getWidth(), searchArea.getHeight(), result);
    }

    public boolean remove(T value){
        Entry<T> entry = root.removeEntry(value);
        if(entry != null){
            size--;
            entry.quadrant = null;
            return true;
        }
        return false;
    }

    public boolean removeAndCollapse(T value){
        Entry<T> entry = root.removeEntry(value);
        if(entry != null){
            collapse(entry.quadrant);
            size--;
            entry.quadrant = null;
            return true;
        }
        return false;
    }

    public boolean remove(Entry<T> entry){
        Quadrant<T> tree = entry.quadrant;
        if(tree != null && tree.entries.remove(entry)){
            size--;
            entry.quadrant = null;
            return true;
        }
        return false;
    }

    public boolean removeAndCollapse(Entry<T> entry){
        Quadrant<T> tree = entry.quadrant;
        if(tree.entries.remove(entry)){
            collapse(tree);
            size--;
            entry.quadrant = null;
            return true;
        }
        return false;
    }

    public void resize(double x, double y, double width, double height){
        checkNonNegativity(width, height);
        boundX = x;
        boundY = y;
        boundWidth = width;
        boundHeight = height;
        clear();
    }

    public void clear(){
        size = 0;
        root.clear();
        root = new Quadrant<>(boundX, boundY, boundWidth, boundHeight, 0, root.maxDepth);
    }

    public int size(){
        return size;
    }

    public boolean isEmpty(){
        return size == 0;
    }

    public Rectangle2D.Double getBounds() {
        return new Rectangle2D.Double(boundX, boundY, boundWidth, boundHeight);
    }

    public ArrayList<Entry<T>> entries(){
        ArrayList<Entry<T>> entries = new ArrayList<>(size);
        root.copyEntries(entries);
        return entries;
    }

    public ArrayList<T> values(){
        ArrayList<T> values = new ArrayList<>(size);
        root.copyValues(values);
        return values;
    }

    @Override
    public Iterator<T> iterator(){
        return values().iterator();
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

    private static void collapse(Quadrant<?> tree){
        Quadrant<?> parent = tree.parent;
        while(parent != null && tree.entries.isEmpty()){
            for(int i = 0; i < 4; i++){
                if(tree.children[i] != null){
                    return;
                }
            }
            Quadrant<?>[] parentChildren = parent.children;
            for(int i = 0; i < 4; i++){
                if(parentChildren[i] == tree){
                    parentChildren[i] = null;
                    tree = parent;
                    parent = parent.parent;
                    break;
                }
            }
        }
    }

    public static class Entry<T>{
        public final T value;
        public final double x, y, width, height;
        private Quadrant<T> quadrant;

        private Entry(T value, double x, double y, double width, double height, Quadrant<T> quadrant){
            this.value = value;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.quadrant = quadrant;
        }
    }

    private static class Quadrant<T>{
        double originX, originY, childW, childH;
        int depth, maxDepth;

        @SuppressWarnings("unchecked")
        Quadrant<T>[] children = new Quadrant[4];
        Quadrant<T> parent;
        ArrayList<Entry<T>> entries = new ArrayList<>();

        Quadrant(double x, double y, double width, double height, int depth, int maxDepth){
            this.originX = x;
            this.originY = y;
            this.childW = width / 2;
            this.childH = height / 2;
            this.depth = depth;
            this.maxDepth = maxDepth;
        }

        Entry<T> add(T value, double x, double y, double w, double h){
            if(depth != maxDepth){
                for(int i = 0; i < 4; i++){
                    double childX = originX + (i % 2) * childW;
                    double childY = originY + (i / 2) * childH;

                    if(contains(childX, childY, childW, childH, x, y, w, h)){
                        Quadrant<T> child = children[i];
                        if(child == null){
                            children[i] = child = new Quadrant<>(childX, childY, childW, childH, depth + 1, maxDepth);
                            child.parent = this;
                        }
                        return child.add(value, x, y, w, h);
                    }
                }
            }
            Entry<T> entry = new Entry<>(value, x, y, w, h, this);
            entries.add(entry);
            return entry;
        }

        void search(double x, double y, double w, double h, Collection<Entry<T>> result){
            for(Entry<T> entry : entries){
                if(intersects(x, y, w, h, entry.x, entry.y, entry.width, entry.height)){
                    result.add(entry);
                }
            }
            for(int i = 0; i < 4; i++){
                Quadrant<T> child = children[i];
                if(child != null){
                    double childX = originX + (i % 2) * childW;
                    double childY = originY + (i / 2) * childH;

                    if(contains(x, y, w, h, childX, childY, childW, childH))
                        child.copyEntries(result);
                    else if(intersects(x, y, w, h, childX, childY, childW, childH))
                        child.search(x, y, w, h, result);
                }
            }
        }

        void copyEntries(Collection<Entry<T>> result){
            result.addAll(entries);
            for(int i = 0; i < 4; i++){
                Quadrant<T> child = children[i];
                if(child != null){
                    child.copyEntries(result);
                }
            }
        }

        void copyValues(Collection<T> result){
            for(Entry<T> entry : entries){
                result.add(entry.value);
            }
            for(int i = 0; i < 4; i++){
                Quadrant<T> child = children[i];
                if(child != null){
                    child.copyValues(result);
                }
            }
        }

        Entry<T> removeEntry(T value){
            for(int i = 0, n = entries.size(); i < n; i++){
                Entry<T> entry = entries.get(i);
                if(entry.value.equals(value)){
                    entries.set(i, entries.set(n-1, entry));
                    return entries.remove(n-1);
                }
            }
            for(int i = 0; i < 4; i++){
                Quadrant<T> child = children[i];
                if(child != null){
                    Entry<T> entry = child.removeEntry(value);
                    if(entry != null){
                        return entry;
                    }
                }
            }
            return null;
        }

        void clear(){
            for(Entry<T> entry : entries){
                entry.quadrant = null;
            }
            for(int i = 0; i < 4; i++){
                Quadrant<T> child = children[i];
                if(child != null){
                    child.clear();
                }
            }
        }
    }
}