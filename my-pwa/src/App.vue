<script setup>
import { ref, onMounted } from 'vue'
import { Network } from '@capacitor/network'
import HelloWorld from './components/HelloWorld.vue'
import { StatusBar } from '@capacitor/status-bar';
import { Capacitor } from '@capacitor/core';

// 1. On crée une variable réactive pour l'interface
const isOnline = ref(true)
const connectionType = ref('unknown')

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
</script>

<template>
  <div :class="['status-bar', isOnline ? 'online' : 'offline']">
    {{ isOnline ? '🟢 Connecté (' + connectionType + ')' : '🔴 Hors-ligne (Mode SQLite activé)' }}
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
</style>