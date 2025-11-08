package com.deedee.thelemia;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.deedee.thelemia.core.Engine;
import com.deedee.thelemia.event.EventBus;
import com.deedee.thelemia.event.common.*;
import com.deedee.thelemia.graphics.AnimatedSprite;
import com.deedee.thelemia.graphics.Fragment;
import com.deedee.thelemia.graphics.Particles;
import com.deedee.thelemia.graphics.TileMap;
import com.deedee.thelemia.input.InputController;
import com.deedee.thelemia.scene.Entity;
import com.deedee.thelemia.scene.Scene;
import com.deedee.thelemia.scene.component.*;
import com.deedee.thelemia.time.Timer;

public class DebugSample {
    private static class CharacterInputAdapter extends InputAdapter {
        private final Entity player;
        public CharacterInputAdapter(Entity player) {
            this.player = player;
        }

        public Entity getPlayer() {
            return player;
        }
    }

    private static class CharacterInputController extends InputController<CharacterInputAdapter> {
        // Tweak these values to taste:
        private static final float MAX_SPEED = 300f;         // pixels per second
        private static final float ACCELERATION = 4000f;     // "how fast we reach max speed" (units/sec^2)
        // The lerp factor controls smoothness — higher = snappier, lower = softer stop/start.
        // We'll compute a per-frame alpha from this (not a raw constant).
        // You can think of ACCELERATION/MAX_SPEED as a base for the lerp alpha.

        private final Vector2 velocity = new Vector2();

        public CharacterInputController(CharacterInputAdapter inputAdapter) {
            super(inputAdapter);
        }

        @Override
        public void update(float delta) {
            // call super first (so input state is updated inside InputController if it does that)
            super.update(delta);

            // Get the player entity from the input adapter (same design as your snippet)
            CharacterInputAdapter adapter = getInputAdapter(); // assume InputController exposes this
            if (adapter == null) return;

            Entity player = adapter.player;
            if (player == null) return;

            TransformComponent transform = player.getComponentByType(TransformComponent.class);
            if (transform == null) return;

            // Build desired direction from key state (using WASD for character movement)
            float dirX = 0f;
            float dirY = 0f;

            if (isKeyPressed(Input.Keys.A))  dirX -= 5f;
            if (isKeyPressed(Input.Keys.D)) dirX += 5f;
            if (isKeyPressed(Input.Keys.W))    dirY += 5f;
            if (isKeyPressed(Input.Keys.S))  dirY -= 5f;

            if (isKeyPressed(Input.Keys.Q)) {
                transform.setScale(transform.getScale().x * 0.8f, transform.getScale().y * 0.8f);
            }
            if (isKeyPressed(Input.Keys.E)) {
                transform.setScale(transform.getScale().x * 1.25f, transform.getScale().y * 1.25f);
            }
            if (isKeyPressed(Input.Keys.SPACE)) {
                transform.setRotation(transform.getRotation() + 45f);
            }

            Vector2 desiredDir = new Vector2(dirX, dirY);
            if (desiredDir.len2() > 1e-6f) {
                desiredDir.nor(); // normalize so diagonal isn't faster
            } else {
                desiredDir.setZero();
            }

            // Desired velocity = direction * max speed
            Vector2 desiredVel = desiredDir.scl(MAX_SPEED);

            // Compute per-frame lerp alpha from acceleration.
            // We want alpha in [0,1]; larger ACCELERATION -> faster approach to desiredVel.
            float alpha = MathUtils.clamp((ACCELERATION * delta) / MAX_SPEED, 0f, 1f);

            // Smoothly move current velocity toward desired velocity
            velocity.lerp(desiredVel, alpha);

            // Apply movement
            float newX = transform.getPosition().x + velocity.x * delta;
            float newY = transform.getPosition().y + velocity.y * delta;
            transform.setPosition(newX, newY);
        }
    }

    private static class CameraInputAdapter extends InputAdapter {
        private final Engine engine;

        public CameraInputAdapter(Engine engine) {
            this.engine = engine;
        }

        public Engine getEngine() {
            return engine;
        }
    }

    private static class CameraInputController extends InputController<CameraInputAdapter> {
        // Camera movement settings
        private static final float CAMERA_SPEED = 400f;  // pixels per second
        private static final float CAMERA_ACCELERATION = 2000f;

        private final Vector2 cameraVelocity = new Vector2();
        private final Vector2 currentPosition = new Vector2();

        public CameraInputController(CameraInputAdapter inputAdapter) {
            super(inputAdapter);
        }

