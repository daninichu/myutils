package com.daninichu.tiled;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TiledMapTest{
    @TempDir
    Path tempDir;

    FileSystemTiledReader reader;

    @BeforeEach
    void setUp(){
        reader = new FileSystemTiledReader();
    }

    @Test
    void hasCorrectDimensions(){
        TiledMap map = readMap("""
            <map orientation="orthogonal" width="20" height="15"
                 tilewidth="32" tileheight="32">
            </map>
            """);

        assertEquals(20, map.getWidth());
        assertEquals(15, map.getHeight());
        assertEquals(32, map.getTileWidth());
        assertEquals(32, map.getTileHeight());
    }

    @Test
    void hasCustomProperties(){
        TiledMap map = readMap("""
            <map orientation="orthogonal" width="10" height="10"
                 tilewidth="32" tileheight="32">
                <properties>
                    <property name="author" value="test"/>
                    <property name="difficulty" type="int" value="5"/>
                </properties>
            </map>
            """);

        assertEquals("test", map.getProperty("author"));
        assertEquals(5, map.<Integer>getProperty("difficulty"));
        assertEquals(2, map.getProperties().size());
    }

    @Test
    void hasOneTileset(){
        TiledMap map = readMap("""
            <map orientation="orthogonal" width="10" height="10"
                 tilewidth="32" tileheight="32">
                <tileset firstgid="1" name="terrain"
                         tilewidth="32" tileheight="32"
                         tilecount="4" columns="2">
                    <image source="terrain.png" width="64" height="64"/>
                </tileset>
            </map>
            """);

        assertEquals(1, map.getTilesets().size());

        TiledTileset tileset = map.getTilesets().getFirst();

        assertEquals("terrain", tileset.getName());
        assertEquals(32, tileset.getTileWidth());
        assertEquals(32, tileset.getTileHeight());
    }

    @Test
    void hasTwoTilesets(){
        TiledMap map = readMap("""
            <map orientation="orthogonal" width="10" height="10"
                 tilewidth="32" tileheight="32">
                <tileset firstgid="1" name="terrain"
                         tilewidth="32" tileheight="32"
                         tilecount="4" columns="2">
                    <image source="terrain.png" width="64" height="64"/>
                </tileset>
                <tileset firstgid="5" name="objects"
                         tilewidth="32" tileheight="32"
                         tilecount="4" columns="2">
                    <image source="objects.png" width="64" height="64"/>
                </tileset>
            </map>
            """);

        assertEquals(2, map.getTilesets().size());

        assertEquals("terrain", map.getTilesets().get(0).getName());
        assertEquals("objects", map.getTilesets().get(1).getName());
    }

    @Test
    void hasOneTileLayer(){
        TiledMap map = readMap("""
            <map orientation="orthogonal" width="2" height="2"
                 tilewidth="32" tileheight="32">
                <tileset firstgid="1" name="terrain"
                         tilewidth="32" tileheight="32"
                         tilecount="1" columns="1">
                    <image source="terrain.png" width="32" height="32"/>
                </tileset>
                <layer id="1" name="Ground" width="2" height="2">
                    <data encoding="csv">
                        1,0,
                        0,1
                    </data>
                </layer>
            </map>
            """);

        assertEquals(1, map.getNonGroupLayers().size());

        TiledTileLayer layer = (TiledTileLayer) map.getNonGroupLayers().getFirst();

        assertEquals("Ground", layer.getName());
        assertNotNull(layer.getTile(0, 0));
        assertNotNull(layer.getTile(1, 1));
        assertNull(layer.getTile(1, 0));
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
}
