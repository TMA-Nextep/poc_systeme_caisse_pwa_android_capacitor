package com.example.mypwa;

import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import com.getcapacitor.BridgeActivity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import android.os.UserManager;
import android.view.WindowManager;

import com.example.mypwa.HardwarePlugin;

public class MainActivity extends BridgeActivity {
  
  private DevicePolicyManager dpm;
  private ComponentName adminName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(HardwarePlugin.class);
        super.onCreate(savedInstanceState); // super.onCreate doit être appelé après l'enregistrement des plugins

        dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminName = new ComponentName(this, AdminReceiver.class);

        if (dpm.isDeviceOwnerApp(getPackageName())) {
            // A. Whitelist des packages
            String[] packages = {getPackageName()};
            dpm.setLockTaskPackages(adminName, packages);

            // B. Configuration des features (Android 9+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // On force explicitement NONE pour ne RIEN laisser passer
                dpm.setLockTaskFeatures(adminName, DevicePolicyManager.LOCK_TASK_FEATURE_NONE);
            }

            dpm.addUserRestriction(adminName, UserManager.DISALLOW_ADJUST_VOLUME);
            dpm.addUserRestriction(adminName, UserManager.DISALLOW_SAFE_BOOT);
            dpm.addUserRestriction(adminName, UserManager.DISALLOW_FACTORY_RESET);
            dpm.addUserRestriction(adminName, UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA);

            // Pour bloquer la barre de statut (le haut)
            dpm.addUserRestriction(adminName, "no_status_bar");

            // Pour bloquer les dialogues système (bouton Power / Erreurs)
            dpm.addUserRestriction(adminName, UserManager.DISALLOW_SYSTEM_ERROR_DIALOGS);
            

            // D. Lancement effectif
            startLockTask();
            Toast.makeText(this, "Mode Kiosk Total Activé", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "⚠️ ERREUR: Pas Device Owner", Toast.LENGTH_LONG).show();
        }

        makeFullScreen();
    }

    public void startKioskModeManual() {
    try {
        if (dpm.isDeviceOwnerApp(getPackageName())) {
            // On s'assure que les packages sont autorisés
            String[] packages = {getPackageName()};
            dpm.setLockTaskPackages(adminName, packages);
            
            // On relance le verrouillage
            startLockTask();
            makeFullScreen(); // On force le plein écran
            Toast.makeText(this, "Mode Kiosk Réactivé", Toast.LENGTH_SHORT).show();
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
}

    // Fonction pour sortir du mode Kiosk (à appeler via un bouton caché par exemple)
    public void stopKioskMode() {
        try {
            stopLockTask();
            Toast.makeText(this, "Mode Kiosk Désactivé", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_DEFAULT);
            }
        } else {
            // Ancienne méthode pour compatibilité
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE
            );
        }
    }
}