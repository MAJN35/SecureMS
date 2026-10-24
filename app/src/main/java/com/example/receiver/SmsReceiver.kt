package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.SecureSmsApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles incoming SMS via android.provider.Telephony.SMS_DELIVER
 * (called when SecureSMS is active default SMS app).
 *
 * Uses goAsync() to guarantee Android OS does not kill the process before
 * background coroutine processing completes.
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val app = context.applicationContext as? SecureSmsApp ?: return

        val fullTextBuilder = StringBuilder()
        var senderPhone = ""

        for (sms in messages) {
            senderPhone = sms.displayOriginatingAddress ?: sms.originatingAddress ?: "Unknown"
            fullTextBuilder.append(sms.displayMessageBody ?: sms.messageBody ?: "")
        }

        val rawText = fullTextBuilder.toString()
        if (rawText.isBlank()) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.smsProcessor.processIncomingSms(senderPhone, rawText)
            } catch (e: Exception) {
                Log.e("SmsReceiver", "Error processing SMS_DELIVER: ${e.message}", e)
            } finally {
                try {
                    pendingResult.finish()
                } catch (ignored: Exception) {}
            }
        }
    }
}
