package com.example.mypwa;

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

  private void makeFullScreen() {
    getWindow().getDecorView().setSystemUiVisibility(
        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // Masque la barre du bas
        | View.SYSTEM_UI_FLAG_FULLSCREEN    // Masque la barre du haut
        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY // Rend le masquage permanent
    );
  }
}