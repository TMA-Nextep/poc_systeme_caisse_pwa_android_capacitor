README — Full command list to build a Vue PWA (Capacitor) + NestJS API running on the same Android arm64 tablet (Termux)
Below are the exact commands, in order, split by environment: Development PC (macOS / Linux / Windows WSL) and Android tablet (Termux on device). This is a copy‑pasteable sequence for a PoC where the Vue/Capacitor app is installed on the tablet and the NestJS (Node) API runs on the tablet, accessible at http://127.0.0.1:3000.

Prerequisites (install on PC)
Node.js 16+ and npm or yarn
Android Studio + Android SDK + ADB
Git
Termux installed on the tablet (install from F‑Droid), with storage permission enabled if copying files
1) Create Vue PWA (on your PC)
Create project and install deps:
bash

# create project using Vite + Vue
npm init vite@latest my-pwa -- --template vue
cd my-pwa
npm install
Install PWA plugin:
bash

npm install -D vite-plugin-pwa
(Optional) Create a minimal vite.config.js in the project root (example):
javascript

// vite.config.js
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { VitePWA } from 'vite-plugin-pwa'

export default defineConfig({
  plugins: [
    vue(),
    VitePWA({
      registerType: 'autoUpdate',
      manifest: {
        name: 'Local PWA',
        short_name: 'LocalPWA',
        start_url: '.',
        display: 'standalone'
      }
    })
  ]
})
Build the web assets:
bash

npm run build
# This produces the `dist` folder
2) Add Capacitor & Android project (on your PC)
Install Capacitor and initialize:
bash

npm install @capacitor/core @capacitor/cli
npx cap init my.local.app com.example.localapp --web-dir=dist
Add Android platform:
bash

npx cap add android
After any web change, rebuild and copy to native:
bash

npm run build
npx cap copy android
Open Android project in Android Studio (to build/install):
bash

npx cap open android
Notes:

In Android Studio you can run the app to produce an APK; default builds include arm64 ABI support.
3) Create NestJS API (on your PC)
Install Nest CLI and create project:
bash

npm i -g @nestjs/cli
nest new api-gateway
cd api-gateway
Install Axios / HttpModule if you will proxy external services:
bash

npm install @nestjs/axios axios
Make sure src/main.ts listens on 0.0.0.0 and enables CORS:
ts

// src/main.ts (example)
import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';
async function bootstrap() {
  const app = await NestFactory.create(AppModule);
  app.enableCors({ origin: true });
  await app.listen(3000, '0.0.0.0');
}
bootstrap();
Build the Nest app (for production run on device):
bash

npm run build
# dist/main.js will be produced
(You may optionally test locally with npm run start:dev during development.)

4) Prepare files to copy to the tablet
Option A — push repo to git host and clone in Termux (recommended). Option B — copy via adb push or scp.

Example with Git (recommended):

bash

# from PC
git init
git add .
git commit -m "initial"
git remote add origin <your-repo-url>
git push -u origin main
Or export archive:

bash

# package project to tarball (api only)
cd api-gateway
tar -czf api-gateway.tgz .
5) Setup Termux on the Android tablet
Install Termux from F‑Droid and open it.
Grant storage permission (optional, required if you want logs on sdcard):
bash

termux-setup-storage
Update and install Node, git:
bash

pkg update -y && pkg upgrade -y
pkg install -y git
pkg install -y nodejs-lts
# verify
node -v
npm -v
6) Clone / copy NestJS server into Termux and install
Option A — clone from Git:

bash

# in Termux
cd $HOME
git clone <your-repo-url> api-gateway
cd api-gateway
npm ci
Option B — push archive from PC and extract in Termux (using adb):

bash

# on PC, push archive to device
adb push api-gateway.tgz /data/data/com.termux/files/home/
# in Termux
cd $HOME
tar -xzf api-gateway.tgz
cd api-gateway
npm ci
Build the Nest app in Termux:
bash

npm run build
(If a dev flow is desired, you can run npm run start:dev if ts-node is available.)

7) Run the NestJS server on the tablet (background)
Start server (production build):
bash

