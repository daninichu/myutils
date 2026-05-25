package com.daninichu;

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
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
        config.setWindowedMode(ViewPanel.SCREEN_W, ViewPanel.SCREEN_H);
        config.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL30, 3, 3);
        new Lwjgl3Application(new ViewPanel(), config);
    }

    static final class ViewPanel extends ApplicationAdapter {
        private static final int SCREEN_W = 1120;
        private static final int SCREEN_H = 840;

        private static final int WORLD_W = 150000;
        private static final int WORLD_H = 150000;

        private static final int N_RECTS = 1000000;
        private static final int MIN_RECT_SIZE = 10;
        private static final int MAX_RECT_SIZE = 50;
        private static final int MAX_SPEED = 5;

        private static final Rectangle2D.Float WORLD_BOUNDS = new Rectangle2D.Float(0, 0, WORLD_W, WORLD_H);

        private static final float[][] PALETTE = {
                {   255/255f,   100/255f,    80/255f}, // red
                {   255/255f,   180/255f,    60/255f}, // orange
                {    60/255f,   210/255f,   160/255f}, // green
                {    80/255f,   160/255f,   255/255f}, // blue
                {   200/255f,   100/255f,   255/255f}, // violet
                {   255/255f,   120/255f,   180/255f}, // pink
        };

        // Camera
        private OrthographicCamera camera;
        private static final float ZOOM_SPEED = 1.05f;
        private static final float MIN_ZOOM = 0.005f;
        private static final float MAX_ZOOM = Math.max(WORLD_W / 750f, WORLD_H / 750f);
        private int dragStartX, dragStartY;

        // Data
        private final ArrayList<ColoredRect> allRects = new ArrayList<>(N_RECTS);
        private final ArrayList<ColoredRect> selectedRects = new ArrayList<>(N_RECTS);
        private float[] rectData;
        private float[] rvx, rvy;

        {
            rectData = new float[N_RECTS * FLOATS_PER_INSTANCE];
            rvx = new float[N_RECTS]; rvy = new float[N_RECTS];
        }

        // GL objects
        private ShaderProgram shader;
        private int unitVBO;       // static 8-vertex unit rect outline
        private int instanceVBO;   // per-instance data, updated every frame
        private int vao;

        // CPU-side instance buffer: 7 floats × N rects (x,y,w,h,r,g,b)
        private static final int FLOATS_PER_INSTANCE = 7;
        private FloatBuffer instanceData;
        private float[] instanceRaw = new float[N_RECTS * FLOATS_PER_INSTANCE];

        // ── Shaders ──────────────────────────────────────────────────────────

        private static final String VERT = """
                #version 330 core
                layout(location = 0) in vec2 a_unitPos;   // unit rect corner
                layout(location = 1) in vec4 a_rect;      // x, y, w, h
                layout(location = 2) in vec3 a_color;     // r, g, b,
                
                uniform mat4 u_proj;
                out vec4 v_color;
                
                void main() {
                    vec2 world = a_unitPos * a_rect.zw + a_rect.xy;
                    gl_Position = u_proj * vec4(world, 0.0, 1.0);
                    v_color = vec4(a_color, 1.0);
                }
                """;

        private static final String FRAG = """
                #version 330 core
                in  vec4 v_color;
                out vec4 fragColor;
                void main() { fragColor = v_color; }
                """;

        @Override
        public void create() {
            camera = new OrthographicCamera();
            camera.setToOrtho(true, SCREEN_W, SCREEN_H);

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

            Gdx.gl.glBindBuffer(GL_ARRAY_BUFFER, unitVBO);
            Gdx.gl.glBufferData(GL_ARRAY_BUFFER, unit.length * Float.BYTES, buf, GL20.GL_STATIC_DRAW);
        }

        private void buildInstanceVBO() {
            IntBuffer id = BufferUtils.newIntBuffer(1);
            Gdx.gl.glGenBuffers(1, id);
            instanceVBO = id.get(0);

            instanceData = BufferUtils.newFloatBuffer(N_RECTS * FLOATS_PER_INSTANCE);

            Gdx.gl.glBindBuffer(GL_ARRAY_BUFFER, instanceVBO);
            Gdx.gl.glBufferData(
                    GL_ARRAY_BUFFER,
                    N_RECTS * FLOATS_PER_INSTANCE * Float.BYTES,
                    null,
                    GL20.GL_STREAM_DRAW);
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

            // attrib 1 — a_rect (x,y,w,h) instanced
            Gdx.gl.glBindBuffer(GL_ARRAY_BUFFER, instanceVBO);
            Gdx.gl.glEnableVertexAttribArray(1);
            Gdx.gl.glVertexAttribPointer(1, 4, GL20.GL_FLOAT, false,
                    FLOATS_PER_INSTANCE * Float.BYTES, 0);
            Gdx.gl30.glVertexAttribDivisor(1, 1);

            // attrib 2 — a_color (r,g,b) instanced
            Gdx.gl.glEnableVertexAttribArray(2);
            Gdx.gl.glVertexAttribPointer(2, 3, GL20.GL_FLOAT, false,
                    FLOATS_PER_INSTANCE * Float.BYTES, 4 * Float.BYTES);
            Gdx.gl30.glVertexAttribDivisor(2, 1);

            Gdx.gl30.glBindVertexArray(0);
        }

        private void regenerate() {
            allRects.clear();
            Random rng = new Random();
            for (int i = 0; i < N_RECTS; i++) {
                int j = i * FLOATS_PER_INSTANCE;
                rectData[j+2]  = MIN_RECT_SIZE + rng.nextFloat() * (MAX_RECT_SIZE - MIN_RECT_SIZE);
                rectData[j+3]  = MIN_RECT_SIZE + rng.nextFloat() * (MAX_RECT_SIZE - MIN_RECT_SIZE);
                rectData[j]  = rng.nextFloat() * (WORLD_W - rectData[j+2]);
                rectData[j+1]  = rng.nextFloat() * (WORLD_H - rectData[j+3]);

                float[] rgb = PALETTE[rng.nextInt(PALETTE.length)];
                rectData[j+4] = rgb[0];
                rectData[j+5] = rgb[1];
                rectData[j+6] = rgb[2];

                rvx[i] = (rng.nextFloat() * 2 - 1) * MAX_SPEED;
                rvy[i] = (rng.nextFloat() * 2 - 1) * MAX_SPEED;

                allRects.add(new ColoredRect(i));
            }
        }

        @Override
        public void render() {
            double updateTime = 0;
            double searchTime = 0;
            double drawTime = 0;
            Timer timer = new Timer();

            if(MAX_SPEED != 0){
//                update();
                updateTime = timer.seconds();
                timer.reset();
            }

            float camLeft   = camera.position.x - (camera.viewportWidth  * camera.zoom) / 2f;
            float camRight  = camera.position.x + (camera.viewportWidth  * camera.zoom) / 2f;
            float camBottom = camera.position.y - (camera.viewportHeight * camera.zoom) / 2f;
            float camTop    = camera.position.y + (camera.viewportHeight * camera.zoom) / 2f;

            selectedRects.clear();
            for (ColoredRect r : allRects) {
                int i = r.index * FLOATS_PER_INSTANCE;

                float x = rectData[i];
                float y = rectData[i+1];
                float w = rectData[i+2];
                float h = rectData[i+3];
                if(x < camRight && x + w > camLeft && y < camTop && y + h > camBottom){
                    selectedRects.add(r);
                }
            }
            searchTime = timer.seconds();
            timer.reset();

            draw(selectedRects);

            drawTime = timer.seconds();

            double totalTime = updateTime + searchTime + drawTime;
            System.out.printf("update: %.9fs\n", updateTime);
            System.out.printf("search: %.9fs\n", searchTime);
            System.out.printf("draw:   %.9fs\n", drawTime);
            System.out.printf("total:  %.9fs\n\n", totalTime);
        }

        private void update() {
            for(ColoredRect r : allRects) {
//                int i = r.index * FLOATS_PER_INSTANCE;
//
//                float maxX;
//                float maxY;
//                float x = rectData[i] + rvx[r.index];
//                float y = r.y + r.vy;
//                if(x < 0){
//                    x = 0;
//                    r.vx = -r.vx;
//                } else if(x > (maxX = WORLD_W - r.w)){
//                    x = maxX;
//                    r.vx = -r.vx;
//                }
//                if(y < 0){
//                    y = 0;
//                    r.vy = -r.vy;
//                } else if(y > (maxY = WORLD_H - r.h)){
//                    y = maxY;
//                    r.vy = -r.vy;
//                }
//                r.x = x;
//                r.y = y;
            }
        }

        private void draw(ArrayList<ColoredRect> selectedRects) {
            Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

            // ── Upload instance data ──────────────────────────────────────

            int count = selectedRects.size();
            int cores = Runtime.getRuntime().availableProcessors();
            int chunk = count / cores;
            IntStream.range(0, cores).parallel().forEach(core -> {
                int from = core * chunk;
                int to   = core == cores - 1 ? count : (core + 1) * chunk;
                for (int r = from; r < to; r++) {
                    int i = selectedRects.get(r).index * FLOATS_PER_INSTANCE;
                    int j = r * FLOATS_PER_INSTANCE;
                    System.arraycopy(rectData, i, instanceRaw, j, FLOATS_PER_INSTANCE);
                }
            });

            // One bulk copy into FloatBuffer
            instanceData.clear();
            instanceData.put(instanceRaw, 0, count * FLOATS_PER_INSTANCE);
            instanceData.flip();

            Gdx.gl.glBindBuffer(GL_ARRAY_BUFFER, instanceVBO);
            Gdx.gl.glBufferSubData(GL_ARRAY_BUFFER, 0,
                    count * FLOATS_PER_INSTANCE * Float.BYTES,
                    instanceData);

            // ── Draw all rects in one call ────────────────────────────────
            shader.bind();
            shader.setUniformMatrix("u_proj", camera.combined);
            Gdx.gl30.glBindVertexArray(vao);
            Gdx.gl30.glDrawArraysInstanced(GL20.GL_LINES, 0, 8, count);
            Gdx.gl30.glBindVertexArray(0);
        }

        private void removeRect(int listIndex) {
            int last     = allRects.size() - 1;
            ColoredRect toRemove = allRects.get(listIndex);
            ColoredRect lastRect = allRects.get(last);

            int slot     = toRemove.index;
            int slotLast = lastRect.index;

            int i = slot * FLOATS_PER_INSTANCE;
            int j = slotLast * FLOATS_PER_INSTANCE;

            // Swap array data into the removed slot
            rectData[i++]  = rectData[j++];
            rectData[i++]  = rectData[j++];
            rectData[i++]  = rectData[j++];
            rectData[i++]  = rectData[j++];
            rectData[i++]  = rectData[j++];
            rectData[i++]  = rectData[j++];
            rectData[i++]  = rectData[j++];

            lastRect.index = slot;

            allRects.set(listIndex, lastRect);
            allRects.remove(last);
        }

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
                    int mouseX = Gdx.input.getX();
                    int mouseY = Gdx.input.getY();

                    Vector3 before = camera.unproject(new Vector3(mouseX, mouseY, 0));
                    float factor = amountY > 0? ZOOM_SPEED : 1f / ZOOM_SPEED;
                    camera.zoom = Math.min(Math.max(MIN_ZOOM, camera.zoom * factor), MAX_ZOOM);
                    camera.update();

                    Vector3 after = camera.unproject(new Vector3(mouseX, mouseY, 0));
                    camera.position.add(before.x - after.x, before.y - after.y, 0);
                    camera.update();
                    return true;
                }
            });
        }
    }

    static final class ColoredRect implements Comparable<ColoredRect> {
        int index;

        ColoredRect(int index) {
            this.index = index;
        }

        public int compareTo(ColoredRect o){
            return index - o.index;
        }
    }
}