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

Si le mirorring de Visual Studio ne fonctionne pas avec en USB avec:
- tablette device  rk3588 (Kiosk)
1. installer scrcpy : https://github.com/Genymobile/scrcpy/releases
2. extraire les fichier
3. se placer dans le dossier

```powershell
adb shell settings put system user_rotation 1
adb shell wm size reset
adb shell wm size 1920x1920
.\scrcpy.exe --video-encoder=OMX.google.h264.encoder --always-on-top --window-title="Mirroring RK3588"
```

Pour utiliser plus facilement adb et scrcpy on va créer des alias de commande
1. on va déplacer le dossier scrcpy dans "Programmes Files"
2. on modifie le profile powershell pour créer des alias
```powershell
# où est sauvegarder le fichier profile ?
$PROFILE
```
3. écrire les commande suivante dans le fichier, en modifiant suivant vos version et spécificités
Set-Alias scrcpy "C:\Program Files\scrcpy-win64-v3.3.4\scrcpy-win64-v3.3.4\scrcpy.exe"
Set-Alias adb "C:\Program Files\scrcpy-win64-v3.3.4\scrcpy-win64-v3.3.4\adb.exe"
4. Relancer la console et vous pourrez utiliser directement les commandes scrcpy et adb

Pour utiliser adb via Wifi-Direct
```powershell
# récupérer id du device
adb devices
# device connecté au Téléphone via USB
adb -s ID_DEVICE tcpip 5555
adb connect 192.168.1.XXXX:5555

# pour se déconnecter
adb disconnect 192.168.1.XXXX:5555
adb -s ID_DEVICE usb
```

Si la tablette n'a pas accès à internet via Ethernet ou Wifi
On utilise l'outil gnirehtet qui va permettre de partager la connexion internet du PC branché en USB à la tablette
```powershell
# télécharger gnirehtet : https://github.com/Genymobile/gnirehtet/releases
# Gnirehtet a besoin de avoir adb directemnet accessible dans son dossier
cp .\adb.exe .\AdbWinApi.dll .\AdbWinUsbApi.dll C:\Users\tm\Downloads\gnirehtet-rust-win6

# Dans le dossier gnirehtet lancer la commande de tunnel "vpn"
.\gnirehtet.exe run

# Pour eviter que le tunnel vpn ne perturbe les interfaces réseeaux, il faut le killer (en aillant le pc et la tablette connecté en usb)
.\gnirehtet.exe stop
```

Comment appliquer Device Owner ?
1. Supprimer tous les comptes Google
2. lancer commande adb
```powershell
adb shell dpm set-device-owner com.example.mypwa/.AdminReceiver

# Pour aller le device owner
adb shell dpm remove-active-admin com.example.mypwa/.AdminReceiver
```

Comment sortir du mode Kiosk Vérouillé
```powershell
# checker si mode kiosk activé 
adb shell dumpsys activity activities | findstr mLockTaskModeState
# LOCKED = Activé

adb shell am stop-lock-task
```


### 2. Installation et Préparation (Root : `my-pwa`)
Si vous venez de cloner le projet, suivez cet ordre précis :

```powershell
cd my-pwa

# Installation des dépendances web et outils de build
npm install

# Installation des plugins natifs Capacitor
npm install @capacitor/device @capacitor/network @capacitor/status-bar @capacitor/preferences

# Installation de l'outil pour gérer le .env dans les scripts Node
npm install dotenv --save-dev

# Synchronisation avec le dossier Android
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

### 5. Passage au Web to Native
Voici les différents points à prendre en compte pour utiliser l'app en mode web to Native. C'est à dire que l'application va se connecter/faire appel à un serveur distant pour l'interface. Mais l'application garde la faculté de faire des appels système/matériel pour par exemple :
- se mettre en mode kiosk
- scanner le réseau
- envoyer des ordres à l'imprimante via le réseau local
- recevoir des données (scanner, balance) via le réseau local
- etc...

```powershell
# dans un premier terminal 
## en mode dev, lancer le serveur localement sur l'adresse ip de l'host dans le reseau local + port spécifique
npx serve -s dist -l tcp://192.168.1.100:5173

# dans un deuxième terminal
## lancer l'update automatique de Vue
npm run watch
```

#### 6. Dans le fichier capacitor.config.json 
le fichier est maintenant mise à jour grâce au fichier update-config.js
```powershell
# Met à jour capacitor.config.json + Build Vue + Sync Android
npm run build-app
```
Attention à cleartext qui est utile uniquement en HTTP. Changer cela quand on passe en HTTPS !

#### 7. Lancement de l'infrastructure (Docker)
Pour démarrer la base de données et l'outil de visualisation.

```powershell
# Démarrer InfluxDB et Grafana en arrière-plan
docker compose -f .\docker-compose.infra.yml up -d

# Vérifier que les conteneurs tournent bien
docker compose -f .\docker-compose.infra.yml ps

# Arrêter l'infrastructure (sans perdre les données)
docker compose -f .\docker-compose.infra.yml stop
```

Maintenance influxDB - Commandes utiles pour la gestion des utilisateurs et mots de passe.
```powershell
# Changer le mot de passe admin sans tout réinstaller
docker exec -it influxdb influx user password -n admin -p TON_NOUVEAU_MDP

# Tout réinitialiser (ATTENTION : supprime TOUTES les données et dashboards)
docker compose -f .\docker-compose.infra.yml down -v
docker compose -f .\docker-compose.infra.yml up -d
```