# in Termux inside api-gateway folder
nohup node dist/main.js > /sdcard/nest.log 2>&1 &
# or write log to home folder
nohup node dist/main.js > ~/nest.log 2>&1 &
Verify it's running:
bash

# check process
ps aux | grep node
# curl the endpoint locally
curl http://127.0.0.1:3000/
# or your health endpoint
curl http://127.0.0.1:3000/health
Notes:

If you prefer a persistent process manager, you can try pm2 (may require build tools) or use Termux:Boot / Termux:Widget to re-run the app on boot.
8) Configure the Capacitor app to call the local API
In your Vue app, set the API base URL to the localhost address on device:
js

// src/config.js (example)
export const API_BASE = 'http://127.0.0.1:3000'
In production build, make sure the bundled web assets call that URL. Rebuild and copy:
bash

npm run build
npx cap copy android
Allow cleartext traffic on Android (PoC only). Edit android/app/src/main/AndroidManifest.xml (in Android Studio) and set:
xml

<application
    android:usesCleartextTraffic="true"
    ... >
    ...
</application>
(Alternatively create a Network Security Config XML to allow cleartext to 127.0.0.1).

9) Build and install the APK to the tablet
Build debug APK from Android project (in project root android/):
bash

# from PC in your project android folder
cd android
./gradlew assembleDebug
# or if on Windows:
# gradlew.bat assembleDebug
Install using ADB (tablet connected via USB with USB debugging enabled):
bash

adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
Launch the app on the tablet; the app's WebView will load the bundled assets and call the NestJS server running on the device at http://127.0.0.1:3000.
10) Useful Termux helpers (on tablet)
View server logs:
bash

tail -f /sdcard/nest.log
# or if log in home
tail -f ~/nest.log
Stop Node processes:
bash

ps aux | grep node
kill <pid>
# or kill by name (use with caution)
pkill node
Re-run server in foreground (to see logs live):
bash

node dist/main.js
To auto-start on boot (Termux:Boot required):
bash

# create file ~/.termux/boot/start-server.sh and make executable
# example content:
#!/data/data/com.termux/files/usr/bin/sh
cd /data/data/com.termux/files/home/api-gateway
nohup node dist/main.js > /data/data/com.termux/files/home/nest.log 2>&1 &
11) Troubleshooting commands
Confirm Node listening:
bash

ss -ltnp | grep 3000
# or
netstat -ltnp | grep 3000
Inspect WebView network failures (use adb logcat on PC while app runs):
bash

adb logcat | grep chromium
# or capture all logs
adb logcat > logcat.txt
On tablet check local IP (if you need it instead of 127.0.0.1):
bash

ifconfig
# or
ip addr show
12) Complete compact command sequence (copy-paste friendly)
This block shows the core commands in minimal form. Adjust <repo-url> and paths as needed.

bash

# ---- On PC: create Vue PWA and Capacitor
npm init vite@latest my-pwa -- --template vue
cd my-pwa
npm install
npm install -D vite-plugin-pwa
# add vite.config.js per README
npm run build
npm install @capacitor/core @capacitor/cli
npx cap init my.local.app com.example.localapp --web-dir=dist
npx cap add android
npx cap copy android
npx cap open android
# build apk in Android Studio or:
cd android
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk

# ---- On PC: prepare NestJS repo (push to git)
npm i -g @nestjs/cli
nest new api-gateway
cd api-gateway
npm install @nestjs/axios axios
# set main.ts listen host to 0.0.0.0 and port to 3000
npm run build
git init
git add .
git commit -m "api for android local PoC"
git remote add origin <repo-url>
git push -u origin main

# ---- On Android tablet (Termux)
pkg update -y && pkg upgrade -y
pkg install -y git nodejs-lts
termux-setup-storage
cd $HOME
git clone <repo-url> api-gateway
cd api-gateway
npm ci
npm run build
nohup node dist/main.js > ~/nest.log 2>&1 &

# ---- Verify on tablet
curl http://127.0.0.1:3000/
tail -f ~/nest.log
Security & Caveats (brief)
The android:usesCleartextTraffic="true" is for PoC only; use HTTPS for production.
Android may kill background Termux processes; use Termux:Boot or other mechanisms for persistence.
Running a local server on-device is insecure for production; this is for local PoC only.