        @Override
        public void update(float delta) {
            super.update(delta);

            CameraInputAdapter adapter = getInputAdapter();
            if (adapter == null) return;

            Engine engine = adapter.getEngine();
            if (engine == null || engine.getRenderer() == null ||
                engine.getRenderer().getCamera() == null) return;

            com.deedee.thelemia.graphics.Camera camera = engine.getRenderer().getCamera();

            // Build desired direction from arrow keys (using arrow keys for camera, WASD for character)
            float dirX = 0f;
            float dirY = 0f;

            // Use arrow keys for camera movement
            if (isKeyPressed(Input.Keys.LEFT))  dirX -= 1f;
            if (isKeyPressed(Input.Keys.RIGHT)) dirX += 1f;
            if (isKeyPressed(Input.Keys.UP))    dirY += 1f;
            if (isKeyPressed(Input.Keys.DOWN))  dirY -= 1f;

            // Optional: Add zoom controls using the internal OrthographicCamera
            if (isKeyPressed(Input.Keys.MINUS) || isKeyPressed(Input.Keys.NUM_0)) {
                camera.getInternalCamera().zoom = MathUtils.clamp(
                    camera.getInternalCamera().zoom * 1.02f, 0.1f, 3.0f);
            }
            if (isKeyPressed(Input.Keys.PLUS) || isKeyPressed(Input.Keys.EQUALS)) {
                camera.getInternalCamera().zoom = MathUtils.clamp(
                    camera.getInternalCamera().zoom * 0.98f, 0.1f, 3.0f);
            }

            Vector2 desiredDir = new Vector2(dirX, dirY);
            if (desiredDir.len2() > 1e-6f) {
                desiredDir.nor(); // normalize so diagonal movement isn't faster
            } else {
                desiredDir.setZero();
            }

            // Desired velocity = direction * camera speed
            Vector2 desiredVel = desiredDir.scl(CAMERA_SPEED);

            // Compute per-frame lerp alpha for smooth camera movement
            float alpha = MathUtils.clamp((CAMERA_ACCELERATION * delta) / CAMERA_SPEED, 0f, 1f);

            // Smoothly move current velocity toward desired velocity
            cameraVelocity.lerp(desiredVel, alpha);

            // Get current camera position and apply movement
            currentPosition.set(camera.getInternalCamera().position.x, camera.getInternalCamera().position.y);
            currentPosition.add(cameraVelocity.x * delta, cameraVelocity.y * delta);

            // Use the Camera's setPosition method
            camera.setPosition(currentPosition);
        }
    }

    private final Engine engine;

    public DebugSample(Engine engine) {
        this.engine = engine;
        engine.getRenderer().loadShader("test", "shaders/vertex.glsl", "shaders/negative.glsl");
    }

    public void setup() {
        Skin skin = new Skin(Gdx.files.internal("skins/metal-ui.json"));

        // Entity setup
        AnimatedSprite testAnimatedSprite = new AnimatedSprite(skin);
        testAnimatedSprite.load("textures/scareton.png", "test", 3, 3, 0.2f);
        testAnimatedSprite.setAnimation("test");
        Entity testAnimatedSpriteEntity = new Entity("animation");

        Fragment testFragment = getSampleFragment(skin);
        Entity testFragmentEntity = new Entity("fragment");

//        TileMap testTileMap = new TileMap(skin, "tilemap/gameart2d-desert.tmx");
        TileMap testTileMap = new TileMap(skin, "tilemap/isometric/DemoMap2.tmx");
        Entity testTileMapEntity = new Entity("tilemap");

        Particles testParticles = new Particles(skin, "particles/explosion/explosion.p");
        Entity testParticlesEntity = new Entity("particles");

        // Component setup
        WidgetComponent widgetComponent = new WidgetComponent(testFragmentEntity, engine.getRenderer().getRoot(), testFragment);
        testFragmentEntity.addComponent(widgetComponent);

        AnimatedSpriteComponent animatedSpriteComponent = new AnimatedSpriteComponent(testAnimatedSpriteEntity, testAnimatedSprite);
        testAnimatedSpriteEntity.addComponent(animatedSpriteComponent);

        TileMapComponent tileMapComponent = new TileMapComponent(testTileMapEntity, testTileMap);
        testTileMapEntity.addComponent(tileMapComponent);

        ParticlesComponent particlesComponent = new ParticlesComponent(testParticlesEntity, testParticles);
        testParticlesEntity.addComponent(particlesComponent);

        Scene testScene1 = getSampleScene1(testAnimatedSpriteEntity);
        Scene testScene2 = getSampleScene2(testAnimatedSpriteEntity);

        testScene1.addEntity(testAnimatedSpriteEntity);
        testScene2.addEntity(testFragmentEntity);
        testScene2.addEntity(testTileMapEntity);
        testScene1.addEntity(testParticlesEntity);
        testScene1.addEntity(testTileMapEntity);

        engine.getSceneManager().addScene(testScene1);
        engine.getSceneManager().addScene(testScene2);

        engine.getSceneManager().loadScene("test1");
    }

