package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.components.LiquidButton as Button
import com.example.ui.components.LiquidOutlinedButton as OutlinedButton
import com.example.ui.components.LiquidTextButton as TextButton
import com.example.util.ContactHelper
import com.example.util.DeviceContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

enum class AvatarPreset(
    val uriKey: String,
    val labelFa: String,
    val icon: ImageVector,
    val startColor: Color,
    val endColor: Color
) {
    EMERALD_SHIELD(
        uriKey = "preset:emerald_shield",
        labelFa = "سپر زمردی",
        icon = Icons.Default.Shield,
        startColor = Color(0xFF059669),
        endColor = Color(0xFF10B981)
    ),
    SAPPHIRE_LOCK(
        uriKey = "preset:sapphire_lock",
        labelFa = "قفل یاقوتی",
        icon = Icons.Default.Lock,
        startColor = Color(0xFF1D4ED8),
        endColor = Color(0xFF38BDF8)
    ),
    ROYAL_CROWN(
        uriKey = "preset:royal_crown",
        labelFa = "نشان ویژه",
        icon = Icons.Default.WorkspacePremium,
        startColor = Color(0xFF6D28D9),
        endColor = Color(0xFFA855F7)
    ),
    CYBER_RADAR(
        uriKey = "preset:cyber_radar",
        labelFa = "رادار سایبری",
        icon = Icons.Default.Radar,
        startColor = Color(0xFF0F766E),
        endColor = Color(0xFF2DD4BF)
    ),
    VIOLET_DIAMOND(
        uriKey = "preset:violet_diamond",
        labelFa = "الماس امن",
        icon = Icons.Default.Diamond,
        startColor = Color(0xFF4338CA),
        endColor = Color(0xFF818CF8)
    ),
    AMBER_BOLT(
        uriKey = "preset:amber_bolt",
        labelFa = "صاعقه طلایی",
        icon = Icons.Default.Bolt,
        startColor = Color(0xFFB45309),
        endColor = Color(0xFFF59E0B)
    ),
    CRIMSON_STAR(
        uriKey = "preset:crimson_star",
        labelFa = "ستاره سرخ",
        icon = Icons.Default.Star,
        startColor = Color(0xFFBE123C),
        endColor = Color(0xFFFB7185)
    ),
    TEAL_FINGERPRINT(
        uriKey = "preset:teal_fingerprint",
        labelFa = "اثر انگشت",
        icon = Icons.Default.Fingerprint,
        startColor = Color(0xFF0369A1),
        endColor = Color(0xFF06B6D4)
    );

    companion object {
        fun fromUri(uri: String?): AvatarPreset? {
            if (uri.isNullOrBlank() || !uri.startsWith("preset:")) return null
            return entries.firstOrNull { it.uriKey == uri }
        }
    }
}

@Composable
fun ContactAvatar(
    name: String,
    avatarUri: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    isGroup: Boolean = false,
    isChannel: Boolean = false,
    fontSize: TextUnit = (size.value * 0.38f).sp,
    showCameraBadge: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val preset = remember(avatarUri) { AvatarPreset.fromUri(avatarUri) }
    var loadedBitmap by remember(avatarUri) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(avatarUri) {
        if (!avatarUri.isNullOrBlank() && !avatarUri.startsWith("preset:")) {
            val bmp = withContext(Dispatchers.IO) {
                ContactHelper.loadAvatarBitmap(context, avatarUri)
            }
            loadedBitmap = bmp?.asImageBitmap()
        } else {
            loadedBitmap = null
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .let { base ->
                if (onClick != null) {
                    base
                        .clip(CircleShape)
                        .clickable { onClick() }
                } else {
                    base
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val currentBitmap = loadedBitmap
        when {
            currentBitmap != null -> {
                Image(
                    bitmap = currentBitmap,
                    contentDescription = "تصویر پروفایل $name",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .border(
                            width = 1.2.dp,
                            color = accentColor.copy(alpha = 0.55f),
                            shape = CircleShape
                        )
                )
            }
            preset != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(preset.startColor, preset.endColor)
                            )
                        )
                        .border(
                            width = 1.2.dp,
                            color = Color.White.copy(alpha = 0.42f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = preset.icon,
                        contentDescription = preset.labelFa,
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.52f)
                    )
                }
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f))
                        .border(
                            width = 1.dp,
                            color = accentColor.copy(alpha = 0.40f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isGroup -> Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = "گروه",
                            tint = accentColor,
                            modifier = Modifier.size(size * 0.5f)
                        )
                        isChannel -> Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = "کانال",
                            tint = accentColor,
                            modifier = Modifier.size(size * 0.48f)
                        )
                        else -> Text(
                            text = name.trim().take(1).ifBlank { "؟" }.uppercase(Locale.getDefault()),
                            fontWeight = FontWeight.Bold,
                            fontSize = fontSize,
                            color = accentColor
                        )
                    }
                }
            }
        }

        if (showCameraBadge) {
            Box(
                modifier = Modifier
                    .size((size.value * 0.34f).coerceIn(18f, 28f).dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "تغییر تصویر پروفایل",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size((size.value * 0.20f).coerceIn(10f, 16f).dp)
                )
            }
        }
    }
}

