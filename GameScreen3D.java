package com.brv.disasterrun3d;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;

public class GameScreen3D extends ScreenAdapter {
    private final PerspectiveCamera cam;
    private final ModelBatch modelBatch;
    private final Model playerModel;
    private final Model floorModel;
    private final ModelInstance playerInstance;
    private final ModelInstance floorInstance;
    private final Environment environment;

    // --- Movement & Physics Variables ---
    private final Vector3 playerPosition = new Vector3(0, 1, 0);
    private float verticalVelocity = 0;
    private boolean isGrounded = true;

    public GameScreen3D(DisasterRunGame game) {
        modelBatch = new ModelBatch();

        // 1. Camera Setup
        cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.near = 1f;
        cam.far = 300f;
        cam.update();

        // 2. Lighting Setup
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        ModelBuilder modelBuilder = new ModelBuilder();

        // 3. Floor Setup (Mas malawak para hindi ka mahulog agad)
        floorModel = modelBuilder.createBox(500f, 1f, 500f,
            new Material(ColorAttribute.createDiffuse(Color.DARK_GRAY)),
            Usage.Position | Usage.Normal);
        floorInstance = new ModelInstance(floorModel);
        floorInstance.transform.setTranslation(0, -0.5f, 0);

        // 4. Player Setup (Green Box)
        playerModel = modelBuilder.createBox(2f, 2f, 2f,
            new Material(ColorAttribute.createDiffuse(Color.GREEN)),
            Usage.Position | Usage.Normal);
        playerInstance = new ModelInstance(playerModel);
    }

    @Override
    public void render(float delta) {
        // Mahalaga: Huwag hayaang mag-zero ang delta
        if (delta > 0.1f) delta = 0.1f;

        handleInput(delta);
        applyPhysics(delta);
        updateCamera();

        // --- RENDERING ---
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        modelBatch.begin(cam);
        modelBatch.render(floorInstance, environment);
        modelBatch.render(playerInstance, environment);
        modelBatch.end();
    }

    private void handleInput(float delta) {
        boolean moved = false;

        // Keyboard Controls with Debug Prints
        // Constants para sa tweakable values
        // Tinaasan ko para ramdam mo ang bilis
        float MOVE_SPEED = 20f;
        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            playerPosition.z -= MOVE_SPEED * delta;
            moved = true;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            playerPosition.z += MOVE_SPEED * delta;
            moved = true;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            playerPosition.x -= MOVE_SPEED * delta;
            moved = true;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            playerPosition.x += MOVE_SPEED * delta;
            moved = true;
        }

        // Jump Logic
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && isGrounded) {
            verticalVelocity = 15f;
            isGrounded = false;
            Gdx.app.log("Player", "Jumping!");
        }

        // Check kung gumagalaw nga sa log
        // Ito ay lalabas sa Logcat/Console para malaman mong gumagana ang keys
        // Gdx.app.log("PlayerPos", playerPosition.toString());
    }

    private void applyPhysics(float delta) {
        // Apply Gravity
        float GRAVITY = -30f;
        verticalVelocity += GRAVITY * delta;
        playerPosition.y += verticalVelocity * delta;

        // Collision detection sa sahig (y=1 balance point)
        if (playerPosition.y <= 1f) {
            playerPosition.y = 1f;
            verticalVelocity = 0;
            isGrounded = true;
        }

        // I-apply ang bagong position sa ModelInstance (Importante ito!)
        playerInstance.transform.setTranslation(playerPosition);
    }

    private void updateCamera() {
        // Camera Follow (Inilayo ko konti para mas kitang-kita ang galaw)
        cam.position.set(playerPosition.x, playerPosition.y + 10f, playerPosition.z + 20f);
        cam.lookAt(playerPosition.x, playerPosition.y, playerPosition.z);
        cam.update();
    }

    @Override
    public void resize(int width, int height) {
        cam.viewportWidth = width;
        cam.viewportHeight = height;
        cam.update();
    }

    @Override
    public void dispose() {
        modelBatch.dispose();
        playerModel.dispose();
        floorModel.dispose();
    }
}
