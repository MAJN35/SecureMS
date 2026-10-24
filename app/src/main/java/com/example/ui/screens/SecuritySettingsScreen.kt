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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.CameraAlt
import com.example.crypto.CryptoEngine
import com.example.data.EncryptedStorageRepository
import com.example.ui.components.AvatarPickerDialog
import com.example.ui.components.ContactAvatar
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassHeaderSurface
import com.example.ui.components.QrCodeCanvas
import com.example.ui.components.ultraGlassSurface
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(
    repository: EncryptedStorageRepository,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onNavigateBack: () -> Unit,
    showTopBar: Boolean = true
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val prefs = remember {
        context.getSharedPreferences("securesms_prefs", android.content.Context.MODE_PRIVATE)
    }
    var myPublicKeyBase64 by remember { mutableStateOf("") }
    var myFingerprint by remember { mutableStateOf("") }
    var appLockEnabled by remember { mutableStateOf(false) }
    var hideNotificationsEnabled by remember { mutableStateOf(true) }
    var defaultSendReadReceipts by remember {
        mutableStateOf(prefs.getBoolean("default_send_read_receipts", false))
    }
    var myAvatarUri by remember {
        mutableStateOf(prefs.getString("my_avatar_uri", "") ?: "")
    }
    var showMyAvatarPicker by remember { mutableStateOf(false) }
    var showWipeConfirmDialog by remember { mutableStateOf(false) }
    var showRegenerateKeyDialog by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var showTweetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val pub = repository.getMyPublicKey()
        myPublicKeyBase64 = CryptoEngine.encodePublicKeyToBase64(pub)
        myFingerprint = CryptoEngine.computeFingerprint(pub)
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            if (showTopBar) {
                GlassHeaderSurface(modifier = Modifier.fillMaxWidth()) {
                    TopAppBar(
                        title = { Text("تنظیمات", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                }
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
            // My Profile Picture Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                cornerRadius = 18.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        ContactAvatar(
                            name = "من",
                            avatarUri = myAvatarUri,
                            size = 56.dp,
                            showCameraBadge = true,
                            onClick = { showMyAvatarPicker = true }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "پروفایل امن من",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "انتخاب عکس از گالری، مخاطبین یا نمادهای امنیتی",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = { showMyAvatarPicker = true },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تغییر عکس", fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Theme Section
            Text(
                text = "پوسته ظاهری",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isDarkTheme) "حالت تیره" else "حالت روشن",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isDarkTheme) "پوسته شیشه‌ای تیره (Obsidian Glass)" else "پوسته شیشه‌ای روشن (Crystal Ice)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = { onToggleTheme() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Identity Section
            Text(
                text = "شناسه امنیتی من",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                cornerRadius = 18.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("کلید عمومی (ذخیره امن در Keystore)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "اثر انگشت کلید:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = myFingerprint,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(myPublicKeyBase64))
                                Toast.makeText(context, "کلید عمومی کپی شد", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("کپی کلید", fontSize = 11.sp)
                        }

                        Button(
                            onClick = { showQrDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("اشتراک و QR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { showRegenerateKeyDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تولید کلید جدید", fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Privacy & Protection
            Text(
                text = "حریم خصوصی",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                cornerRadius = 18.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("ارسال رسید «دیده شد» (Seen)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                "ارسال وضعیت خوانده شدن پیام‌ها (پیش‌فرض: غیرفعال، قابل تنظیم برای هر مخاطب در گفتگو).",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = defaultSendReadReceipts,
                            onCheckedChange = { enabled ->
                                defaultSendReadReceipts = enabled
                                prefs.edit().putBoolean("default_send_read_receipts", enabled).apply()
                                coroutineScope.launch {
                                    repository.updateAllConversationsReadReceipts(enabled)
                                    Toast.makeText(
                                        context,
                                        if (enabled) "ارسال رسید «دیده شد» برای همه مخاطبین فعال شد"
                                        else "ارسال رسید «دیده شد» برای همه مخاطبین غیرفعال شد",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("مخفی کردن متن در اعلان", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                "متن پیام در نوار اعلان نمایش داده نمی‌شود.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = hideNotificationsEnabled,
                            onCheckedChange = { hideNotificationsEnabled = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("قفل برنامه با پین / اثر انگشت", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                "درخواست احراز هویت هنگام ورود به برنامه.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = appLockEnabled,
                            onCheckedChange = { appLockEnabled = it }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Encrypted Backup
            Text(
                text = "پشتیبان‌گیری",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                cornerRadius = 18.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "پشتیبان‌گیری رمزگذاری‌شده به صورت فایل آفلاین روی حافظه دستگاه ذخیره می‌شود.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            Toast.makeText(context, "نسخه پشتیبان محلی ایجاد شد", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ایجاد نسخه پشتیبان", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Free Speech & Community
            Text(
                text = "جامعه و آزادی ارتباطات",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                cornerRadius = 18.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .ultraGlassSurface(
                                    shape = CircleShape,
                                    cornerRadius = 18.dp,
                                    tintColor = Color(0xFF1DA1F2).copy(alpha = 0.25f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF1DA1F2), modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("نشر صدای آزادی در توییتر / X", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("متن‌های آماده درباره حق ارتباط و معرفی اپ برای جوانان", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { showTweetDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DA1F2)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("settings_tweet_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مشاهده و ارسال توییت", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Danger Zone
            Text(
                text = "حذف اطلاعات",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFFEF476F)
            )
            Spacer(modifier = Modifier.height(8.dp))

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                cornerRadius = 18.dp,
                tintColor = Color(0xFFEF476F).copy(alpha = 0.22f),
                borderTint = Color(0xFFEF476F).copy(alpha = 0.65f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "پاکسازی کامل پیام‌ها و کلیدها",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF476F),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "تمامی پیام‌ها، گفتگوها و کلیدهای محلی پاک خواهند شد.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { showWipeConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEF476F),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("wipe_data_button")
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حذف تمام اطلاعات", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showWipeConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showWipeConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF476F))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تأیید حذف اطلاعات", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text("آیا مطمئن هستید؟ تمامی مکالمات و کلیدها پاک خواهند شد.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showWipeConfirmDialog = false
                        coroutineScope.launch {
                            repository.wipeAllData()
                            Toast.makeText(context, "اطلاعات پاکسازی شدند", Toast.LENGTH_LONG).show()
                            onNavigateBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF476F))
                ) {
                    Text("بله، پاک شود", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirmDialog = false }) {
                    Text("لغو")
                }
            }
        )
    }

    if (showRegenerateKeyDialog) {
        AlertDialog(
            onDismissRequest = { showRegenerateKeyDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تولید کلید امنیتی جدید", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text("یک کلید امنیتی جدید تولید می‌شود. برای ادامه ارتباط امن، باید کلید جدید را با مخاطبین خود به اشتراک بگذارید (با QR یا کپی دستی).")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRegenerateKeyDialog = false
                        coroutineScope.launch {
                            val newKp = repository.regenerateIdentityKeyPair()
                            myPublicKeyBase64 = CryptoEngine.encodePublicKeyToBase64(newKp.public)
                            myFingerprint = CryptoEngine.computeFingerprint(newKp.public)
                            Toast.makeText(context, "کلید امنیتی جدید تولید شد", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("تولید کلید", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRegenerateKeyDialog = false }) {
                    Text("لغو")
                }
            }
        )
    }

    if (showQrDialog) {
        com.example.ui.components.KeyShareDialog(
            publicKeyBase64 = myPublicKeyBase64,
            fingerprint = myFingerprint,
            onDismiss = { showQrDialog = false }
        )
    }

    if (showMyAvatarPicker) {
        AvatarPickerDialog(
            contactName = "پروفایل من",
            phoneNumber = "",
            currentAvatarUri = myAvatarUri,
            onAvatarSelected = { newUri ->
                myAvatarUri = newUri
                prefs.edit().putString("my_avatar_uri", newUri).apply()
            },
            onDismiss = { showMyAvatarPicker = false }
        )
    }

    if (showTweetDialog) {
        TweetShareDialog(
            onDismiss = { showTweetDialog = false }
        )
    }
}