    private Scene getSampleScene1(Entity testAnimatedSpriteEntity) {
        CharacterInputAdapter testInputAdapter = new CharacterInputAdapter(testAnimatedSpriteEntity);
        CameraInputAdapter cameraInputAdapter = new CameraInputAdapter(engine);

        return new Scene("test1", new CharacterInputController(testInputAdapter), engine.getSceneManager()) {
            private CameraInputController cameraController;

            @Override
            public void show() {
                super.show();

                // Initialize camera controller
                cameraController = new CameraInputController(cameraInputAdapter);

                Entity testAnimatedSpriteEntity = getEntityById("animation");
                Entity testParticlesEntity = getEntityById("particles");

                AnimatedSpriteComponent animatedSpriteComponent = testAnimatedSpriteEntity.getComponentByType(AnimatedSpriteComponent.class);
                ParticlesComponent testParticlesComponent = testParticlesEntity.getComponentByType(ParticlesComponent.class);

                EventBus.getInstance().post(new RenderAnimatedSpriteEvent(animatedSpriteComponent));
                Timer particlesTimer = new Timer(2f, false, () -> {
                    EventBus.getInstance().post(new RenderParticlesEvent(testParticlesComponent, 350, 530, false));
//                    EventBus.getInstance().post(new ApplyShaderEvent("test"));
                });
                Timer changeSceneTimer = new Timer(5f, false, () -> {
//                    EventBus.getInstance().post(new ResetShaderEvent());
                    engine.getSceneManager().loadScene("test2");
                });
                EventBus.getInstance().post(new AddTimerEvent("test1", particlesTimer));
                EventBus.getInstance().post(new AddTimerEvent("test2", changeSceneTimer));
            }

            @Override
            public void update(float delta) {
                super.update(delta);
                // Update camera controller
                if (cameraController != null) {
                    cameraController.update(delta);
                }
            }

            @Override
            public void hide() {
                super.hide();
//                engine.getRenderer().clearScreen(null);
            }
        };
    }

    private Scene getSampleScene2(Entity testAnimatedSpriteEntity) {
        CharacterInputAdapter testInputAdapter = new CharacterInputAdapter(testAnimatedSpriteEntity);
        CameraInputAdapter cameraInputAdapter = new CameraInputAdapter(engine);

        return new Scene("test2", new CameraInputController(cameraInputAdapter), engine.getSceneManager()) {
            private CameraInputController cameraController;

            @Override
            public void show() {
                super.show();

                // Initialize camera controller
                cameraController = new CameraInputController(cameraInputAdapter);

                Entity testFragmentEntity = getEntityById("fragment");
                Entity testTileMapEntity = getEntityById("tilemap");

                WidgetComponent widgetComponent = testFragmentEntity.getComponentByType(WidgetComponent.class);
                TileMapComponent tileMapComponent = testTileMapEntity.getComponentByType(TileMapComponent.class);

                EventBus.getInstance().post(new RenderFragmentEvent(widgetComponent, 1.0f));
                EventBus.getInstance().post(new ChangeMapEvent(tileMapComponent));
            }

            @Override
            public void update(float delta) {
                super.update(delta);
                // Update camera controller
                if (cameraController != null) {
                    cameraController.update(delta);
                }
            }
        };
    }

    private Fragment getSampleFragment(Skin skin) {
        return new Fragment(skin,1.0f) {
            @Override
            public void create() {
                super.create();
                Table table = new Table();
                engine.getRenderer().getRoot().add(table).width(300f).expand();

                Slider slider = new Slider(0, 100, 1, false, skin);
                TextButton textButton = new TextButton("Test", skin);
                textButton.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        System.out.println("Button clicked!");
                    }
                });
                TextButton textButton2 = new TextButton("Test2", skin);
                textButton2.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        System.out.println("Button clicked!");
                    }
                });
                table.add(textButton).pad(10f);
                table.add(textButton2).pad(10f);
                table.row();
                table.add(slider).colspan(2).expand().fill().pad(10f);

                table.setDebug(true, true);

                widgetGroup = table;
            }

        };
    }
}
