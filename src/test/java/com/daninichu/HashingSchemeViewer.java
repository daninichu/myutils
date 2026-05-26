package com.daninichu;

import com.daninichu.util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.util.stream.IntStream;

public class HashingSchemeViewer extends AbstractViewer {
    public static void main(String[] args){
        new HashingSchemeViewer();
    }

    HashingSchemeViewer(){
        super(SCREEN_W, SCREEN_H);
        minZoom = 750f / Math.max(MAX_X - MIN_X, MAX_Y - MIN_Y);
        maxZoom = 150.0f;

        regenerate();
    }

    private static final int SCREEN_W = 1120;
    private static final int SCREEN_H = 810;

    private static final int MIN_X = 0;
    private static final int MIN_Y = 0;
    private static final int MAX_X = 100000;
    private static final int MAX_Y = 100000;

    // Data
    private Grid<Integer> grid = new HashGrid<>();
    private int highlightedHash;
    private int highlightedCellX = MIN_X;
    private int highlightedCellY = MIN_Y;
    private int collisions;

    private int schemeIndex = 0;
    private HashingScheme[] schemes = new HashingScheme[]{new LinearHashingScheme(31), new Point2DHashingScheme(), new CantorHashingScheme(), new SzudzikHashingScheme(), new FnvHashingScheme(),
    };

    // Colors
    private static final Color CELL_LINE_COLOR = new Color(90, 90, 120, 100);
    private static final Color HIGHLIGHT_COLOR = new Color(0, 205, 0, 200);
    private static final Color TEXT_COLOR = new Color(180, 220, 255, 200);

    // -----------------------------------------------------------------
    // Data generation
    // -----------------------------------------------------------------

    private void regenerate(){
        HashingScheme hashingScheme = schemes[schemeIndex];
        highlightedHash = hashingScheme.hashCode(highlightedCellX, highlightedCellY);

        collisions = 0;
        final Grid<Integer> grid = this.grid = new HashGrid<>();
        IntStream.rangeClosed(MIN_Y, MAX_Y).parallel().forEach(y -> {
            for (int x = MIN_X; x <= MAX_X; x++){
                int hash = hashingScheme.hashCode(x, y);
                if (hash == highlightedHash){
                    synchronized(grid){
                        collisions++;
                        grid.set(x, y, hash);
                    }
                }
            }
        });

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g){
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int x0 = toScreenX(0);
        int y0 = toScreenY(0);
        g2.setColor(new Color(255, 255, 255, 100));
        g2.drawLine(x0, 0, x0, SCREEN_H);
        g2.drawLine(0, y0, SCREEN_W, y0);

        g2.setColor(new Color(255, 255, 255));
        g2.drawRect(toScreenX(MIN_X), toScreenY(MIN_Y), toScreenLen(MAX_X - MIN_X + 1), toScreenLen(MAX_Y - MIN_Y + 1));

        Rectangle2D.Float viewRect = viewRect();

        int cellSize = toScreenLen(1);
        for(Point point : grid.points()){
            int x = point.x;
            int y = point.y;
            if(viewRect.intersects(x, y, 1, 1)){
                int sx = toScreenX(x);
                int sy = toScreenY(y);

                g2.setColor(CELL_LINE_COLOR);
                g2.drawRect(sx, sy, cellSize, cellSize);
                g2.setColor(HIGHLIGHT_COLOR);
                g2.fillRect(sx, sy, cellSize, cellSize);
            }
        }

        int threshold = 10;
        if (cellSize >= threshold){
            int tr = CELL_LINE_COLOR.getRed();
            int tg = CELL_LINE_COLOR.getGreen();
            int tb = CELL_LINE_COLOR.getBlue();
            int ta = Math.min((cellSize - threshold) * 3 / 2, 255);
            g2.setColor(new Color(tr, tg, tb, ta));

            int minX = (int) Math.max(Math.floor(viewRect.x), MIN_X);
            int minY = (int) Math.max(Math.floor(viewRect.y), MIN_Y);
            int maxX = (int) Math.min(Math.ceil(viewRect.x + viewRect.width), MAX_X);
            int maxY = (int) Math.min(Math.ceil(viewRect.y + viewRect.height), MAX_Y);
            for(int y = minY; y <= maxY; y++){
                for(int x = minX; x <= maxX; x++){
                    int sx = toScreenX(x);
                    int sy = toScreenY(y);
                    g2.drawRect(sx, sy, cellSize, cellSize);
                }
            }

            int fontSize = Math.max(8, cellSize / 8);
            g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, fontSize));
            tr = TEXT_COLOR.getRed();
            tg = TEXT_COLOR.getGreen();
            tb = TEXT_COLOR.getBlue();
            g2.setColor(new Color(tr, tg, tb, ta));

