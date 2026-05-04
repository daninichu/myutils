package com.daninichu;

import com.daninichu.util.QuadTree;
import com.daninichu.util.Timer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Swing viewer for QuadTree.
 *
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

        // Rectangles
        private static final double N_RECTANGLES = 1000000;
        private static final double MAX_RECTANGLE_SIZE = 50;

        // World dimensions
        private static final double WORLD_W = 120000;
        private static final double WORLD_H = 120000;

        // Camera state (world-space offset of the top-left corner of the viewport)
        private double camX = 0;
        private double camY = 0;
        private double zoom = 0.35;   // world-units per screen-pixel = 1/zoom

        // Drag state
        private int dragStartX, dragStartY;
        private double camXAtDrag, camYAtDrag;
        private boolean dragging = false;
        private boolean delete = false;

        // Data
        private final List<ColoredRect> allRects = new ArrayList<>();
        private QuadTree<ColoredRect> quadTree;
        private Rectangle2D deleteRect = new Rectangle2D.Double(0, 0, 1000, 1000);

        // Options
        private boolean showQuadCells = false;
        private boolean quadSearch = false;

        private BufferedImage canvas;
        private int[] pixels;
        private static final int width = 1200;
        private static final int height = 800;


        // In constructor / resize:

        ViewPanel() {
            setBackground(new Color(15, 15, 20));
            addMouseListener(this);
            addMouseMotionListener(this);
            addMouseWheelListener(this);
            regenerate();
        canvas = new BufferedImage(1200, 800, BufferedImage.TYPE_INT_RGB);
        pixels = ((DataBufferInt) canvas.getRaster().getDataBuffer()).getData();
        }

        // -----------------------------------------------------------------
        // Data generation
        // -----------------------------------------------------------------

        private void regenerate() {
            allRects.clear();
            Rectangle2D worldBounds = new Rectangle2D.Double(0, 0, WORLD_W, WORLD_H);
            quadTree = new QuadTree<>(worldBounds);

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
                double w = 10 + rng.nextDouble() * MAX_RECTANGLE_SIZE;
                double h = 10 + rng.nextDouble() * MAX_RECTANGLE_SIZE;
                double x = rng.nextDouble() * (WORLD_W - w);
                double y = rng.nextDouble() * (WORLD_H - h);
                Color color = palette[rng.nextInt(palette.length)];
                ColoredRect rect = new ColoredRect(x, y, w, h, color);
                allRects.add(rect);
                quadTree.add(rect, rect.bounds);
            }
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
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Viewport in world space
            double vpW = getWidth() / zoom;
            double vpH = getHeight() / zoom;
            Rectangle2D viewRect = new Rectangle2D.Double(camX, camY, vpW, vpH);

            double repaintTime = 0;
            double searchTime = 0;

            Timer repaintTimer = new Timer();
            // QuadTree cell outlines
            if (showQuadCells) {
                drawQuadCells(g2, viewRect, quadTree);
            }
            repaintTime += repaintTimer.seconds();

            List<ColoredRect> visible = new ArrayList<>();

            Timer searchTimer = new Timer();
            if (quadSearch)
                quadTree.search(viewRect, visible);
            else
                bruteForce(viewRect, visible);
            searchTime += searchTimer.seconds();

            repaintTimer.reset();
            Arrays.fill(pixels, 0x000000); // clear to black
            for (ColoredRect rect : visible) {
                drawRectOutline(rect);
            }
            g.drawImage(canvas, 0, 0, null);

            // World boundary
            g2.setColor(new Color(255, 255, 255, 40));
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_MITER, 10, new float[]{6, 4}, 0));
            g2.drawRect(toScreenX(0), toScreenY(0),
                    toScreenLen(WORLD_W), toScreenLen(WORLD_H));

            if(delete){
                List<ColoredRect> inside = new ArrayList<>();
                quadTree.search(deleteRect, inside);
                for (ColoredRect rect : inside) {
                    int sx = toScreenX(rect.bounds.getX());
                    int sy = toScreenY(rect.bounds.getY());
                    int sw = toScreenLen(rect.bounds.getWidth());
                    int sh = toScreenLen(rect.bounds.getHeight());
                    g2.setColor(new Color(
                            rect.color.getRed(),
                            rect.color.getGreen(),
                            rect.color.getBlue(), 60));
                    g2.fillRect(sx, sy, sw, sh);

                    g2.setColor(rect.color);
                    g2.setStroke(new BasicStroke(1f));
                    g2.drawRect(sx, sy, sw, sh);
                }
                g2.setColor(new Color(255, 255, 255, 40));
                g2.fillRect(
                        toScreenX(deleteRect.getX()),
                        toScreenY(deleteRect.getY()),
                        toScreenLen(deleteRect.getWidth()),
                        toScreenLen(deleteRect.getHeight())
                );
            }
            repaintTime += repaintTimer.seconds();
            // HUD
            drawHud(g2, visible.size(), searchTime, repaintTime);
        }

        private void drawRectOutline(ColoredRect rect) {
            int x = toScreenX(rect.bounds.getX());
            int y = toScreenY(rect.bounds.getY());
            int w = toScreenLen(rect.bounds.getWidth());
            int h = toScreenLen(rect.bounds.getHeight());
            int color = rect.color.getRGB();

            int x2 = x + w;
            int y2 = y + h;

            // Top and bottom edges
            for (int i = x; i <= x2; i++) {
                setPixel(i, y,  color);
                setPixel(i, y2, color);
            }

            // Left and right edges
            for (int j = y; j <= y2; j++) {
                setPixel(x,  j, color);
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
                if(viewRect.intersects(rect.bounds)) {
                    visible.add(rect);
                }
            }
        }

        private void drawQuadCells(Graphics2D g2, Rectangle2D viewRect, QuadTree<?> node) {
            if (node == null) {
                return;
            }
            Rectangle2D b = node.getBounds();
            if(!viewRect.intersects(b)) {
                return;
            }
            g2.setColor(new Color(100, 100, 200));
            g2.setStroke(new BasicStroke(0.5f));
            g2.drawRect(toScreenX(b.getX()), toScreenY(b.getY()),
                    toScreenLen(b.getWidth()), toScreenLen(b.getHeight()));

            for (QuadTree<?> child : node.childTrees) {
                drawQuadCells(g2, viewRect, child);
            }
        }

        private void drawHud(Graphics2D g2, int visibleCount, double searchTime, double repaintTime) {
            g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            String[] lines = {
                String.format("zoom   %.2fx", zoom),
                String.format("cam    (%.0f, %.0f)", camX, camY),
                String.format("drawn  %d/%d rects", visibleCount, allRects.size()),
                String.format("mode   %s", quadSearch ? "quadtree" : "brute force"),
                String.format("search time   %f", searchTime),
                String.format("repaint time   %f", repaintTime),
                "",
                "drag   pan",
                "wheel  zoom",
                "R      regenerate",
                "Q      toggle cells",
            };

            int lineH = 17;
            int padX = 14, padY = 14;
            int boxW = 220, boxH = lines.length * lineH + 12;

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
            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            dragging = false;
            setCursor(Cursor.getDefaultCursor());
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (!dragging) {
                return;
            }
            if(!delete){
                double dx = (e.getX() - dragStartX) / zoom;
                double dy = (e.getY() - dragStartY) / zoom;
                camX = camXAtDrag - dx;
                camY = camYAtDrag - dy;
            } else{
                double w = deleteRect.getWidth();
                double h = deleteRect.getHeight();
                deleteRect.setRect(toWorldX(e.getX()) - w/2, toWorldY(e.getY()) - h/2, w, h);
            }
            repaint();
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            double mx = toWorldX(e.getX());
            double my = toWorldY(e.getY());

            double factor = e.getWheelRotation() < 0 ? 1.03 : 1.0 / 1.03;
            zoom = Math.max(0.005, Math.min(zoom * factor, 20.0));

            // Zoom towards cursor
            camX = mx - e.getX() / zoom;
            camY = my - e.getY() / zoom;
            repaint();
        }

        // -----------------------------------------------------------------
        // Key events
        // -----------------------------------------------------------------

        @Override
        public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_R -> regenerate();
                case KeyEvent.VK_Q -> {
                    showQuadCells = !showQuadCells;
                    repaint();
                }
                case KeyEvent.VK_SPACE -> quadSearch = !quadSearch;
                case KeyEvent.VK_BACK_SPACE -> delete = !delete;
            }
            repaint();
        }

        // Unused interface methods
        @Override public void mouseClicked(MouseEvent e) {}
        @Override public void mouseEntered(MouseEvent e) {}
        @Override public void mouseExited(MouseEvent e) {}
        @Override public void mouseMoved(MouseEvent e) {}
        @Override public void keyReleased(KeyEvent e) {}
        @Override public void keyTyped(KeyEvent e) {}
    }

    // =========================================================================

    record ColoredRect(double x, double y, double w, double h,
                       Color color, Rectangle2D bounds) {

        ColoredRect(double x, double y, double w, double h, Color color) {
            this(x, y, w, h, color, new Rectangle2D.Double(x, y, w, h));
        }
    }
}
