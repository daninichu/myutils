package com.daninichu.util;

import java.awt.geom.Rectangle2D;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class QuadTreeContainer<T> {
    private final Quad root;

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

    private final int maxDepth;

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
        this.maxDepth = maxDepth;
        root = new Quad(x, y, width, height, 0);
    }

    class Quad{
        /** @noinspection unchecked*/
        public Quad[] childTrees;
        private final Rectangle2D.Double[] childBounds = new Rectangle2D.Double[4];
        private final int depth;
        private final List<Entry> entries = new ArrayList<>();

        private Quad(double x, double y, double width, double height, int depth) {
            childTrees = (Quad[]) Array.newInstance(Quad.class, 4);
//            childTrees = (Quad[]) new Object[1];
            this.depth = depth;

            for(int i = 0; i < 4; i++){
                childBounds[i] = new Rectangle2D.Double();
            }
            resize(x, y, width, height);
        }

        public void add(T element, double x, double y, double width, double height) {
            if(depth != maxDepth){
                for(int i = 0; i < 4; i++){
                    Rectangle2D.Double bound = childBounds[i];
                    double bx = bound.x;
                    double by = bound.y;
                    double bw = bound.width;
                    double bh = bound.height;

                    if(contains(bx, by, bw, bh, x, y, width, height)){
                        Quad tree = childTrees[i];
                        if(tree == null)
                            tree = childTrees[i] = new Quad(bx, by, bw, bh, depth + 1);
                        tree.add(element, x, y, width, height);
                        return;
                    }
                }
            }
            entries.add(new Entry(element, x, y, width, height));
        }

        public void search(double x, double y, double width, double height, Collection<T> result){
            for(Entry entry : entries){
                if(intersects(x, y, width, height, entry.x, entry.y, entry.width, entry.height))
                    result.add(entry.element);
            }
            for(int i = 0; i < 4; i++){
                Quad tree = childTrees[i];
                if(tree == null)
                    continue;
                Rectangle2D.Double bound = childBounds[i];
                double bx = bound.x;
                double by = bound.y;
                double bw = bound.width;
                double bh = bound.height;

                if(contains(x, y, width, height, bx, by, bw, bh))
                    tree.copyElements(result);
                else if(intersects(x, y, width, height, bx, by, bw, bh))
                    tree.search(x, y, width, height, result);
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

    public void add(T element, Rectangle2D bounds){
        root.add(element, bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
    }

    public ArrayList<T> search(Rectangle2D searchArea){
        ArrayList<T> result = new ArrayList<>();
        root.search(searchArea.getX(), searchArea.getY(), searchArea.getWidth(), searchArea.getHeight(), result);
        return result;
    }

    public void search(Rectangle2D searchArea, Collection<T> result){
        root.search(searchArea.getX(), searchArea.getY(), searchArea.getWidth(), searchArea.getHeight(), result);
    }

    public void clear(){
        root.clear();
    }
}