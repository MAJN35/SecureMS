package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Required component for Default SMS App compliance on Android 4.4+ (KitKat+).
 * SecureSMS explicitly refuses unencrypted MMS data for privacy preservation.
 */
class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // MMS transport is not supported by SecureSMS's zero-knowledge SMS protocol
    }
}
