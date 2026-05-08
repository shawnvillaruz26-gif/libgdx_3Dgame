package com.brv.disasterrun;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color; // Import para sa kulay
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture; // Import para sa texture
import com.badlogic.gdx.graphics.VertexAttributes.Usage; // Import para sa mesh attributes
import com.badlogic.gdx.graphics.g3d.Material; // Import para sa material
import com.badlogic.gdx.graphics.g3d.Model; // Import para sa model data
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute; // Import para sa material color
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute; // Import para sa material texture
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder; // Import para makagawa ng model sa code
import com.badlogic.gdx.math.Vector3;
import net.mgsx.gltf.loaders.glb.GLBLoader;
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import net.mgsx.gltf.scene3d.scene.SceneManager;
import net.mgsx.gltf.scene3d.shaders.PBRShaderConfig;
import net.mgsx.gltf.scene3d.shaders.PBRShaderProvider;

public class DisasterRunGame extends ApplicationAdapter {
    private SceneAsset sceneAsset;
    private SceneManager sceneManager;
    private Scene characterScene;
    private PerspectiveCamera cam;
    private AnimationController animationController;

    // Mga variable para sa Code-Only Road
    private Model roadModel;
    private Scene roadScene;
    private Texture whiteTexture; // Gagamitin nating base texture

    // Variables para sa Third Person Camera
    private Vector3 characterPosition = new Vector3();
    private Vector3 cameraOffset = new Vector3(0, 2.5f, 8f); 
    private Vector3 tempVector = new Vector3();

    @Override
    public void create() {
        // 1. Setup PBR Config
        PBRShaderConfig config = PBRShaderProvider.createDefaultConfig();
        config.numBones = 64; 
        config.numBoneWeights = 8; 

        // 2. Initialize SceneManager
        sceneManager = new SceneManager(new PBRShaderProvider(config), null);

        // --- GUMAWA NG BACKGROUND/KALSADA SA CODE LANG DITO ---

        // A. Gumawa ng isang simpleng puting texture (1x1 pixel)
        // Ito ay para magkaroon ng base texture ang material
        whiteTexture = new Texture(1, 1, Texture.TextureFormat.RGBA8888);
        whiteTexture.getTextureData().prepare();
        whiteTexture.getTextureData().consumePixmap().drawPixel(0, 0, Color.rgba8888(Color.WHITE));

        // B. Gumawa ng Material para sa kalsada (Aspalto Gray)
        Material roadMaterial = new Material(
            TextureAttribute.createDiffuse(whiteTexture),
            ColorAttribute.createDiffuse(Color.GRAY) // Kulay aspalto
        );

        // C. Gamitin ang ModelBuilder para gumawa ng isang malawak na plataporma
        ModelBuilder modelBuilder = new ModelBuilder();
        // Gumawa ng box: Lapad=20m, Taas=0.2m (manipis), Haba=500m (mahabang kalsada)
        roadModel = modelBuilder.createBox(20f, 0.2f, 500f, roadMaterial, 
            Usage.Position | Usage.Normal | Usage.TextureCoordinates);

        // D. I-wrap ang model sa isang Scene at idagdag sa SceneManager
        roadScene = new Scene(roadModel);
        // I-position ang kalsada nang bahagya sa ilalim ng character (Y = -0.1f)
        roadScene.modelInstance.transform.setTranslation(0, -0.1f, -200f); 
        sceneManager.addScene(roadScene);

        // ---------------------------------------------------------

        // 3. I-load ang character (Siguraduhin na tama ang filename)
        sceneAsset = new GLBLoader().load(Gdx.files.internal("models/character_05.glb"));
        characterScene = new Scene(sceneAsset.scene);
        sceneManager.addScene(characterScene);

        // 4. Setup Animation
        animationController = new AnimationController(characterScene.modelInstance);
        if (sceneAsset.scene.animations.size > 0) {
            // Siguraduhin na "idle" o "final_player" ang animation ID mo sa Blender
            animationController.setAnimation("idle", -1);
        }

        // 5. Setup Perspective Camera
        cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.near = 0.1f;
        cam.far = 1000f;
        sceneManager.setCamera(cam);

        // 6. Lighting (Mahalaga ito para makita ang kulay ng kalsada)
        DirectionalLightEx light = new DirectionalLightEx();
        light.direction.set(1, -3, 1).nor();
        light.color.set(1, 1, 1, 1);
        sceneManager.environment.add(light);
        sceneManager.setAmbientLight(0.5f);
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        
        // Update animation
        if (animationController != null) {
            animationController.update(delta);
        }

        // Third Person Camera Logic
        characterScene.modelInstance.transform.getTranslation(characterPosition);
        tempVector.set(characterPosition).add(cameraOffset);
        cam.position.set(tempVector);
        
        // Tumingin sa itaas ng kaunti para sa GTA vibe
        tempVector.set(characterPosition).add(0, 1.5f, 0); 
        cam.lookAt(tempVector);
        cam.update();

        // Linisin ang screen at i-render
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        sceneManager.update(delta);
        sceneManager.render();
    }

    @Override
    public void dispose() {
        // Napaka-importante: I-dispose ang mga manually created assets
        if (roadModel != null) roadModel.dispose();
        if (whiteTexture != null) whiteTexture.dispose();
        if (sceneAsset != null) sceneAsset.dispose();
        if (sceneManager != null) sceneManager.dispose();
    }
}
