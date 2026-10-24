package com.example.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Required headless service for Default SMS App quick-reply support.
 */
class HeadlessSmsSendService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
