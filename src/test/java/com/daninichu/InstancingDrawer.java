package com.daninichu;

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.BufferUtils;
import com.daninichu.util.Timer;

import java.awt.geom.Rectangle2D;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Random;

public class InstancingDrawer{
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setWindowedMode(ViewPanel.SCREEN_WIDTH, ViewPanel.SCREEN_HEIGHT);
        config.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL30, 3, 3); // add this
        new Lwjgl3Application(new ViewPanel(), config);
    }

    // -------------------------------------------------------------------------

    static final class ViewPanel extends ApplicationAdapter {
        private static final int SCREEN_WIDTH = 1120;
        private static final int SCREEN_HEIGHT = 840;

        private static final int WORLD_W = 150000;
        private static final int WORLD_H = 150000;

        private static final int N_RECTS = 6000000;
        private static final int MIN_RECT_SIZE = 10;
        private static final int MAX_RECT_SIZE = 50;
        private static final int MAX_SPEED = 5;

        private static final Rectangle2D.Float WORLD_BOUNDS =
                new Rectangle2D.Float(0, 0, WORLD_W, WORLD_H);

        private static final Color[] PALETTE = {
                new Color(255/255f,  100/255f,  80/255f, 1f),
                new Color(255/255f, 180/255f,  60/255f, 1f),
                new Color( 60/255f, 210/255f, 160/255f, 1f),
                new Color( 80/255f, 160/255f, 255/255f, 1f),
                new Color(200/255f, 100/255f, 255/255f, 1f),
                new Color(255/255f, 120/255f, 180/255f, 1f),
        };

        // Camera
        private OrthographicCamera camera;
        private static final float MIN_ZOOM = 0.005f;
        private static final float MAX_ZOOM = Math.max(WORLD_W / 750f, WORLD_H / 750f);
        private int dragStartX, dragStartY;

        // Data
        private final ArrayList<ColoredRect> allRects = new ArrayList<>(N_RECTS);

        // GL objects
        private ShaderProgram shader;
        private int unitVBO;       // static 8-vertex unit rect outline
        private int instanceVBO;   // per-instance data, updated every frame
        private int vao;

        // CPU-side instance buffer: 8 floats × N rects (x,y,w,h,r,g,b,a)
        private static final int FLOATS_PER_INSTANCE = 8;
        private FloatBuffer instanceData;

        // ── Shaders ──────────────────────────────────────────────────────────

        private static final String VERT = """
                #version 330 core
                layout(location = 0) in vec2 a_unitPos;   // unit rect corner
                layout(location = 1) in vec4 a_rect;      // x, y, w, h  (instanced)
                layout(location = 2) in vec4 a_color;     // r, g, b, a  (instanced)
                
                uniform mat4 u_proj;
                out vec4 v_color;
                
                void main() {
                    vec2 world = a_unitPos * a_rect.zw + a_rect.xy;
                    gl_Position = u_proj * vec4(world, 0.0, 1.0);
                    v_color = a_color;
                }
                """;

        private static final String FRAG = """
                #version 330 core
                in  vec4 v_color;
                out vec4 fragColor;
                void main() { fragColor = v_color; }
                """;

        // ── Init ─────────────────────────────────────────────────────────────

        @Override
        public void create() {
            camera = new OrthographicCamera(SCREEN_WIDTH, SCREEN_HEIGHT);
            camera.setToOrtho(true, SCREEN_WIDTH, SCREEN_HEIGHT);

            ShaderProgram.pedantic = false;
            shader = new ShaderProgram(VERT, FRAG);
            if (!shader.isCompiled())
                throw new RuntimeException("Shader error:\n" + shader.getLog());

            buildUnitRectVBO();
            buildInstanceVBO();
            buildVAO();

            regenerate();
            setupInput();
        }

        private void buildUnitRectVBO() {
            // 4 line segments (8 verts) describing a [0,1]×[0,1] rect outline
            float[] unit = {
                    0f, 0f,  1f, 0f,   // bottom
                    1f, 0f,  1f, 1f,   // right
                    1f, 1f,  0f, 1f,   // top
                    0f, 1f,  0f, 0f,   // left
            };

            IntBuffer id = BufferUtils.newIntBuffer(1);
            Gdx.gl.glGenBuffers(1, id);
            unitVBO = id.get(0);

            FloatBuffer buf = BufferUtils.newFloatBuffer(unit.length);
            buf.put(unit).flip();

            Gdx.gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, unitVBO);
            Gdx.gl.glBufferData(GL20.GL_ARRAY_BUFFER,
                    unit.length * Float.BYTES, buf, GL20.GL_STATIC_DRAW);
        }

        private void buildInstanceVBO() {
            IntBuffer id = BufferUtils.newIntBuffer(1);
            Gdx.gl.glGenBuffers(1, id);
            instanceVBO = id.get(0);               // ← was accidentally creating a VAO here

            instanceData = BufferUtils.newFloatBuffer(N_RECTS * FLOATS_PER_INSTANCE);

            Gdx.gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, instanceVBO);
            Gdx.gl.glBufferData(
                    GL20.GL_ARRAY_BUFFER,
                    N_RECTS * FLOATS_PER_INSTANCE * Float.BYTES,
                    null,
                    GL20.GL_STREAM_DRAW);
        }

        private void buildVAO() {
            IntBuffer id = BufferUtils.newIntBuffer(1);
            Gdx.gl30.glGenVertexArrays(1, id);     // ← was using int[] overload
            vao = id.get(0);

            Gdx.gl30.glBindVertexArray(vao);

            // attrib 0 — unit rect verts (non-instanced)
            Gdx.gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, unitVBO);
            Gdx.gl.glEnableVertexAttribArray(0);
            Gdx.gl.glVertexAttribPointer(0, 2, GL20.GL_FLOAT, false, 2 * Float.BYTES, 0);
            Gdx.gl30.glVertexAttribDivisor(0, 0);

            // attrib 1 — a_rect (x,y,w,h) instanced
            Gdx.gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, instanceVBO);  // ← must bind BEFORE defining attribs
            Gdx.gl.glEnableVertexAttribArray(1);
            Gdx.gl.glVertexAttribPointer(1, 4, GL20.GL_FLOAT, false,
                    FLOATS_PER_INSTANCE * Float.BYTES, 0);
            Gdx.gl30.glVertexAttribDivisor(1, 1);

            // attrib 2 — a_color (r,g,b,a) instanced
            Gdx.gl.glEnableVertexAttribArray(2);
            Gdx.gl.glVertexAttribPointer(2, 4, GL20.GL_FLOAT, false,
                    FLOATS_PER_INSTANCE * Float.BYTES, 4 * Float.BYTES);
            Gdx.gl30.glVertexAttribDivisor(2, 1);

            Gdx.gl30.glBindVertexArray(0);
        }
        // ── Data ─────────────────────────────────────────────────────────────

        private void regenerate() {
            allRects.clear();
            Random rng = new Random();
            for (int i = 0; i < N_RECTS; i++) {
                float w  = MIN_RECT_SIZE + rng.nextFloat() * (MAX_RECT_SIZE - MIN_RECT_SIZE);
                float h  = MIN_RECT_SIZE + rng.nextFloat() * (MAX_RECT_SIZE - MIN_RECT_SIZE);
                float x  = rng.nextFloat() * (WORLD_W - w);
                float y  = rng.nextFloat() * (WORLD_H - h);
                float vx = (rng.nextFloat() * 2 - 1) * MAX_SPEED;
                float vy = (rng.nextFloat() * 2 - 1) * MAX_SPEED;
                allRects.add(new ColoredRect(x, y, w, h, vx, vy, PALETTE[rng.nextInt(PALETTE.length)]));
            }
        }

        // ── Render ───────────────────────────────────────────────────────────

        @Override
        public void render() {
//            update();
            draw();
        }

        private void update(int count) {
            for(ColoredRect r : allRects) {
                float x = r.x + r.vx;
                float y = r.y + r.vy;
                if(x < 0){
                    x = 0;
                    r.vx = -r.vx;
                } else if(x + r.w > WORLD_W){
                    x = WORLD_W - r.w;
                    r.vx = -r.vx;
                }
                if(y < 0){
                    y = 0;
                    r.vy = -r.vy;
                } else if(y + r.h > WORLD_H){
                    y = WORLD_H - r.h;
                    r.vy = -r.vy;
                }
                r.x = x;
                r.y = y;
            }
        }

        private void draw() {
            double totalTime = 0;
            Timer timer = new Timer();

            Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

            // ── Upload instance data ──────────────────────────────────────
            instanceData.clear();
            for (ColoredRect r : allRects) {
                instanceData.put(r.x).put(r.y).put(r.w).put(r.h);
                instanceData.put(r.cr).put(r.cg).put(r.cb).put(r.ca);
            }
            instanceData.flip();

            double fillTime = timer.seconds();
            totalTime += fillTime;
            System.out.printf("fill:   %.9fs\n", fillTime);
            timer.reset();

            Gdx.gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, instanceVBO);
            Gdx.gl.glBufferSubData(GL20.GL_ARRAY_BUFFER, 0,
                    allRects.size() * FLOATS_PER_INSTANCE * Float.BYTES,
                    instanceData);

            double uploadTime = timer.seconds();
            totalTime += uploadTime;
            System.out.printf("upload: %.9fs\n", uploadTime);
            timer.reset();

            // ── Draw all rects in one call ────────────────────────────────
            shader.bind();
            shader.setUniformMatrix("u_proj", camera.combined);

            Gdx.gl30.glBindVertexArray(vao);
            Gdx.gl30.glDrawArraysInstanced(
                    GL20.GL_LINES,
                    0,
                    8,                  // 8 verts in the unit rect
                    allRects.size()     // one instance per rect
            );
            Gdx.gl30.glBindVertexArray(0);

            double drawTime = timer.seconds();
            totalTime += drawTime;
            System.out.printf("draw:   %.9fs\n", drawTime);
            System.out.printf("total:  %.9fs\n", totalTime);
            System.out.println();
        }

        // ── Input ─────────────────────────────────────────────────────────────

        private void setupInput() {
            Gdx.input.setInputProcessor(new InputAdapter() {
                public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                    dragStartX = screenX;
                    dragStartY = screenY;
                    return true;
                }

                public boolean touchDragged(int screenX, int screenY, int pointer) {
                    camera.position.x -= (screenX - dragStartX) * camera.zoom;
                    camera.position.y -= (screenY - dragStartY) * camera.zoom;
                    dragStartX = screenX;
                    dragStartY = screenY;
                    camera.update();
                    return true;
                }

                public boolean scrolled(float amountX, float amountY) {
                    Vector3 before = camera.unproject(
                            new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
                    camera.zoom *= amountY > 0 ? 1.05f : 1f / 1.05f;
                    camera.zoom  = Math.min(Math.max(MIN_ZOOM, camera.zoom), MAX_ZOOM);
                    Vector3 after = camera.unproject(
                            new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
                    camera.position.add(before.x - after.x, before.y - after.y, 0);
                    camera.update();
                    return true;
                }
            });
        }

        @Override
        public void dispose() {
            IntBuffer bufs = BufferUtils.newIntBuffer(2);
            bufs.put(0, unitVBO).put(1, instanceVBO);
            Gdx.gl.glDeleteBuffers(2, bufs);

            IntBuffer vaoId = BufferUtils.newIntBuffer(1);
            vaoId.put(0, vao);
            Gdx.gl30.glDeleteVertexArrays(1, vaoId);
        }
    }

    static final class ColoredRect {
        float x, y, w, h, vx, vy;
        float cr, cg, cb, ca;

        ColoredRect(float x, float y, float w, float h, float vx, float vy, Color color) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.vx = vx;
            this.vy = vy;
            cr = color.r;
            cg = color.g;
            cb = color.b;
            ca = color.a;
        }
    }
}