package com.brv.disasterrun;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.math.Vector3;
import net.mgsx.gltf.loaders.glb.GLBLoader;
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute;
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import net.mgsx.gltf.scene3d.scene.SceneManager;
import net.mgsx.gltf.scene3d.shaders.PBRShaderConfig;
import net.mgsx.gltf.scene3d.shaders.PBRShaderProvider;
import net.mgsx.gltf.scene3d.utils.IBLBuilder;

public class DisasterRunGame extends ApplicationAdapter {
    private SceneAsset sceneAsset;
    private SceneManager sceneManager;
    private Scene characterScene;
    private PerspectiveCamera cam;
    private AnimationController animationController;

    private final float moveSpeed = 5f;
    private final Vector3 characterPosition = new Vector3(0, 0, 0);
    private String currentAnimation = "";
    private boolean isJumping = false;
    private final float characterScale = 0.5f;

    @Override
    public void create() {
        PBRShaderConfig config = PBRShaderProvider.createDefaultConfig();
        config.numBones = 64;
        config.numBoneWeights = 8;

        sceneManager = new SceneManager(new PBRShaderProvider(config), null);

        sceneAsset = new GLBLoader().load(Gdx.files.internal("models/character_05.glb"));
        characterScene = new Scene(sceneAsset.scene);

        // Initial setup ng transform
        updateCharacterTransform();

        sceneManager.addScene(characterScene);

        animationController = new AnimationController(characterScene.modelInstance);
        setAnimation("idle", -1);

        cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.near = 0.1f;
        cam.far = 1000f;
        sceneManager.setCamera(cam);

        // LIGHTING
        DirectionalLightEx light = new DirectionalLightEx();
        light.direction.set(1, -3, 1).nor();
        light.color.set(Color.WHITE);
        sceneManager.environment.add(light);

        // IBL setup para sa PBR materials
        IBLBuilder iblBuilder = IBLBuilder.createOutdoor(light);
        sceneManager.environment.set(new PBRCubemapAttribute(PBRCubemapAttribute.DiffuseEnv, iblBuilder.buildIrradianceMap(256)));
        sceneManager.environment.set(new PBRCubemapAttribute(PBRCubemapAttribute.SpecularEnv, iblBuilder.buildRadianceMap(10)));
        iblBuilder.dispose();

        sceneManager.setAmbientLight(0.5f);
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        handleInput(delta);
        if (animationController != null) animationController.update(delta);
        updateCamera();

        Gdx.gl.glClearColor(0.4f, 0.6f, 0.9f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        sceneManager.update(delta);
        sceneManager.render();
    }

    private void handleInput(float delta) {
        // JUMP LOGIC
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && !isJumping) {
            isJumping = true;
            setAnimation("jump", 1);
            // Animation Listener para bumalik sa idle pagkatapos tumalon
            animationController.setAnimation("jump", 1, 1f, new AnimationController.AnimationListener() {
                @Override
                public void onEnd(AnimationController.AnimationDesc animation) {
                    isJumping = false;
                    currentAnimation = ""; // Force update sa next frame
                }
                @Override
                public void onLoop(AnimationController.AnimationDesc animation) {}
            });
        }

        if (!isJumping) {
            boolean isMoving = false;
            String nextAnim = "idle";

            // W - Forward
            if (Gdx.input.isKeyPressed(Input.Keys.W)) {
                characterPosition.z -= moveSpeed * delta;
                nextAnim = "running";
                isMoving = true;
            }
            // S - Backward
            else if (Gdx.input.isKeyPressed(Input.Keys.S)) {
                characterPosition.z += moveSpeed * delta;
                nextAnim = "running_backward";
                isMoving = true;
            }

            // A - Left Strafe
            if (Gdx.input.isKeyPressed(Input.Keys.A)) {
                characterPosition.x -= moveSpeed * delta;
                if (!isMoving) nextAnim = "left_strafe"; // Prioritize forward/back kung sabay pinindot
                isMoving = true;
            }
            // D - Right Strafe
            else if (Gdx.input.isKeyPressed(Input.Keys.D)) {
                characterPosition.x += moveSpeed * delta;
                if (!isMoving) nextAnim = "right_strafe";
                isMoving = true;
            }

            updateCharacterTransform();
            setAnimation(nextAnim, -1);
        }
    }

    private void updateCharacterTransform() {
        // Naka-fixed ang rotation (0 degrees sa Y) para laging nakaharap sa malayo (GTA style strafing)
        characterScene.modelInstance.transform.setToScaling(characterScale, characterScale, characterScale);
        characterScene.modelInstance.transform.setTranslation(characterPosition);
    }

    private void updateCamera() {
        // GTA Vibe: Mataas ng kaunti at nasa likod
        float camY = characterPosition.y + 2.5f;
        float camZ = characterPosition.z + 7f;

        // Gamit ang lerp para hindi masyadong "stiff" ang camera (optional)
        cam.position.set(characterPosition.x, camY, camZ);
        cam.lookAt(characterPosition.x, characterPosition.y + 1.0f, characterPosition.z);
        cam.update();
    }

    private void setAnimation(String name, int loopCount) {
        if (!currentAnimation.equals(name)) {
            currentAnimation = name;
            try {
                // Crossfade ng 0.2 seconds para smooth ang transition ng animations
                animationController.animate(name, loopCount, 1f, null, 0.2f);
            } catch (Exception e) {
                // Fallback kung hindi mahanap ang animation name
                Gdx.app.log("Animation", "Could not find: " + name);
            }
        }
    }

    @Override
    public void dispose() {
        if (sceneAsset != null) sceneAsset.dispose();
        if (sceneManager != null) sceneManager.dispose();
    }
}
