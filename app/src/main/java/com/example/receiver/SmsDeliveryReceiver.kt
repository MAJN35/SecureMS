package com.example.receiver

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.SecureSmsApp
import com.example.data.DeliveryStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsDeliveryReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? SecureSmsApp ?: return
        val payloadId = intent.getIntExtra("payloadId", 0)
        if (payloadId == 0) return

        val action = intent.action
        val resultCode = resultCode

        CoroutineScope(Dispatchers.IO).launch {
            when (action) {
                "com.example.securesms.SMS_SENT" -> {
                    if (resultCode == Activity.RESULT_OK) {
                        app.repository.updateStatusByPayloadId(payloadId, DeliveryStatus.SENT_TO_CARRIER)
                    } else {
                        app.repository.updateStatusByPayloadId(payloadId, DeliveryStatus.FAILED)
                    }
                }
                "com.example.securesms.SMS_DELIVERED" -> {
                    if (resultCode == Activity.RESULT_OK) {
                        app.repository.updateStatusByPayloadId(payloadId, DeliveryStatus.DELIVERED_CARRIER)
                    }
                }
            }
        }
    }
}
