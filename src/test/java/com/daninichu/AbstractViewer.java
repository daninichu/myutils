package com.daninichu;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;

public class AbstractViewer extends JPanel implements MouseListener,
        MouseMotionListener, MouseWheelListener, KeyListener {


    AbstractViewer(int width, int height){
        JFrame frame = new JFrame();
        SwingUtilities.invokeLater(() -> {
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
        });
        frame.setVisible(true);
    }

    protected float camX, camY;
    protected float zoom = 0.35f;
    protected float zoomSpeed = 1.03f;
    protected float minZoom = 1;
    protected float maxZoom = 100;

    protected int dragStartX, dragStartY;
    protected float camXAtDrag, camYAtDrag;

    protected float toWorldX(int sx){
        return camX + sx / zoom;
    }

    protected float toWorldY(int sy){
        return camY + sy / zoom;
    }

    protected int toScreenX(double wx){
        return (int) Math.round((wx - camX) * zoom);
    }

    protected int toScreenY(double wy){
        return (int) Math.round((wy - camY) * zoom);
    }

    protected int toScreenLen(double wl){
        return Math.max(1, (int) Math.round(wl * zoom));
    }

    protected Rectangle2D.Float viewRect(){
        return new Rectangle2D.Float(camX, camY, getWidth() / zoom, getHeight() / zoom);
    }

    protected void drawCenteredString(Graphics2D g2, String str, double x, double y, double w, double h){
        int sx = toScreenX(x);
        int sy = toScreenY(y);
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
        camXAtDrag = camX;
        camYAtDrag = camY;
        repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e){
        float dx = (e.getX() - dragStartX) / zoom;
        float dy = (e.getY() - dragStartY) / zoom;
        camX = camXAtDrag - dx;
        camY = camYAtDrag - dy;
        repaint();
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e){
        float mx = toWorldX(e.getX());
        float my = toWorldY(e.getY());

        int rotation = e.getWheelRotation();
        float factor = rotation == 0? 1 : rotation < 0 ? zoomSpeed : 1 / zoomSpeed;
        zoom = Math.max(minZoom, Math.min(zoom * factor, maxZoom));

        camX = mx - e.getX() / zoom;
        camY = my - e.getY() / zoom;
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