@Composable
fun AvatarPickerDialog(
    contactName: String,
    phoneNumber: String,
    currentAvatarUri: String?,
    isGroup: Boolean = false,
    isChannel: Boolean = false,
    onAvatarSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var previewUri by remember(currentAvatarUri) { mutableStateOf(currentAvatarUri ?: "") }
    var showContactsListPicker by remember { mutableStateOf(false) }
    var deviceContacts by remember { mutableStateOf<List<DeviceContact>>(emptyList()) }

    var hasContactPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Zero-permission Android Photo Picker (Play Policy compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                val savedUri = ContactHelper.copyUriToInternalAvatarStorage(
                    context = context,
                    sourceUri = uri,
                    identifier = phoneNumber.ifBlank { contactName }
                )
                withContext(Dispatchers.Main) {
                    if (!savedUri.isNullOrBlank()) {
                        previewUri = savedUri
                        onAvatarSelected(savedUri)
                        Toast.makeText(context, "تصویر پروفایل با موفقیت ذخیره شد", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    } else {
                        Toast.makeText(context, "خطا در بارگذاری تصویر انتخابی", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val contactPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasContactPermission = granted
        if (granted) {
            coroutineScope.launch(Dispatchers.IO) {
                val matchedPhoto = ContactHelper.getContactPhotoUriByPhone(context, phoneNumber)
                val allContacts = ContactHelper.getDeviceContacts(context)
                withContext(Dispatchers.Main) {
                    deviceContacts = allContacts
                    if (!matchedPhoto.isNullOrBlank()) {
                        val localCopy = withContext(Dispatchers.IO) {
                            ContactHelper.copyUriToInternalAvatarStorage(
                                context = context,
                                sourceUri = android.net.Uri.parse(matchedPhoto),
                                identifier = phoneNumber
                            )
                        } ?: matchedPhoto
                        previewUri = localCopy
                        onAvatarSelected(localCopy)
                        Toast.makeText(context, "تصویر پروفایل از مخاطبین گوشی دریافت شد", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    } else {
                        showContactsListPicker = true
                        Toast.makeText(
                            context,
                            "برای این شماره عکس مستقیمی در مخاطبین ثبت نشده؛ می‌توانید از فهرست مخاطبین انتخاب کنید",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        } else {
            Toast.makeText(context, "دسترسی به مخاطبین داده نشد", Toast.LENGTH_SHORT).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "تصویر پروفایل $contactName",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Live Avatar Preview Header
                ContactAvatar(
                    name = contactName,
                    avatarUri = previewUri,
                    size = 82.dp,
                    isGroup = isGroup,
                    isChannel = isChannel
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = contactName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!isGroup && !isChannel && phoneNumber.isNotBlank()) {
                    Text(
                        text = phoneNumber,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 1. Choose from Gallery Button (Android Photo Picker)
                Button(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pick_avatar_gallery_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("انتخاب تصویر از گالری گوشی", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 2. Get Photo from Device Contacts Button
                OutlinedButton(
                    onClick = {
                        if (!hasContactPermission) {
                            contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                        } else {
                            coroutineScope.launch(Dispatchers.IO) {
                                val matchedPhoto = if (!isGroup && !isChannel) {
                                    ContactHelper.getContactPhotoUriByPhone(context, phoneNumber)
                                } else null
                                val allContacts = ContactHelper.getDeviceContacts(context)
                                withContext(Dispatchers.Main) {
                                    deviceContacts = allContacts
                                    if (!matchedPhoto.isNullOrBlank()) {
                                        val localCopy = withContext(Dispatchers.IO) {
                                            ContactHelper.copyUriToInternalAvatarStorage(
                                                context = context,
                                                sourceUri = android.net.Uri.parse(matchedPhoto),
                                                identifier = phoneNumber
                                            )
                                        } ?: matchedPhoto
                                        previewUri = localCopy
                                        onAvatarSelected(localCopy)
                                        Toast.makeText(
                                            context,
                                            "تصویر پروفایل از مخاطبین گوشی همگام‌سازی شد",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        onDismiss()
                                    } else {
                                        showContactsListPicker = !showContactsListPicker
                                        if (allContacts.none { !it.photoUri.isNullOrBlank() }) {
                                            Toast.makeText(
                                                context,
                                                "هیچ عکسی در مخاطبین گوشی یافت نشد؛ می‌توانید از گالری یا نمادهای زیر انتخاب کنید",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sync_avatar_contacts_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContactPhone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "دریافت تصویر از مخاطبین گوشی",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Expandable list of contacts if the user wants to pick a contact's photo from address book
                AnimatedVisibility(visible = showContactsListPicker && deviceContacts.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Text(
                            text = "انتخاب از مخاطبین گوشی (${deviceContacts.size} مخاطب):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 150.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            deviceContacts.forEach { c ->
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    cornerRadius = 12.dp,
                                    onClick = {
                                        if (!c.photoUri.isNullOrBlank()) {
                                            coroutineScope.launch(Dispatchers.IO) {
                                                val copied = ContactHelper.copyUriToInternalAvatarStorage(
                                                    context = context,
                                                    sourceUri = android.net.Uri.parse(c.photoUri),
                                                    identifier = c.phoneNumber
                                                ) ?: c.photoUri
                                                withContext(Dispatchers.Main) {
                                                    previewUri = copied
                                                    onAvatarSelected(copied)
                                                    Toast.makeText(
                                                        context,
                                                        "تصویر مخاطب «${c.name}» اعمال شد",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    onDismiss()
                                                }
                                            }
                                        } else {
                                            // Assign a deterministic preset if that contact has no photo in Android Contacts
                                            val fallbackPreset = AvatarPreset.entries[
                                                Math.abs(c.name.hashCode()) % AvatarPreset.entries.size
                                            ]
                                            previewUri = fallbackPreset.uriKey
                                            onAvatarSelected(fallbackPreset.uriKey)
                                            Toast.makeText(
                                                context,
                                                "مخاطب «${c.name}» فاقد عکس در سیم‌کارت/گوشی بود؛ نماد اختصاصی اعمال شد",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            onDismiss()
                                        }
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ContactAvatar(
                                            name = c.name,
                                            avatarUri = c.photoUri,
                                            size = 32.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = c.name,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = if (!c.photoUri.isNullOrBlank()) "${c.phoneNumber} • دارای عکس"
                                                else "${c.phoneNumber} • بدون عکس در گوشی",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.Sync,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Built-in Executive & Security Avatar Presets
                Text(
                    text = "یا انتخاب از نمادهای امنیتی:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AvatarPreset.entries.forEach { preset ->
                        val isSelected = previewUri == preset.uriKey
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    previewUri = preset.uriKey
                                    onAvatarSelected(preset.uriKey)
                                    Toast.makeText(
                                        context,
                                        "نماد «${preset.labelFa}» انتخاب شد",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onDismiss()
                                }
                                .padding(4.dp)
                                .testTag("avatar_preset_${preset.name.lowercase()}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                ContactAvatar(
                                    name = contactName,
                                    avatarUri = preset.uriKey,
                                    size = 46.dp
                                )
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.35f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = preset.labelFa,
                                fontSize = 10.sp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // 4. Remove Avatar Option
                if (previewUri.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(
                        onClick = {
                            previewUri = ""
                            onAvatarSelected("")
                            Toast.makeText(context, "تصویر پروفایل حذف شد", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        modifier = Modifier.testTag("remove_avatar_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "حذف تصویر پروفایل و بازگشت به پیش‌فرض",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("بستن")
            }
        }
    )
}
