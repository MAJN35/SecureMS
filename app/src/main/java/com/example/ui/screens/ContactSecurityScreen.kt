package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.example.ui.components.LiquidButton as Button
import com.example.ui.components.LiquidIconButton as IconButton
import com.example.ui.components.LiquidOutlinedButton as OutlinedButton
import com.example.ui.components.LiquidTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.CryptoEngine
import com.example.data.EncryptedStorageRepository
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContactPhone
import com.example.data.VerificationStatus
import com.example.ui.components.AvatarPickerDialog
import com.example.ui.components.ContactAvatar
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassHeaderSurface
import com.example.ui.components.QrCodeCanvas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactSecurityScreen(
    conversationId: Long,
    repository: EncryptedStorageRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val conversationFlow = remember(repository, conversationId) { repository.getConversationFlow(conversationId) }
    val conversation by conversationFlow.collectAsState(initial = null)
    var showImportKeyDialog by remember { mutableStateOf(false) }
    var showRegenerateMyKeyDialog by remember { mutableStateOf(false) }
    var showShareMyKeyDialog by remember { mutableStateOf(false) }
    var showAvatarPickerDialog by remember { mutableStateOf(false) }
    var myFingerprint by remember { mutableStateOf("") }
    var myPublicKeyBase64 by remember { mutableStateOf("") }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        val myPub = repository.getMyPublicKey()
        myPublicKeyBase64 = CryptoEngine.encodePublicKeyToBase64(myPub)
        myFingerprint = CryptoEngine.computeFingerprint(myPub)
    }

    val safetyNumber = conversation?.safetyNumber ?: "هنوز کلیدی تبادل نشده است"
    val theirPubKeyBase64 = conversation?.theirPublicKeyBase64

    val fingerprint = remember(theirPubKeyBase64) {
        if (theirPubKeyBase64 != null) {
            try {
                val pub = CryptoEngine.parsePublicKeyFromBase64(theirPubKeyBase64)
                CryptoEngine.computeFingerprint(pub)
            } catch (e: Exception) {
                "کلید نامعتبر"
            }
        } else {
            "کلید عمومی موجود نیست"
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            GlassHeaderSurface(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = {
                        Text(
                            text = "امنیت و تنظیمات ${conversation?.contactName ?: "مخاطب"}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            lineHeight = 20.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status & Profile Header Glass Card
            val status = conversation?.verificationStatus ?: VerificationStatus.UNVERIFIED
            val isLight = MaterialTheme.colorScheme.surface.red > 0.5f
            val statusColor = when (status) {
                VerificationStatus.VERIFIED -> if (isLight) Color(0xFF059669) else Color(0xFF00E5A3)
                VerificationStatus.IDENTITY_CHANGED -> if (isLight) Color(0xFFDC2626) else Color(0xFFEF476F)
                else -> if (isLight) Color(0xFFD97706) else Color(0xFFFFB703)
            }

            // Profile Picture & Identity Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                cornerRadius = 18.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ContactAvatar(
                        name = conversation?.contactName ?: "؟",
                        avatarUri = conversation?.avatarUri,
                        size = 78.dp,
                        accentColor = statusColor,
                        isGroup = conversation?.isGroup == true,
                        isChannel = conversation?.isChannel == true,
                        showCameraBadge = true,
                        onClick = { showAvatarPickerDialog = true },
                        modifier = Modifier.testTag("contact_security_avatar")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = conversation?.contactName ?: "مخاطب",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (conversation?.isDirect == true) {
                        Text(
                            text = conversation?.phoneNumber ?: "",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showAvatarPickerDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("change_profile_pic_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("انتخاب عکس پروفایل", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        if (conversation?.isDirect == true) {
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val synced = repository.syncContactPhotoForConversation(context, conversationId)
                                        withContext(Dispatchers.Main) {
                                            if (synced) {
                                                Toast.makeText(context, "تصویر پروفایل از مخاطبین گوشی دریافت شد", Toast.LENGTH_SHORT).show()
                                            } else {
                                                showAvatarPickerDialog = true
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("get_pic_from_contacts_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.ContactPhone, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("دریافت از مخاطبین", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                cornerRadius = 18.dp,
                tintColor = statusColor.copy(alpha = 0.22f),
                borderTint = statusColor.copy(alpha = 0.65f),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (status) {
                            VerificationStatus.VERIFIED -> Icons.Default.VerifiedUser
                            VerificationStatus.IDENTITY_CHANGED -> Icons.Default.Warning
                            else -> Icons.Default.Security
                        },
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = status.label,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            lineHeight = 19.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = statusColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = when (status) {
                                VerificationStatus.VERIFIED -> "کلید این مخاطب توسط شما تأیید شده است."
                                VerificationStatus.IDENTITY_CHANGED -> "کلید مخاطب تغییر کرده! احتمال حمله مرد میانی وجود دارد."
                                else -> "کد امنیتی زیر را به صورت حضوری یا تماس صوتی با مخاطب مقایسه کنید."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Per-Contact Seen Receipt Toggle Card (Default OFF)
            val seenReceiptsEnabled = conversation?.sendReadReceipts == true
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
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = null,
                            tint = if (seenReceiptsEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ارسال رسید «دیده شد» (Seen)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (seenReceiptsEnabled)
                                    "فعال: هنگام باز کردن پیام‌ها، وضعیت «دیده شد» برای این مخاطب ارسال می‌شود."
                                else
                                    "غیرفعال (پیش‌فرض): این مخاطب متوجه زمان خواندن پیام‌ها نمی‌شود.",
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = seenReceiptsEnabled,
                        onCheckedChange = { enabled ->
                            coroutineScope.launch {
                                repository.updateSendReadReceipts(conversationId, enabled)
                                Toast.makeText(
                                    context,
                                    if (enabled) "رسید «دیده شد» برای این مخاطب فعال شد"
                                    else "رسید «دیده شد» برای این مخاطب غیرفعال شد",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Safety Number Display
            Text(
                text = "کد امنیتی ۶۰ رقمی (Safety Number)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                cornerRadius = 18.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = safetyNumber,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        lineHeight = 28.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(safetyNumber))
                            Toast.makeText(context, "کد امنیتی کپی شد", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("کپی کد امنیتی", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Visual Matrix QR Pattern
            Text(
                text = "ماتریس بصری احراز هویت",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))

            QrCodeCanvas(
                contentString = safetyNumber,
                modifier = Modifier.size(184.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Public Key Fingerprint
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                cornerRadius = 18.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "اثر انگشت کلید عمومی (SHA-256 Fingerprint):",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = fingerprint,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Verification Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            repository.updateVerificationStatus(conversationId, VerificationStatus.VERIFIED)
                            Toast.makeText(context, "مخاطب تأیید شد", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("verify_contact_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تأیید هویت", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            repository.updateVerificationStatus(conversationId, VerificationStatus.UNVERIFIED)
                            Toast.makeText(context, "وضعیت به تأیید نشده تغییر یافت", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("عدم تأیید")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Manual Key Input Button
            OutlinedButton(
                onClick = { showImportKeyDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("به‌روزرسانی یا وارد کردن کلید عمومی مخاطب")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // My Own Key Section (Regenerate & Copy)
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                cornerRadius = 18.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("کلید اختصاصی من", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "اثر انگشت: ${if (myFingerprint.isNotBlank()) myFingerprint else "در حال بارگذاری..."}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(myPublicKeyBase64))
                                Toast.makeText(context, "کلید عمومی شما کپی شد", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("کپی کلید من", fontSize = 11.sp)
                        }

                        Button(
                            onClick = { showRegenerateMyKeyDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تولید کلید جدید من", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { showShareMyKeyDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("اشتراک‌گذاری کلید با QR، تلگرام یا بلوتوث", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showRegenerateMyKeyDialog) {
        AlertDialog(
            onDismissRequest = { showRegenerateMyKeyDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تولید مجدد کلید امنیتی", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            },
            text = {
                Text(
                    text = "با تولید کلید جدید، شناسه امنیتی شما تغییر می‌کند و باید کلید جدید را برای مخاطبین بفرستید. آیا ادامه می‌دهید؟",
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRegenerateMyKeyDialog = false
                        coroutineScope.launch {
                            val newKp = repository.regenerateIdentityKeyPair()
                            myPublicKeyBase64 = CryptoEngine.encodePublicKeyToBase64(newKp.public)
                            myFingerprint = CryptoEngine.computeFingerprint(newKp.public)
                            clipboardManager.setText(AnnotatedString(myPublicKeyBase64))
                            Toast.makeText(context, "کلید جدید شما تولید و در حافظه کپی شد", Toast.LENGTH_LONG).show()
                        }
                    }
                ) {
                    Text("تولید کلید")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRegenerateMyKeyDialog = false }) {
                    Text("لغو")
                }
            }
        )
    }

    if (showImportKeyDialog) {
        var keyText by remember { mutableStateOf("") }
        var errorMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showImportKeyDialog = false },
            title = { Text("وارد کردن کلید عمومی مخاطب") },
            text = {
                Column {
                    Text(
                        text = "کلید عمومی دریافتی از مخاطب (Base64) را در کادر زیر جای‌گذاری کنید:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = keyText,
                        onValueChange = { keyText = it },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4,
                        placeholder = { Text("MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE...") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            val clip = clipboardManager.getText()?.text
                            if (!clip.isNullOrBlank()) {
                                keyText = com.example.util.KeySharingHelper.extractBase64Key(clip)
                                Toast.makeText(context, "کلید از کلیپ‌بورد جای‌گذاری شد", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "حافظه موقت خالی است", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("جای‌گذاری از کلیپ‌بورد (Paste)", fontSize = 12.sp)
                    }
                    if (errorMsg != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(errorMsg ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            val cleanKey = com.example.util.KeySharingHelper.extractBase64Key(keyText)
                            val parsedKey = CryptoEngine.parsePublicKeyFromBase64(cleanKey)
                            coroutineScope.launch {
                                val conv = repository.getConversationById(conversationId) ?: return@launch
                                repository.createOrUpdateConversation(
                                    phoneNumber = conv.phoneNumber,
                                    contactName = conv.contactName,
                                    theirPublicKey = parsedKey
                                )
                                showImportKeyDialog = false
                            }
                        } catch (e: Exception) {
                            errorMsg = "کلید نامعتبر است: ${e.message}"
                        }
                    }
                ) {
                    Text("ذخیره کلید")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportKeyDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    if (showShareMyKeyDialog) {
        com.example.ui.components.KeyShareDialog(
            publicKeyBase64 = myPublicKeyBase64,
            fingerprint = myFingerprint,
            onDismiss = { showShareMyKeyDialog = false }
        )
    }

    if (showAvatarPickerDialog) {
        AvatarPickerDialog(
            contactName = conversation?.contactName ?: "مخاطب",
            phoneNumber = conversation?.phoneNumber ?: "",
            currentAvatarUri = conversation?.avatarUri,
            isGroup = conversation?.isGroup == true,
            isChannel = conversation?.isChannel == true,
            onAvatarSelected = { newUri ->
                coroutineScope.launch(Dispatchers.IO) {
                    repository.updateConversationAvatar(conversationId, newUri)
                }
            },
            onDismiss = { showAvatarPickerDialog = false }
        )
    }
}
