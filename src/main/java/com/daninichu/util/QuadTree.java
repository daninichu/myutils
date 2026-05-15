package com.daninichu.util;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;

public class QuadTree<T> implements Iterable<T> {
    private Quadrant<T> root;
    private int size;
    private double boundX, boundY, boundW, boundH;

    public QuadTree(Rectangle2D bounds) {
        this(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(), 8);
    }

    public QuadTree(Rectangle2D bounds, int maxDepth) {
        this(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(), maxDepth);
    }

    public QuadTree(double x, double y, double width, double height) {
        this(x, y, width, height, 8);
    }

    public QuadTree(double x, double y, double width, double height, int maxDepth) {
        checkNonNegativity(width, height);
        if(maxDepth < 0){
            throw new IllegalArgumentException("maxDepth cannot be negative");
        }
        boundX = x;
        boundY = y;
        boundW = width;
        boundH = height;
        root = new Quadrant<>(x, y, width, height, 0, maxDepth);
    }

    private static void checkNonNegativity(double width, double height){
        if(width < 0 || height < 0)
            throw new IllegalArgumentException("width or height cannot be negative");
    }

    /**
     * Adds a value enclosed in a rectangular boundary.
     * The value is stored in the deepest node that can fully contain the value.
     * If the value is not inside the bounds of this quadtree, then it is stored in the root.
     * @param value The value to be stored in the tree.
     * @param x The x coordinate of the value.
     * @param y The y coordinate of the value.
     * @param width The width of the value.
     * @param height The height of the value.
     * @return An entry which holds the value and its bounds.
     * @throws IllegalArgumentException If {@code width} or {@code height} are negative.
     */
    public Entry<T> add(T value, double x, double y, double width, double height){
        checkNonNegativity(width, height);

        Quadrant<T> quadrant = root.findQuadrant(x, y, width, height);
        Entry<T> entry = new Entry<>(value, x, y, width, height);
        entry.quadrant = quadrant;

        ArrayList<Entry<T>> entries = quadrant.entries;
        entry.index = entries.size();
        entries.add(entry);

        size++;
        return entry;
    }

