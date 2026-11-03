package com.daninichu.tiled;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TiledMapRenderer{
    private TiledMap map;
    private Map<String, BufferedImage> cachedImages = new HashMap<>();

    /**
     * Use this if you want a new set of cached images
     * @param map
     */
    public TiledMapRenderer(TiledMap map){
        setMap(map);
    }

    /**
     * Use this to keep cached images.
     * @param map
     * @throws NullPointerException
     */
    public void setMap(TiledMap map){
        for(TiledTileset tileset : map.getTilesets()){
            TiledImage image = tileset.getImage();
            if(image != null){
                loadImage(image.getSource());
            }
            for(TiledTile tile : tileset.getTiles()){
                image = tile.getImage();
                if(image != null){
                    loadImage(image.getSource());
                }
            }
        }
        this.map = map;
    }

    public BufferedImage loadImage(String path){
        BufferedImage image = cachedImages.get(path);
        if(image == null){
            try{
                cachedImages.put(path, image = ImageIO.read(new File(path)));
            } catch(IOException e){
                throw new RuntimeException(e);
            }
        }
        return image;
    }

    public void unloadImage(String path){
        cachedImages.remove(path);
    }

    public void renderMap(Graphics2D g){
        for(TiledLayer layer : map.getTopLevelLayers()){
            if(layer instanceof TiledGroupLayer gl)
                renderGroupLayer(g, gl);
            else if(layer instanceof TiledTileLayer tl)
                renderTileLayer(g, tl);
        }
    }

    public void renderGroupLayer(Graphics2D g, TiledGroupLayer groupLayer){
        for(TiledLayer child : groupLayer.getChildren()){
            if(child instanceof TiledGroupLayer gl)
                renderGroupLayer(g, gl);
            else if(child instanceof TiledTileLayer tl)
                renderTileLayer(g, tl);
        }
    }

    public void renderTileLayer(Graphics2D g, TiledTileLayer layer){
        for(Point point : layer.getTileLocations()){
            int x = point.x;
            int y = point.y;

            TiledTile tile = layer.getTile(point);
            TiledTileset tileset = tile.getTileset();
            int dstX = x * map.getTileWidth();
            int dstY = y * map.getTileHeight();

            TiledImage image = tile.getImage();
            if(image != null){
                g.drawImage(loadImage(image.getSource()), dstX, dstY, null);
            } else{
                int tileWidth = tileset.getTileWidth();
                int tileHeight = tileset.getTileHeight();
                int margin = tileset.getMargin();
                int spacing = tileset.getSpacing();
                int srcX = margin + tile.getTilesetX() * (tileWidth + spacing);
                int srcY = margin + tile.getTilesetY() * (tileHeight + spacing);

                g.drawImage(loadImage(tileset.getImage().getSource()),
                        dstX, dstY, dstX + tileWidth, dstY + tileHeight,
                        srcX, srcY, srcX + tileWidth, srcY + tileHeight, null
                );
            }
        }
    }
}
