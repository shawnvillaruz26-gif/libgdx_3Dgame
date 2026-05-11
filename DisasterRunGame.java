package com.brv.disasterrun;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import net.mgsx.gltf.loaders.glb.GLBLoader;
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute;
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import net.mgsx.gltf.scene3d.scene.SceneManager;
import net.mgsx.gltf.scene3d.shaders.PBRShaderConfig;
import net.mgsx.gltf.scene3d.shaders.PBRShaderProvider;
import net.mgsx.gltf.scene3d.utils.IBLBuilder;

public class DisasterRunGame implements Screen {
    private final Main game;
    private SceneAsset sceneAsset, groundAsset;
    private SceneManager sceneManager;
    private Scene characterScene, groundScene;
    private PerspectiveCamera cam;
    private AnimationController animationController;;
    private Stage stage;
    private Skin skin;
    private ProgressBar healthBar;
    private Label scoreLabel;
    private float score = 0;

    // Physics & Movement Variables
    private final Vector3 characterPosition = new Vector3(0, 0, 0);
    private final float moveSpeed = 5f;
    private float verticalVelocity = 0;
    private final float gravity = -15f;
    private final float jumpForce = 6f;
    private boolean isGrounded = true;

    private String currentAnimation = "";
    private final float characterScale = 0.5f;

    // Mouse Look
    private float camPitch = -20f;
    private float camYaw = 180f;
    private final float mouseSensitivity = 0.15f;


    public DisasterRunGame(Main game) {
        this.game = game;

        PBRShaderConfig config = PBRShaderProvider.createDefaultConfig();
        config.numBones = 64;
        config.numBoneWeights = 8;
        sceneManager = new SceneManager(new PBRShaderProvider(config), null);

        // 1. LOAD CHARACTER
        sceneAsset = new GLBLoader().load(Gdx.files.internal("models/character_05.glb"));
        characterScene = new Scene(sceneAsset.scene);
        sceneManager.addScene(characterScene);

        // 2. LOAD GROUND
        try {
            groundAsset = new GLBLoader().load(Gdx.files.internal("images/ground.glb"));
            groundScene = new Scene(groundAsset.scene);
            groundScene.modelInstance.transform.setToScaling(15f, 1f, 200f);
            // Ibaba ang kalsada ng kaunti para saktong nasa ibabaw ang character
            groundScene.modelInstance.transform.setTranslation(0, -0.05f, 0);
            sceneManager.addScene(groundScene);
        } catch (Exception e) {
            Gdx.app.error("GROUND", "Missing ground.glb!");
        }

        animationController = new AnimationController(characterScene.modelInstance);
        setAnimation("idle", -1);

        cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.near = 0.1f;
        cam.far = 1000f;
        sceneManager.setCamera(cam);

        // LIGHTING Setup
        DirectionalLightEx light = new DirectionalLightEx();
        light.direction.set(1, -3, 1).nor();
        light.color.set(Color.WHITE);
        sceneManager.environment.add(light);
        IBLBuilder iblBuilder = IBLBuilder.createOutdoor(light);
        sceneManager.environment.set(new PBRCubemapAttribute(PBRCubemapAttribute.DiffuseEnv, iblBuilder.buildIrradianceMap(256)));
        sceneManager.environment.set(new PBRCubemapAttribute(PBRCubemapAttribute.SpecularEnv, iblBuilder.buildRadianceMap(10)));
        iblBuilder.dispose();
        sceneManager.setAmbientLight(0.5f);

        Gdx.input.setCursorCatched(true);
    }

    @Override
    public void render(float delta) {
        handleMouseLook();
        applyPhysics(delta);
        handleInput(delta);

        if (animationController != null) animationController.update(delta);
        updateCamera();

        Gdx.gl.glClearColor(0.4f, 0.6f, 0.9f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        sceneManager.update(delta);
        sceneManager.render();

        if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) Gdx.input.setCursorCatched(false);
    }

