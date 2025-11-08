package com.deedee.thelemia;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.maps.tiled.renderers.IsometricTiledMapRenderer;
import com.badlogic.gdx.math.Vector2;
import com.deedee.thelemia.core.Engine;
import com.deedee.thelemia.graphics.RenderConfig;
import com.deedee.thelemia.physics.PhysicsConfig;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class ThelemiaLauncher extends ApplicationAdapter {
    private final RenderConfig renderConfig = new RenderConfig(new Color(0.15f, 0.15f, 0.2f, 1.0f), IsometricTiledMapRenderer.class);
    private final PhysicsConfig physicsConfig = new PhysicsConfig(new Vector2());
    private final Engine engine = new Engine(renderConfig, physicsConfig);

    @Override
    public void create() {
        engine.create();

        DebugSample debugSample = new DebugSample(engine);
        debugSample.setup();
    }

    @Override
    public void render() {
        engine.render();
    }

    @Override
    public void resize(int width, int height) {
        engine.resize(width, height);
    }

    @Override
    public void dispose() {
        engine.dispose();
    }
}
