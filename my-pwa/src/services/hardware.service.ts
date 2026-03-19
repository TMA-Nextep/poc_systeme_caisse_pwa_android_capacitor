import { registerPlugin } from '@capacitor/core';

// On définit l'interface pour avoir l'autocomplétion (Pro)
interface HardwarePlugin {
  simulatePrint(options: { duration: number }): Promise<{ status: string }>;
  realPrint(options: { ip: string, message: string }): Promise<void>;
}

const Hardware = registerPlugin<HardwarePlugin>('HardwarePlugin');

export const printTicket = async (message: string) => {
  console.log("Préparation du ticket :", message);
  
  // En mode POC (pas d'imprimante) :
  try {
    await Hardware.simulatePrint({ duration: 2 });
    console.log("Le téléphone a vibré : simulation réussie !");
  } catch (e) {
    console.error("Erreur hardware :", e);
  }
};