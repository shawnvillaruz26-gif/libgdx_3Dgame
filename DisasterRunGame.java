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

    // Mouse Look Variables
    private float camPitch = -20f; // Taas/Baba ng tingin
    private float camYaw = 0f;    // Kaliwa/Kanan na ikot
    private final float mouseSensitivity = 0.2f;

    public DisasterRunGame(Main game) {
        this.game = game;

        PBRShaderConfig config = PBRShaderProvider.createDefaultConfig();
        config.numBones = 64;
        config.numBoneWeights = 8;
        sceneManager = new SceneManager(new PBRShaderProvider(config), null);

        sceneAsset = new GLBLoader().load(Gdx.files.internal("models/character_05.glb"));
        characterScene = new Scene(sceneAsset.scene);
        
        sceneManager.addScene(characterScene);

        animationController = new AnimationController(characterScene.modelInstance);
        setAnimation("idle", -1);

        cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.near = 0.1f;
        cam.far = 1000f;
        sceneManager.setCamera(cam);

        // LIGHTING & IBL
        DirectionalLightEx light = new DirectionalLightEx();
        light.direction.set(1, -3, 1).nor();
        light.color.set(Color.WHITE);
        sceneManager.environment.add(light);
        IBLBuilder iblBuilder = IBLBuilder.createOutdoor(light);
        sceneManager.environment.set(new PBRCubemapAttribute(PBRCubemapAttribute.DiffuseEnv, iblBuilder.buildIrradianceMap(256)));
        sceneManager.environment.set(new PBRCubemapAttribute(PBRCubemapAttribute.SpecularEnv, iblBuilder.buildRadianceMap(10)));
        iblBuilder.dispose();
        sceneManager.setAmbientLight(0.5f);

        // Itago ang cursor para sa Mouse Look
        Gdx.input.setCursorCatched(true);
    }

    @Override
    public void show() {}

    @Override
    public void render(float delta) {
        handleMouseLook();
        handleInput(delta);
        if (animationController != null) animationController.update(delta);
        updateCamera();

        Gdx.gl.glClearColor(0.4f, 0.6f, 0.9f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        sceneManager.update(delta);
        sceneManager.render();
        
        // Pindutin ang ESC para lumabas sa mouse catch
        if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) Gdx.input.setCursorCatched(false);
    }

    private void handleMouseLook() {
        if (!Gdx.input.isCursorCatched()) return;

        float deltaX = -Gdx.input.getDeltaX() * mouseSensitivity;
        float deltaY = -Gdx.input.getDeltaY() * mouseSensitivity;

        camYaw += deltaX;
        camPitch += deltaY;

        // Limitahan ang tingin sa taas at baba (Pitch) para hindi tumambaliktad
        camPitch = MathUtils.clamp(camPitch, -45f, 20f);
    }

    private void handleInput(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && !isJumping) {
            isJumping = true;
            animationController.setAnimation("jump", 1, 1f, new AnimationController.AnimationListener() {
                @Override public void onEnd(AnimationController.AnimationDesc animation) { isJumping = false; currentAnimation = ""; }
                @Override public void onLoop(AnimationController.AnimationDesc animation) {}
            });
        }

        if (!isJumping) {
            boolean isMoving = false;
            String nextAnim = "idle";
            
            // Movement base sa kung saan nakatingin ang camera (camYaw)
            float moveX = 0;
            float moveZ = 0;

            if (Gdx.input.isKeyPressed(Input.Keys.W)) { moveZ -= 1; nextAnim = "running"; isMoving = true; }
            if (Gdx.input.isKeyPressed(Input.Keys.S)) { moveZ += 1; nextAnim = "running_backward"; isMoving = true; }
            if (Gdx.input.isKeyPressed(Input.Keys.A)) { moveX -= 1; if(!isMoving) nextAnim = "left_strafe"; isMoving = true; }
            if (Gdx.input.isKeyPressed(Input.Keys.D)) { moveX += 1; if(!isMoving) nextAnim = "right_strafe"; isMoving = true; }

            if (isMoving) {
                // I-rotate ang movement vector base sa camYaw
                Vector3 moveVector = new Vector3(moveX, 0, moveZ).nor().rotate(Vector3.Y, camYaw);
                characterPosition.add(moveVector.scl(moveSpeed * delta));
            }
            
            updateCharacterTransform();
            setAnimation(nextAnim, -1);
        }
    }

    private void updateCharacterTransform() {
        characterScene.modelInstance.transform.setToScaling(characterScale, characterScale, characterScale);
        characterScene.modelInstance.transform.setTranslation(characterPosition);
        // Ang character ay laging nakatalikod sa camera (facing where we look)
        characterScene.modelInstance.transform.rotate(Vector3.Y, camYaw);
    }

    private void updateCamera() {
        // Third-person camera math (Spherical coordinates)
        float distance = 6f; // Layo ng camera
        float x = distance * MathUtils.sinDeg(camYaw) * MathUtils.cosDeg(camPitch);
        float z = distance * MathUtils.cosDeg(camYaw) * MathUtils.cosDeg(camPitch);
        float y = distance * MathUtils.sinDeg(camPitch);

        cam.position.set(characterPosition.x - x, characterPosition.y - y + 2.5f, characterPosition.z - z);
        cam.lookAt(characterPosition.x, characterPosition.y + 1.2f, characterPosition.z);
        cam.update();
    }

    private void setAnimation(String name, int loopCount) {
        if (!currentAnimation.equals(name)) {
            currentAnimation = name;
            try { animationController.animate(name, loopCount, 1f, null, 0.2f); } catch (Exception e) {}
        }
    }

    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() { sceneAsset.dispose(); sceneManager.dispose(); }
}