            for(int y = minY; y <= maxY; y++){
                for(int x = minX; x <= maxX; x++){
                    drawCenteredString(g2, "("+x+","+y+")", x, y, 1, 1);
                }
            }
        }

        drawHud(g2);
    }

    private void drawHud(Graphics2D g2){
        g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        String[] lines = {
                String.format("zoom   %.3fx", zoom),
                String.format("cam    (%.0f, %.0f)", camX, camY),
                "",
                "grid dimensions",
                "x [%d,%d]".formatted(MIN_X, MAX_X),
                "y [%d,%d]".formatted(MIN_Y, MAX_Y),
                "",
                "hashing scheme: " + schemes[schemeIndex].toString(),
                "hash: " + highlightedHash,
                "from (%d,%d)".formatted(highlightedCellX, highlightedCellY),
                "collisions: " + collisions,
                "",
                "drag   pan",
                "wheel  zoom",
        };

        int lineH = 17;
        int padX = 14;
        int padY = 14;
        int boxW = 220;
        int boxH = lines.length * lineH + 12;

        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRoundRect(padX, padY, boxW, boxH, 8, 8);
        g2.setColor(new Color(255, 255, 255, 30));
        g2.drawRoundRect(padX, padY, boxW, boxH, 8, 8);

        g2.setColor(TEXT_COLOR);
        int ty = padY + lineH;
        for (String line : lines){
            g2.drawString(line, padX + 10, ty);
            ty += lineH;
        }
    }

    @Override
    public void mouseClicked(MouseEvent e){
        int cellX = (int) Math.floor(toWorldX(e.getX()));
        int cellY = (int) Math.floor(toWorldY(e.getY()));
        if(MIN_X <= cellX && cellX <= MAX_X && MIN_Y <= cellY && cellY <= MAX_Y){
            highlightedHash = schemes[schemeIndex].hashCode(cellX, cellY);
            highlightedCellX = cellX;
            highlightedCellY = cellY;
        }
        regenerate();
    }

    @Override
    public void keyPressed(KeyEvent e){
        switch (e.getKeyCode()){
            case KeyEvent.VK_UP -> schemeIndex = (schemeIndex + schemes.length - 1) % schemes.length;
            case KeyEvent.VK_DOWN -> schemeIndex = (schemeIndex + 1) % schemes.length;
        }
        regenerate();
    }

    abstract static class HashingScheme{
        public abstract int hashCode(int x, int y);
        public abstract String toString();
    }
    static class LinearHashingScheme extends HashingScheme{
        final int a;
        LinearHashingScheme(int a){
            this.a = a;
        }
        public int hashCode(int x, int y){
            return a * x + y;
        }
        public String toString(){
            return a+" * x + y";
        }
    }
    static class CantorHashingScheme extends HashingScheme{
        public int hashCode(int x, int y){
            int a = x>=0?2*x:(-2*x)-1;
            int b = y>=0?2*y:(-2*y)-1;
            return (a+b)*(a+b+1)/2 + b;
        }
        public String toString(){
            return "Cantor";
        }
    }
    static class SzudzikHashingScheme extends HashingScheme{
        public int hashCode(int x, int y){
            return x >= y ? x*x + x + y : y*y + x;
        }
        public String toString(){
            return "Szudzik";
        }
    }
    static class FnvHashingScheme extends HashingScheme{
        public int hashCode(int x, int y){
            int h = x * 0x9e3779b9;
            return h ^ (h >>> 16) ^ y * 0x6c62272e;
        }
        public String toString(){
            return "Fnv";
        }
    }
    static class Point2DHashingScheme extends HashingScheme{
        public int hashCode(int x, int y){
            long bits = java.lang.Double.doubleToLongBits(x);
            bits ^= java.lang.Double.doubleToLongBits(y) * 31;
            return (((int) bits) ^ ((int) (bits >> 32)));
        }
        public String toString(){
            return "Point2D";
        }
    }
}
