import fs from 'fs';
import dotenv from 'dotenv';

// 1. Charger les variables du .env
dotenv.config();

const serverIP = process.env.VITE_SERVER_IP;

if (!serverIP) {
  console.error("❌ Erreur : VITE_SERVER_IP n'est pas défini dans le .env");
  process.exit(1);
}

// 2. Lire le fichier config actuel
const configPath = './capacitor.config.json';
const config = JSON.parse(fs.readFileSync(configPath, 'utf-8'));

// 3. Mettre à jour les valeurs
config.server = {
  "url": `http://${serverIP}:5173`,
  "cleartext": true,
  "allowNavigation": [ `${serverIP.substring(0, serverIP.lastIndexOf('.'))}.*` ]
};

// 4. Sauvegarder
fs.writeFileSync(configPath, JSON.stringify(config, null, 2));

console.log(`✅ Capacitor Config mis à jour avec l'IP : ${serverIP}`);