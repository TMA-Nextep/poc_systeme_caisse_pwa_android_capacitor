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
                vibrator.vibrate(VibrationEffect.createOneShot((seconds * 1000).toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
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
        // ICI : Tu colleras le code TCP Socket quand tu auras l'imprimante
        call.unimplemented("L'imprimante n'est pas encore connectée au Bridge")
    }
}