    private void applyPhysics(float delta) {
        // 1. Gravity logic
        verticalVelocity += gravity * delta;
        characterPosition.y += verticalVelocity * delta;

        // --- FIX: ITAAS ANG COLLISION ---
        // Palitan natin ang `0` ng `0.1f` para tumigil siya sa ibabaw ng sahig.
        if (characterPosition.y <= 0.1f) {
            characterPosition.y = 0.1f; // Itaas nang kaunti ang paanan
            verticalVelocity = 0;
            isGrounded = true;
        } else {
            isGrounded = false;
        }
    }

    private void handleMouseLook() {
        if (!Gdx.input.isCursorCatched()) return;
        camYaw += -Gdx.input.getDeltaX() * mouseSensitivity;
        camPitch = MathUtils.clamp(camPitch + (-Gdx.input.getDeltaY() * mouseSensitivity), -45f, 15f);
    }

    private void handleInput(float delta) {
        // 1. JUMP LOGIC
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && isGrounded) {
            verticalVelocity = jumpForce;
            isGrounded = false; // Force grounded to false agad
            setAnimation("jump", 1); // Patakbuhin ang jump animation nang isang beses
        }

        // 2. MOVEMENT & ANIMATION SELECTION
        boolean isMoving = false;
        float moveX = 0, moveZ = 0;

        if (Gdx.input.isKeyPressed(Input.Keys.W)) { moveZ -= 1; isMoving = true; }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) { moveZ += 1; isMoving = true; }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) { moveX -= 1; isMoving = true; }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) { moveX += 1; isMoving = true; }

        // Pumili ng tamang animation base sa sitwasyon
        String nextAnim;
        if (!isGrounded) {
            // Hayaan lang ang "jump" animation, huwag palitan habang nasa hangin
            nextAnim = "jump";
        } else if (isMoving) {
            // Kung naglalakad sa sahig
            nextAnim = (Gdx.input.isKeyPressed(Input.Keys.S)) ? "running_backward" : "running";
        } else {
            // Kung nakatayo lang sa sahig
            nextAnim = "idle";
        }

        // 3. APPLY MOVEMENT
        if (isMoving) {
            Vector3 moveVector = new Vector3(moveX, 0, moveZ).nor().rotate(Vector3.Y, camYaw);
            characterPosition.add(moveVector.scl(moveSpeed * delta));
        }

        updateCharacterTransform();
        setAnimation(nextAnim, (nextAnim.equals("jump") ? 1 : -1));
    }

    private void updateCharacterTransform() {
        characterScene.modelInstance.transform.setToScaling(characterScale, characterScale, characterScale);
        characterScene.modelInstance.transform.setTranslation(characterPosition.x, characterPosition.y, characterPosition.z);
        characterScene.modelInstance.transform.rotate(Vector3.Y, camYaw + 180f);
    }

    private void updateCamera() {
        float distance = 6f;
        float x = distance * MathUtils.sinDeg(camYaw) * MathUtils.cosDeg(camPitch);
        float z = distance * MathUtils.cosDeg(camYaw) * MathUtils.cosDeg(camPitch);
        float y = distance * MathUtils.sinDeg(camPitch);

        cam.position.set(characterPosition.x - x, characterPosition.y - y + 2.5f, characterPosition.z - z);
        cam.lookAt(characterPosition.x, characterPosition.y + 1.2f, characterPosition.z);
        cam.up.set(Vector3.Y);
        cam.update();
    }

    private void setAnimation(String name, int loopCount) {
        if (!currentAnimation.equals(name)) {
            currentAnimation = name;
            try { animationController.animate(name, loopCount, 1f, null, 0.2f); } catch (Exception e) {}
        }
    }

    @Override public void show() {}
    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {
        if(sceneAsset != null) sceneAsset.dispose();
        if(groundAsset != null) groundAsset.dispose();
        if(sceneManager != null) sceneManager.dispose();
    }
}
