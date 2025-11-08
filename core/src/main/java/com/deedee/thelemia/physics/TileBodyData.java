package com.deedee.thelemia.physics;

public class TileBodyData extends BodyData {
    private final String layerName;
    private final int tileX;
    private final int tileY;
    private final boolean isSensor;

    public TileBodyData(String layerName, int tileX, int tileY, short categoryMask, short collisionMask, boolean isSensor) {
        super(generateTileBodyName(layerName, tileX, tileY), categoryMask, collisionMask);
        this.layerName = layerName;
        this.tileX = tileX;
        this.tileY = tileY;
        this.isSensor = isSensor;
    }

    private static String generateTileBodyName(String layerName, int tileX, int tileY) {
        return layerName + "_tile_" + tileX + "_" + tileY;
    }

    public String getLayerName() {
        return layerName;
    }
    public int getTileX() {
        return tileX;
    }
    public int getTileY() {
        return tileY;
    }
    public boolean isSensor() {
        return isSensor;
    }

    /**
     * Convenience method to check if this is a tile body
     */
    public static boolean isTileBody(BodyData bodyData) {
        return bodyData instanceof TileBodyData;
    }

    /**
     * Safe cast to TileBodyData if the body is a tile
     */
    public static TileBodyData asTileBodyData(BodyData bodyData) {
        return bodyData instanceof TileBodyData ? (TileBodyData) bodyData : null;
    }

    @Override
    public String toString() {
        return "TileBodyData{" +
            "name='" + getName() + '\'' +
            ", layerName='" + layerName + '\'' +
            ", tileX=" + tileX +
            ", tileY=" + tileY +
            ", categoryMask=" + getCategoryMask() +
            ", collisionMask=" + getCollisionMask() +
            ", isSensor=" + isSensor +
            '}';
    }
}
