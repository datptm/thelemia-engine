package com.deedee.thelemia.graphics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.renderers.BatchTiledMapRenderer;
import com.badlogic.gdx.maps.tiled.renderers.IsometricTiledMapRenderer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;

import java.lang.reflect.Constructor;

public class RenderConfig {
    private final Color defaultBackground;
    private final Class<? extends BatchTiledMapRenderer> mapRenderer;

    public RenderConfig(Color defaultBackground, Class<? extends BatchTiledMapRenderer> mapRenderer) {
        this.defaultBackground = defaultBackground;
        this.mapRenderer = mapRenderer;
    }

    public Color getDefaultBackground() {
        return defaultBackground;
    }
    public BatchTiledMapRenderer getMapRenderer(TiledMap map, float unitScale, SpriteBatch batch) {
        if (mapRenderer.equals(OrthogonalTiledMapRenderer.class)) {
            return new OrthogonalTiledMapRenderer(map, unitScale, batch);
        } else if (mapRenderer.equals(IsometricTiledMapRenderer.class)) {
            return new IsometricTiledMapRenderer(map, unitScale, batch);
        } else {
            return null;
        }
    }
}
