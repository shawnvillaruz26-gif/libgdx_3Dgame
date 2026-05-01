package com.brv.disasterrun3d;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.kotcrab.vis.ui.VisUI;

/**
 * Main Game Class para sa Disaster Run.
 * Ito ang taga-manage kung anong screen ang dapat ipakita (Menu o 3D Game).
 */
public class DisasterRunGame extends Game {

    @Override
    public void create() {
        // 1. I-load ang VisUI Skin.
        // Mahalaga ito para gumana ang mga buttons at UI elements sa MenuScreen.
        try {
            if (!VisUI.isLoaded()) {
                VisUI.load();
            }
        } catch (Exception e) {
            Gdx.app.error("DisasterRunGame", "Hindi ma-load ang VisUI: " + e.getMessage());
        }

        // 2. Itakda ang unang screen na lilitaw (Menu Screen).
        // Ipinapasa natin ang 'this' para makapag-switch ng screen ang Menu mamaya.
        this.setScreen(new MenuScreen(this));
    }

    @Override
    public void render() {
        // Napaka-importante: Tawagin ang super.render()
        // para mag-update at mag-draw ang kasalukuyang screen.
        super.render();
    }

    @Override
    public void dispose() {
        // 1. Linisin ang kasalukuyang screen para iwas memory leak.
        if (getScreen() != null) {
            getScreen().dispose();
        }

        // 2. Linisin ang VisUI resources pag-exit ng laro.
        if (VisUI.isLoaded()) {
            VisUI.dispose();
        }
    }
}
