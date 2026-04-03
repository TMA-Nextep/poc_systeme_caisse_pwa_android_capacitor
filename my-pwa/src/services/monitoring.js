// src/services/monitoring.js
import { bus } from './eventBus';
import { Capacitor } from '@capacitor/core';
import { Device } from '@capacitor/device';

const logDeviceInfo = async () => {
  const info = await Device.getId();
  return info.identifier; // Ex: "987654321-abc-123"
};

// On récupère les variables du fichier .env
const TOKEN = import.meta.env.VITE_INFLUX_TOKEN;
const BASE_URL = import.meta.env.VITE_INFLUX_URL;
const ORG = import.meta.env.VITE_INFLUX_ORG;
const BUCKET = import.meta.env.VITE_INFLUX_BUCKET;

const INFLUX_URL = `${BASE_URL}/api/v2/write?org=${ORG}&bucket=${BUCKET}&precision=s`;

let deviceUUID = 'web_browser'; // Valeur par défaut
let lastData = { printer: "unknown", scanner: "online", battery: 0 };

const initDevice = async () => {
    try {
        if (Capacitor.getPlatform() === 'web') {
            deviceUUID = "PC-Developpement";
        } else {
            const info = await Device.getId();
            deviceUUID = info.identifier;
        }
    } catch (e) {
        deviceUUID = "PC-Developpement";
    }
};
initDevice();

bus.on('hardware-update', (data) => {
    lastData = { ...lastData, ...data };
});

setInterval(async () => {
    const ts = Math.floor(Date.now() / 1000);
    const line = `heartbeat,device=${deviceUUID} printer="${lastData.printer}",scanner="${lastData.scanner}",battery=${lastData.battery} ${ts}`;

    try {
        await fetch(INFLUX_URL, {
            method: 'POST',
            headers: { 
                'Authorization': `Token ${TOKEN}`,
                'Content-Type': 'text/plain; charset=utf-8'
            },
            body: line
        });
    } catch (e) {
        console.error("Erreur monitoring:", e);
    }
}, 10000);