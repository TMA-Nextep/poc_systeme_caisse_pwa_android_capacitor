package com.example.mypwa
import android.content.Context

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin

import kotlinx.coroutines.*
import java.net.Socket
import java.io.OutputStream

@CapacitorPlugin(name = "HardwarePlugin")
class HardwarePlugin : Plugin() {

    @PluginMethod
    fun simulatePrint(call: PluginCall) {
        val seconds = call.getInt("duration", 1) ?: 1

        // On récupère le service de vibration du téléphone
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        if (vibrator.hasVibrator()) {
            // On fait vibrer pendant que "l'impression" est censée avoir lieu
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        (seconds * 1000).toLong(),
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                vibrator.vibrate((seconds * 1000).toLong())
            }
            call.resolve(JSObject().put("status", "Impression simulée terminée"))
        } else {
            call.reject("Pas de vibreur sur ce device")
        }
    }

    @PluginMethod
    fun realPrint(call: PluginCall) {
        val ip = call.getString("ip") ?: "192.168.1.100" // IP de ton imprimante
        val port = call.getInt("port") ?: 9100           // Port standard ESC/POS
        val text = call.getString("content") ?: ""

        // On lance la connexion dans un thread séparé (obligatoire pour le réseau)
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val socket = Socket(ip, port)
                val outputStream: OutputStream = socket.getOutputStream()

                // Commandes ESC/POS de base (Initialisation + Texte + Coupe papier)
                val initPrinter = byteArrayOf(0x1B, 0x40)
                val cutPaper = byteArrayOf(0x1D, 0x56, 0x41, 0x00)

                outputStream.write(initPrinter)
                outputStream.write(text.toByteArray(Charsets.ISO_8859_1)) // Encodage standard
                outputStream.write("\n\n\n".toByteArray()) // Espace pour la découpe
                outputStream.write(cutPaper)

                outputStream.flush()
                socket.close()

                call.resolve(JSObject().put("status", "Impression réussie"))
            } catch (e: Exception) {
                call.reject("Erreur de connexion à l'imprimante : ${e.message}")
            }
        }
    }
}