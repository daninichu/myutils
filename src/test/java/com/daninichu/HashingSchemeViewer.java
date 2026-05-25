package com.daninichu;

import com.daninichu.util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.util.stream.IntStream;

public class HashingSchemeViewer extends JFrame {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new HashingSchemeViewer().setVisible(true));
    }

    HashingSchemeViewer() {
        super();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(ViewPanel.SCREEN_W, ViewPanel.SCREEN_H);
        setLocationRelativeTo(null);

        ViewPanel panel = new ViewPanel();
        add(panel);
        addKeyListener(panel);
        setFocusable(true);
    }

    // =========================================================================

    static final class ViewPanel extends JPanel implements MouseListener,
            MouseMotionListener, MouseWheelListener, KeyListener {

        // World dimensions
        private static final int MIN_X = -000;
        private static final int MIN_Y = -000;
        private static final int MAX_X = 100000;
        private static final int MAX_Y = 100000;

        // Camera state (world-space offset of the top-left corner of the viewport)
        private float camX = MIN_X;
        private float camY = MIN_Y;
        private float zoom = MIN_ZOOM;
        private static final float MIN_ZOOM = 750f / Math.max(MAX_X - MIN_X, MAX_Y - MIN_Y);
        private static final float MAX_ZOOM = 150.0f;

        // Drag state
        private int dragStartX, dragStartY;
        private float camXAtDrag, camYAtDrag;
        private boolean dragging = false;

        // Data
        private Grid<Integer> grid = new HashGrid<>();
        private int highlightedHash;
        private int highlightedCellX = MIN_X;
        private int highlightedCellY = MIN_Y;
        private int collisions;

        private int scheme = 0;
        private HashingScheme[] hashingSchemes = new HashingScheme[]{
                new LinearHashingScheme(65537),
                new CantorHashingScheme(),
                new SzudzikHashingScheme(),
                new FnvHashingScheme(),
        };

        private static final int SCREEN_W = 1120;
        private static final int SCREEN_H = 810;

        // Colors
        private static final Color CELL_LINE_COLOR = new Color(90, 90, 120, 100);
        private static final Color HIGHLIGHT_COLOR = new Color(0, 255, 0);
        private static final Color TEXT_COLOR = new Color(180, 220, 255, 200);

        // In constructor / resize:

        ViewPanel() {
            setBackground(new Color(15, 15, 20));
            addMouseListener(this);
            addMouseMotionListener(this);
            addMouseWheelListener(this);
            regenerate();
        }

        // -----------------------------------------------------------------
        // Data generation
        // -----------------------------------------------------------------

        private void regenerate() {
            HashingScheme hashingScheme = hashingSchemes[scheme];
            highlightedHash = hashingScheme.hashCode(highlightedCellX, highlightedCellY);

            final Grid<Integer> grid = this.grid = new HashGrid<>();

            collisions = 0;

            IntStream.rangeClosed(MIN_Y, MAX_Y).parallel().forEach(y -> {
                for (int x = MIN_X; x <= MAX_X; x++) {
                    int hash = hashingScheme.hashCode(x, y);
                    if (hash == highlightedHash) {
                        synchronized(grid) {
                            collisions++;
                            grid.set(x, y, hash);
                        }
                    }
                }
            });

            repaint();
        }

        // -----------------------------------------------------------------
        // Coordinate helpers
        // -----------------------------------------------------------------

        /** Screen → world */
        private float toWorldX(int sx) {
            return camX + sx / zoom;
        }

        private float toWorldY(int sy) {
            return camY + sy / zoom;
        }

        /** World → screen */
        private int toScreenX(double wx) {
            return (int) Math.round((wx - camX) * zoom);
        }

        private int toScreenY(double wy) {
            return (int) Math.round((wy - camY) * zoom);
        }

        private int toScreenLen(double wl) {
            return Math.max(1, (int) Math.round(wl * zoom));
        }

        // -----------------------------------------------------------------
        // Painting
        // -----------------------------------------------------------------

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Viewport in world space
            Rectangle2D.Float viewRect = new Rectangle2D.Float(
                    camX,
                    camY,
                    getWidth() / zoom,
                    getHeight() / zoom
            );

            int x0 = toScreenX(0);
            int y0 = toScreenY(0);
            g2.setColor(new Color(255, 255, 255, 100));
            g2.drawLine(x0, 0, x0, SCREEN_H);
            g2.drawLine(0, y0, SCREEN_W, y0);

            g2.setColor(new Color(255, 255, 255));
            g2.drawRect(toScreenX(MIN_X), toScreenY(MIN_Y), toScreenLen(MAX_X - MIN_X), toScreenLen(MAX_Y - MIN_Y));

            int minX = (int) Math.max(Math.floor(viewRect.x), MIN_X);
            int minY = (int) Math.max(Math.floor(viewRect.y), MIN_Y);
            int maxX = (int) Math.min(Math.ceil(viewRect.x + viewRect.width), MAX_X);
            int maxY = (int) Math.min(Math.ceil(viewRect.y + viewRect.height), MAX_Y);

            for(Point point : grid.points()) {
                int x = point.x;
                int y = point.y;
                if(!viewRect.intersects(x, y, 1, 1))
                    continue;
                int sx = toScreenX(x);
                int sy = toScreenY(y);
                int sw = toScreenLen(1);
                int sh = toScreenLen(1);

                g2.setColor(CELL_LINE_COLOR);
                g2.drawRect(sx, sy, sw, sh);
                g2.setColor(HIGHLIGHT_COLOR);
                g2.fillRect(sx, sy, sw, sh);
            }

            int cellSize = toScreenLen(1);
            int fontSize = Math.max(8, cellSize / 6);

            int threshold = 10;
            if (toScreenLen(1) >= threshold) {
                int tr = CELL_LINE_COLOR.getRed();
                int tg = CELL_LINE_COLOR.getGreen();
                int tb = CELL_LINE_COLOR.getBlue();
                int ta = Math.min((cellSize - threshold) * 3 / 2, 255);
                g2.setColor(new Color(tr, tg, tb, ta));

                for(int y = minY; y <= maxY; y++) {
                    for(int x = minX; x <= maxX; x++) {
                        int sx = toScreenX(x);
                        int sy = toScreenY(y);
                        int sw = toScreenLen(1);
                        int sh = toScreenLen(1);
                        g2.drawRect(sx, sy, sw, sh);
                    }
                }

                g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, fontSize));
                tr = TEXT_COLOR.getRed();
                tg = TEXT_COLOR.getGreen();
                tb = TEXT_COLOR.getBlue();
                g2.setColor(new Color(tr, tg, tb, ta));

                for(int y = minY; y <= maxY; y++){
                    for(int x = minX; x <= maxX; x++){
                        String point = String.format("(%d,%d)", x, y);
                        drawCellText(g2, x, y, point, "");
                    }
                }
            }

            drawHud(g2);
        }

        private void drawCellText(Graphics2D g2, int x, int y, String line1, String line2) {
            int sx = toScreenX(x);
            int sy = toScreenY(y);
            int sw = toScreenLen(1);
            int sh = toScreenLen(1);

            FontMetrics fm = g2.getFontMetrics();
            int lineHeight = fm.getHeight();

            int totalHeight = lineHeight * 2;

            int y1 = sy + (sh - totalHeight) / 2 + fm.getAscent();
            int y2 = y1 + lineHeight;

            int x1 = sx + (sw - fm.stringWidth(line1)) / 2;
            int x2 = sx + (sw - fm.stringWidth(line2)) / 2;

            g2.drawString(line1, x1, y1);
            g2.drawString(line2, x2, y2);
        }

        private void drawHud(Graphics2D g2) {
            g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            String[] lines = {
                    String.format("zoom   %.3fx", zoom),
                    String.format("cam    (%.0f, %.0f)", camX, camY),
                    "",
                    "grid dimensions",
                    "x [%d,%d]".formatted(MIN_X, MAX_X),
                    "y [%d,%d]".formatted(MIN_Y, MAX_Y),
                    "",
                    "hashing scheme: " + hashingSchemes[scheme].toString(),
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
            for (String line : lines) {
                g2.drawString(line, padX + 10, ty);
                ty += lineH;
            }
        }

        // -----------------------------------------------------------------
        // Mouse events
        // -----------------------------------------------------------------

        @Override
        public void mousePressed(MouseEvent e) {
            dragStartX = e.getX();
            dragStartY = e.getY();
            camXAtDrag = camX;
            camYAtDrag = camY;
            dragging = true;
            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            repaint();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            dragging = false;
            setCursor(Cursor.getDefaultCursor());
            repaint();
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (!dragging) {
                return;
            }
            float dx = (e.getX() - dragStartX) / zoom;
            float dy = (e.getY() - dragStartY) / zoom;
            camX = camXAtDrag - dx;
            camY = camYAtDrag - dy;
            repaint();
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            float mx = toWorldX(e.getX());
            float my = toWorldY(e.getY());

            int rotation = e.getWheelRotation();
            float factor = rotation == 0? 1 : rotation < 0 ? 1.03f : 1 / 1.03f;
            zoom = Math.max(MIN_ZOOM, Math.min(zoom * factor, MAX_ZOOM));

            // Zoom towards cursor
            camX = mx - e.getX() / zoom;
            camY = my - e.getY() / zoom;
            repaint();
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            int cellX = (int) Math.floor(toWorldX(e.getX()));
            int cellY = (int) Math.floor(toWorldY(e.getY()));
            if(MIN_X <= cellX && cellX <= MAX_X && MIN_Y <= cellY && cellY <= MAX_Y){
                highlightedHash = hashingSchemes[scheme].hashCode(cellX, cellY);
                highlightedCellX = cellX;
                highlightedCellY = cellY;
            }
            regenerate();
        }


        // -----------------------------------------------------------------
        // Key events
        // -----------------------------------------------------------------

        @Override
        public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_LEFT -> scheme = (scheme + hashingSchemes.length - 1) % hashingSchemes.length;
                case KeyEvent.VK_RIGHT -> scheme = (scheme + 1) % hashingSchemes.length;
            }
            regenerate();
        }

        @Override public void mouseMoved(MouseEvent e) {}
        @Override public void mouseEntered(MouseEvent e) {}
        @Override public void mouseExited(MouseEvent e) {}
        @Override public void keyReleased(KeyEvent e) {}
        @Override public void keyTyped(KeyEvent e) {}
    }
}

