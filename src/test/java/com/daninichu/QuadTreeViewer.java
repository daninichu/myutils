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

public class QuadTreeViewer extends AbstractViewer{
    public static void main(String[] args){
        SwingUtilities.invokeLater(QuadTreeViewer::new);
    }

    QuadTreeViewer(){
        super("Quad Tree Viewer", SCREEN_W, SCREEN_H);
        minZoom = 750f / Math.max(WORLD_W, WORLD_H);
        maxZoom = 10;

        regenerate();

        canvas = new BufferedImage(SCREEN_W, SCREEN_H, BufferedImage.TYPE_INT_RGB);
        pixels = ((DataBufferInt) canvas.getRaster().getDataBuffer()).getData();

        try{
            // QuadTree.root  (private field)
            fRoot = QuadTree.class.getDeclaredField("root");
            fRoot.setAccessible(true);

            // Quadrant is a private static nested class — get it by name
            Class<?> quadrantClass = null;
            for(Class<?> c : QuadTree.class.getDeclaredClasses()){
                if(c.getSimpleName().equals("Quadrant")){
                    quadrantClass = c;
                    break;
                }
            }
            if(quadrantClass == null)
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

        } catch (NoSuchFieldException e){
            throw new IllegalStateException(
                    "QuadTreePanel reflection setup failed — did QuadTree's " +
                            "internal field names change?", e);
        }

        if(MAX_SPEED != 0){
            loop = new GameLoop(30, dt -> {
                System.out.println("dt = "+dt);
                System.out.println("fps = " + 1/dt);
                System.out.println();
                synchronized(lock){
                    Timer timer = new Timer();
                    update(60*dt);
                    updateTime = timer.seconds();
                }
                repaint();
            });
        }
    }

    private GameLoop loop;

    // Rectangles
    private float viewBorderW = 400;
    private float viewBorderH = 400;
    private static final int N_RECTS = 100000;
    private static final int MIN_RECT_SIZE = 10;
    private static final int MAX_RECT_SIZE = 50;
    private static final int MAX_SPEED = 8;
    private static final int CURSOR_SIZE = 1000;
    private float[] vx, vy;

    private static final int SCREEN_W = 1120;
    private static final int SCREEN_H = 810;

    // World dimensions
    private static final int WORLD_W = (int) Math.sqrt(N_RECTS) * 200;
    private static final int WORLD_H = (int) Math.sqrt(N_RECTS) * 200;
    private static final int MAX_DEPTH = 8;

    private boolean delete = false;

    // Data
    private QuadTree.Entry<Integer>[] allEntries = new QuadTree.Entry[N_RECTS];
    private List<QuadTree.Entry<Integer>> visibleEntries = new ArrayList<>(N_RECTS);
    private List<QuadTree.Entry<Integer>> selectedEntries = new ArrayList<>(N_RECTS);

    private QuadTree<Integer> quadTree = new QuadTree<>(0, 0, WORLD_W, WORLD_H, MAX_DEPTH);
    private final Rectangle2D.Double rectCursor = new Rectangle2D.Double(0,0, CURSOR_SIZE, CURSOR_SIZE);

    // Options
    private boolean showQuadCells = false;
    private boolean showRectCursor = false;
    private boolean quadTreeMode = true;
    private boolean selectionMode = false;

    private final BufferedImage canvas;
    private final int[] pixels;

    // Colors
    private static final int QUAD_CELL_COLOR = new Color(90, 90, 120).getRGB();
    private static final int BORDER_COLOR = new Color(100, 100, 150).getRGB();
    private static final int FADED_RECT_COLOR = new Color(55, 55, 88).getRGB();

    // Times
    private double generationTime = 0;
    private double updateTime = 0;
    private double searchTime = 0;
    private double drawTime = 0;
    private double totalTime = 0;

    private final Object lock = new Object();

    private Field fRoot;
    private Field fX;
    private Field fY;
    private Field fChildW;
    private Field fChildH;
    private Field fChildren;

