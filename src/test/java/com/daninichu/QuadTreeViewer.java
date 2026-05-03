package com.daninichu;

import com.daninichu.util.QuadTree;
import com.daninichu.util.Timer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
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
        setSize(1100, 780);
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
        private static final double WORLD_W = 200000;
        private static final double WORLD_H = 200000;

        // Camera state (world-space offset of the top-left corner of the viewport)
        private double camX = 0;
        private double camY = 0;
        private double zoom = 0.35;   // world-units per screen-pixel = 1/zoom

        // Drag state
        private int dragStartX, dragStartY;
        private double camXAtDrag, camYAtDrag;
        private boolean dragging = false;

        // Data
        private final List<ColoredRect> allRects = new ArrayList<>();
        private QuadTree<ColoredRect> quadTree;

        // Options
        private boolean showQuadCells = true;
        private boolean quadSearch = false;

        ViewPanel() {
            setBackground(new Color(15, 15, 20));
            addMouseListener(this);
            addMouseMotionListener(this);
            addMouseWheelListener(this);
            regenerate();

//            new Thread(() -> {
//                while (true){
//                    repaint();
//                }
//            }).start();
        }

        // -----------------------------------------------------------------
        // Data generation
        // -----------------------------------------------------------------

        private void regenerate() {
            allRects.clear();
            Rectangle2D worldBounds = new Rectangle2D.Double(0, 0, WORLD_W, WORLD_H);
            quadTree = new QuadTree<>(worldBounds, 9);

            Random rng = new Random();
            Color[] palette = {
                new Color(255, 80,  60),   // red-orange
                new Color(255, 180, 40),   // amber
                new Color(60,  210, 160),  // teal
                new Color(80,  160, 255),  // sky blue
                new Color(200, 100, 255),  // violet
                new Color(255, 120, 180),  // pink
            };

            for (int i = 0; i < 100000; i++) {
                double w = 10 + rng.nextDouble() * 80;
                double h = 10 + rng.nextDouble() * 80;
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
            Timer repaintTimer = new Timer();
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Viewport in world space
            double vpW = getWidth() / zoom;
            double vpH = getHeight() / zoom;
            Rectangle2D viewRect = new Rectangle2D.Double(camX, camY, vpW, vpH);

            // Grid
//            drawGrid(g2, viewRect);

            // QuadTree cell outlines
            if (showQuadCells) {
                drawQuadCells(g2, quadTree);
            }

            List<ColoredRect> visible = new ArrayList<>();

            Timer searchTimer = new Timer();
            if (quadSearch)
                quadTree.search(viewRect, visible);
            else
                bruteForce(viewRect, visible);

            for (ColoredRect rect : visible) {
                int sx = toScreenX(rect.bounds.getX());
                int sy = toScreenY(rect.bounds.getY());
                int sw = toScreenLen(rect.bounds.getWidth());
                int sh = toScreenLen(rect.bounds.getHeight());

                // Fill
//                g2.setColor(new Color(
//                        rect.color.getRed(),
//                        rect.color.getGreen(),
//                        rect.color.getBlue(), 60));
//                g2.fillRect(sx, sy, sw, sh);

                // Border
                g2.setColor(rect.color);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRect(sx, sy, sw, sh);
            }


            // World boundary
            g2.setColor(new Color(255, 255, 255, 40));
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_MITER, 10, new float[]{6, 4}, 0));
            g2.drawRect(toScreenX(0), toScreenY(0),
                    toScreenLen(WORLD_W), toScreenLen(WORLD_H));

            // HUD
            drawHud(g2, visible.size(), searchTimer.seconds(), repaintTimer.seconds());
        }

        private void bruteForce(Rectangle2D viewRect, List<ColoredRect> visible){
            for(ColoredRect rect : allRects) {
                if(viewRect.intersects(rect.bounds)) {
                    visible.add(rect);
                }
            }
        }

        private void drawGrid(Graphics2D g2, Rectangle2D view) {
            double step = niceGridStep();
            g2.setColor(new Color(255, 255, 255, 12));
            g2.setStroke(new BasicStroke(0.5f));

            double startX = Math.floor(view.getX() / step) * step;
            double startY = Math.floor(view.getY() / step) * step;

            for (double wx = startX; wx < view.getMaxX(); wx += step) {
                int sx = toScreenX(wx);
                g2.drawLine(sx, 0, sx, getHeight());
            }
            for (double wy = startY; wy < view.getMaxY(); wy += step) {
                int sy = toScreenY(wy);
                g2.drawLine(0, sy, getWidth(), sy);
            }
        }

        private double niceGridStep() {
            double raw = 100.0 / zoom;
            double mag = Math.pow(10, Math.floor(Math.log10(raw)));
            double norm = raw / mag;
            if (norm < 2) return mag;
            if (norm < 5) return 2 * mag;
            return 5 * mag;
        }

        private void drawQuadCells(Graphics2D g2, QuadTree<?> node) {
//            if (node == null) {
//                return;
//            }
//            g2.setColor(new Color(255, 255, 255, 18));
//            g2.setStroke(new BasicStroke(0.5f));
//            Rectangle2D b = node.getBounds();
//            g2.drawRect(toScreenX(b.getX()), toScreenY(b.getY()),
//                    toScreenLen(b.getWidth()), toScreenLen(b.getHeight()));
//
//            for (QuadTree<?> child : node.childTrees) {
//                drawQuadCells(g2, child);
//            }
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
            int boxW = 180, boxH = lines.length * lineH + 12;

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
            double dx = (e.getX() - dragStartX) / zoom;
            double dy = (e.getY() - dragStartY) / zoom;
            camX = camXAtDrag - dx;
            camY = camYAtDrag - dy;
            repaint();
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            double mx = toWorldX(e.getX());
            double my = toWorldY(e.getY());

            double factor = e.getWheelRotation() < 0 ? 1.012 : 1.0 / 1.012;
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
