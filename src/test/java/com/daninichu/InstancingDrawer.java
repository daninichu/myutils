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
import java.util.stream.IntStream;

import static com.badlogic.gdx.graphics.GL20.GL_ARRAY_BUFFER;

public class InstancingDrawer{
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setWindowedMode(ViewPanel.width, ViewPanel.height);
        config.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL30, 3, 3); // add this
        new Lwjgl3Application(new ViewPanel(), config);
    }

    // -------------------------------------------------------------------------

    static final class ViewPanel extends ApplicationAdapter {

        private static final int   WORLD_W            = 150_000;
        private static final int   WORLD_H            = 150_000;
        private static final int   N_RECTANGLES       = 6000000;
        private static final float MIN_RECT_SIZE = 10f;
        private static final float MAX_RECT_SIZE = 50f;
        private static final float MAX_SPEED          = 5f;
        static  final        int   width              = 1120;
        static  final        int   height             = 840;

        private static final Rectangle2D.Float worldBounds =
                new Rectangle2D.Float(0, 0, WORLD_W, WORLD_H);

        private static final Color[] palette = {
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
        private final ArrayList<ColoredRect> allRects = new ArrayList<>(N_RECTANGLES);

        // GL objects
        private ShaderProgram shader;
        private int unitVBO;       // static 8-vertex unit rect outline
        private int vao;

        // CPU-side buffers
        private float[] xyRaw = new float[N_RECTANGLES * XY_FLOATS];
        private FloatBuffer xyData;     // x, y          — updated every frame
        private FloatBuffer staticData; // w, h, r, g, b, a — uploaded once

        private int xyVBO;
        private int staticVBO;

        private static final int XY_FLOATS     = 2;
        private static final int STATIC_FLOATS = 6;


        // ── Shaders ──────────────────────────────────────────────────────────

        private static final String VERT = """
                // VERT
                #version 330 core
                layout(location = 0) in vec2 a_unitPos;
                layout(location = 1) in vec2 a_xy;          // instanced — updated every frame
                layout(location = 2) in vec4 a_wh;          // instanced — static (w, h, r, g)
                layout(location = 3) in vec2 a_ba;          // instanced — static (b, a)
                
                uniform mat4 u_proj;
                out vec4 v_color;
                
                void main() {
                    vec2 world  = a_unitPos * a_wh.xy + a_xy;
                    gl_Position = u_proj * vec4(world, 0.0, 1.0);
                    v_color     = vec4(a_wh.zw, a_ba);
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
            camera = new OrthographicCamera(width, height);
            camera.setToOrtho(true, width, height);

            ShaderProgram.pedantic = false;
            shader = new ShaderProgram(VERT, FRAG);
            if (!shader.isCompiled())
                throw new RuntimeException("Shader error:\n" + shader.getLog());

            buildUnitRectVBO();
            buildInstanceVBOs();
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

            Gdx.gl.glBindBuffer(GL_ARRAY_BUFFER, unitVBO);
            Gdx.gl.glBufferData(GL_ARRAY_BUFFER,
                    unit.length * Float.BYTES, buf, GL20.GL_STATIC_DRAW);
        }

        private void buildInstanceVBOs() {
            IntBuffer id = BufferUtils.newIntBuffer(1);

            // ── xy VBO ───────────────────────────────────────────────────────────
            xyData = BufferUtils.newFloatBuffer(N_RECTANGLES * XY_FLOATS);

            Gdx.gl.glGenBuffers(1, id);
            xyVBO = id.get(0);
            Gdx.gl.glBindBuffer(GL_ARRAY_BUFFER, xyVBO);
            Gdx.gl.glBufferData(GL_ARRAY_BUFFER,
                    N_RECTANGLES * XY_FLOATS * Float.BYTES,
                    null, GL20.GL_STREAM_DRAW);

            // ── static VBO ───────────────────────────────────────────────────────
            staticData = BufferUtils.newFloatBuffer(N_RECTANGLES * STATIC_FLOATS);

            Gdx.gl.glGenBuffers(1, id);
            staticVBO = id.get(0);
            Gdx.gl.glBindBuffer(GL_ARRAY_BUFFER, staticVBO);
            Gdx.gl.glBufferData(GL_ARRAY_BUFFER,
                    N_RECTANGLES * STATIC_FLOATS * Float.BYTES,
                    null, GL20.GL_STATIC_DRAW);
        }

        private void buildVAO() {
            IntBuffer id = BufferUtils.newIntBuffer(1);
            Gdx.gl30.glGenVertexArrays(1, id);
            vao = id.get(0);
            Gdx.gl30.glBindVertexArray(vao);

            // attrib 0 — unit rect verts (non-instanced)
            Gdx.gl.glBindBuffer(GL_ARRAY_BUFFER, unitVBO);
            Gdx.gl.glEnableVertexAttribArray(0);
            Gdx.gl.glVertexAttribPointer(0, 2, GL20.GL_FLOAT, false, 2 * Float.BYTES, 0);
            Gdx.gl30.glVertexAttribDivisor(0, 0);

            // attrib 1 — a_xy (instanced, updated every frame)
            Gdx.gl.glBindBuffer(GL_ARRAY_BUFFER, xyVBO);
            Gdx.gl.glEnableVertexAttribArray(1);
            Gdx.gl.glVertexAttribPointer(1, 2, GL20.GL_FLOAT, false, XY_FLOATS * Float.BYTES, 0);
            Gdx.gl30.glVertexAttribDivisor(1, 1);

            // attrib 2 — a_wh (w, h, r, g) from staticVBO
            Gdx.gl.glBindBuffer(GL_ARRAY_BUFFER, staticVBO);
            Gdx.gl.glEnableVertexAttribArray(2);
            Gdx.gl.glVertexAttribPointer(2, 4, GL20.GL_FLOAT, false,
                    STATIC_FLOATS * Float.BYTES, 0);
            Gdx.gl30.glVertexAttribDivisor(2, 1);

            // attrib 3 — a_ba (b, a) from staticVBO, offset past w,h,r,g
            Gdx.gl.glEnableVertexAttribArray(3);
            Gdx.gl.glVertexAttribPointer(3, 2, GL20.GL_FLOAT, false,
                    STATIC_FLOATS * Float.BYTES, 4 * Float.BYTES);
            Gdx.gl30.glVertexAttribDivisor(3, 1);

            Gdx.gl30.glBindVertexArray(0);
        }

        // ── Data ─────────────────────────────────────────────────────────────

        private void regenerate() {
            allRects.clear();

            Random rng = new Random();
            for (int i = 0; i < N_RECTANGLES; i++) {
                float w  = MIN_RECT_SIZE + rng.nextFloat() * (MAX_RECT_SIZE - MIN_RECT_SIZE);
                float h  = MIN_RECT_SIZE + rng.nextFloat() * (MAX_RECT_SIZE - MIN_RECT_SIZE);
                float x  = rng.nextFloat() * (WORLD_W - w);
                float y  = rng.nextFloat() * (WORLD_H - h);
                float vx = (rng.nextFloat() * 2 - 1) * MAX_SPEED;
                float vy = (rng.nextFloat() * 2 - 1) * MAX_SPEED;
                allRects.add(new ColoredRect(x, y, w, h, vx, vy,
                        palette[rng.nextInt(palette.length)]));
            }

            staticData.clear();
            for (ColoredRect r : allRects) {
                staticData.put(r.w).put(r.h)
                        .put(r.cr).put(r.cg).put(r.cb).put(r.ca);
            }
            staticData.flip();

            Gdx.gl.glBindBuffer(GL_ARRAY_BUFFER, staticVBO);
            Gdx.gl.glBufferSubData(GL_ARRAY_BUFFER, 0,
                    N_RECTANGLES * STATIC_FLOATS * Float.BYTES, staticData);
        }

        // ── Render ───────────────────────────────────────────────────────────

        @Override
        public void render() {
//            update();
            int count = allRects.size();
            draw(count);
        }

        private void update(){
            for(ColoredRect rect : allRects){
                rect.x += rect.vx;
                rect.y += rect.vy;
                if(rect.x < 0){
                    rect.x = 0;
                    rect.vx = -rect.vx;
                } else if(rect.x + rect.w > WORLD_W){
                    rect.x = WORLD_W - rect.w;
                    rect.vx = -rect.vx;
                }
                if(rect.y < 0){
                    rect.y = 0;
                    rect.vy = -rect.vy;
                } else if(rect.y + rect.h > WORLD_H){
                    rect.y = WORLD_H - rect.h;
                    rect.vy = -rect.vy;
                }
            }
        }

        private void draw(int count) {
            double totalTime = 0;
            Timer timer = new Timer();

            Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

            // ── Upload instance data ──────────────────────────────────────

            int cores = Runtime.getRuntime().availableProcessors();
            int chunk = count / cores;

            IntStream.range(0, cores).parallel().forEach(t -> {
                int from = t * chunk;
                int to   = (t == cores - 1) ? count : from + chunk;
                for (int i = from; i < to; i++) {
                    ColoredRect r = allRects.get(i);
                    xyRaw[i * XY_FLOATS]     = r.x;
                    xyRaw[i * XY_FLOATS + 1] = r.y;
                }
            });

            xyData.clear();
            xyData.put(xyRaw, 0, count * XY_FLOATS);
            xyData.flip();

//            xyData.clear();
//            for (ColoredRect r : allRects) {
//                xyData.put(r.x).put(r.y);
//            }
//            xyData.flip();


            double fillTime = timer.seconds();
            totalTime += fillTime;
            System.out.printf("fill:   %.9fs\n", fillTime);
            timer.reset();

            Gdx.gl.glBindBuffer(GL_ARRAY_BUFFER, xyVBO);
            Gdx.gl.glBufferSubData(GL_ARRAY_BUFFER, 0, count * XY_FLOATS * Float.BYTES, xyData);

            double uploadTime = timer.seconds();
            totalTime += uploadTime;
            System.out.printf("upload: %.9fs\n", uploadTime);
            timer.reset();

            // ── Draw all rects in one call ────────────────────────────────
            shader.bind();
            shader.setUniformMatrix("u_proj", camera.combined);

            Gdx.gl30.glBindVertexArray(vao);
            Gdx.gl30.glDrawArraysInstanced(GL20.GL_LINES, 0, 8, count);
            Gdx.gl30.glBindVertexArray(0);

            double drawTime = timer.seconds();
            totalTime += drawTime;
            System.out.printf("draw:   %.9fs\n", drawTime);
            System.out.printf("total:  %.9fs\n", totalTime);
            System.out.println();
        }

        private void removeRect(int index) {
            int last = allRects.size() - 1;
            allRects.set(index, allRects.get(last));
            allRects.remove(last);

            uploadStaticAt(index, allRects.get(index));
        }

        private void uploadStaticAt(int index, ColoredRect r) {
            staticData.clear();
            staticData.put(r.w).put(r.h)
                    .put(r.cr).put(r.cg).put(r.cb).put(r.ca);
            staticData.flip();

            Gdx.gl.glBindBuffer(GL_ARRAY_BUFFER, staticVBO);
            Gdx.gl.glBufferSubData(GL_ARRAY_BUFFER,
                    index * STATIC_FLOATS * Float.BYTES,
                    STATIC_FLOATS * Float.BYTES,
                    staticData);
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
            shader.dispose();

            IntBuffer bufs = BufferUtils.newIntBuffer(3);
            bufs.put(0, unitVBO).put(1, xyVBO).put(2, staticVBO);
            Gdx.gl.glDeleteBuffers(3, bufs);

            IntBuffer vaoId = BufferUtils.newIntBuffer(1);
            vaoId.put(0, vao);
            Gdx.gl30.glDeleteVertexArrays(1, vaoId);
        }
    }

    // ── ColoredRect ──────────────────────────────────────────────────────────

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