abstract class HashingScheme{
    public abstract int hashCode(int x, int y);
    public abstract String toString();
}
class LinearHashingScheme extends HashingScheme{
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
class CantorHashingScheme extends HashingScheme{
    public int hashCode(int x, int y){
        int a = x>=0?2*x:(-2*x)-1; int b = y>=0?2*y:(-2*y)-1;
        return (a+b)*(a+b+1)/2 + b;
    }
    public String toString(){
        return "Cantor";
    }
}
class SzudzikHashingScheme extends HashingScheme{
    public int hashCode(int x, int y){
        return x >= y ? x*x + x + y : y*y + x;
    }
    public String toString(){
        return "Szudzik";
    }
}
class FnvHashingScheme extends HashingScheme{
    public int hashCode(int x, int y){
        int h = x * 0x9e3779b9;
        return h ^ (h >>> 16) ^ y * 0x6c62272e;
    }
    public String toString(){
        return "Fnv";
    }
}
class Poind2DHashingScheme extends HashingScheme{
    public int hashCode(int x, int y){
        long bits = java.lang.Double.doubleToLongBits(x);
        bits ^= java.lang.Double.doubleToLongBits(y) * 31;
        return (((int) bits) ^ ((int) (bits >> 32)));
    }
    public String toString(){
        return "Poind2D";
    }
}