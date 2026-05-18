package com.daninichu;

import com.daninichu.util.*;
import com.daninichu.util.Timer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.lang.reflect.Field;
import java.util.*;
import java.util.List;

public class QuadTreeViewer extends JFrame {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new QuadTreeViewer().setVisible(true));
    }

    QuadTreeViewer() {
        super("QuadTree Viewer");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(ViewPanel.width, ViewPanel.height);
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
        private static final double WORLD_W = 150000;
        private static final double WORLD_H = 150000;
        private static final int MAX_DEPTH = 9;

        // Rectangles
        private double viewBorderW = 100;
        private double viewBorderH = 100;
        private static final int N_RECTANGLES = 1000000;
        private static final double MIN_RECTANGLE_SIZE = 10;
        private static final double MAX_RECTANGLE_SIZE = 50;
        private static final double MAX_SPEED = 0;
        private static final double RECT_CURSOR_SIZE = 1000;
        private static final Rectangle2D worldBounds = new Rectangle2D.Double(0, 0, WORLD_W, WORLD_H);

        // Camera state (world-space offset of the top-left corner of the viewport)
        private double camX = 0;
        private double camY = 0;
        private double zoom = 0.35;   // world-units per screen-pixel = 1/zoom
        private static final double MIN_ZOOM = 0.01/2;
        private static final double MAX_ZOOM = 10.0;

        // Drag state
        private int dragStartX, dragStartY;
        private double camXAtDrag, camYAtDrag;
        private boolean dragging = false;
        private boolean delete = false;

        // Data
        private final List<ColoredRect> allRects = new ArrayList<>(N_RECTANGLES);
        private List<QuadTree.Entry<ColoredRect>> visibleEntries = new ArrayList<>(N_RECTANGLES);
        private List<QuadTree.Entry<ColoredRect>> selectedEntries = new ArrayList<>(N_RECTANGLES);
        private List<ColoredRect> visibleRects = new ArrayList<>(N_RECTANGLES);
        private List<ColoredRect> selectedRects = new ArrayList<>(N_RECTANGLES);

        private QuadTree<ColoredRect> quadTree;
        private final Rectangle2D.Double rectCursor = new Rectangle2D.Double(-RECT_CURSOR_SIZE, -RECT_CURSOR_SIZE, RECT_CURSOR_SIZE, RECT_CURSOR_SIZE);

        // Options
        private boolean showQuadCells = false;
        private boolean showRectCursor = false;
        private boolean quadTreeMode = true;
        private boolean selectionMode = false;

        private final BufferedImage canvas;
        private final int[] pixels;
        private static final int width = 1120;
        private static final int height = 840;

        // Colors
        private static final int QUAD_CELL_COLOR = new Color(90, 90, 120).getRGB();
        private static final int BORDER_COLOR = new Color(100, 100, 150).getRGB();
        private static final int FADED_RECT_COLOR = new Color(55, 55, 88).getRGB();

        // Times
        private double updateTime = 0;
        private double searchTime = 0;
        private double drawTime = 0;
        private double totalTime = 0;

        // ── reflected fields, resolved once at construction ───────────────────────
        private Field fRoot;       // QuadTree.root
        private Field fX;    // Quadrant.originX
        private Field fY;    // Quadrant.originY
        private Field fChildW;     // Quadrant.childW
        private Field fChildH;     // Quadrant.childH
        private Field fChildren;   // Quadrant.children

        // In constructor / resize:

        ViewPanel() {
            setBackground(new Color(15, 15, 20));
            addMouseListener(this);
            addMouseMotionListener(this);
            addMouseWheelListener(this);
            regenerate();

            canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            pixels = ((DataBufferInt) canvas.getRaster().getDataBuffer()).getData();

            try {
                // QuadTree.root  (private field)
                fRoot = QuadTree.class.getDeclaredField("root");
                fRoot.setAccessible(true);

                // Quadrant is a private static nested class — get it by name
                Class<?> quadrantClass = null;
                for (Class<?> c : QuadTree.class.getDeclaredClasses()) {
                    if (c.getSimpleName().equals("Quadrant")) {
                        quadrantClass = c;
                        break;
                    }
                }
                if (quadrantClass == null)
                    throw new IllegalStateException("Could not find Quadrant class");

                fX = quadrantClass.getDeclaredField("x");
                fY = quadrantClass.getDeclaredField("y");
                fChildW   = quadrantClass.getDeclaredField("childW");
                fChildH   = quadrantClass.getDeclaredField("childH");
                fChildren = quadrantClass.getDeclaredField("children");

                fX.setAccessible(true);
                fY.setAccessible(true);
                fChildW  .setAccessible(true);
                fChildH  .setAccessible(true);
                fChildren.setAccessible(true);

            } catch (NoSuchFieldException e) {
                throw new IllegalStateException(
                        "QuadTreePanel reflection setup failed — did QuadTree's " +
                                "internal field names change?", e);
            }

            if(MAX_SPEED != 0){
                new GameLoop(60, this::repaint).start();
            }
        }

        // -----------------------------------------------------------------
        // Data generation
        // -----------------------------------------------------------------

        private void regenerate() {
            allRects.clear();
            quadTree = new QuadTree<>(worldBounds, MAX_DEPTH);

            Random rng = new Random();
            Color[] palette = {
                new Color(255, 80,  60),   // red-orange
                new Color(255, 180, 40),   // amber
                new Color(60,  210, 160),  // teal
                new Color(80,  160, 255),  // sky blue
                new Color(200, 100, 255),  // violet
                new Color(255, 120, 180),  // pink
            };

            for (int i = 0; i < N_RECTANGLES; i++) {
                double w = MIN_RECTANGLE_SIZE + rng.nextDouble() * (MAX_RECTANGLE_SIZE - MIN_RECTANGLE_SIZE);
                double h = MIN_RECTANGLE_SIZE + rng.nextDouble() * (MAX_RECTANGLE_SIZE - MIN_RECTANGLE_SIZE);
                double x = rng.nextDouble() * (WORLD_W - w);
                double y = rng.nextDouble() * (WORLD_H - h);
                double vx = (rng.nextDouble() * 2 - 1) * MAX_SPEED;
                double vy = (rng.nextDouble() * 2 - 1) * MAX_SPEED;
                Color color = palette[rng.nextInt(palette.length)];
                ColoredRect rect = new ColoredRect(x, y, w, h, vx, vy, color.getRGB());
                allRects.add(rect);
                quadTree.add(rect, x, y, w, h);
            }
            allRects.sort(Comparator.comparingDouble(rect -> rect.y));

            repaint();
        }

        // -----------------------------------------------------------------
        // Coordinate helpers
        // -----------------------------------------------------------------

        /** Screen → world */
        private double toWorldX(int sx) {
            return camX + sx / zoom;
        }

        private double toWorldY(int sy) {
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
            updateTime = drawTime = searchTime = totalTime = 0;
            Timer totalTimer = new Timer();
            if(MAX_SPEED != 0){
                update();
                updateTime = totalTimer.seconds();
            }

            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Viewport in world space
            Rectangle2D viewRect = new Rectangle2D.Double(
                    camX,
                    camY,
                    getWidth() / zoom,
                    getHeight() / zoom
            );
            Rectangle2D selectionRect = viewRect;
            if(selectionMode){
                selectionRect = new Rectangle2D.Double(
                        camX + viewBorderW / zoom / 2,
                        camY + viewBorderH / zoom / 2,
                        (getWidth() - viewBorderW) / zoom,
                        (getHeight() - viewBorderH) / zoom
                );
            }

            // Gather values
            totalTimer.reset();

            if(showRectCursor && delete){
                quadTree.searchEntries(rectCursor).forEach(entry -> quadTree.removeAndCollapse(entry));
            }

            if (quadTreeMode){
                visibleEntries.clear();
                selectedEntries.clear();
                searchQuadTree(viewRect, selectionRect);
            } else{
                visibleRects.clear();
                selectedRects.clear();
                searchBruteForce(viewRect, selectionRect);
            }

            searchTime = totalTimer.seconds();
            totalTime += searchTime;

            // Drawing
            totalTimer.reset();

            Arrays.fill(pixels, getBackground().getRGB());
            if (showQuadCells) {
                try{
                    drawQuadCells(viewRect, fRoot.get(quadTree));
                } catch(IllegalAccessException e){
                    throw new RuntimeException(e);
                }
            }
            if(quadTreeMode){
                if(selectionMode){
                    visibleEntries.parallelStream().forEach(
                            e -> drawRect(e.getX(), e.getY(), e.getWidth(), e.getHeight(), FADED_RECT_COLOR)
                    );
                }
                selectedEntries.parallelStream().forEach(
                        e -> drawRect(e.getX(), e.getY(), e.getWidth(), e.getHeight(), e.value.color)
                );
            } else {
                if(selectionMode){
                    visibleRects.parallelStream().forEach(r -> drawRect(r.x, r.y, r.w, r.h, FADED_RECT_COLOR));
                }
                selectedRects.parallelStream().forEach(r -> drawRect(r.x, r.y, r.w, r.h, r.color));
            }
            drawRect(0, 0, WORLD_W, WORLD_H, BORDER_COLOR);
            if(selectionMode){
                drawRect(selectionRect.getX(), selectionRect.getY(), selectionRect.getWidth(), selectionRect.getHeight(), -1);
            }
            g2.drawImage(canvas, 0, 0, null);

            if(showRectCursor){
                g2.setColor(new Color(255, 255, 255, 40));
                g2.fillRect(
                        toScreenX(rectCursor.getX()),
                        toScreenY(rectCursor.getY()),
                        toScreenLen(rectCursor.getWidth()),
                        toScreenLen(rectCursor.getHeight())
                );
            }

            drawTime = totalTimer.seconds();
            totalTime += drawTime;

            // HUD

            if(quadTreeMode){
                drawHud(g2, visibleEntries.size(), quadTree.size());
            } else {
                drawHud(g2, visibleRects.size(), allRects.size());
            }
        }

        private void searchQuadTree(Rectangle2D viewRect, Rectangle2D selectionRect) {
            if(selectionMode){
                quadTree.searchEntries(viewRect, visibleEntries);
            }
            quadTree.searchEntries(selectionRect, selectedEntries);
        }

        private void searchBruteForce(Rectangle2D viewRect, Rectangle2D selectionRect) {
            for(ColoredRect rect : allRects) {
                if(selectionMode && viewRect.intersects(rect.x, rect.y, rect.w, rect.h)){
                    visibleRects.add(rect);
                }
                if(selectionRect.intersects(rect.x, rect.y, rect.w, rect.h)) {
                    selectedRects.add(rect);
                }
            }
        }

        private void drawRect(double x, double y, double w, double h, int color) {
            int sx = toScreenX(x);
            int sy = toScreenY(y);
            int sw = toScreenLen(w);
            int sh = toScreenLen(h);

            int x2 = sx + sw;
            int y2 = sy + sh;

            // Top and bottom edges
            for (int i = sx; i <= x2; i++) {
                setPixel(i, sy,  color);
                setPixel(i, y2, color);
            }
            // Left and right edges
            for (int j = sy; j <= y2; j++) {
                setPixel(sx,  j, color);
                setPixel(x2, j, color);
            }
        }

        private void setPixel(int x, int y, int color) {
            if (x >= 0 && x < width && y >= 0 && y < height) {
                pixels[y * width + x] = color;
            }
        }

        private void drawQuadCells(Rectangle2D viewRect, Object node) throws IllegalAccessException{
            if (node == null) {
                return;
            }
            Rectangle2D b = new Rectangle2D.Double(
                    (double) fX.get(node),
                    (double) fY.get(node),
                    (double) fChildW.get(node) * 2,
                    (double) fChildH.get(node) * 2
            );
            if(!viewRect.intersects(b)) {
                return;
            }
            double x = b.getX();
            double y = b.getY();
            double w = b.getWidth();
            double h = b.getHeight();
            drawRect(x, y, w, h, QUAD_CELL_COLOR);
            for (Object child : (Object[]) fChildren.get(node)) {
                drawQuadCells(viewRect, child);
            }
        }

        private void drawHud(Graphics2D g2, int visibleCount, int allCount) {
            g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            String[] lines = {
                String.format("zoom   %.2fx", zoom),
                String.format("cam    (%.0f, %.0f)", camX, camY),
                String.format("drawn  %d/%d rects", visibleCount, allCount),
                String.format("mode   %s", quadTreeMode? "quadtree" : "brute force"),
                "",
                "TIME (seconds)",
                String.format("update %f", updateTime),
                String.format("search %f", searchTime),
                String.format("draw   %f", drawTime),
                String.format("total  %f", totalTime),
                "",
                "drag   pan",
                "wheel  zoom",
                "R      regenerate",
                "W      show selection",
                "Q      show quad cells (%s)".formatted(showQuadCells? "ON" : "OFF"),
                "B-SPACE   rect cursor (%s)".formatted(showRectCursor? "ON" : "OFF"),
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

            g2.setColor(new Color(180, 220, 255, 200));
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
            delete = true;
            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            if(showRectCursor){
                rectCursor.x = toWorldX(e.getX()) - RECT_CURSOR_SIZE/2;
                rectCursor.y = toWorldY(e.getY()) - RECT_CURSOR_SIZE/2;
            }
            repaint();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            dragging = false;
            delete = false;
            setCursor(Cursor.getDefaultCursor());
            repaint();
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (!dragging) {
                return;
            }
            if(!showRectCursor){
                double dx = (e.getX() - dragStartX) / zoom;
                double dy = (e.getY() - dragStartY) / zoom;
                camX = camXAtDrag - dx;
                camY = camYAtDrag - dy;
            } else{
                rectCursor.x = toWorldX(e.getX()) - RECT_CURSOR_SIZE/2;
                rectCursor.y = toWorldY(e.getY()) - RECT_CURSOR_SIZE/2;
            }
            repaint();
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            double mx = toWorldX(e.getX());
            double my = toWorldY(e.getY());

            double rotation = e.getWheelRotation();
            double factor = rotation == 0? 1 : rotation < 0 ? 1.03 : 1.0 / 1.03;
            zoom = Math.max(MIN_ZOOM, Math.min(zoom * factor, MAX_ZOOM));

            // Zoom towards cursor
            camX = mx - e.getX() / zoom;
            camY = my - e.getY() / zoom;
            repaint();
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            if(showRectCursor){
                rectCursor.x = toWorldX(e.getX()) - RECT_CURSOR_SIZE/2;
                rectCursor.y = toWorldY(e.getY()) - RECT_CURSOR_SIZE/2;
                repaint();
            }
        }

        // -----------------------------------------------------------------
        // Key events
        // -----------------------------------------------------------------

        @Override
        public void keyPressed(KeyEvent e) {
            int d = 4;
            switch (e.getKeyCode()) {
                case KeyEvent.VK_R -> regenerate();
                case KeyEvent.VK_W -> selectionMode = !selectionMode;
                case KeyEvent.VK_Q -> showQuadCells = !showQuadCells;
                case KeyEvent.VK_SPACE -> quadTreeMode = !quadTreeMode;
                case KeyEvent.VK_BACK_SPACE -> showRectCursor = !showRectCursor;
                case KeyEvent.VK_RIGHT -> viewBorderW = Math.max(0, viewBorderW - d);
                case KeyEvent.VK_LEFT -> viewBorderW = Math.min(viewBorderW + d, width / 2);
                case KeyEvent.VK_UP -> viewBorderH = Math.max(0, viewBorderH - d);
                case KeyEvent.VK_DOWN -> viewBorderH = Math.min(viewBorderH + d, height / 2);
            }
            repaint();
        }

        // Unused interface methods
        @Override public void mouseClicked(MouseEvent e) {}
        @Override public void mouseEntered(MouseEvent e) {}
        @Override public void mouseExited(MouseEvent e) {}
        @Override public void keyReleased(KeyEvent e) {}
        @Override public void keyTyped(KeyEvent e) {}

        private void update(){
            for(QuadTree.Entry<ColoredRect> e : quadTree.entries()){
                ColoredRect rect = e.value;
                rect.x += rect.vx;
                rect.y += rect.vy;
                if(rect.x < worldBounds.getX()){
                    rect.x = worldBounds.getX();
                    rect.vx = -rect.vx;
                } else if(rect.x + rect.w > worldBounds.getMaxX()){
                    rect.x = worldBounds.getMaxX() - rect.w;
                    rect.vx = -rect.vx;
                }
                if(rect.y < worldBounds.getY()){
                    rect.y = worldBounds.getY();
                    rect.vy = -rect.vy;
                } else if(rect.y + rect.h > worldBounds.getMaxY()){
                    rect.y = worldBounds.getMaxY() - rect.h;
                    rect.vy = -rect.vy;
                }
//                quadTree.remove(e);
//                quadTree.add(rect, rect.x, rect.y, rect.w, rect.h);
                quadTree.move(e, rect.x, rect.y, rect.w, rect.h);
            }
        }
    }

    // =========================================================================

    static final class ColoredRect{
        double x;
        double y;
        double w;
        double h;
        double vx;
        double vy;
        int color;

        ColoredRect(double x, double y, double w, double h, double vx, double vy, int color){
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.vx = vx;
            this.vy = vy;
            this.color = color;
        }
    }
}
