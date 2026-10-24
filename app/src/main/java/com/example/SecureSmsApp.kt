package com.example

import android.app.Application
import com.example.crypto.KeystoreHelper
import com.example.data.AppDatabase
import com.example.data.EncryptedStorageRepository
import com.example.protocol.FragmentationEngine
import com.example.sms.SmsProcessor
import com.example.sms.SmsTransportManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SecureSmsApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var keystoreHelper: KeystoreHelper
        private set

    lateinit var repository: EncryptedStorageRepository
        private set

    lateinit var fragmentationEngine: FragmentationEngine
        private set

    lateinit var smsProcessor: SmsProcessor
        private set

    lateinit var smsTransportManager: SmsTransportManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getInstance(this)
        keystoreHelper = KeystoreHelper(this)
        repository = EncryptedStorageRepository(database.secureSmsDao(), keystoreHelper)
        fragmentationEngine = FragmentationEngine()
        smsProcessor = SmsProcessor(this, repository, fragmentationEngine)
        smsTransportManager = SmsTransportManager(this, repository, fragmentationEngine, smsProcessor)
        smsProcessor.transportManager = smsTransportManager

        // Initialize local cryptographic keys in background
        CoroutineScope(Dispatchers.IO).launch {
            repository.getOrCreateIdentityKeyPair()
        }
    }

    companion object {
        lateinit var instance: SecureSmsApp
            private set
    }
}
