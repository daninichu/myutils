package com.daninichu;

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.daninichu.util.Timer;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Random;

public class ShapeRendererDrawer{

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setWindowedMode(ViewPanel.width, ViewPanel.height);

        new Lwjgl3Application(
                new ViewPanel(),
                config
        );
    }

    static final class ViewPanel extends ApplicationAdapter{

        // World dimensions
        private static final int WORLD_W = 150000;
        private static final int WORLD_H = 150000;

        // Rectangles
        private static final int N_RECTANGLES = 1000000;
        private static final float MIN_RECTANGLE_SIZE = 10;
        private static final float MAX_RECTANGLE_SIZE = 50;
        private static final float MAX_SPEED = 50;
        private static final Rectangle2D.Float worldBounds = new Rectangle2D.Float(0, 0, WORLD_W, WORLD_H);
        private static final Color[] palette = {
                new Color(255/255f, 80/255f,  60/255f, 1),   // red-orange
                new Color(255/255f, 180/255f, 40/255f, 1),   // amber
                new Color(60/255f,  210/255f, 160/255f, 1),  // teal
                new Color(80/255f,  160/255f, 255/255f, 1),  // sky blue
                new Color(200/255f, 100/255f, 255/255f, 1),  // violet
                new Color(255/255f, 120/255f, 180/255f, 1),  // pink
        };

        // Camera state
        private OrthographicCamera camera;
        private ShapeRenderer shapeRenderer;
        private static final float MIN_ZOOM = 0.005f;
        private static final float MAX_ZOOM = Math.max(WORLD_W / 750, WORLD_H / 750);

        // Drag state
        private int dragStartX, dragStartY;

        // Data
        private final ArrayList<ColoredRect> allRects = new ArrayList<>(N_RECTANGLES);
        private static final int width = 1120;
        private static final int height = 840;

        private void regenerate() {
            allRects.clear();

            Random rng = new Random();
            for (int i = 0; i < N_RECTANGLES; i++) {
                float w = MIN_RECTANGLE_SIZE + rng.nextFloat() * (MAX_RECTANGLE_SIZE - MIN_RECTANGLE_SIZE);
                float h = MIN_RECTANGLE_SIZE + rng.nextFloat() * (MAX_RECTANGLE_SIZE - MIN_RECTANGLE_SIZE);
                float x = rng.nextFloat() * (WORLD_W - w);
                float y = rng.nextFloat() * (WORLD_H - h);
                float vx = (rng.nextFloat() * 2 - 1) * MAX_SPEED;
                float vy = (rng.nextFloat() * 2 - 1) * MAX_SPEED;
                Color color = palette[rng.nextInt(palette.length)];
                ColoredRect rect = new ColoredRect(x, y, w, h, vx, vy, color);
                allRects.add(rect);
            }
        }

        private void draw(){
            Timer timer = new Timer();

            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

            // -------------------
            // Use instancing here
            // -------------------
            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

            for(ColoredRect rect : allRects){
                shapeRenderer.setColor(rect.color);
                shapeRenderer.rect(rect.x, rect.y, rect.w, rect.h);
            }
            shapeRenderer.setColor(Color.WHITE);
            shapeRenderer.rect(worldBounds.x, worldBounds.y, worldBounds.width, worldBounds.height);

            shapeRenderer.end();

            System.out.println(timer.seconds());
        }

        @Override
        public void render() {
            draw();
        }

        @Override
        public void create(){
            camera = new OrthographicCamera(width, height);
            camera.setToOrtho(true, width, height);
            shapeRenderer = new ShapeRenderer();

            regenerate();

            Gdx.input.setInputProcessor(new InputAdapter() {
                public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                    dragStartX = screenX;
                    dragStartY = screenY;
                    return true;
                }

                public boolean touchDragged(int screenX, int screenY, int pointer) {
                    int dx = screenX - dragStartX;
                    int dy = screenY - dragStartY;

                    camera.position.x -= dx * camera.zoom;
                    camera.position.y -= dy * camera.zoom;

                    dragStartX = screenX;
                    dragStartY = screenY;

                    camera.update();

                    return true;
                }

                public boolean scrolled(float amountX, float amountY) {
                    Vector3 before = new Vector3(
                            Gdx.input.getX(),
                            Gdx.input.getY(),
                            0
                    );

                    camera.unproject(before);

                    camera.zoom *= amountY > 0 ? 1.05f : 1f / 1.05f;
                    camera.zoom = Math.min(Math.max(MIN_ZOOM, camera.zoom), MAX_ZOOM);

                    Vector3 after = new Vector3(
                            Gdx.input.getX(),
                            Gdx.input.getY(),
                            0
                    );

                    camera.unproject(after);

                    camera.position.add(
                            before.x - after.x,
                            before.y - after.y,
                            0
                    );
                    camera.update();

                    return true;
                }
            });
        }
    }

    // =========================================================================

    static final class ColoredRect{
        float x, y, w, h, vx, vy;
        Color color;

        ColoredRect(float x, float y, float w, float h, float vx, float vy, Color color){
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