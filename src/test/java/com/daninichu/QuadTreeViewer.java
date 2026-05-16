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

/**
 * Swing viewer for QuadTree.
 * <p>
 * Controls:
 *   - Click + drag  → pan camera
 *   - Mouse wheel   → zoom
 *   - R             → regenerate random rectangles
 *   - Q             → toggle QuadTree cell outlines
 */
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
        private final QuadTree<ColoredRect> quadTree = new QuadTree<>(worldBounds, MAX_DEPTH);
        private final Rectangle2D.Double rectCursor = new Rectangle2D.Double(-RECT_CURSOR_SIZE, -RECT_CURSOR_SIZE, RECT_CURSOR_SIZE, RECT_CURSOR_SIZE);

        // Options
        private boolean showQuadCells = false;
        private boolean showRectCursor = false;
        private boolean quadSearch = true;
        private boolean whiteOnly = false;

        private final BufferedImage canvas;
        private final int[] pixels;
        private static final int width = 1120;
        private static final int height = 840;

        // Colors
        private static final int QUAD_CELL_COLOR = new Color(100, 100, 150).getRGB();
        private static final int BORDER_COLOR = new Color(100, 100, 150).getRGB();

        // Times
        private double updateTime = 0;
        private double searchTime = 0;
        private double drawTime = 0;
        private double totalTime = 0;

        // ── reflected fields, resolved once at construction ───────────────────────
        private final Field fRoot;       // QuadTree.root
        private final Field fOriginX;    // Quadrant.originX
        private final Field fOriginY;    // Quadrant.originY
        private final Field fChildW;     // Quadrant.childW
        private final Field fChildH;     // Quadrant.childH
        private final Field fChildren;   // Quadrant.children

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

                fOriginX  = quadrantClass.getDeclaredField("originX");
                fOriginY  = quadrantClass.getDeclaredField("originY");
                fChildW   = quadrantClass.getDeclaredField("childW");
                fChildH   = quadrantClass.getDeclaredField("childH");
                fChildren = quadrantClass.getDeclaredField("children");

                fOriginX .setAccessible(true);
                fOriginY .setAccessible(true);
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
            quadTree.clear();

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
                quadTree.add(rect, new Rectangle2D.Double(x, y, w, h));
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
            double vpW = getWidth() / zoom;
            double vpH = getHeight() / zoom;
            Rectangle2D viewRect = new Rectangle2D.Double(camX, camY, vpW, vpH);


            Timer searchTimer = new Timer();


            // Gather values
            List<QuadTree.Entry<ColoredRect>> visibleEntries = new ArrayList<>();

            if(showRectCursor && delete){
                quadTree.searchEntries(rectCursor, visibleEntries);
            }

            List<ColoredRect> visibleRects = new ArrayList<>();
            if (quadSearch){
                for (QuadTree.Entry<ColoredRect> entry : visibleEntries) {
                    quadTree.removeAndCollapse(entry);
                }
                visibleEntries.clear();
                quadTree.searchEntries(viewRect, visibleEntries);
            } else{
                for (QuadTree.Entry<ColoredRect> entry : visibleEntries) {
                    allRects.remove(entry.value);
                }
                bruteForce(viewRect, visibleRects);
            }

            searchTime += searchTimer.seconds();

            Timer repaintTimer = new Timer();

            // Drawing
            Arrays.fill(pixels, getBackground().getRGB());
            if (showQuadCells) {
                try{
                    drawQuadCells(viewRect, fRoot.get(quadTree));
                } catch(IllegalAccessException e){
                    throw new RuntimeException(e);
                }
            }
            if(quadSearch){
                visibleEntries.parallelStream().forEach(e -> {
                    drawRectOutline(e.getX(), e.getY(), e.getWidth(), e.getHeight(), e.value.color);
                });
            } else {
                visibleRects.parallelStream().forEach(r -> drawRectOutline(r.x, r.y, r.w, r.h, r.color));
            }
            drawRectOutline(0, 0, WORLD_W, WORLD_H, BORDER_COLOR);
            g2.drawImage(canvas, 0, 0, null);

            if(showRectCursor){
                drawRectCursor(g2);
            }
            drawTime += repaintTimer.seconds();
            totalTime += totalTimer.seconds();
            // HUD
            drawHud(g2, visibleEntries.size());
        }

        private void drawRectCursor(Graphics2D g2){
            g2.setColor(new Color(255, 255, 255, 40));
            g2.fillRect(
                    toScreenX(rectCursor.getX()),
                    toScreenY(rectCursor.getY()),
                    toScreenLen(rectCursor.getWidth()),
                    toScreenLen(rectCursor.getHeight())
            );
        }

        private void drawRectOutline(double x, double y, double w, double h, int color) {
            int sx = toScreenX(x);
            int sy = toScreenY(y);
            int sw = toScreenLen(w);
            int sh = toScreenLen(h);
            if(whiteOnly){
                color = -1;
            }

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

        private void bruteForce(Rectangle2D viewRect, List<ColoredRect> visible){
            for(ColoredRect rect : allRects) {
                if(viewRect.intersects(rect.x, rect.y, rect.w, rect.h)) {
                    visible.add(rect);
                }
            }
        }

        private void drawQuadCells(Rectangle2D viewRect, Object node) throws IllegalAccessException{
            if (node == null) {
                return;
            }
            Rectangle2D b = new Rectangle2D.Double(
                    (double) fOriginX.get(node),
                    (double) fOriginY.get(node),
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
            drawRectOutline(x, y, w, h, QUAD_CELL_COLOR);
            for (Object child : (Object[]) fChildren.get(node)) {
                drawQuadCells(viewRect, child);
            }
        }

        private void drawHud(Graphics2D g2, int visibleCount) {
            g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            String[] lines = {
                String.format("zoom   %.2fx", zoom),
                String.format("cam    (%.0f, %.0f)", camX, camY),
                String.format("drawn  %d/%d rects", visibleCount, quadTree.size()),
                String.format("mode   %s", quadSearch ? "quadtree" : "brute force"),
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
                "W      show rect colors (%s)".formatted(!whiteOnly? "ON" : "OFF"),
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
            switch (e.getKeyCode()) {
                case KeyEvent.VK_R -> regenerate();
                case KeyEvent.VK_W -> whiteOnly = !whiteOnly;
                case KeyEvent.VK_Q -> showQuadCells = !showQuadCells;
                case KeyEvent.VK_SPACE -> quadSearch = !quadSearch;
                case KeyEvent.VK_BACK_SPACE -> showRectCursor = !showRectCursor;
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
