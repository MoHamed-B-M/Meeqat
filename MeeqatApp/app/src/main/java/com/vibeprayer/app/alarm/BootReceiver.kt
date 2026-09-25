package com.vibeprayer.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Slice 2 reschedules exact alarms from cached prayer times.
        // Slice 1 keeps this as a no-op receiver so manifest stays valid.
    }
}
