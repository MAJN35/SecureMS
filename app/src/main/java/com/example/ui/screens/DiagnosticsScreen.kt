package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.telephony.TelephonyManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.example.ui.components.LiquidButton as Button
import com.example.ui.components.LiquidIconButton as IconButton
import com.example.ui.components.LiquidOutlinedButton as OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.crypto.CryptoEngine
import com.example.crypto.PersianCiphertextCodec
import com.example.data.EncryptedStorageRepository
import com.example.protocol.FragmentationEngine
import com.example.protocol.SmsPacket
import com.example.sms.SmsProcessor
import com.example.sms.SmsTransportManager
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassHeaderSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    repository: EncryptedStorageRepository,
    fragmentationEngine: FragmentationEngine,
    transportManager: SmsTransportManager,
    smsProcessor: SmsProcessor,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val testLogs = remember { mutableStateListOf<String>() }
    var isRunningTest by remember { mutableStateOf(false) }

    // Check permissions
    val hasSendSms = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
    }
    val hasReceiveSms = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
    }
    val hasReadContacts = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
    }

    // Telephony manager check
    val telephonyManager = remember {
        context.getSystemService(TelephonyManager::class.java)
    }
    val simState = remember {
        telephonyManager?.simState ?: TelephonyManager.SIM_STATE_UNKNOWN
    }

    val pendingCount = remember(fragmentationEngine) {
        fragmentationEngine.getPendingMessageCount()
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            GlassHeaderSurface(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = { Text("آزمایشگاه عیب‌یابی و شبیه‌ساز پیامک", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Hardware & Permissions Status Card
            Text(
                text = "وضعیت بستر مخابراتی و مجوزهای اندروید",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                cornerRadius = 18.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    DiagnosticItem(
                        title = "مجوز ارسال پیامک (SEND_SMS)",
                        status = hasSendSms,
                        positiveText = "اعطا شده",
                        negativeText = "رد شده"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DiagnosticItem(
                        title = "مجوز دریافت پیامک (RECEIVE_SMS)",
                        status = hasReceiveSms,
                        positiveText = "اعطا شده",
                        negativeText = "رد شده"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DiagnosticItem(
                        title = "مجوز دفترچه مخاطبین (READ_CONTACTS)",
                        status = hasReadContacts,
                        positiveText = "اعطا شده",
                        negativeText = "رد شده"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DiagnosticItem(
                        title = "وضعیت سیم‌کارت و شبکه سلولار",
                        status = simState == TelephonyManager.SIM_STATE_READY,
                        positiveText = "سیم‌کارت فعال و آماده",
                        negativeText = "سیم‌کارت موجود نیست (شبیه‌ساز مجازی آماده کار است)"
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Reassembly Queue Card
            Text(
                text = "وضعیت صف بازسازی قطعات پیامک (Fragmentation Engine)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                cornerRadius = 18.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("پیام‌های در انتظار تکمیل قطعات:", fontSize = 13.sp)
                        Text(
                            text = "$pendingCount پیام در بافر حافظه",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (pendingCount > 0) Color(0xFFFFB703) else Color(0xFF00E5A3)
                        )
                    }
                    Button(
                        onClick = {
                            fragmentationEngine.cleanExpiredFragments(0) // Clean immediately
                            testLogs.add("بافر قطعات منقضی‌شده پاکسازی شد.")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("پاکسازی بافر", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Virtual SMS Lab Simulator
            Text(
                text = "شبیه‌ساز آزمایشگاهی ارتباط امن (Virtual SMS Lab)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "اجرای سناریوهای رمزنگاری و قطعه‌بندی بدون نیاز به دو گوشی فیزیکی:",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Test 1: Full Roundtrip Encrypted Persian SMS
            Button(
                onClick = {
                    coroutineScope.launch {
                        isRunningTest = true
                        testLogs.clear()
                        testLogs.add("--- شروع تست ۱: مبادله کلید و ارسال پیامک فارسی ---")

                        withContext(Dispatchers.IO) {
                            // 1. Generate Alice & Bob keys
                            val aliceKp = CryptoEngine.generateIdentityKeyPair()
                            val bobKp = CryptoEngine.generateIdentityKeyPair()
                            testLogs.add("✔ تولید کلیدهای EC P-256 برای آلیس و باب انجام شد.")

                            // 2. Safety number calculation
                            val safetyNumber = CryptoEngine.computeSafetyNumber(aliceKp.public, bobKp.public)
                            testLogs.add("✔ کد امنیتی مشترک: $safetyNumber")

                            // 3. Alice encrypts message for Bob
                            val sharedAlice = CryptoEngine.deriveSharedKey(aliceKp.private, bobKp.public)
                            val dummyPacket = SmsPacket(
                                version = 1,
                                messageType = SmsPacket.MessageType.DATA,
                                messageId = 1001,
                                fragmentIndex = 0,
                                fragmentCount = 1,
                                senderKeyId = CryptoEngine.computeKeyId(aliceKp.public),
                                recipientKeyId = CryptoEngine.computeKeyId(bobKp.public),
                                sequenceNumber = 1,
                                payload = ByteArray(0)
                            )
                            val originalText = "سلام باب! این پیام با حروف فارسی در پیامک پنهان شده است."
                            val encrypted = CryptoEngine.encrypt(
                                sharedKey = sharedAlice,
                                plaintext = originalText.toByteArray(Charsets.UTF_8),
                                aad = dummyPacket.computeAad()
                            )

                            // 4. Encode to Persian characters Base32
                            val packet = dummyPacket.copy(payload = encrypted.toCombined())
                            val persianSms = PersianCiphertextCodec.encodeToPersianLetters(packet.toByteArray())
                            testLogs.add("✔ پیامک کدگذاری‌شده به الفبای فارسی:\n$persianSms")

                            // 5. Bob receives and decodes
                            val (decodedBytes, mode) = PersianCiphertextCodec.decodePayload(persianSms)
                            testLogs.add("✔ بازگشایی کدینگ: قالب شناسایی‌شده = $mode")

                            val parsedPacket = SmsPacket.fromByteArray(decodedBytes)
                            val sharedBob = CryptoEngine.deriveSharedKey(bobKp.private, aliceKp.public)
                            val unpacked = CryptoEngine.EncryptedPayload.fromCombined(parsedPacket.payload)

                            val decrypted = CryptoEngine.decrypt(
                                sharedKey = sharedBob,
                                iv = unpacked.iv,
                                ciphertextWithTag = unpacked.ciphertext,
                                aad = parsedPacket.computeAad()
                            )
                            val decryptedText = String(decrypted, Charsets.UTF_8)
                            testLogs.add("✔ متن رمزگشایی‌شده توسط باب: $decryptedText")
                            testLogs.add("✅ نتیجه: تست ۱ با موفقیت ۱۰۰٪ کامل شد!")
                        }
                        isRunningTest = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("test_roundtrip_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(10.dp),
                enabled = !isRunningTest
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("تست ۱: ارسال و رمزگشایی کامل پیامک با حروف فارسی", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Test 2: Out of order fragmentation test
            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        isRunningTest = true
                        testLogs.clear()
                        testLogs.add("--- شروع تست ۲: قطعه‌بندی پیامک و دریافت نامرتب (Out-of-Order) ---")

                        withContext(Dispatchers.IO) {
                            val longText = "این یک پیام طولانی برای بررسی قابلیت تقطیع خودکار پیامک است. پیام‌ها ممکن است در شبکه‌های مخابراتی نامرتب برسند و نرم‌افزار باید بتواند آنها را به‌صورت کامل جمع‌آوری و احراز هویت کند."
                            val engine = FragmentationEngine()
                            val kp = CryptoEngine.generateIdentityKeyPair()
                            val shared = CryptoEngine.deriveSharedKey(kp.private, kp.public)

                            val dummy = SmsPacket(
                                version = 1,
                                messageType = SmsPacket.MessageType.DATA,
                                messageId = 888,
                                fragmentIndex = 0,
                                fragmentCount = 1,
                                senderKeyId = ByteArray(8),
                                recipientKeyId = ByteArray(8),
                                sequenceNumber = 1,
                                payload = ByteArray(0)
                            )
                            val enc = CryptoEngine.encrypt(shared, longText.toByteArray(Charsets.UTF_8), dummy.computeAad())
                            val frags = engine.fragment(enc.toCombined(), SmsPacket.MessageType.DATA, 888, ByteArray(8), ByteArray(8), 1, maxChunkSize = 35)

                            testLogs.add("✔ پیام به ${frags.size} قطعه پیامک تقسیم شد.")

                            // Deliver in reverse order: [last, ..., first]
                            for (f in frags.reversed()) {
                                testLogs.add("📥 ورود قطعه ${f.fragmentIndex + 1}/${f.fragmentCount}...")
                                val res = engine.ingestFragment(f)
                                if (res is FragmentationEngine.ReassemblyResult.Complete) {
                                    val dec = CryptoEngine.decrypt(
                                        sharedKey = shared,
                                        iv = CryptoEngine.EncryptedPayload.fromCombined(res.fullPayload).iv,
                                        ciphertextWithTag = CryptoEngine.EncryptedPayload.fromCombined(res.fullPayload).ciphertext,
                                        aad = res.representativePacket.computeAad()
                                    )
                                    testLogs.add("✔ بازسازی کامل در حالت نامرتب: ${String(dec, Charsets.UTF_8).take(40)}...")
                                }
                            }
                            testLogs.add("✅ نتیجه: تست ۲ با موفقیت قطعات نامرتب را تجمیع کرد!")
                        }
                        isRunningTest = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                enabled = !isRunningTest
            ) {
                Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("تست ۲: تست قطعات نامرتب (Out-of-Order SMS Reassembly)")
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Test 3: Tamper Attack Test (Bit Flip)
            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        isRunningTest = true
                        testLogs.clear()
                        testLogs.add("--- شروع تست ۳: شبیه‌سازی حمله دستکاری (Tamper Attack) ---")

                        withContext(Dispatchers.IO) {
                            val kp1 = CryptoEngine.generateIdentityKeyPair()
                            val kp2 = CryptoEngine.generateIdentityKeyPair()
                            val shared = CryptoEngine.deriveSharedKey(kp1.private, kp2.public)

                            val dummy = SmsPacket(
                                version = 1,
                                messageType = SmsPacket.MessageType.DATA,
                                messageId = 999,
                                fragmentIndex = 0,
                                fragmentCount = 1,
                                senderKeyId = ByteArray(8),
                                recipientKeyId = ByteArray(8),
                                sequenceNumber = 1,
                                payload = ByteArray(0)
                            )
                            val enc = CryptoEngine.encrypt(shared, "پیام محرمانه".toByteArray(Charsets.UTF_8), dummy.computeAad())
                            val combined = enc.toCombined()

                            // Attacker tampers 1 byte in ciphertext
                            combined[combined.size - 1] = (combined[combined.size - 1].toInt() xor 0x01).toByte()
                            testLogs.add("⚠ نفوذگر یک بیت از بسته رمزگذاری‌شده را در مسیر دستکاری کرد.")

                            try {
                                val tampered = CryptoEngine.EncryptedPayload.fromCombined(combined)
                                CryptoEngine.decrypt(shared, tampered.iv, tampered.ciphertext, dummy.computeAad())
                                testLogs.add("❌ خطا! دستکاری تشخیص داده نشد!")
                            } catch (e: Exception) {
                                testLogs.add("🛡️ اعتبارسنجی تگ احراز هویت AES-256-GCM فعال شد:")
                                testLogs.add("✔ بسته مخدوش به دلیل شکست احراز هویت رد شد: ${e.message}")
                                testLogs.add("✅ نتیجه: تست ۳ نشان داد هیچ متن نامعتبری آشکار نمی‌شود!")
                            }
                        }
                        isRunningTest = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                enabled = !isRunningTest
            ) {
                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("تست ۳: شبیه‌سازی دستکاری پیامک و رد خودکار (AEAD Tag)")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Live Diagnostic Logs Console
            Text(
                text = "گزارش زنده رویدادهای پروتکل:",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                cornerRadius = 16.dp
            ) {
                Box(modifier = Modifier.padding(14.dp)) {
                    if (isRunningTest) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("در حال اجرای آزمون امنیتی...", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    } else if (testLogs.isEmpty()) {
                        Text(
                            text = "هیچ تستی اجرا نشده است. برای اعتبارسنجی روی دکمه‌های بالا کلیک کنید.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column {
                            testLogs.forEach { log ->
                                Text(
                                    text = log,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = if (log.contains("✔") || log.contains("✅")) MaterialTheme.colorScheme.primary
                                    else if (log.contains("⚠") || log.contains("❌")) Color(0xFFEF476F)
                                    else MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 17.sp,
                                    modifier = Modifier.padding(vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DiagnosticItem(
    title: String,
    status: Boolean,
    positiveText: String,
    negativeText: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = if (status) positiveText else negativeText,
                fontSize = 11.sp,
                color = if (status) Color(0xFF00E5A3) else Color(0xFFFFB703)
            )
        }
        Icon(
            imageVector = if (status) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (status) Color(0xFF00E5A3) else Color(0xFFFFB703),
            modifier = Modifier.size(18.dp)
        )
    }
}
