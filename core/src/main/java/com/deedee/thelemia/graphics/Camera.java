package com.deedee.thelemia.graphics;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.deedee.thelemia.core.GameObject;

public class Camera extends GameObject {
    private final OrthographicCamera camera;
    private final Viewport viewport;

    public Camera(int width, int height) {
        camera = new OrthographicCamera();
        camera.setToOrtho(false, width, height);
        viewport = new ScreenViewport(camera);
        viewport.update(width, height, true);
    }

    @Override
    public void create() {
        super.create();
    }
    @Override
    public void start() {
        super.start();
    }
    @Override
    public void update(float delta) {
        camera.update();
    }
    @Override
    public void stop() {
        super.stop();
    }
    @Override
    public void dispose() {
        super.dispose();
    }

    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    public void setPosition(Vector2 position) {
        camera.position.set(position.x, position.y, 0);
    }
    public void setViewport(float width, float height) {
        viewport.update((int) width, (int) height, true);
    }
    public Matrix4 getProjectionMatrix() {
        return camera.combined;
    }

    public OrthographicCamera getInternalCamera() {
        return camera;
    }
    public Viewport getViewport() {
        return viewport;
    }
}
