package com.deedee.thelemia.graphics;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.deedee.thelemia.event.EventBus;
import com.deedee.thelemia.event.common.CreateRigidBodyEvent;
import com.deedee.thelemia.event.common.DestroyRigidBodyEvent;
import com.deedee.thelemia.physics.TileBodyData;
import com.deedee.thelemia.physics.RigidBody;
import com.deedee.thelemia.utils.Carrier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TileMap extends RenderableObject {
    private final TiledMap tiledMap;
    private final int tileWidth;
    private final int tileHeight;
    private final int mapWidth;
    private final int mapHeight;
    private final float pixelsPerMeter;

    // Physics integration
    private final Map<String, RigidBody> tileColliders = new HashMap<>();
    private final Map<String, LayerCollisionConfig> collisionLayers = new HashMap<>();

    public static final long FLIP_H = 0x80000000L;
    public static final long FLIP_V = 0x40000000L;
    public static final long FLIP_D = 0x20000000L;
    public static final long FLIP_MASK = FLIP_H | FLIP_V | FLIP_D;

    // Collision categories for tiles
    public static final short TILE_CATEGORY = 0x0002;
    public static final short TILE_COLLISION_MASK = (short) 0xFFFF; // Collides with everything by default

    public static class LayerCollisionConfig {
        public final short categoryMask;
        public final short collisionMask;
        public final boolean isSensor;

        public LayerCollisionConfig(short categoryMask, short collisionMask, boolean isSensor) {
            this.categoryMask = categoryMask;
            this.collisionMask = collisionMask;
            this.isSensor = isSensor;
        }
    }

    public TileMap(Skin skin, String tmxPath) {
        this(skin, tmxPath, 32.0f); // Default pixels per meter
    }

    public TileMap(Skin skin, String tmxPath, float pixelsPerMeter) {
        super(skin);
        this.pixelsPerMeter = pixelsPerMeter;
        tiledMap = new TmxMapLoader().load(tmxPath);
        MapProperties props = tiledMap.getProperties();
        tileWidth = props.get("tilewidth", Integer.class);
        tileHeight = props.get("tileheight", Integer.class);
        mapWidth = props.get("width", Integer.class);
        mapHeight = props.get("height", Integer.class);
    }

    /**
     * Configures collision properties for a specific layer
     */
    public void setLayerCollisionInfo(String layerName, short categoryMask, short collisionMask, boolean isSensor) {
        collisionLayers.put(layerName, new LayerCollisionConfig(categoryMask, collisionMask, isSensor));
    }

    /**
     * Generates collision bodies for all solid tiles in a layer
     */
    public void generateCollisionBodies(String layerName) {
        TiledMapTileLayer layer = getTileLayer(layerName);
        if (layer == null) return;

        LayerCollisionConfig collisionConfig = collisionLayers.get(layerName);
        if (collisionConfig == null) {
            // Use default collision info
            collisionConfig = new LayerCollisionConfig(TILE_CATEGORY, TILE_COLLISION_MASK, false);
        }

        // Clear existing collision bodies for this layer
        clearLayerCollisionBodies(layerName);

        for (int x = 0; x < layer.getWidth(); x++) {
            for (int y = 0; y < layer.getHeight(); y++) {
                Cell cell = layer.getCell(x, y);
                if (cell != null && cell.getTile() != null) {
                    createTileCollisionBody(layerName, x, y, collisionConfig);
                }
            }
        }
    }

    /**
     * Creates a collision body for a single tile
     */
    public void createTileCollisionBody(String layerName, int tileX, int tileY, LayerCollisionConfig collisionConfig) {
        String bodyName = getTileBodyName(layerName, tileX, tileY);

        // Remove existing body if it exists
        destroyTileCollisionBody(layerName, tileX, tileY);

        // Convert tile coordinates to world coordinates
        float worldX = (tileX * tileWidth + tileWidth * 0.5f) / pixelsPerMeter;
        float worldY = (tileY * tileHeight + tileHeight * 0.5f) / pixelsPerMeter;
        float halfWidth = (tileWidth * 0.5f) / pixelsPerMeter;
        float halfHeight = (tileHeight * 0.5f) / pixelsPerMeter;

        // Create body definition
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;
        bodyDef.position.set(worldX, worldY);

        // Create fixture definition
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(halfWidth, halfHeight);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = 0.0f;
        fixtureDef.friction = 0.5f;
        fixtureDef.restitution = 0.0f;
        fixtureDef.isSensor = collisionConfig.isSensor;
        fixtureDef.filter.categoryBits = collisionConfig.categoryMask;
        fixtureDef.filter.maskBits = collisionConfig.collisionMask;

        List<FixtureDef> fixtureDefs = new ArrayList<>();
        fixtureDefs.add(fixtureDef);

        // Create tile body data using the new TileBodyData class
        TileBodyData tileBodyData = new TileBodyData(layerName, tileX, tileY, collisionConfig.categoryMask, collisionConfig.collisionMask, collisionConfig.isSensor);

        // Create the rigid body using event system
        EventBus.getInstance().post(new CreateRigidBodyEvent(new Carrier<>(tileColliders), bodyDef, fixtureDefs, tileBodyData));

        // Clean up shape
        shape.dispose();
    }

    /**
     * Destroys a collision body for a specific tile
     */
    public void destroyTileCollisionBody(String layerName, int tileX, int tileY) {
        String bodyName = getTileBodyName(layerName, tileX, tileY);
        RigidBody body = tileColliders.get(bodyName);
        if (body != null) {
            EventBus.getInstance().post(new DestroyRigidBodyEvent(bodyName));
            tileColliders.remove(bodyName);
        }
    }

    /**
     * Clears all collision bodies for a specific layer
     */
    public void clearLayerCollisionBodies(String layerName) {
        List<String> bodiesToRemove = new ArrayList<>();
        String layerPrefix = layerName + "_tile_";

        for (String bodyName : tileColliders.keySet()) {
            if (bodyName.startsWith(layerPrefix)) {
                bodiesToRemove.add(bodyName);
            }
        }

        EventBus.getInstance().post(new DestroyRigidBodyEvent(bodiesToRemove));
    }

    /**
     * Gets the collision body for a specific tile
     */
    public RigidBody getTileCollisionBody(String layerName, int tileX, int tileY) {
        String bodyName = getTileBodyName(layerName, tileX, tileY);
        return tileColliders.get(bodyName);
    }

    /**
     * Checks if a tile has a collision body
     */
    public boolean hasTileCollisionBody(String layerName, int tileX, int tileY) {
        return getTileCollisionBody(layerName, tileX, tileY) != null;
    }

    /**
     * Gets the TileBodyData for a specific tile collision body
     */
    public TileBodyData getTileBodyData(String layerName, int tileX, int tileY) {
        RigidBody rigidBody = getTileCollisionBody(layerName, tileX, tileY);
        if (rigidBody != null && TileBodyData.isTileBody(rigidBody.getData())) {
            return TileBodyData.asTileBodyData(rigidBody.getData());
        }
        return null;
    }

    /**
     * Gets all tile bodies for a specific layer
     */
    public List<RigidBody> getLayerTileColliders(String layerName) {
        List<RigidBody> layerBodies = new ArrayList<>();
        String layerPrefix = layerName + "_tile_";

        for (String bodyName : tileColliders.keySet()) {
            if (bodyName.startsWith(layerPrefix)) {
                layerBodies.add(tileColliders.get(bodyName));
            }
        }

        return layerBodies;
    }

    private String getTileBodyName(String layerName, int tileX, int tileY) {
        return layerName + "_tile_" + tileX + "_" + tileY;
    }

    @Override
    public void update(float delta) {
        AnimatedTiledMapTile.updateAnimationBaseTime();
    }

    @Override
    public void dispose() {
        // Clean up all collision bodies
        List<String> bodiesToRemove = new ArrayList<>(tileColliders.keySet());
        EventBus.getInstance().post(new DestroyRigidBodyEvent(bodiesToRemove));
        tileColliders.clear();
        collisionLayers.clear();

        super.dispose();
        if (tiledMap != null) tiledMap.dispose();
    }

    public TiledMapTileLayer getTileLayer(String layerName) {
        MapLayer layer = tiledMap.getLayers().get(layerName);
        if (layer instanceof TiledMapTileLayer) {
            return (TiledMapTileLayer) layer;
        }
        return null;
    }

    /**
     * Sets a tile identified by layerName and coordinates
     */
    public void setTile(String layerName, int tx, int ty, int id) {
        TiledMapTileLayer layer = getTileLayer(layerName);
        if (layer == null) return;
        if (tx < 0 || ty < 0 || tx >= layer.getWidth() || ty >= layer.getHeight()) return;

        int startId = layer.getProperties().get("firstgid", Integer.class);
        TiledMapTile tile = tiledMap.getTileSets().getTileSet(layerName).getTile(startId + id);

        Cell cell = layer.getCell(tx, ty);
        if (cell == null) {
            cell = new Cell();
        }
        cell.setTile(tile);
        layer.setCell(tx, ty, cell);

        // Update collision body if this layer has collision enabled
        if (collisionLayers.containsKey(layerName)) {
            if (tile != null) {
                createTileCollisionBody(layerName, tx, ty, collisionLayers.get(layerName));
            } else {
                destroyTileCollisionBody(layerName, tx, ty);
            }
        }
    }

    /**
     * Clears a tile identified by layerName and coordinates
     */
    public void clearTile(String layerName, int tx, int ty) {
        TiledMapTileLayer layer = getTileLayer(layerName);
        if (layer == null) return;
        if (tx < 0 || ty < 0 || tx >= layer.getWidth() || ty >= layer.getHeight()) return;

        // Remove collision body if it exists
        destroyTileCollisionBody(layerName, tx, ty);

        layer.setCell(tx, ty, null);
    }

    private String getLayerName(TiledMapTileLayer layer) {
        for (MapLayer mapLayer : tiledMap.getLayers()) {
            if (mapLayer == layer) {
                return mapLayer.getName();
            }
        }
        return null;
    }

    // Layer mask methods (modified to take layerName instead of layer object)

    /**
     * Sets the flip properties (layer mask) for a tile at the specified position
     * @param layerName The name of the tile layer
     * @param tx Tile x coordinate
     * @param ty Tile y coordinate
     * @param flipHorizontally Whether to flip horizontally
     * @param flipVertically Whether to flip vertically
     * @param flipDiagonally Whether to flip diagonally
     */
    public void setTileFlip(String layerName, int tx, int ty, boolean flipHorizontally, boolean flipVertically, boolean flipDiagonally) {
        TiledMapTileLayer layer = getTileLayer(layerName);
        if (layer == null) return;
        if (tx < 0 || ty < 0 || tx >= layer.getWidth() || ty >= layer.getHeight()) return;

        Cell cell = layer.getCell(tx, ty);
        if (cell == null) return; // Cannot set flip on non-existent tile

        cell.setFlipHorizontally(flipHorizontally);
        cell.setFlipVertically(flipVertically);
        cell.setRotation(flipDiagonally ? Cell.ROTATE_90 : Cell.ROTATE_0);
    }

    /**
     * Sets the flip properties using a bitmask
     * @param layerName The name of the tile layer
     * @param tx Tile x coordinate
     * @param ty Tile y coordinate
     * @param flipMask Bitmask containing flip flags (FLIP_H, FLIP_V, FLIP_D)
     */
    public void setTileFlipMask(String layerName, int tx, int ty, long flipMask) {
        boolean flipH = (flipMask & FLIP_H) != 0;
        boolean flipV = (flipMask & FLIP_V) != 0;
        boolean flipD = (flipMask & FLIP_D) != 0;
        setTileFlip(layerName, tx, ty, flipH, flipV, flipD);
    }

    /**
     * Gets the current flip mask for a tile
     * @param layerName The name of the tile layer
     * @param tx Tile x coordinate
     * @param ty Tile y coordinate
     * @return Bitmask containing current flip flags, or 0 if tile doesn't exist
     */
    public long getTileFlipMask(String layerName, int tx, int ty) {
        TiledMapTileLayer layer = getTileLayer(layerName);
        if (layer == null) return 0;
        if (tx < 0 || ty < 0 || tx >= layer.getWidth() || ty >= layer.getHeight()) return 0;

        Cell cell = layer.getCell(tx, ty);
        if (cell == null) return 0;

        long mask = 0;
        if (cell.getFlipHorizontally()) mask |= FLIP_H;
        if (cell.getFlipVertically()) mask |= FLIP_V;
        if (cell.getRotation() == Cell.ROTATE_90) mask |= FLIP_D;

        return mask;
    }

    /**
     * Sets a tile with specified flip properties in one operation
     * @param layerName The name of the tile layer
     * @param tx Tile x coordinate
     * @param ty Tile y coordinate
     * @param tile The tile to set
     * @param flipHorizontally Whether to flip horizontally
     * @param flipVertically Whether to flip vertically
     * @param flipDiagonally Whether to flip diagonally
     */
    public void setTileWithFlip(String layerName, int tx, int ty, TiledMapTile tile, boolean flipHorizontally, boolean flipVertically, boolean flipDiagonally) {
        TiledMapTileLayer layer = getTileLayer(layerName);
        if (layer == null) return;
        if (tx < 0 || ty < 0 || tx >= layer.getWidth() || ty >= layer.getHeight()) return;

        Cell cell = layer.getCell(tx, ty);
        if (cell == null) {
            cell = new Cell();
        }

        cell.setTile(tile);
        cell.setFlipHorizontally(flipHorizontally);
        cell.setFlipVertically(flipVertically);
        cell.setRotation(flipDiagonally ? Cell.ROTATE_90 : Cell.ROTATE_0);

        layer.setCell(tx, ty, cell);

        // Update collision body if this layer has collision enabled
        if (collisionLayers.containsKey(layerName)) {
            if (tile != null) {
                createTileCollisionBody(layerName, tx, ty, collisionLayers.get(layerName));
            } else {
                destroyTileCollisionBody(layerName, tx, ty);
            }
        }
    }

    /**
     * Sets a tile with flip mask in one operation
     * @param layerName The name of the tile layer
     * @param tx Tile x coordinate
     * @param ty Tile y coordinate
     * @param tile The tile to set
     * @param flipMask Bitmask containing flip flags (FLIP_H, FLIP_V, FLIP_D)
     */
    public void setTileWithFlipMask(String layerName, int tx, int ty, TiledMapTile tile, long flipMask) {
        boolean flipH = (flipMask & FLIP_H) != 0;
        boolean flipV = (flipMask & FLIP_V) != 0;
        boolean flipD = (flipMask & FLIP_D) != 0;
        setTileWithFlip(layerName, tx, ty, tile, flipH, flipV, flipD);
    }

    /**
     * Clears all flip properties for a tile (resets to no flipping)
     * @param layerName The name of the tile layer
     * @param tx Tile x coordinate
     * @param ty Tile y coordinate
     */
    public void clearTileFlip(String layerName, int tx, int ty) {
        setTileFlip(layerName, tx, ty, false, false, false);
    }

    /**
     * Checks if a tile has any flip properties set
     * @param layerName The name of the tile layer
     * @param tx Tile x coordinate
     * @param ty Tile y coordinate
     * @return true if the tile has any flip properties, false otherwise
     */
    public boolean hasTileFlip(String layerName, int tx, int ty) {
        return getTileFlipMask(layerName, tx, ty) != 0;
    }

    // Getters
    public TiledMap getTiledMap() {
        return tiledMap;
    }
    public int getTileWidth() {
        return tileWidth;
    }
    public int getTileHeight() {
        return tileHeight;
    }
    public int getMapWidth() {
        return mapWidth;
    }
    public int getMapHeight() {
        return mapHeight;
    }
    public float getPixelsPerMeter() {
        return pixelsPerMeter;
    }
    public Map<String, RigidBody> getTileColliders() {
        return tileColliders;
    }
}