    public Entry<T> add(T value, Rectangle2D bounds){
        return add(value, bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
    }

    /**
     * Finds all entries in the quadtree that intersect with a given search area.
     * @param x The x coordinate of the search area.
     * @param y The y coordinate of the search area.
     * @param width The width of the search area.
     * @param height The height of the search area.
     * @return A list of all entries whose bounds intersect with the search area.
     * @throws IllegalArgumentException If {@code width} or {@code height} are negative.
     */
    public ArrayList<Entry<T>> searchEntries(double x, double y, double width, double height){
        checkNonNegativity(width, height);
        ArrayList<Entry<T>> result = new ArrayList<>();
        root.searchEntries(x, y, width, height, result);
        return result;
    }

    public ArrayList<Entry<T>> searchEntries(Rectangle2D searchArea){
        return searchEntries(searchArea.getX(), searchArea.getY(), searchArea.getWidth(), searchArea.getHeight());
    }

    /**
     * Finds all entries in the quadtree that intersect with a given search area,
     * and adds them to a given collection.
     * <p>
     * This method uses {@code Collection::addAll} internally,
     * which might make this faster than {@link #searchValues(double, double, double, double, Collection)},
     * @param x The x coordinate of the search area.
     * @param y The y coordinate of the search area.
     * @param width The width of the search area.
     * @param height The height of the search area.
     * @param result The collection to fill with entries.
     * @throws IllegalArgumentException If {@code width} or {@code height} are negative.
     */
    public void searchEntries(double x, double y, double width, double height, Collection<? super Entry<T>> result){
        checkNonNegativity(width, height);
        root.searchEntries(x, y, width, height, result);
    }

    public void searchEntries(Rectangle2D searchArea, Collection<? super Entry<T>> result){
        searchEntries(searchArea.getX(), searchArea.getY(), searchArea.getWidth(), searchArea.getHeight(), result);
    }

    public ArrayList<T> searchValues(double x, double y, double width, double height){
        checkNonNegativity(width, height);
        ArrayList<T> result = new ArrayList<>();
        root.searchValues(x, y, width, height, result);
        return result;
    }

    public ArrayList<T> searchValues(Rectangle2D searchArea){
        return searchValues(searchArea.getX(), searchArea.getY(), searchArea.getWidth(), searchArea.getHeight());
    }

    /**
     * Finds all values in the quadtree that intersect with a given search area,
     * and adds them to a given collection.
     * <p>
     * The values are retrieved through {@code Entry.value}.
     * If you just need to access the values and don’t need them in a collection of their type,
     * then it might be faster to use {@link #searchEntries(double, double, double, double, Collection)},
     * which uses {@code Collection::addAll} internally.
     * @param x The x coordinate of the search area.
     * @param y The y coordinate of the search area.
     * @param width The width of the search area.
     * @param height The height of the search area.
     * @param result The collection to fill with values.
     * @throws IllegalArgumentException If {@code width} or {@code height} are negative.
     */
    public void searchValues(double x, double y, double width, double height, Collection<? super T> result){
        checkNonNegativity(width, height);
        root.searchValues(x, y, width, height, result);
    }

    public void searchValues(Rectangle2D searchArea, Collection<? super T> result){
        searchValues(searchArea.getX(), searchArea.getY(), searchArea.getWidth(), searchArea.getHeight(), result);
    }

    /**
     * Performs a search of the entire tree looking for the value to be removed.
     * For a faster removal method, use {@link #remove(Entry)}, which avoids performing a search.
     * <p>
     * The tree does not collapse itself.
     * @param value The value to search for and remove from the tree.
     * @return {@code true} if the value was found and removed from the tree.
     */
    public boolean remove(T value){
        Entry<T> entry = root.removeEntry(value);
        if(entry != null){
            entry.quadrant = null;
            size--;
            return true;
        }
        return false;
    }

    /**
     * Performs a search of the entire tree looking for the value to be removed.
     * For a faster removal method, use {@link #removeAndCollapse(Entry)}, which avoids performing a search.
     * <p>
     * The tree collapses on itself if a leaf node is empty.
     * @param value The value to search for and remove from the tree.
     * @return {@code true} if the value was found and removed from the tree.
     */
    public boolean removeAndCollapse(T value){
        Entry<T> entry = root.removeEntry(value);
        if(entry != null){
            collapse(entry.quadrant);
            entry.quadrant = null;
            size--;
            return true;
        }
        return false;
    }

    /**
     * Removes the given entry from this quadtree without having to perform a search.
     * <p>
     * The tree does not collapse itself.
     * @param entry The entry to be removed.
     * @return {@code true} if the entry was present and removed from the tree.
     */
    public boolean remove(Entry<T> entry){
        Quadrant<T> tree = entry.quadrant;
        if(tree != null){
            fastRemove(tree.entries, entry);
            entry.quadrant = null;
            size--;
            return true;
        }
        return false;
    }

    /**
     * Removes the given entry from this quadtree without having to perform a search.
     * <p>
     * The tree collapses on itself if a leaf node is empty.
     * @param entry The entry to be removed.
     * @return {@code true} if the entry was present and removed from the tree.
     */
    public boolean removeAndCollapse(Entry<T> entry){
        Quadrant<T> tree = entry.quadrant;
        if(tree != null){
            fastRemove(tree.entries, entry);
            collapse(tree);
            entry.quadrant = null;
            size--;
            return true;
        }
        return false;
    }

    private void fastRemove(ArrayList<Entry<T>> entries, Entry<T> entry) {
        int lastIndex = entries.size() - 1;
        Entry<T> last = entries.get(lastIndex);
        entries.set(last.index = entry.index, last);
        entries.remove(lastIndex);
    }

    /**
     * Sets a new boundary for this quadtree.
     * All entries are reinserted into the tree to be stored in the correct nodes.
     * @param x The x coordinate of the new boundary.
     * @param y The y coordinate of the new boundary.
     * @param width The width of the new boundary.
     * @param height The height of the new boundary.
     * @throws IllegalArgumentException If {@code width} or {@code height} are negative.
     */
    public void resize(double x, double y, double width, double height){
        checkNonNegativity(width, height);

        ArrayList<Entry<T>> entries = new ArrayList<>(size);
        root.copyEntries(entries);

        boundX = x;
        boundY = y;
        boundW = width;
        boundH = height;

        root = new Quadrant<>(x, y, width, height, 0, root.maxDepth);
        for(Entry<T> entry : entries){
            Quadrant<T> quadrant = root.findQuadrant(entry.x, entry.y, entry.width, entry.height);
            entry.quadrant = quadrant;
            quadrant.entries.add(entry);
        }
    }

    public void resize(Rectangle2D bounds){
        resize(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
    }

    /**
     * Removes all values from this quadtree. All external entry objects become invalid.
     */
    public void clear(){
        root.clear();
        root = new Quadrant<>(boundX, boundY, boundW, boundH, 0, root.maxDepth);
        size = 0;
    }

    /**
     * @return The amount of values in this quadtree.
     */
    public int size(){
        return size;
    }

    /**]
     * @return {@code true} if this quadtree contains no values.
     */
    public boolean isEmpty(){
        return size == 0;
    }

    public Rectangle2D.Double getBounds() {
        return new Rectangle2D.Double(boundX, boundY, boundW, boundH);
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

    private static void collapse(Quadrant<?> quadrant){
        Quadrant<?> parent = quadrant.parent;
        while(parent != null && quadrant.entries.isEmpty()){
            Quadrant<?>[] children = quadrant.children;
            for(int i = 0; i < 4; i++){
                if(children[i] != null){
                    return;
                }
            }
            children = parent.children;
            for(int i = 0; i < 4; i++){
                if(children[i] == quadrant){
                    children[i] = null;
                    quadrant = parent;
                    parent = parent.parent;
                    break;
                }
            }
        }
    }

    /**
     * An intermediate class that holds the value inside the quadtree.
     * This class can be used for fast removal without needing to search the entire tree.
     * @param <T> The type of values in the quadtree.
     */
    public static class Entry<T>{
        public final T value;
        public final double x, y, width, height;
        private Quadrant<T> quadrant;
        private int index;

        private Entry(T value, double x, double y, double width, double height){
            this.value = value;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    private static class Quadrant<T>{
        double originX, originY, childW, childH;
        int depth, maxDepth;

        @SuppressWarnings("unchecked")
        Quadrant<T>[] children = new Quadrant[4];
        Quadrant<T> parent;
        ArrayList<Entry<T>> entries = new ArrayList<>();

        Quadrant(double x, double y, double w, double h, int depth, int maxDepth){
            this.originX = x;
            this.originY = y;
            this.childW = w / 2;
            this.childH = h / 2;
            this.depth = depth;
            this.maxDepth = maxDepth;
        }

        Quadrant<T> findQuadrant(double x, double y, double w, double h){
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
                        return child.findQuadrant(x, y, w, h);
                    }
                }
            }
            return this;
        }

        void searchEntries(double x, double y, double w, double h, Collection<? super Entry<T>> result){
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
                        child.searchEntries(x, y, w, h, result);
                }
            }
        }

        void searchValues(double x, double y, double w, double h, Collection<? super T> result){
            for(Entry<T> entry : entries){
                if(intersects(x, y, w, h, entry.x, entry.y, entry.width, entry.height)){
                    result.add(entry.value);
                }
            }
            for(int i = 0; i < 4; i++){
                Quadrant<T> child = children[i];
                if(child != null){
                    double childX = originX + (i % 2) * childW;
                    double childY = originY + (i / 2) * childH;

                    if(contains(x, y, w, h, childX, childY, childW, childH))
                        child.copyValues(result);
                    else if(intersects(x, y, w, h, childX, childY, childW, childH))
                        child.searchValues(x, y, w, h, result);
                }
            }
        }

        void copyEntries(Collection<? super Entry<T>> result){
            result.addAll(entries);
            for(int i = 0; i < 4; i++){
                Quadrant<T> child = children[i];
                if(child != null){
                    child.copyEntries(result);
                }
            }
        }

        void copyValues(Collection<? super T> result){
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
            for(int i = 0, lastIndex = entries.size() - 1; i <= lastIndex; i++){
                Entry<T> entry = entries.get(i);
                if(Objects.equals(entry.value, value)){
                    entries.set(i, entries.get(lastIndex));
                    entries.remove(lastIndex);
                    return entry;
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