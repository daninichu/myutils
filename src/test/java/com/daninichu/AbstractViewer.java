package com.daninichu;

import com.daninichu.util.Camera;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;

public class AbstractViewer extends JPanel implements MouseListener,
        MouseMotionListener, MouseWheelListener, KeyListener {

    AbstractViewer(String title, int width, int height){
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame(title);
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setSize(width, height);
            frame.setLocationRelativeTo(null);

            setBackground(new Color(15, 15, 20));
            frame.add(this);
            addKeyListener(this);
            addMouseListener(this);
            addMouseMotionListener(this);
            addMouseWheelListener(this);
            setFocusable(true);
            frame.setVisible(true);
        });
    }

    protected Camera camera = new Camera();
    protected float zoomSpeed = 1.03f;
    protected float minZoom = 1;
    protected float maxZoom = 100;

    protected int dragStartX, dragStartY;
    protected double camXAtDrag, camYAtDrag;

    protected int toScreenLen(double wl){
        return Math.max(1, (int) Math.round(wl * camera.getZoom()));
    }

    protected Rectangle2D.Double viewRect(){
        return new Rectangle2D.Double(camera.x, camera.y, getWidth() / camera.getZoom(), getHeight() / camera.getZoom());
    }

    protected void drawCenteredString(Graphics2D g2, String str, double x, double y, double w, double h){
        int sx = camera.toScreenX(x);
        int sy = camera.toScreenY(y);
        int sw = toScreenLen(w);
        int sh = toScreenLen(h);

        FontMetrics fm = g2.getFontMetrics();

        int textWidth = fm.stringWidth(str);
        int textHeight = fm.getAscent();

        int tx = sx + (sw - textWidth) / 2;
        int ty = sy + (sh + textHeight) / 2;

        g2.drawString(str, tx, ty);
    }

    @Override
    public void mousePressed(MouseEvent e){
        dragStartX = e.getX();
        dragStartY = e.getY();
        camXAtDrag = camera.x;
        camYAtDrag = camera.y;
    }

    @Override
    public void mouseDragged(MouseEvent e){
        double dx = (e.getX() - dragStartX) / camera.getZoom();
        double dy = (e.getY() - dragStartY) / camera.getZoom();
        camera.setPosition(camXAtDrag - dx, camYAtDrag - dy);
        repaint();
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e){
        double mx = camera.toWorldX(e.getX());
        double my = camera.toWorldY(e.getY());

        int rotation = e.getWheelRotation();
        float factor = rotation == 0? 1 : rotation < 0 ? zoomSpeed : 1 / zoomSpeed;
        camera.setZoom(Math.max(minZoom, Math.min(camera.getZoom() * factor, maxZoom)));

        camera.setPosition(mx - e.getX() / camera.getZoom(), my - e.getY() / camera.getZoom());
        repaint();
    }

    @Override public void mouseMoved(MouseEvent e){}
    @Override public void mouseReleased(MouseEvent e){}
    @Override public void mouseClicked(MouseEvent e){}
    @Override public void mouseEntered(MouseEvent e){}
    @Override public void mouseExited(MouseEvent e){}
    @Override public void keyPressed(KeyEvent e){}
    @Override public void keyReleased(KeyEvent e){}
    @Override public void keyTyped(KeyEvent e){}
}
