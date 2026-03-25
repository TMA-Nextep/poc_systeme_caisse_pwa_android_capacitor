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
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    @PluginMethod
    fun discoverPrinters(call: PluginCall) {
        
        val port = 9100
        
        // 1. On cherche l'IP actuelle (Ethernet ou Wi-Fi)
        var activeIp = ""
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                val addrs = iface.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    // On veut une IPv4 qui n'est pas le "localhost" (127.0.0.1)
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        activeIp = addr.hostAddress ?: ""
                    }
                }
            }
        } catch (e: Exception) { }

        // 2. Si on a rien trouvé avec la méthode robuste, on tente le fallback Wi-Fi classique
        if (activeIp.isEmpty() || activeIp == "0.0.0.0") {
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
            val ip = wifiManager.connectionInfo.ipAddress
            if (ip != 0) {
                activeIp = String.format("%d.%d.%d.%d", (ip and 0xff), (ip shr 8 and 0xff), (ip shr 16 and 0xff), (ip shr 24 and 0xff))
            }
        }

        // 3. On génère le préfixe final (ex: "192.168.1.")
        val prefix = if (activeIp.isNotEmpty() && activeIp.contains(".")) {
            activeIp.substring(0, activeIp.lastIndexOf('.') + 1)
        } else {
            "192.168.1." // Fallback de secours si vraiment rien n'est détecté
        }

        scope.launch {
            val foundDevices = mutableListOf<String>()
            val jobs = mutableListOf<Job>()

            for (i in 1..254) {
                val testIp = prefix + i
                jobs += launch(Dispatchers.IO) {
                    // Envoyer l'info à Vue que l'on scanne cette IP
                    val progress = JSObject()
                    progress.put("scanning", testIp)
                    notifyListeners("scanProgress", progress)

                    try {
                        val socket = java.net.Socket()
                        socket.connect(java.net.InetSocketAddress(testIp, port), 400)
                        socket.close()
                        
                        synchronized(foundDevices) { foundDevices.add(testIp) }
                        
                        // Envoyer l'info qu'une imprimante a été trouvée
                        val found = JSObject()
                        found.put("ip", testIp)
                        notifyListeners("printerFound", found)
                    } catch (e: Exception) { }
                }
            }
            
            jobs.joinAll()

            withContext(Dispatchers.Main) {
                val ret = JSObject()
                ret.put("printers", com.getcapacitor.JSArray(foundDevices))
                call.resolve(ret)
            }
        }
    }
}