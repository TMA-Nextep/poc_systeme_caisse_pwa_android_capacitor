<script setup>
import { ref, onMounted } from 'vue'
import { Network } from '@capacitor/network'
import HelloWorld from './components/HelloWorld.vue'
import { StatusBar } from '@capacitor/status-bar';
import { Capacitor, registerPlugin } from '@capacitor/core'; 

// Enregistrement du plugin Hardware (le pont vers le vibreur/imprimante)
const Hardware = registerPlugin('HardwarePlugin');

// 1. On crée une variable réactive pour l'interface
const isOnline = ref(true)
const connectionType = ref('unknown')
const isPrinting = ref(false)

onMounted(async () => {

  // On vérifie qu'on est bien sur un appareil mobile (pas sur un navigateur PC)
  if (Capacitor.isNativePlatform()) {
    try {
      // Masque complètement la barre de notification
      await StatusBar.hide();
    } catch (error) {
      console.error('Erreur StatusBar:', error);
    }
  }

  // 2. Vérification du statut au démarrage de l'app
  const status = await Network.getStatus()
  isOnline.value = status.connected
  connectionType.value = status.connectionType

  // 3. On écoute les changements (Wi-Fi qui coupe, etc.)
  await Network.addListener('networkStatusChange', status => {
    isOnline.value = status.connected
    connectionType.value = status.connectionType
  })
})

// Fonction pour tester la vibration (Simulation d'impression)
const handleTestPrint = async () => {
  if (!Capacitor.isNativePlatform()) {
    alert("La vibration ne fonctionne que sur un vrai smartphone Android.");
    return;
  }

  isPrinting.value = true;
  try {
    // On appelle la méthode Kotlin que nous avons créée
    await Hardware.simulatePrint({ duration: 2 });
  } catch (e) {
    console.error("Erreur Hardware:", e);
  } finally {
    // On simule un petit délai avant de libérer le bouton
    setTimeout(() => { isPrinting.value = false; }, 2000);
  }
}

</script>

<template>
  <div :class="['status-bar', isOnline ? 'online' : 'offline']">
    {{ isOnline ? '🟢 Connecté (' + connectionType + ')' : '🔴 Hors-ligne (Mode SQLite activé)' }}
  </div>

  <div class="test-container">
    <button 
      @click="handleTestPrint" 
      :disabled="isPrinting"
      :class="['print-btn', { 'printing': isPrinting }]"
    >
      {{ isPrinting ? '🖨️ Impression en cours...' : '🧾 Tester l\'Imprimante' }}
    </button>
  </div>

  <HelloWorld />
</template>

<style scoped>
.status-bar {
  padding: 10px;
  text-align: center;
  font-weight: bold;
  transition: background-color 0.3s;
}
.online {
  background-color: #d4edda;
  color: #155724;
}
.offline {
  background-color: #f8d7da;
  color: #721c24;
}
/* Styles pour le bouton de test */
.test-container {
  padding: 20px;
  display: flex;
  justify-content: center;
}
.print-btn {
  padding: 15px 30px;
  font-size: 18px;
  font-weight: bold;
  border-radius: 12px;
  border: none;
  background-color: #4f46e5;
  color: white;
  box-shadow: 0 4px 6px rgba(0,0,0,0.1);
  transition: all 0.2s;
  cursor: pointer;
}
.print-btn:active {
  transform: scale(0.95);
}
.print-btn:disabled {
  background-color: #9ca3af;
  cursor: not-allowed;
}
.printing {
  animation: pulse 1s infinite;
}
@keyframes pulse {
  0% { opacity: 1; }
  50% { opacity: 0.7; }
  100% { opacity: 1; }
}
</style>