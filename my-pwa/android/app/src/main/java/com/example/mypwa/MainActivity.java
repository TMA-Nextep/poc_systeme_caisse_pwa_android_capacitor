package com.example.mypwa;

import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import com.getcapacitor.BridgeActivity;

import com.example.mypwa.HardwarePlugin;

public class MainActivity extends BridgeActivity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    // Code pour le mode Immersif "Sticky"
    registerPlugin(HardwarePlugin.class); // On enregistre le nouveau plugin
    super.onCreate(savedInstanceState);
    makeFullScreen();
  }

  // Permet de remettre en plein écran si l'utilisateur fait glisser son doigt
  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
    if (hasFocus) {
      makeFullScreen();
    }
  }

  // Optimisation : On force le plein écran à chaque fois que l'app revient au premier plan
  @Override
  public void onResume() {
      super.onResume();
      makeFullScreen();
  }

  public void makeFullScreen() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        final WindowInsetsController controller = getWindow().getInsetsController();
        if (controller != null) {
            // Masque les barres de navigation et de statut
            controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            // Empêche les barres de réapparaître au moindre toucher (mode immersif)
            controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
    } else {
        // Ancienne méthode pour compatibilité
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }
}
}