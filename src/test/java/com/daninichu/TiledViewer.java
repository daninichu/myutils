package com.daninichu;

import com.daninichu.tiled.*;
import com.daninichu.util.Stopwatch;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;

class TiledViewer extends AbstractViewer{
    public static void main(String[] args){
        SwingUtilities.invokeLater(TiledViewer::new);
    }

    TiledMap tiledMap;
    TiledMapRenderer tiledMapRenderer;

    TiledViewer(){
        super("Shape Viewer", SCREEN_W, SCREEN_H);
        TiledReader reader = new FileSystemTiledReader();
        tiledMap = reader.getMap("src/test/resources/dirt_map.tmx");
//        tiledMap = reader.getMap("src/test/resources/untitled.tmx");
        tiledMapRenderer = new TiledMapRenderer(tiledMap);

        camera.setZoom(3);
    }

    private static final int SCREEN_W = 800;
    private static final int SCREEN_H = 600;

    @Override
    protected void paintComponent(Graphics g){
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        Stopwatch stopwatch = new Stopwatch();

        AffineTransform oldTransform = g2.getTransform();
        camera.apply(g2);
        tiledMapRenderer.renderMap(g2);
        g2.setTransform(oldTransform);

        System.out.println(stopwatch.millis());
        drawHud(g2);
    }

    private void drawHud(Graphics2D g2){
        g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        String[] lines = {
                String.format("zoom   %.3fx", camera.getZoom()),
                String.format("cam    (%.0f, %.0f)", camera.x, camera.y),
                "",
                "drag   pan",
                "wheel  zoom"
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
}