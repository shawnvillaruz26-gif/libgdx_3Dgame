package com.brv.disasterrun3d;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;

public class MenuScreen extends ScreenAdapter {
    private final DisasterRunGame game;
    private final Stage stage;

    public MenuScreen(DisasterRunGame game) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport());
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);

        // Pangunahing table na sasakop sa buong screen
        VisTable root = new VisTable();
        root.setFillParent(true);
        stage.addActor(root);

        // --- DESIGN ELEMENTS ---

        // 1. Title na may Kulay at Shadow (Simple Design Trick)
        VisLabel title = new VisLabel("DISASTER RUN 3D");
        title.setFontScale(3f);
        title.setColor(Color.SCARLET); // Gawing pula para sa "Disaster" theme
        title.setAlignment(Align.center);

        // 2. Subtitle o Tagline
        VisLabel tagline = new VisLabel("Survival is the only option.");
        tagline.setFontScale(1.2f);
        tagline.setColor(Color.GRAY);

        // 3. Container para sa Buttons (para mas organize ang spacing)
        VisTable menuButtons = new VisTable();

        // Custom Styled Buttons gamit ang built-in styles ng VisUI
        VisTextButton startBtn = new VisTextButton("RUN NOW");
        VisTextButton settingsBtn = new VisTextButton("SETTINGS");
        VisTextButton exitBtn = new VisTextButton("QUIT GAME");

        // --- LAYOUT ARRANGEMENT ---

        root.add(title).padTop(50).expandX().center().row();
        root.add(tagline).padBottom(60).row();

        // Pag-stack ng buttons sa gitna
        menuButtons.add(startBtn).width(300).height(70).padBottom(15).row();
        menuButtons.add(settingsBtn).width(300).height(70).padBottom(15).row();
        menuButtons.add(exitBtn).width(300).height(70).row();

        root.add(menuButtons).expandY().top();

        // Footer Text
        VisLabel footer = new VisLabel("v1.0 - Created by BRV");
        footer.setColor(0.5f, 0.5f, 0.5f, 1f); // Dark gray
        root.row();
        root.add(footer).padBottom(20).bottom();

        // --- BUTTON LOGIC ---

        startBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new GameScreen3D(game));
            }
        });

        exitBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });
    }

    @Override
    public void render(float delta) {
        // Background Color - Gawing "Dark Blue-Black" para sa cinematic feel
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
