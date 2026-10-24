package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.EncryptedStorageRepository
import com.example.ui.components.AvatarPickerDialog
import com.example.ui.components.ContactAvatar
import com.example.ui.components.GlassCard
import com.example.ui.components.LiquidButton as Button
import com.example.ui.components.LiquidIconButton
import com.example.ui.components.LiquidOutlinedButton as OutlinedButton
import com.example.ui.components.glassTextFieldColors
import com.example.ui.components.ultraGlassSurface
import com.example.util.ContactHelper
import com.example.util.DeviceContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ContactsScreen(
    repository: EncryptedStorageRepository,
    onSelectContact: (Long) -> Unit,
    onManualNewChat: () -> Unit,
    onCreateGroup: () -> Unit = {},
    onCreateChannel: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val conversationsFlow = remember(repository) { repository.getAllConversationsFlow() }
    val conversations by conversationsFlow.collectAsState(initial = emptyList())

    var hasContactPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        )
    }

    var contactsList by remember { mutableStateOf<List<DeviceContact>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var editingContactAvatar by remember { mutableStateOf<DeviceContact?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasContactPermission = isGranted
        if (isGranted) {
            coroutineScope.launch(Dispatchers.IO) {
                val list = ContactHelper.getDeviceContacts(context)
                repository.syncContactNames(context)
                withContext(Dispatchers.Main) {
                    contactsList = list
                }
            }
        }
    }

    LaunchedEffect(hasContactPermission) {
        if (hasContactPermission) {
            isLoading = true
            withContext(Dispatchers.IO) {
                val list = ContactHelper.getDeviceContacts(context)
                repository.syncContactNames(context)
                withContext(Dispatchers.Main) {
                    contactsList = list
                    isLoading = false
                }
            }
        }
    }

    val filteredContacts = remember(contactsList, searchQuery) {
        if (searchQuery.isBlank()) contactsList
        else contactsList.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.phoneNumber.contains(searchQuery)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Pure Single-Layer Glass Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .ultraGlassSurface(
                    shape = RoundedCornerShape(22.dp),
                    cornerRadius = 22.dp,
                    shadowElevation = 6.dp
                )
                .testTag("search_contacts_input"),
            placeholder = { Text("جستجوی نام یا شماره در مخاطبین...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    LiquidIconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "پاک کردن جستجو",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(22.dp),
            colors = glassTextFieldColors(transparentContainer = true)
        )

        // Quick Action Row: New Number, New Group, New Channel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GlassCard(
                modifier = Modifier
                    .weight(1f)
                    .testTag("manual_new_number_card"),
                shape = RoundedCornerShape(18.dp),
                cornerRadius = 18.dp,
                tintColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                borderTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                onClick = onManualNewChat
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "شماره جدید",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            GlassCard(
                modifier = Modifier
                    .weight(1f)
                    .testTag("contacts_new_group_card"),
                shape = RoundedCornerShape(18.dp),
                cornerRadius = 18.dp,
                tintColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f),
                borderTint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f),
                onClick = onCreateGroup
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "گروه جدید",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            GlassCard(
                modifier = Modifier
                    .weight(1f)
                    .testTag("contacts_new_channel_card"),
                shape = RoundedCornerShape(18.dp),
                cornerRadius = 18.dp,
                tintColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f),
                borderTint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f),
                onClick = onCreateChannel
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "کانال جدید",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (!hasContactPermission) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    cornerRadius = 26.dp,
                    shadowElevation = 12.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .ultraGlassSurface(
                                    shape = CircleShape,
                                    cornerRadius = 36.dp,
                                    tintColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                    shadowElevation = 4.dp
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContactPhone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "دسترسی به مخاطبین گوشی",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "برای نمایش مخاطبین و دریافت خودکار تصاویر پروفایل، مجوز دسترسی را فعال کنید.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("grant_contacts_permission_button")
                        ) {
                            Text("فعال‌سازی دسترسی", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else if (filteredContacts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "مخاطبی با این مشخصات یافت نشد" else "هنوز مخاطبی در گوشی یافت نشده است",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "می‌توانید به سادگی شماره جدید اضافه کنید یا یک مخاطب نمونه جهت تست سریع بسازید.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onManualNewChat,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("شماره جدید", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    val conv = repository.createOrUpdateConversation(
                                        phoneNumber = "09129998877",
                                        contactName = "سارا رضایی (تست رمزنگاری)",
                                        avatarUri = "preset:emerald_shield"
                                    )
                                    withContext(Dispatchers.Main) {
                                        onSelectContact(conv.id)
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مخاطب تستی سریع", fontSize = 12.sp)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredContacts, key = { "${it.id}_${it.phoneNumber}" }) { contact ->
                    val matchingConversation = remember(conversations, contact.phoneNumber) {
                        conversations.firstOrNull { conv ->
                            conv.isDirect && ContactHelper.isSamePhoneNumber(conv.phoneNumber, contact.phoneNumber)
                        }
                    }
                    val effectiveAvatarUri = matchingConversation?.avatarUri?.takeIf { it.isNotBlank() }
                        ?: contact.photoUri

                    GlassCard(
                        modifier = Modifier
                            .animateItem(
                                fadeInSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                placementSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                fadeOutSpec = spring(stiffness = Spring.StiffnessMediumLow)
                            )
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(20.dp),
                        cornerRadius = 20.dp,
                        shadowElevation = 6.dp,
                        onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                val conv = repository.createOrUpdateConversation(
                                    phoneNumber = contact.phoneNumber,
                                    contactName = contact.name,
                                    avatarUri = effectiveAvatarUri
                                )
                                withContext(Dispatchers.Main) {
                                    onSelectContact(conv.id)
                                }
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ContactAvatar(
                                name = contact.name,
                                avatarUri = effectiveAvatarUri,
                                size = 48.dp,
                                showCameraBadge = true,
                                onClick = { editingContactAvatar = contact }
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = contact.name,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = contact.phoneNumber,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            LiquidIconButton(
                                onClick = { editingContactAvatar = contact }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "انتخاب تصویر پروفایل",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Button(
                                onClick = {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val conv = repository.createOrUpdateConversation(
                                            phoneNumber = contact.phoneNumber,
                                            contactName = contact.name,
                                            avatarUri = effectiveAvatarUri
                                        )
                                        withContext(Dispatchers.Main) {
                                            onSelectContact(conv.id)
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("گفتگو", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    editingContactAvatar?.let { targetContact ->
        val matchingConv = conversations.firstOrNull { conv ->
            conv.isDirect && ContactHelper.isSamePhoneNumber(conv.phoneNumber, targetContact.phoneNumber)
        }
        val currentUri = matchingConv?.avatarUri?.takeIf { it.isNotBlank() } ?: targetContact.photoUri

        AvatarPickerDialog(
            contactName = targetContact.name,
            phoneNumber = targetContact.phoneNumber,
            currentAvatarUri = currentUri,
            onAvatarSelected = { newUri ->
                coroutineScope.launch(Dispatchers.IO) {
                    val conv = repository.createOrUpdateConversation(
                        phoneNumber = targetContact.phoneNumber,
                        contactName = targetContact.name,
                        avatarUri = newUri
                    )
                    repository.updateConversationAvatar(conv.id, newUri)
                }
            },
            onDismiss = { editingContactAvatar = null }
        )
    }
}
