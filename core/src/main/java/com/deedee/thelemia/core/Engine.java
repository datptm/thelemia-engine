package com.deedee.thelemia.core;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.deedee.thelemia.audio.AudioEmitter;
import com.deedee.thelemia.event.EventBus;
import com.deedee.thelemia.graphics.RenderConfig;
import com.deedee.thelemia.graphics.Renderer;
import com.deedee.thelemia.input.InputHandler;
import com.deedee.thelemia.physics.PhysicsConfig;
import com.deedee.thelemia.physics.PhysicsEngine;
import com.deedee.thelemia.scene.GameSystem;
import com.deedee.thelemia.scene.SceneManager;
import com.deedee.thelemia.time.TimerController;

import java.util.*;

public class Engine extends ApplicationAdapter {
    private final RenderConfig renderConfig;
    private final PhysicsConfig physicsConfig;

    private final Map<Class<? extends GameSystem>, GameSystem> defaultSystems = new HashMap<>();
    private final List<GameSystem> optionalSystems = new ArrayList<>();

    public Engine(RenderConfig renderConfig, PhysicsConfig physicsConfig) {
        this.renderConfig = renderConfig;
        this.physicsConfig = physicsConfig;
    }

    @Override
    public void create() {
        defaultSystems.put(SceneManager.class, new SceneManager());
        defaultSystems.put(InputHandler.class, new InputHandler()); // Must initialize before Renderer
        defaultSystems.put(AudioEmitter.class, new AudioEmitter());
        defaultSystems.put(PhysicsEngine.class, new PhysicsEngine(physicsConfig));
        defaultSystems.put(TimerController.class, new TimerController());
        defaultSystems.put(Renderer.class, new Renderer(renderConfig));
    }
    @Override
    public void render() {
        // Rendering logic for the engine
        float delta = Gdx.graphics.getDeltaTime();
        for (GameSystem system : defaultSystems.values()) {
            system.update(delta);
        }
        for (GameSystem system : optionalSystems) {
            system.update(delta);
        }

        EventBus.getInstance().process();
    }
    @Override
    public void resize(int width, int height) {
        // Resize logic for the engine
        getRenderer().resize(width, height);
    }
    @Override
    public void pause() {
        // Pause logic for the engine
    }
    @Override
    public void resume() {
        // Resume logic for the engine
    }
    @Override
    public void dispose() {
        // Dispose logic for the engine
        for (GameSystem system : defaultSystems.values()) {
            system.dispose();
        }
        for (GameSystem system : optionalSystems) {
            system.dispose();
        }

    }

    public void addSystem(GameSystem system) {
        optionalSystems.add(system);
    }
    public void removeSystem(GameSystem system) {
        optionalSystems.remove(system);
    }

    public RenderConfig getRenderConfig() {
        return renderConfig;
    }
    public PhysicsConfig getPhysicsConfig() {
        return physicsConfig;
    }

    public Renderer getRenderer() {
        return (Renderer) defaultSystems.get(Renderer.class);
    }
    public AudioEmitter getAudioEmitter() {
        return (AudioEmitter) defaultSystems.get(AudioEmitter.class);
    }
    public InputHandler getInputHandler() {
        return (InputHandler) defaultSystems.get(InputHandler.class);
    }
    public PhysicsEngine getPhysicsEngine() {
        return (PhysicsEngine) defaultSystems.get(PhysicsEngine.class);
    }
    public SceneManager getSceneManager() {
        return (SceneManager) defaultSystems.get(SceneManager.class);
    }
    public TimerController getTimerManager() {
        return (TimerController) defaultSystems.get(TimerController.class);
    }
}
