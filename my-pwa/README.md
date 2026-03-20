Voici une version enrichie et complète de ton `README.md`. J'ai intégré les étapes manquantes de synchronisation, la correction automatique des erreurs courantes et la partie **Android Studio** pour l'émulation.

---

## 🚀 Guide Complet : Build & Simulation Android (Capacitor)

Ce dépôt contient une PWA (Vue.js + Vite) encapsulée pour Android. Ce guide détaille les étapes pour installer, corriger, synchroniser et simuler l'application.

### 1. Configuration de l'Environnement (Windows)
Avant toute commande Gradle, assurez-vous que le terminal pointe vers le JDK d'Android Studio :

```powershell
# Définir le JAVA_HOME pour la session actuelle
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"

# Vérifier la configuration
echo $env:JAVA_HOME
```

### 2. Installation et Préparation (Root : `my-pwa`)
Si vous venez de cloner le projet, suivez cet ordre précis :

```powershell
cd my-pwa

# 1. Installer les dépendances (avec gestion des conflits Vite/PWA)
npm install --legacy-peer-deps

# 2. Builder le projet Web (génère le dossier /dist)
npm run build

# 3. Ajouter la plateforme Android (si non présente)
npx cap add android

# 4. Synchroniser (Crucial : copie le /dist et met à jour les plugins)
npx cap sync android
```

### 3. Compilation de l'APK (Terminal)
Pour générer le fichier installable manuellement :

```powershell
cd android
./gradlew clean
./gradlew assembleDebug
```
*L'APK sera généré dans : `android\app\build\outputs\apk\debug\app-debug.apk`*

### 4. Simulation et Debug (Android Studio)
Pour tester l'application dans un environnement simulé (Émulateur) :

1.  **Ouvrir le projet :**
    ```powershell
    npx cap open android
    ```
    *Cela lance Android Studio directement sur le bon dossier.*

2.  **Lancer l'émulateur :**
    * Dans Android Studio, allez dans **Device Manager** (icône téléphone).
    * Créez ou lancez un appareil virtuel (Pixel 6, etc.).
    * Cliquez sur le bouton **"Run" (flèche verte)** en haut à droite.

3.  **Hot Reload (Optionnel) :**
    Si vous modifiez le code Web, n'oubliez pas de refaire :
    `npm run build` puis `npx cap copy android` pour voir les changements dans l'émulateur.
