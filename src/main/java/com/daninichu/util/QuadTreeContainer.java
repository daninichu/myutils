package com.daninichu.util;

import java.awt.geom.Rectangle2D;
import java.util.*;

public class QuadTreeContainer<T> implements Iterable<T> {
    private final Quadrant<T> root;

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
        if(width < 0 || height < 0)
            throw new IllegalArgumentException("width or height cannot be negative");
        if(maxDepth < 0)
            throw new IllegalArgumentException("maxDepth cannot be negative");
        root = new Quadrant<>(x, y, width, height, 0, maxDepth);
    }

    public Entry<T> add(T element, double x, double y, double w, double h){
        return root.add(element, x, y, w, h);
    }

    public Entry<T> add(T element, Rectangle2D bounds){
        return root.add(element, bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
    }

    public ArrayList<Entry<T>> search(Rectangle2D searchArea){
        ArrayList<Entry<T>> result = new ArrayList<>();
        root.search(searchArea.getX(), searchArea.getY(), searchArea.getWidth(), searchArea.getHeight(), result);
        return result;
    }

    public void search(Rectangle2D searchArea, Collection<Entry<T>> result){
        root.search(searchArea.getX(), searchArea.getY(), searchArea.getWidth(), searchArea.getHeight(), result);
    }

    public boolean remove(T element){
        return root.removeEntry(element) != null;
    }

    public boolean removeAndCollapse(T element){
        Entry<T> entry = root.removeEntry(element);
        if(entry != null){
            collapse(entry.tree);
            return true;
        }
        return false;
    }

    public boolean remove(Entry<T> entry){
        return entry.tree.entries.remove(entry);
    }

    public boolean removeAndCollapse(Entry<T> entry){
        Quadrant<T> tree = entry.tree;
        if(tree.entries.remove(entry)){
            collapse(tree);
            return true;
        }
        return false;
    }

    public void clear(){
        root.clear();
    }

    @Override
    public Iterator<T> iterator(){
        List<T> entries = new ArrayList<>();
        root.copyElements(entries);
        return entries.iterator();
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
                if(tree.childTrees[i] != null){
                    return;
                }
            }
            Quadrant<?>[] parentChildTrees = parent.childTrees;
            for(int i = 0; i < 4; i++){
                if(parentChildTrees[i] == tree){
                    parentChildTrees[i] = null;
                    tree = parent;
                    parent = parent.parent;
                    break;
                }
            }
        }
    }

    public static class Entry<T>{
        public final T element;
        Quadrant<T> tree;
        double x, y, width, height;

        Entry(T element, Quadrant<T> tree, double x, double y, double width, double height){
            this.element = element;
            this.tree = tree;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    static class Quadrant<T>{
        /** @noinspection unchecked*/
        public final Quadrant<T>[] childTrees = new Quadrant[4];
        private Quadrant<T> parent;
        private double originX, originY, childW, childH;
        private final int depth, maxDepth;
        private final ArrayList<Entry<T>> entries = new ArrayList<>();

        private Quadrant(double x, double y, double width, double height, int depth, int maxDepth){
            this.depth = depth;
            this.maxDepth = maxDepth;
            resize(x, y, width, height);
        }

        public Entry<T> add(T element, double x, double y, double width, double height){
            if(depth != maxDepth){
                for(int i = 0; i < 4; i++){
                    double childX = originX + (i % 2) * childW;
                    double childY = originY + (i / 2) * childH;

                    if(contains(childX, childY, childW, childH, x, y, width, height)){
                        Quadrant<T> tree = childTrees[i];
                        if(tree == null){
                            childTrees[i] = tree = new Quadrant<>(childX, childY, childW, childH, depth + 1, maxDepth);
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
                Quadrant<T> tree = childTrees[i];
                if(tree != null){
                    double childX = originX + (i % 2) * childW;
                    double childY = originY + (i / 2) * childH;

                    if(contains(x, y, width, height, childX, childY, childW, childH))
                        tree.copyEntries(result);
                    else if(intersects(x, y, width, height, childX, childY, childW, childH))
                        tree.search(x, y, width, height, result);
                }
            }
        }

        public void search(Rectangle2D searchArea, Collection<Entry<T>> result){
            search(searchArea.getX(), searchArea.getY(), searchArea.getWidth(), searchArea.getHeight(), result);
        }

        private void copyEntries(Collection<Entry<T>> result){
            result.addAll(entries);
            for(int i = 0; i < 4; i++){
                Quadrant<T> tree = childTrees[i];
                if(tree != null)
                    tree.copyEntries(result);
            }
        }

        private void copyElements(Collection<T> result){
            for(Entry<T> entry : entries){
                result.add(entry.element);
            }
            for(int i = 0; i < 4; i++){
                Quadrant<T> tree = childTrees[i];
                if(tree != null)
                    tree.copyElements(result);
            }
        }

        public Entry<T> removeEntry(T element){
            for(int i = 0, n = entries.size(); i < n; i++){
                if(entries.get(i).element.equals(element)){
                    Collections.swap(entries, i, n - 1);
                    return entries.remove(n - 1);
                }
            }
            for(int i = 0; i < 4; i++){
                Quadrant<T> tree = childTrees[i];
                if(tree != null){
                    Entry<T> entry = tree.removeEntry(element);
                    if(entry != null){
                        return entry;
                    }
                }
            }
            return null;
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

        public Rectangle2D.Double getBounds(){
            return new Rectangle2D.Double(originX, originY, childW * 2, childH * 2);
        }
    }
}