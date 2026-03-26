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
  const scannedData = ref('')
  const scannedCodes = ref([])
  const scanInput = ref(null)

  // AJOUT DES VARIABLES DE SCAN Network MANQUANTES
  const currentScanningNetworkIp = ref('')
  const scannedNetworkIpsCount = ref(0)

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
        currentScanningNetworkIp.value = data.scanning;
        scannedNetworkIpsCount.value++;
      });

      await Hardware.addListener('printerFound', (data) => {
        if (!foundPrinters.value.includes(data.ip)) {
          foundPrinters.value.push(data.ip);
        }
      });
    }
  })

  // --- MÉTHODES ---

  const handleBarcode = (e) => {
    if (e.key === 'Enter') {
      if (scannedData.value.trim() !== '') {
        // On ajoute le code au début de la liste (unshift)
        // On ajoute aussi l'heure pour s'y retrouver
        const newEntry = {
          code: scannedData.value,
          time: new Date().toLocaleTimeString()
        };
        
        scannedCodes.value.unshift(newEntry);
        
        // On vide le champ pour le prochain scan
        scannedData.value = '';
      }
    }
  };

  const clearList = () => {
    scannedCodes.value = [];
  };

  const keepFocus = () => {
    if (scanInput.value) scanInput.value.focus();
  };

  const scanForPrinters = async () => {
    if (!Hardware) return;
    isScanning.value = true;
    foundPrinters.value = [];
    scannedNetworkIpsCount.value = 0;
    try {
      await Hardware.discoverPrinters();
    } catch (e) {
      console.error(e);
    } finally {
      isScanning.value = false;
      currentScanningNetworkIp.value = 'Terminé';
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

  const enterKiosk = async () => {
  if (Hardware) {
    try {
      await Hardware.enterKioskMode();
    } catch (e) {
      console.error("Erreur lors de l'entrée en mode Kiosk:", e);
      alert("Erreur : " + e.message);
    }
  } else {
    alert("Le plugin Hardware n'est pas disponible");
  }
};

  const quitKiosk = async () => {
    if (Hardware) {
      try {
        await Hardware.quitKioskMode();
      } catch (e) {
        console.error("Erreur lors de la sortie du mode Kiosk:", e);
        alert("Erreur : " + e.message);
      }
    } else {
      alert("Le plugin Hardware n'est pas disponible (Plateforme non-native)");
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
        <p>Scan en cours : <strong>{{ currentScanningNetworkIp }}</strong></p>
        <progress :value="scannedNetworkIpsCount" max="254" style="width: 100%"></progress>
        <small>{{ scannedNetworkIpsCount }} / 254 adresses testées</small>
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

  <div class="section scanner-section">
    <h3>Scanner QR / NFC</h3>
    
    <div class="input-wrapper">
      <input 
        ref="scanInput"
        v-model="scannedData" 
        @keydown="handleBarcode"
        placeholder="Scannez ici..."
        class="scan-input"
        autocomplete="off"
      />
      <button @click="keepFocus" class="focus-indicator">🎯</button>
    </div>

    <div class="history-container">
      <div class="history-header">
        <span>Historique ({{ scannedCodes.length }})</span>
        <button @click="clearList" class="clear-btn" v-if="scannedCodes.length > 0">
          🗑️ Effacer tout
        </button>
      </div>

      <ul class="code-list">
        <li v-for="(item, index) in scannedCodes" :key="index" class="code-item">
          <span class="code-text">{{ item.code }}</span>
          <span class="code-time">{{ item.time }}</span>
        </li>
        <li v-if="scannedCodes.length === 0" class="empty-msg">
          Aucun scan pour le moment
        </li>
      </ul>
    </div>
  </div>

  <HelloWorld />

  <div class="container">
    
    <div class="section admin-demo">
      <div class="button-group">
        <button @click="enterKiosk" class="enter-btn">
          🔒 Entrer en mode Kiosk
        </button>
        <button @click="quitKiosk" class="quit-btn">
          🔓 Quitter le mode Kiosk (Démo)
        </button>
      </div>      
    </div>

    </div>
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
.scanner-section {
  background: #f3f4f6;
  padding: 15px;
  border-radius: 12px;
  margin-top: 20px;
}

.input-wrapper {
  display: flex;
  gap: 10px;
  margin-bottom: 15px;
}

.scan-input {
  flex: 1;
  padding: 12px;
  border: 2px solid #6366f1;
  border-radius: 8px;
  font-size: 1rem;
}

.history-container {
  background: white;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  overflow: hidden;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
  background: #e5e7eb;
  font-weight: bold;
  font-size: 0.9rem;
}

.clear-btn {
  background: #ef4444;
  color: white;
  border: none;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 0.8rem;
  cursor: pointer;
}

.code-list {
  list-style: none;
  padding: 0;
  margin: 0;
  max-height: 200px; /* Liste défilante */
  overflow-y: auto;
}

.code-item {
  display: flex;
  justify-content: space-between;
  padding: 10px;
  border-bottom: 1px solid #f3f4f6;
  font-family: monospace;
}

.code-text {
  color: #1f2937;
  font-weight: bold;
}

.code-time {
  color: #9ca3af;
  font-size: 0.8rem;
}

.empty-msg {
  padding: 20px;
  text-align: center;
  color: #9ca3af;
  font-style: italic;
}

.admin-demo {
  margin-top: 10px;
  margin-bottom: 20px;
}

.button-group {
  display: flex;
  gap: 10px;
  justify-content: center;
}

.enter-btn {
  background-color: #10b981; /* Vert */
  color: white;
  padding: 12px 20px;
  border-radius: 8px;
  border: 2px solid #059669;
  flex: 1;
  font-weight: bold;
  cursor: pointer;
}

.quit-btn {
  flex: 1;
  background-color: #ef4444; /* Rouge */
  color: white;
  padding: 12px 20px;
  border-radius: 8px;
  border: 2px solid #b91c1c;
  width: 100%;
  font-weight: bold;
  cursor: pointer;
  font-size: 1rem;
}

.quit-btn:active {
  background-color: #b91c1c;
}

</style>