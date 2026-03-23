<script setup>
import { ref, onMounted } from 'vue'
import { Network } from '@capacitor/network'
import HelloWorld from './components/HelloWorld.vue'
import { StatusBar } from '@capacitor/status-bar';
import { Capacitor, registerPlugin } from '@capacitor/core'; 

// Enregistrement du plugin
const Hardware = Capacitor.isNativePlatform() 
  ? registerPlugin('HardwarePlugin') 
  : null;

// --- VARIABLES RÉACTIVES ---
const isOnline = ref(true)
const connectionType = ref('unknown')
const isPrinting = ref(false)
const isScanning = ref(false)
const foundPrinters = ref([])
const selectedIp = ref('192.168.1.50')

// AJOUT DES VARIABLES DE SCAN MANQUANTES
const currentScanningIp = ref('')
const scannedIpsCount = ref(0)

onMounted(async () => {
  if (Capacitor.isNativePlatform()) {
    try {
      await StatusBar.hide();
    } catch (error) {
      console.error('Erreur StatusBar:', error);
    }
  }

  const status = await Network.getStatus()
  isOnline.value = status.connected
  connectionType.value = status.connectionType

  await Network.addListener('networkStatusChange', status => {
    isOnline.value = status.connected
    connectionType.value = status.connectionType
  })

  // ON PLACE LES ÉCOUTEURS ICI, DANS LE ONMOUNTED
  if (Hardware) {
    await Hardware.addListener('scanProgress', (data) => {
      currentScanningIp.value = data.scanning;
      scannedIpsCount.value++;
    });

    await Hardware.addListener('printerFound', (data) => {
      if (!foundPrinters.value.includes(data.ip)) {
        foundPrinters.value.push(data.ip);
      }
    });
  }
})

// --- MÉTHODES ---

const scanForPrinters = async () => {
  if (!Hardware) return;
  isScanning.value = true;
  foundPrinters.value = [];
  scannedIpsCount.value = 0;
  try {
    await Hardware.discoverPrinters();
  } catch (e) {
    console.error(e);
  } finally {
    isScanning.value = false;
    currentScanningIp.value = 'Terminé';
  }
};

const selectPrinter = (ip) => {
  selectedIp.value = ip;
};

const handleRealPrint = async () => {
  if (!Hardware) return;
  try {
    await Hardware.realPrint({
      ip: selectedIp.value,
      port: 9100,
      content: 'BON DE COMMANDE\n----------------\n1x Pizza Regina\n1x Coca Cola\n----------------\nMerci !'
    });
    alert("Impression lancée sur " + selectedIp.value);
  } catch (e) {
    alert("Erreur : " + e.message);
  }
};

const handleTestPrint = async () => {
  if (!Capacitor.isNativePlatform() || !Hardware) {
    alert("La vibration ne fonctionne que sur un vrai smartphone Android.");
    return;
  }
  isPrinting.value = true;
  try {
    await Hardware.simulatePrint({ duration: 2 });
  } catch (e) {
    console.error("Erreur Hardware:", e);
  } finally {
    setTimeout(() => { isPrinting.value = false; }, 2000);
  }
};
</script>

<template>
  <div :class="['status-bar', isOnline ? 'online' : 'offline']">
    {{ isOnline ? '🟢 Connecté (' + connectionType + ')' : '🔴 Hors-ligne' }}
  </div>

  <div class="container">
    <div class="section">
      <button @click="scanForPrinters" :disabled="isScanning" class="scan-btn">
        {{ isScanning ? '🔍 Recherche...' : '🔎 Scanner le réseau' }}
      </button>

      <div v-if="isScanning" class="scan-monitor">
        <p>Scan en cours : <strong>{{ currentScanningIp }}</strong></p>
        <progress :value="scannedIpsCount" max="254" style="width: 100%"></progress>
        <small>{{ scannedIpsCount }} / 254 adresses testées</small>
      </div>

      <div v-if="foundPrinters.length > 0" class="printer-list">
        <p>Imprimantes détectées :</p>
        <button 
          v-for="ip in foundPrinters" 
          :key="ip" 
          @click="selectPrinter(ip)"
          :class="['ip-badge', { 'active': selectedIp === ip }]"
        >
          {{ ip }}
        </button>
      </div>
    </div>

    <hr />

    <div class="actions">
      <p>IP Cible : <strong>{{ selectedIp }}</strong></p>
      
      <button @click="handleRealPrint" class="print-btn">
        🖨️ Imprimer Ticket Réel
      </button>

      <button @click="handleTestPrint" :disabled="isPrinting" class="vibrate-btn">
        {{ isPrinting ? '📳 Vibration...' : '📳 Tester Vibreur' }}
      </button>
    </div>
  </div>

  <HelloWorld />
</template>

<style scoped>
/* Ajout d'un peu de style pour le moniteur de scan */
.scan-monitor {
  margin: 15px 0;
  padding: 10px;
  background: #f3f4f6;
  border-radius: 8px;
  font-size: 0.9rem;
}
.container { padding: 20px; font-family: sans-serif; }
.section { margin-bottom: 20px; text-align: center; }
.printer-list { margin-top: 15px; display: flex; flex-wrap: wrap; gap: 10px; justify-content: center; }
.ip-badge { padding: 8px 15px; border: 2px solid #10b981; border-radius: 20px; background: white; cursor: pointer; }
.ip-badge.active { background: #10b981; color: white; }
.scan-btn { background-color: #10b981; color: white; padding: 10px 20px; border-radius: 8px; border: none; width: 100%; font-weight: bold; }
.actions { display: flex; flex-direction: column; gap: 10px; align-items: center; }
.vibrate-btn { background-color: #6b7280; color: white; padding: 10px; border-radius: 8px; border: none; width: 100%; }
.status-bar { padding: 10px; text-align: center; font-weight: bold; }
.online { background-color: #d4edda; color: #155724; }
.offline { background-color: #f8d7da; color: #721c24; }
.print-btn { padding: 15px 30px; font-size: 18px; font-weight: bold; border-radius: 12px; border: none; background-color: #4f46e5; color: white; width: 100%; cursor: pointer; }
.print-btn:disabled { background-color: #9ca3af; }
</style>