    private void regenerate(){
        synchronized(lock){
            Timer timer = new Timer();

            quadTree.clear();

            Random rng = new Random();
            Color[] palette = {
                    new Color(255, 80, 60),   // red-orange
                    new Color(255, 180, 40),   // amber
                    new Color(60, 210, 160),  // teal
                    new Color(80, 160, 255),  // sky blue
                    new Color(200, 100, 255),  // violet
                    new Color(255, 120, 180),  // pink
            };

            for(int i = 0; i < N_RECTS; i++){
                float w = MIN_RECT_SIZE + rng.nextFloat() * (MAX_RECT_SIZE - MIN_RECT_SIZE);
                float h = MIN_RECT_SIZE + rng.nextFloat() * (MAX_RECT_SIZE - MIN_RECT_SIZE);
                float x = rng.nextFloat() * (WORLD_W - w);
                float y = rng.nextFloat() * (WORLD_H - h);
                Color color = palette[rng.nextInt(palette.length)];
                allEntries[i] = (quadTree.add(color.getRGB(), x, y, w, h));
            }
            if(MAX_SPEED != 0){
                vx = new float[N_RECTS];
                vy = new float[N_RECTS];
                for(int i = 0; i < N_RECTS; i++){
                    vx[i] = (rng.nextFloat() * 2 - 1) * MAX_SPEED;
                    vy[i] = (rng.nextFloat() * 2 - 1) * MAX_SPEED;
                }
            }
            Arrays.parallelSort(allEntries, Comparator.comparingDouble(QuadTree.Entry::getY));

            generationTime = timer.seconds();
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g){
        super.paintComponent(g);
        synchronized(lock){
            render((Graphics2D) g);
        }
    }

    private void render(Graphics2D g2){
        drawTime = searchTime = totalTime = 0;
        totalTime += updateTime;

        Timer timer = new Timer();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Viewport in world space
        Rectangle2D.Double viewRect = new Rectangle2D.Double(
                camera.x,
                camera.y,
                getWidth() / camera.getZoom(),
                getHeight() / camera.getZoom()
        );
        Rectangle2D.Double selectionRect = viewRect;
        if(selectionMode){
            selectionRect = new Rectangle2D.Double(
                    camera.x + viewBorderW / camera.getZoom() / 2,
                    camera.y + viewBorderH / camera.getZoom() / 2,
                    (getWidth() - viewBorderW) / camera.getZoom(),
                    (getHeight() - viewBorderH) / camera.getZoom()
            );
        }

        // Gather values
        timer.reset();

        if(showRectCursor && delete){
            quadTree.searchEntries(rectCursor).forEach(e -> quadTree.removeAndCollapse(e));
        }

        visibleEntries.clear();
        selectedEntries.clear();
        if(quadTreeMode){
            searchQuadTree(viewRect, selectionRect);
        } else{
            searchBruteForce(viewRect, selectionRect);
        }

        totalTime += searchTime = timer.seconds();

        // Drawing
        timer.reset();

        Arrays.fill(pixels, getBackground().getRGB());
        if(showQuadCells){
            try{
                drawQuadCells(viewRect, fRoot.get(quadTree));
            } catch(IllegalAccessException e){
                throw new RuntimeException(e);
            }
        }
        if(selectionMode){
            visibleEntries.parallelStream().forEach(
                    e -> drawRect(e.getX(), e.getY(), e.getWidth(), e.getHeight(), FADED_RECT_COLOR)
            );
        }
        selectedEntries.parallelStream().forEach(
                e -> drawRect(e.getX(), e.getY(), e.getWidth(), e.getHeight(), e.value)
        );
        drawRect(0, 0, WORLD_W, WORLD_H, BORDER_COLOR);
        if(selectionMode){
            drawRect(selectionRect.x, selectionRect.y, selectionRect.width, selectionRect.height, -1);
        }
        g2.drawImage(canvas, 0, 0, null);

        if(showRectCursor){
            g2.setColor(new Color(255, 255, 255, 40));
            g2.fillRect(
                    camera.toScreenX(rectCursor.x),
                    camera.toScreenY(rectCursor.y),
                    toScreenLen(rectCursor.width),
                    toScreenLen(rectCursor.height)
            );
        }

        totalTime += drawTime = timer.seconds();

        // HUD
        drawHud(g2, selectedEntries.size(), quadTree.size());
    }

    private void searchQuadTree(Rectangle2D viewRect, Rectangle2D selectionRect){
        if(selectionMode){
            quadTree.searchEntries(viewRect, visibleEntries);
        }
        quadTree.searchEntries(selectionRect, selectedEntries);
    }

    private void searchBruteForce(Rectangle2D viewRect, Rectangle2D selectionRect){
        for(QuadTree.Entry<Integer> e : allEntries){
            if(selectionMode && viewRect.intersects(e.getX(), e.getY(), e.getWidth(), e.getHeight())){
                visibleEntries.add(e);
            }
            if(selectionRect.intersects(e.getX(), e.getY(), e.getWidth(), e.getHeight())){
                selectedEntries.add(e);
            }
        }
    }

    private void drawRect(double x, double y, double w, double h, int color){
        int sx = camera.toScreenX(x);
        int sy = camera.toScreenY(y);
        int sw = toScreenLen(w);
        int sh = toScreenLen(h);

        int x2 = sx + sw;
        int y2 = sy + sh;

        // Top and bottom edges
        for(int i = sx; i <= x2; i++){
            setPixel(i, sy, color);
            setPixel(i, y2, color);
        }
        // Left and right edges
        for(int j = sy; j <= y2; j++){
            setPixel(sx, j, color);
            setPixel(x2, j, color);
        }
    }

    private void setPixel(int x, int y, int color){
        if(x >= 0 && x < SCREEN_W && y >= 0 && y < SCREEN_H){
            pixels[y * SCREEN_W + x] = color;
        }
    }

    private void drawQuadCells(Rectangle2D viewRect, Object node) throws IllegalAccessException{
        if(node == null){
            return;
        }
        double x = (double) fX.get(node);
        double y = (double) fY.get(node);
        double w = (double) fChildW.get(node) * 2;
        double h = (double) fChildH.get(node) * 2;
        if(!viewRect.intersects(x, y, w, h)){
            return;
        }
        drawRect(x, y, w, h, QUAD_CELL_COLOR);
        for(Object child : (Object[]) fChildren.get(node)){
            drawQuadCells(viewRect, child);
        }
    }

    private void drawHud(Graphics2D g2, int visibleCount, int allCount){
        g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        String[] lines = {
            String.format("camera.getZoom()   %.3fx", camera.getZoom()),
            String.format("cam    (%.0f, %.0f)", camera.x, camera.y),
            String.format("drawn  %d/%d rects", visibleCount, allCount),
            String.format("mode   %s", quadTreeMode? "quadtree" : "brute force"),
            "",
            String.format("generation time %f", generationTime),
            "TIME (seconds)",
            String.format("update %f", updateTime),
            String.format("search %f", searchTime),
            String.format("draw   %f", drawTime),
            String.format("total  %f", totalTime),
            "",
            "drag   pan",
            "wheel  camera.getZoom()",
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
        for(String line : lines){
            g2.drawString(line, padX + 10, ty);
            ty += lineH;
        }
    }

    @Override
    public void mousePressed(MouseEvent e){
        super.mousePressed(e);
        delete = true;

        if(showRectCursor){
            rectCursor.x = camera.toWorldX(e.getX()) - CURSOR_SIZE /2;
            rectCursor.y = camera.toWorldY(e.getY()) - CURSOR_SIZE /2;
        }
        repaint();
    }

    @Override
    public void mouseReleased(MouseEvent e){
        delete = false;
        repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e){
        if(!showRectCursor){
            double dx = (e.getX() - dragStartX) / camera.getZoom();
            double dy = (e.getY() - dragStartY) / camera.getZoom();
            camera.setPosition(camXAtDrag - dx, camYAtDrag - dy);
        } else{
            rectCursor.x = camera.toWorldX(e.getX()) - CURSOR_SIZE /2;
            rectCursor.y = camera.toWorldY(e.getY()) - CURSOR_SIZE /2;
        }
        repaint();
    }

    @Override
    public void mouseMoved(MouseEvent e){
        if(showRectCursor){
            rectCursor.x = camera.toWorldX(e.getX()) - CURSOR_SIZE /2;
            rectCursor.y = camera.toWorldY(e.getY()) - CURSOR_SIZE /2;
            repaint();
        }
    }

    @Override
    public void keyPressed(KeyEvent e){
        int d = 8;
        switch (e.getKeyCode()){
            case KeyEvent.VK_R -> regenerate();
            case KeyEvent.VK_W -> selectionMode = !selectionMode;
            case KeyEvent.VK_Q -> showQuadCells = !showQuadCells;
            case KeyEvent.VK_SPACE -> quadTreeMode = !quadTreeMode;
            case KeyEvent.VK_BACK_SPACE -> showRectCursor = !showRectCursor;
            case KeyEvent.VK_RIGHT -> viewBorderW = Math.max(0, viewBorderW - d);
            case KeyEvent.VK_LEFT -> viewBorderW = Math.min(viewBorderW + d, SCREEN_W / 2);
            case KeyEvent.VK_UP -> viewBorderH = Math.max(0, viewBorderH - d);
            case KeyEvent.VK_DOWN -> viewBorderH = Math.min(viewBorderH + d, SCREEN_H / 2);
        }
        repaint();
    }

    private void update(double dt){
        for(int i = 0; i < N_RECTS; i++){
            QuadTree.Entry<Integer> e = allEntries[i];
            double maxX;
            double maxY;
            double x = e.getX() + vx[i]*dt;
            double y = e.getY() + vy[i]*dt;
            if(x < 0){
                x = 0;
                vx[i] = -vx[i];
            } else if(x > (maxX = WORLD_W - e.getWidth())){
                x = maxX;
                vx[i] = -vx[i];
            }
            if(y < 0){
                y = 0;
                vy[i] = -vy[i];
            } else if(y > (maxY = WORLD_H - e.getHeight())){
                y = maxY;
                vy[i] = -vy[i];
            }
            quadTree.move(e, x, y, e.getWidth(), e.getHeight());
        }
    }
}
