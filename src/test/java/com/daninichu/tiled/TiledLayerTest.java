package com.daninichu.tiled;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TiledLayerTest{
    @TempDir
    Path tempDir;

    private FileSystemTiledReader reader;

    @BeforeEach
    void setUp(){
        reader = new FileSystemTiledReader();
    }

    @Test
    void hasName(){
        TiledLayer layer = readLayer("""
            <layer id="1" name="Ground" width="1" height="1">
                <data encoding="csv">1</data>
            </layer>
            """);

        assertEquals("Ground", layer.getName());
    }

    @Test
    void hasClass(){
        TiledLayer layer = readLayer("""
            <layer id="1" name="Ground" class="terrain"
                   width="1" height="1">
                <data encoding="csv">1</data>
            </layer>
            """);

        assertEquals("terrain", layer.getType());
    }

    @Test
    void hasCustomProperties(){
        TiledLayer layer = readLayer("""
            <layer id="1" name="Ground" width="1" height="1">
                <properties>
                    <property name="terrain" value="grass"/>
                    <property name="walkable" type="bool" value="true"/>
                </properties>
                <data encoding="csv">1</data>
            </layer>
            """);

        assertEquals("grass", layer.getProperty("terrain"));
        assertEquals(true, layer.getProperty("walkable"));
        assertEquals(2, layer.getProperties().size());
    }

    private TiledLayer readLayer(String layerXml){
        return readMap("""
            <map orientation="orthogonal" width="2" height="2"
                 tilewidth="32" tileheight="32">
                %s
            </map>
            """.formatted(layerXml)).getNonGroupLayers().getFirst();
    }

    private TiledMap readMap(String xml){
        try{
            Path path = tempDir.resolve("test.tmx");
            Files.writeString(path, xml);
            return reader.getMap(path.toString());
        } catch(Exception e){
            throw new RuntimeException(e);
        }
    }


    @Test
    void hasOneTileset(){
        TiledTileLayer layer = readTileLayer("""
            <tileset firstgid="1" name="terrain"
                     tilewidth="32" tileheight="32"
                     tilecount="1" columns="1">
                <image source="terrain.png" width="32" height="32"/>
            </tileset>
            """, """
            1,0,
            0,0
            """);

        assertNotNull(layer.getTile(0, 0));
        assertEquals("terrain", layer.getTile(0, 0).getTileset().getName());
    }

    @Test
    void isFull(){
        TiledTileLayer layer = readTileLayer("""
            <tileset firstgid="1" name="terrain"
                     tilewidth="32" tileheight="32"
                     tilecount="4" columns="2">
                <image source="terrain.png" width="64" height="64"/>
            </tileset>
            """, """
            1,2,
            3,4
            """);

        assertEquals(4, layer.getTileLocations().size());

        assertNotNull(layer.getTile(0, 0));
        assertNotNull(layer.getTile(1, 0));
        assertNotNull(layer.getTile(0, 1));
        assertNotNull(layer.getTile(1, 1));
    }

    @Test
    void isPartiallyFilled(){
        TiledTileLayer layer = readTileLayer("""
            <tileset firstgid="1" name="terrain"
                     tilewidth="32" tileheight="32"
                     tilecount="4" columns="2">
                <image source="terrain.png" width="64" height="64"/>
            </tileset>
            """, """
            1,0,
            0,4
            """);

        assertEquals(2, layer.getTileLocations().size());

        assertNotNull(layer.getTile(0, 0));
        assertNull(layer.getTile(1, 0));
        assertNull(layer.getTile(0, 1));
        assertNotNull(layer.getTile(1, 1));
    }

    private TiledTileLayer readTileLayer(String tilesetXml, String data){
        return (TiledTileLayer) readMap("""
            <map orientation="orthogonal" width="2" height="2"
                 tilewidth="32" tileheight="32">
                %s
                <layer id="1" name="Ground" width="2" height="2">
                    <data encoding="csv">
                        %s
                    </data>
                </layer>
            </map>
            """.formatted(tilesetXml, data)).getNonGroupLayers().getFirst();
    }


    @Test
    void isEmpty(){
        TiledObjectLayer layer = readObjectLayer("");

        assertTrue(layer.getObjects().isEmpty());
    }

    @Test
    void hasOneObject(){
        TiledObjectLayer layer = readObjectLayer("""
            <object id="1" name="Player" x="32" y="64"
                    width="32" height="32"/>
            """);

        assertEquals(1, layer.getObjects().size());

        TiledObject object = layer.getObjects().getFirst();

        assertEquals("Player", object.getName());
        assertEquals(32, object.getX());
        assertEquals(64, object.getY());
        assertEquals(32, object.getWidth());
        assertEquals(32, object.getHeight());
    }

    @Test
    void hasTwoObjects(){
        TiledObjectLayer layer = readObjectLayer("""
            <object id="1" name="Player" x="32" y="64"/>
            <object id="2" name="Enemy" x="96" y="128"/>
            """);

        assertEquals(2, layer.getObjects().size());

        TiledObject player = layer.getObjects().get(0);
        TiledObject enemy = layer.getObjects().get(1);

        assertEquals("Player", player.getName());
        assertEquals("Enemy", enemy.getName());

        assertEquals(32, player.getX());
        assertEquals(64, player.getY());

        assertEquals(96, enemy.getX());
        assertEquals(128, enemy.getY());
    }

    private TiledObjectLayer readObjectLayer(String objectsXml){
        try{
            String xml = """
                <map orientation="orthogonal" width="10" height="10"
                     tilewidth="32" tileheight="32">
                    <objectgroup id="1" name="Objects">
                        %s
                    </objectgroup>
                </map>
                """.formatted(objectsXml);

            Path path = tempDir.resolve("test.tmx");
            Files.writeString(path, xml);

            return (TiledObjectLayer) reader.getMap(path.toString())
                    .getNonGroupLayers().getFirst();
        } catch(Exception e){
            throw new RuntimeException(e);
        }
    }
}