package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.CryptoEngine
import com.example.crypto.PersianCiphertextCodec
import com.example.data.DeliveryStatus
import com.example.data.EncryptedStorageRepository
import com.example.data.VerificationStatus
import com.example.sms.SmsTransportManager
import com.example.ui.components.AvatarPickerDialog
import com.example.ui.components.ContactAvatar
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassHeaderSurface
import com.example.ui.components.KeyShareDialog
import com.example.ui.components.LiquidButton as Button
import com.example.ui.components.LiquidFilterChip as FilterChip
import com.example.ui.components.LiquidIconButton as IconButton
import com.example.ui.components.LiquidMessageBubbleSurface
import com.example.ui.components.LiquidOutlinedButton as OutlinedButton
import com.example.ui.components.LiquidTextButton as TextButton
import com.example.ui.components.glassTextFieldColors
import com.example.ui.components.ultraGlassSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val QUICK_REACTION_EMOJIS = listOf("👍", "❤️", "😂", "😮", "😢", "🙏", "🔥")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: Long,
    repository: EncryptedStorageRepository,
    transportManager: SmsTransportManager,
    onNavigateBack: () -> Unit,
    onOpenContactSecurity: (Long) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val conversationFlow = remember(repository, conversationId) {
        repository.getConversationFlow(conversationId)
    }
    val messagesFlow = remember(repository, conversationId) {
        repository.getDecryptedMessagesFlow(conversationId)
    }
    val conversation by conversationFlow.collectAsState(initial = null)
    val messages by messagesFlow.collectAsState(initial = emptyList())

    var messageInput by remember { mutableStateOf("") }
    var replyingToMessage by remember { mutableStateOf<EncryptedStorageRepository.DecryptedMessage?>(null) }
    var useVirtualSimulation by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showCiphertextPreviewDialog by remember { mutableStateOf(false) }
    var showLargeMessageStackDialog by remember { mutableStateOf(false) }
    var showSendChoiceDialog by remember { mutableStateOf(false) }
    var showImportContactKeyDialog by remember { mutableStateOf(false) }
    var showRegenerateMyKeyDialog by remember { mutableStateOf(false) }
    var showShareKeyDialog by remember { mutableStateOf(false) }
    var showManageMembersDialog by remember { mutableStateOf(false) }
    var showAvatarPickerDialog by remember { mutableStateOf(false) }
    var myPublicKeyBase64 by remember { mutableStateOf("") }
    var myFingerprint by remember { mutableStateOf("") }
    var selectedMessageForInspection by remember { mutableStateOf<EncryptedStorageRepository.DecryptedMessage?>(null) }
    var currentEncoding by remember { mutableStateOf(PersianCiphertextCodec.EncodingMode.PERSIAN_LETTERS) }

    val isGroup = conversation?.isGroup == true
    val isChannel = conversation?.isChannel == true
    val isChannelOwner = conversation?.isOwner != false
    val membersList = remember(conversation?.memberPhones, conversation?.memberNames) {
        conversation?.parsedMembers() ?: emptyList()
    }

    LaunchedEffect(Unit) {
        val myPub = repository.getMyPublicKey()
        myPublicKeyBase64 = CryptoEngine.encodePublicKeyToBase64(myPub)
        myFingerprint = CryptoEngine.computeFingerprint(myPub)
    }

    val listState = rememberLazyListState()

    // Track real-time scroll velocity for liquid message physics
    var targetScrollVelocity by remember { mutableFloatStateOf(0f) }
    var lastIndex by remember { mutableIntStateOf(0) }
    var lastOffset by remember { mutableIntStateOf(0) }

    LaunchedEffect(listState) {
        snapshotFlow {
            Triple(
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset,
                listState.isScrollInProgress
            )
        }.collect { (index, offset, isScrolling) ->
            if (!isScrolling) {
                targetScrollVelocity = 0f
            } else {
                val indexDelta = (index - lastIndex) * 180
                val pixelDelta = offset - lastOffset + indexDelta
                targetScrollVelocity = (pixelDelta / 95f).coerceIn(-1.4f, 1.4f)
            }
            lastIndex = index
            lastOffset = offset
        }
    }

    // Smoothly decay scroll velocity so liquid stretch settles organically
    LaunchedEffect(targetScrollVelocity) {
        if (targetScrollVelocity != 0f) {
            delay(90)
            if (!listState.isScrollInProgress) {
                targetScrollVelocity = 0f
            }
        }
    }

    val smoothScrollVelocity by animateFloatAsState(
        targetValue = targetScrollVelocity,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "liquidScrollVelocity"
    )

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Mark local unread count as 0 and optionally transmit Seen receipt ONLY if enabled for this user (default OFF)
    LaunchedEffect(conversationId, messages.size, conversation?.sendReadReceipts) {
        repository.markConversationRead(conversationId)
        if (conversation?.sendReadReceipts == true) {
            val latestInbound = messages.lastOrNull { !it.isOutgoing && it.messagePayloadId != 0 }
            if (latestInbound != null) {
                transportManager.sendSeenReceiptIfEnabled(
                    conversationId = conversationId,
                    latestInboundPayloadId = latestInbound.messagePayloadId,
                    encodingMode = currentEncoding,
                    isVirtualSimulation = useVirtualSimulation
                )
            }
        }
    }

    LaunchedEffect(conversation?.preferredEncoding) {
        conversation?.preferredEncoding?.let {
            currentEncoding = try {
                PersianCiphertextCodec.EncodingMode.valueOf(it)
            } catch (e: Exception) {
                PersianCiphertextCodec.EncodingMode.PERSIAN_LETTERS
            }
        }
    }

    val isLargeMessage = remember(messageInput) {
        messageInput.toByteArray(Charsets.UTF_8).size >= 1024
    }

    val segmentEstimate = remember(messageInput, currentEncoding) {
        val len = messageInput.length
        if (len == 0) Pair(0, "۰ بخش")
        else {
            val approxRawBytes = len * 2 + 58
            val approxChars = when (currentEncoding) {
                PersianCiphertextCodec.EncodingMode.PERSIAN_LETTERS -> (approxRawBytes * 8) / 5
                PersianCiphertextCodec.EncodingMode.STANDARD_B64 -> (approxRawBytes * 4) / 3
                PersianCiphertextCodec.EncodingMode.PERSIAN_NUMERALS -> approxRawBytes * 2
                PersianCiphertextCodec.EncodingMode.AUTOMATIC -> (approxRawBytes * 8) / 5
            }
            val segments = Math.max(1, Math.ceil(approxChars.toDouble() / 65.0).toInt())
            val warning = if (segments > 2) " ($segments قطعه پیامک)" else ""
            Pair(segments, "$segments بخش$warning")
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            val isLightBar = MaterialTheme.colorScheme.surface.luminance() > 0.5f
            val verifiedTint = if (isLightBar) Color(0xFF059669) else Color(0xFF00E5A3)
            val unverifiedTint = if (isLightBar) Color(0xFFD97706) else Color(0xFFFFB703)
            val changedTint = if (isLightBar) Color(0xFFDC2626) else Color(0xFFEF476F)
            val groupTint = if (isLightBar) Color(0xFF6366F1) else Color(0xFF818CF8)
            val channelTint = if (isLightBar) Color(0xFF0284C7) else Color(0xFF38BDF8)

            val status = conversation?.verificationStatus ?: VerificationStatus.UNVERIFIED
            val badgeColor = when {
                isGroup -> groupTint
                isChannel -> channelTint
                status == VerificationStatus.VERIFIED -> verifiedTint
                status == VerificationStatus.IDENTITY_CHANGED -> changedTint
                else -> unverifiedTint
            }
            val seenEnabledForContact = conversation?.sendReadReceipts == true

            GlassHeaderSurface(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    if (isGroup || isChannel) {
                                        showManageMembersDialog = true
                                    } else {
                                        onOpenContactSecurity(conversationId)
                                    }
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            ContactAvatar(
                                name = conversation?.contactName ?: "؟",
                                avatarUri = conversation?.avatarUri,
                                size = 38.dp,
                                accentColor = badgeColor,
                                isGroup = isGroup,
                                isChannel = isChannel,
                                onClick = { showAvatarPickerDialog = true },
                                modifier = Modifier.testTag("chat_header_avatar")
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = conversation?.contactName ?: "گفتگو",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        lineHeight = 19.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))

                                    // Properly proportioned single-line verification / group / channel badge pill
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(badgeColor.copy(alpha = 0.16f))
                                            .border(
                                                width = 0.8.dp,
                                                color = badgeColor.copy(alpha = 0.45f),
                                                shape = RoundedCornerShape(50)
                                            )
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = when {
                                                isGroup -> Icons.Default.Groups
                                                isChannel -> Icons.Default.Campaign
                                                status == VerificationStatus.VERIFIED -> Icons.Default.VerifiedUser
                                                status == VerificationStatus.IDENTITY_CHANGED -> Icons.Default.Warning
                                                else -> Icons.Default.Lock
                                            },
                                            contentDescription = null,
                                            tint = badgeColor,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = when {
                                                isGroup -> "گروه امن"
                                                isChannel -> "کانال"
                                                status == VerificationStatus.VERIFIED -> "تأیید شده"
                                                status == VerificationStatus.IDENTITY_CHANGED -> "تغییر کلید!"
                                                else -> "تأیید نشده"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp,
                                            lineHeight = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false,
                                            color = badgeColor
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = when {
                                            isGroup -> "${membersList.size} عضو • ارسال چندگانه پیامک"
                                            isChannel -> "${membersList.size} مشترک • ${if (isChannelOwner) "مدیر کانال" else "مشترک"}"
                                            else -> conversation?.phoneNumber ?: ""
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 11.sp,
                                        lineHeight = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (seenEnabledForContact) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "• رسید خواندن فعال",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp,
                                            lineHeight = 12.sp,
                                            maxLines = 1,
                                            softWrap = false,
                                            color = verifiedTint,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "بازگشت"
                            )
                        }
                    },
                    actions = {
                        if (isGroup || isChannel) {
                            IconButton(
                                onClick = { showManageMembersDialog = true },
                                modifier = Modifier.testTag("manage_members_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GroupAdd,
                                    contentDescription = "مدیریت اعضا",
                                    tint = badgeColor
                                )
                            }
                        }

                        IconButton(
                            onClick = { onOpenContactSecurity(conversationId) },
                            modifier = Modifier.testTag("security_shield_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "امنیت مخاطب و کد اعتبارسنجی",
                                tint = if (status == VerificationStatus.VERIFIED) verifiedTint else unverifiedTint
                            )
                        }

                        Box {
                            IconButton(
                                onClick = { showOptionsMenu = true },
                                modifier = Modifier.testTag("chat_options_menu_button")
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "بیشتر")
                            }
                            DropdownMenu(
                                expanded = showOptionsMenu,
                                onDismissRequest = { showOptionsMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "تغییر تصویر پروفایل / دریافت از مخاطبین",
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    },
                                    onClick = {
                                        showOptionsMenu = false
                                        showAvatarPickerDialog = true
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                )
                                if (isGroup || isChannel) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = if (isGroup) "مدیریت اعضای گروه (${membersList.size} نفر)"
                                                else "مدیریت مشترکین کانال (${membersList.size} نفر)",
                                                fontWeight = FontWeight.Bold
                                            )
                                        },
                                        onClick = {
                                            showOptionsMenu = false
                                            showManageMembersDialog = true
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Groups, contentDescription = null, tint = badgeColor)
                                        }
                                    )
                                }

                                if (isGroup) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "شبیه‌سازی پیام ورودی از عضو گروه",
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        },
                                        onClick = {
                                            showOptionsMenu = false
                                            val sampleMember = membersList.randomOrNull()
                                                ?: ("09121112233" to "امیرحسین رضایی")
                                            val sampleTexts = listOf(
                                                "سلام دوستان! بسته رمزنگاری‌شده گروه با موفقیت دریافت شد.",
                                                "جلسه هماهنگی ساعت ۱۸ برگزار می‌شود، لطفاً کلیدهای امنیتی را تایید کنید.",
                                                "تست انیمیشن مایع (Liquid Glass) روی پیام‌های گروهی فوق‌العاده است! 🌊✨"
                                            )
                                            coroutineScope.launch {
                                                transportManager.simulateIncomingGroupMemberMessage(
                                                    conversationId = conversationId,
                                                    memberName = sampleMember.second,
                                                    memberPhone = sampleMember.first,
                                                    text = sampleTexts.random()
                                                )
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Chat,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    )
                                }

                                if (isChannel) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = if (isChannelOwner) "تغییر به نمای مشترک (فقط خواندنی)"
                                                else "تغییر به نمای مدیر کانال (امکان ارسال)",
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        },
                                        onClick = {
                                            showOptionsMenu = false
                                            coroutineScope.launch {
                                                repository.updateChannelRole(conversationId, !isChannelOwner)
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null)
                                        }
                                    )
                                }

                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = "ارسال رسید «دیده شد» (Seen)",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = if (seenEnabledForContact) "فعال برای این گفتگو" else "غیرفعال (پیش‌فرض)",
                                                fontSize = 11.sp,
                                                color = if (seenEnabledForContact) verifiedTint else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        val nextState = !seenEnabledForContact
                                        coroutineScope.launch {
                                            repository.updateSendReadReceipts(conversationId, nextState)
                                            Toast.makeText(
                                                context,
                                                if (nextState) "ارسال رسید «دیده شد» فعال شد"
                                                else "ارسال رسید «دیده شد» غیرفعال شد",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.DoneAll,
                                            contentDescription = null,
                                            tint = if (seenEnabledForContact) verifiedTint else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    trailingIcon = {
                                        Switch(
                                            checked = seenEnabledForContact,
                                            onCheckedChange = null
                                        )
                                    }
                                )
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                DropdownMenuItem(
                                    text = { Text("ورود یا ویرایش کلید مخاطب") },
                                    onClick = {
                                        showOptionsMenu = false
                                        showImportContactKeyDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("اشتراک‌گذاری کلید من (QR / تلگرام / بلوتوث)") },
                                    onClick = {
                                        showOptionsMenu = false
                                        showShareKeyDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("تولید کلید جدید من") },
                                    onClick = {
                                        showOptionsMenu = false
                                        showRegenerateMyKeyDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("کپی کلید عمومی من") },
                                    onClick = {
                                        showOptionsMenu = false
                                        coroutineScope.launch {
                                            val myPub = repository.getMyPublicKey()
                                            val b64 = CryptoEngine.encodePublicKeyToBase64(myPub)
                                            clipboardManager.setText(AnnotatedString(b64))
                                            Toast.makeText(context, "کلید عمومی کپی شد", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("کد امنیتی و احراز هویت") },
                                    onClick = {
                                        showOptionsMenu = false
                                        onOpenContactSecurity(conversationId)
                                    },
                                    leadingIcon = { Icon(Icons.Default.Security, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("پاکسازی این گفتگو") },
                                    onClick = {
                                        showOptionsMenu = false
                                        coroutineScope.launch {
                                            repository.deleteConversation(conversationId)
                                            onNavigateBack()
                                        }
                                    },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                                )
                            }
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
                .imePadding()
        ) {
            // Identity Changed Warning Glass Banner
            if (conversation?.verificationStatus == VerificationStatus.IDENTITY_CHANGED) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    cornerRadius = 16.dp,
                    tintColor = Color(0xFFEF476F).copy(alpha = 0.18f),
                    borderTint = Color(0xFFEF476F).copy(alpha = 0.55f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFEF476F),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "هشدار: کلید این مخاطب تغییر یافته است! هویت را احراز کنید.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFEF476F),
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(onClick = { onOpenContactSecurity(conversationId) }) {
                            Text("بررسی", color = Color(0xFFEF476F), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Group / Channel Interactive Header Strip
            if (isGroup || isChannel) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(14.dp),
                    cornerRadius = 14.dp,
                    tintColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    borderTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isGroup) Icons.Default.Groups else Icons.Default.Campaign,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isGroup)
                                        "اعضای گروه: ${membersList.joinToString("، ") { it.second }.ifBlank { "بدون عضو" }}"
                                    else
                                        "کانال پخش پیامکی • ${membersList.size} مشترک",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!conversation?.description.isNullOrBlank()) {
                                    Text(
                                        text = conversation?.description ?: "",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        if (isGroup) {
                            OutlinedButton(
                                onClick = {
                                    val sampleMember = membersList.randomOrNull()
                                        ?: ("09121112233" to "امیرحسین رضایی")
                                    val sampleTexts = listOf(
                                        "سلام دوستان! پیام گروهی شما دریافت شد.",
                                        "همه اعضا آنلاین هستند، ارسال گروهی با موفقیت انجام شد.",
                                        "بسته رمزنگاری‌شده گروه دریافت و اصالت‌سنجی شد."
                                    )
                                    coroutineScope.launch {
                                        transportManager.simulateIncomingGroupMemberMessage(
                                            conversationId = conversationId,
                                            memberName = sampleMember.second,
                                            memberPhone = sampleMember.first,
                                            text = sampleTexts.random()
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("simulate_group_msg_button")
                            ) {
                                Text("پیام آزمایشی عضو", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { showManageMembersDialog = true },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("مشترکین", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Messages List with Liquid Scroll Physics
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                state = listState
            ) {
                if (messages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            GlassCard(
                                shape = RoundedCornerShape(22.dp),
                                cornerRadius = 22.dp,
                                shadowElevation = 10.dp
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = when {
                                            isGroup -> Icons.Default.Groups
                                            isChannel -> Icons.Default.Campaign
                                            else -> Icons.Default.Lock
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(30.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = when {
                                            isGroup -> "گروه امن پیامکی آماده گفتگو است"
                                            isChannel -> "کانال پخش پیامکی آماده انتشار است"
                                            else -> "پیام‌های این گفتگو سرتاسر رمزنگاری می‌شوند"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = when {
                                            isGroup -> "هر پیام برای تمام اعضای گروه به صورت جداگانه ارسال می‌شود."
                                            isChannel -> "پست‌های شما برای تمام مشترکین کانال ارسال می‌شود."
                                            else -> "روی پیام‌ها ضربه بزنید یا اسکرول کنید تا افکت مایع را تجربه کنید."
                                        },
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                itemsIndexed(messages, key = { _, item -> item.id }) { index, message ->
                    val totalVisible = listState.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
                    val relativePos = ((index - listState.firstVisibleItemIndex).toFloat() / totalVisible.toFloat())
                        .coerceIn(0f, 1f)

                    MessageBubble(
                        message = message,
                        isGroup = isGroup,
                        isChannel = isChannel,
                        subscriberCount = membersList.size.coerceAtLeast(1),
                        scrollVelocityFactor = smoothScrollVelocity,
                        viewportFraction = relativePos,
                        onReply = {
                            replyingToMessage = message
                        },
                        onReact = { emoji ->
                            val phone = conversation?.phoneNumber ?: ""
                            coroutineScope.launch {
                                transportManager.sendReaction(
                                    conversationId = conversationId,
                                    recipientPhone = phone,
                                    targetMessageId = message.id,
                                    targetPayloadId = message.messagePayloadId,
                                    emoji = emoji,
                                    encodingMode = currentEncoding,
                                    isVirtualSimulation = useVirtualSimulation
                                )
                            }
                        },
                        onClickQuotedReply = { targetId ->
                            val idx = messages.indexOfFirst { it.id == targetId }
                            if (idx >= 0) {
                                coroutineScope.launch {
                                    listState.animateScrollToItem(idx)
                                }
                            }
                        },
                        onInspectEnvelope = { selectedMessageForInspection = message },
                        onCopyPlaintext = {
                            clipboardManager.setText(AnnotatedString(message.plaintext))
                            Toast.makeText(context, "متن پیام کپی شد", Toast.LENGTH_SHORT).show()
                        },
                        onDelete = {
                            if (replyingToMessage?.id == message.id) {
                                replyingToMessage = null
                            }
                            coroutineScope.launch {
                                repository.deleteMessage(message.id)
                            }
                        },
                        modifier = Modifier.animateItem(
                            fadeInSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            placementSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            fadeOutSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        )
                    )
                }
            }

            // Missing Public Key Friendly Glass Banner (only for Direct chats when key is unset)
            if (conversation?.isDirect == true && conversation?.theirPublicKeyBase64 == null) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(18.dp),
                    cornerRadius = 18.dp,
                    tintColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                    borderTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.50f),
                    shadowElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "گفتگوی امن آفلاین با ${conversation?.contactName ?: ""}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "کلید امنیتی مخاطب تنظیم نشده است. برای پیام‌های رمزشده، کلید را تنظیم کنید.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = { onOpenContactSecurity(conversationId) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تنظیم کلید مخاطب (کد QR / کپی)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Read-Only Subscriber Bar when in a Channel and !isChannelOwner
            if (isChannel && !isChannelOwner) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    cornerRadius = 24.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "شما مشترک این کانال هستید (فقط مدیر امکان انتشار دارد)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    repository.updateChannelRole(conversationId, true)
                                }
                            }
                        ) {
                            Text("حالت مدیر", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                // Floating Frosted Glass Composer Dock
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .ultraGlassSurface(
                            shape = RoundedCornerShape(28.dp),
                            cornerRadius = 28.dp,
                            shadowElevation = 14.dp,
                            surfaceAlphaMultiplier = 1.05f
                        )
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        // Active Reply Preview Banner
                        AnimatedVisibility(
                            visible = replyingToMessage != null,
                            enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMedium)) + fadeIn(),
                            exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMedium)) + fadeOut()
                        ) {
                            val targetReply = replyingToMessage
                            if (targetReply != null) {
                                GlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    cornerRadius = 14.dp,
                                    tintColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                    borderTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                                    shadowElevation = 4.dp
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height(30.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Reply,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (targetReply.isOutgoing) "پاسخ به پیام خودتان"
                                                else if (targetReply.senderDisplayName.isNotBlank()) "پاسخ به ${targetReply.senderDisplayName}"
                                                else "پاسخ به ${conversation?.contactName ?: "مخاطب"}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                lineHeight = 14.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = targetReply.plaintext,
                                                fontSize = 12.sp,
                                                lineHeight = 16.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(
                                            onClick = { replyingToMessage = null },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "لغو پاسخ",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Large Payload (>1KB) Chunk Stack Banner
                        if (isLargeMessage) {
                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                shape = RoundedCornerShape(14.dp),
                                cornerRadius = 14.dp,
                                tintColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                shadowElevation = 4.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Layers,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "پیام طولانی (${messageInput.toByteArray(Charsets.UTF_8).size} بایت)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "ارسال خودکار در ${segmentEstimate.first} قطعه مجزا",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    TextButton(onClick = { showLargeMessageStackDialog = true }) {
                                        Text("قطعات", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Encoding switcher & Segment indicator row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FilterChip(
                                    selected = currentEncoding == PersianCiphertextCodec.EncodingMode.PERSIAN_LETTERS,
                                    onClick = {
                                        currentEncoding = if (currentEncoding == PersianCiphertextCodec.EncodingMode.PERSIAN_LETTERS) {
                                            PersianCiphertextCodec.EncodingMode.STANDARD_B64
                                        } else {
                                            PersianCiphertextCodec.EncodingMode.PERSIAN_LETTERS
                                        }
                                        coroutineScope.launch {
                                            repository.updatePreferredEncoding(conversationId, currentEncoding.name)
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = if (currentEncoding == PersianCiphertextCodec.EncodingMode.PERSIAN_LETTERS)
                                                "حروف فارسی (Base32)" else "قالب لاتین (Base64)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                        selectedLabelColor = MaterialTheme.colorScheme.primary
                                    )
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                FilterChip(
                                    selected = useVirtualSimulation,
                                    onClick = { useVirtualSimulation = !useVirtualSimulation },
                                    label = {
                                        Text(
                                            text = if (useVirtualSimulation) "شبیه‌ساز فعال" else "شبکه پیامک",
                                            fontSize = 11.sp
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.SimCard,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF38BDF8).copy(alpha = 0.22f),
                                        selectedLabelColor = Color(0xFF38BDF8)
                                    )
                                )
                            }

                            if (segmentEstimate.first > 0) {
                                Text(
                                    text = segmentEstimate.second,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (segmentEstimate.first > 2) Color(0xFFFFB703) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Text Field & Liquid Send Action Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = messageInput,
                                onValueChange = { messageInput = it },
                                placeholder = {
                                    Text(
                                        when {
                                            replyingToMessage != null -> "پاسخ خود را بنویسید..."
                                            isGroup -> "ارسال پیام به اعضای گروه..."
                                            isChannel -> "انتشار پست در کانال..."
                                            else -> "متن پیام محرمانه..."
                                        }
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .animateContentSize(
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                    .testTag("message_input_field"),
                                maxLines = 4,
                                shape = RoundedCornerShape(24.dp),
                                colors = glassTextFieldColors()
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            AnimatedVisibility(
                                visible = messageInput.isNotBlank(),
                                enter = expandHorizontally(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                ) + scaleIn(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                ) + fadeIn(),
                                exit = shrinkHorizontally(
                                    animationSpec = spring(stiffness = Spring.StiffnessMedium)
                                ) + scaleOut() + fadeOut()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { showCiphertextPreviewDialog = true },
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .ultraGlassSurface(shape = CircleShape, cornerRadius = 21.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Visibility,
                                            contentDescription = "پیش‌نمایش محتوای رمزشده",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                            }

                            val hasText = messageInput.isNotBlank()
                            val sendButtonColor by animateColorAsState(
                                targetValue = if (hasText) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "sendBtnColor"
                            )
                            val sendButtonScale by animateFloatAsState(
                                targetValue = if (hasText) 1.0f else 0.92f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                label = "sendBtnScale"
                            )

                            IconButton(
                                onClick = {
                                    val textToSend = messageInput.trim()
                                    val phone = conversation?.phoneNumber
                                    val replyTarget = replyingToMessage
                                    if (textToSend.isNotBlank() && phone != null) {
                                        if (conversation?.theirPublicKeyBase64 != null) {
                                            coroutineScope.launch {
                                                try {
                                                    transportManager.sendEncryptedMessage(
                                                        conversationId = conversationId,
                                                        recipientPhone = phone,
                                                        plaintext = textToSend,
                                                        encodingMode = currentEncoding,
                                                        isVirtualSimulation = useVirtualSimulation,
                                                        replyToMessageId = replyTarget?.id ?: 0L,
                                                        replyToPayloadId = replyTarget?.messagePayloadId ?: 0,
                                                        replyToPreview = replyTarget?.plaintext?.take(48) ?: ""
                                                    )
                                                    messageInput = ""
                                                    replyingToMessage = null
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "خطا در ارسال: ${e.message}", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        } else if (isGroup || isChannel) {
                                            // Group or Channel without pairwise key sends multi-recipient fanout immediately
                                            coroutineScope.launch {
                                                try {
                                                    transportManager.sendStandardSms(
                                                        conversationId = conversationId,
                                                        recipientPhone = phone,
                                                        plaintext = textToSend,
                                                        isVirtualSimulation = useVirtualSimulation,
                                                        replyToMessageId = replyTarget?.id ?: 0L,
                                                        replyToPayloadId = replyTarget?.messagePayloadId ?: 0,
                                                        replyToPreview = replyTarget?.plaintext?.take(48) ?: ""
                                                    )
                                                    messageInput = ""
                                                    replyingToMessage = null
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "خطا در ارسال: ${e.message}", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        } else {
                                            showSendChoiceDialog = true
                                        }
                                    }
                                },
                                enabled = hasText,
                                modifier = Modifier
                                    .size(48.dp)
                                    .scale(sendButtonScale)
                                    .clip(CircleShape)
                                    .background(sendButtonColor)
                                    .testTag("send_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "ارسال پیامک",
                                    tint = if (hasText)
                                        MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAvatarPickerDialog) {
        AvatarPickerDialog(
            contactName = conversation?.contactName ?: "مخاطب",
            phoneNumber = conversation?.phoneNumber ?: "",
            currentAvatarUri = conversation?.avatarUri,
            isGroup = isGroup,
            isChannel = isChannel,
            onAvatarSelected = { newUri ->
                coroutineScope.launch(Dispatchers.IO) {
                    repository.updateConversationAvatar(conversationId, newUri)
                }
            },
            onDismiss = { showAvatarPickerDialog = false }
        )
    }

    // Group / Channel Member Management Dialog
    if (showManageMembersDialog) {
        var newMemberName by remember { mutableStateOf("") }
        var newMemberPhone by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showManageMembersDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isGroup) Icons.Default.Groups else Icons.Default.Campaign,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isGroup) "مدیریت اعضای گروه" else "مدیریت مشترکین کانال",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isGroup)
                            "پیام‌های ارسالی در گروه به صورت خودکار به شماره تمامی اعضای زیر ارسال می‌شوند:"
                        else
                            "پست‌های کانال به شماره تمامی مشترکین زیر ارسال می‌شوند:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = newMemberName,
                            onValueChange = { newMemberName = it },
                            placeholder = { Text("نام", fontSize = 11.sp) },
                            modifier = Modifier.weight(0.9f),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = glassTextFieldColors()
                        )
                        OutlinedTextField(
                            value = newMemberPhone,
                            onValueChange = { newMemberPhone = it },
                            placeholder = { Text("0912...", fontSize = 11.sp) },
                            modifier = Modifier.weight(1.1f),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = glassTextFieldColors()
                        )
                        IconButton(
                            onClick = {
                                val phone = newMemberPhone.trim()
                                if (phone.isNotBlank()) {
                                    val name = newMemberName.trim().ifBlank { phone }
                                    coroutineScope.launch {
                                        repository.addMemberToGroupOrChannel(conversationId, phone, name)
                                    }
                                    newMemberPhone = ""
                                    newMemberName = ""
                                }
                            }
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "افزودن", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (membersList.isEmpty()) {
                        Text(
                            text = "هنوز عضوی ثبت نشده است.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            membersList.forEach { (phone, name) ->
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    cornerRadius = 14.dp
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(phone, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        IconButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    repository.removeMemberFromGroupOrChannel(conversationId, phone)
                                                }
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "حذف عضو",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showManageMembersDialog = false }) {
                    Text("بستن")
                }
            }
        )
    }

    // Large message stack inspection modal
    if (showLargeMessageStackDialog) {
        val bytes = messageInput.toByteArray(Charsets.UTF_8).size
        val count = segmentEstimate.first
        AlertDialog(
            onDismissRequest = { showLargeMessageStackDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Layers, contentDescription = null, tint = Color(0xFF38BDF8))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("چیدمان قطعات پیامک (>1KB)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    Text(
                        text = "این پیام دارای حجم $bytes بایت است و به صورت خودکار به $count قطعه امن تقسیم شده است.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "ساختار ارسال پله‌ای (Chunk Stack):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF00E5A3)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    ChunkStackPills(totalChunks = count, sentCount = 0)

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "• هر قطعه شامل هدر پروتکل ۲۹ بایتی، شناسه یکتای پیام و شماره ترتیب است.\n• گیرنده پیام پس از وصول تمامی قطعات، اصالت‌سنجی نهایی را انجام می‌دهد.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showLargeMessageStackDialog = false }) {
                    Text("متوجه شدم")
                }
            }
        )
    }

    // Send Choice Dialog (uses pure GlassCard instead of solid Card rectangles)
    if (showSendChoiceDialog) {
        val phone = conversation?.phoneNumber ?: ""
        val textToSend = messageInput.trim()
        val replyTarget = replyingToMessage
        AlertDialog(
            onDismissRequest = { showSendChoiceDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تبادل امن کلید", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    Text(
                        text = "کلید عمومی مخاطب هنوز ثبت نشده است. به منظور حفظ امنیت و حریم خصوصی، کلیدها در پیامک ارسال نمی‌شوند.",
                        fontSize = 12.sp,
                        lineHeight = 19.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Option 1: Enter contact key
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        cornerRadius = 14.dp,
                        tintColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                        borderTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                        onClick = {
                            showSendChoiceDialog = false
                            showImportContactKeyDialog = true
                        }
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("ورود کلید عمومی مخاطب", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text("جای‌گذاری کلید مخاطب برای فعال‌سازی چت رمزنگاری‌شده", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Option 2: Share my own key
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        cornerRadius = 14.dp,
                        onClick = {
                            showSendChoiceDialog = false
                            showShareKeyDialog = true
                        }
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("اشتراک‌گذاری کلید من (QR، تلگرام، بلوتوث)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text("ارسال کلید امنیتی با تلگرام، بلوتوث یا اسکن کد QR بدون پیامک", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Option 3: Send as standard SMS
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        cornerRadius = 14.dp,
                        onClick = {
                            showSendChoiceDialog = false
                            coroutineScope.launch {
                                try {
                                    transportManager.sendStandardSms(
                                        conversationId = conversationId,
                                        recipientPhone = phone,
                                        plaintext = textToSend,
                                        isVirtualSimulation = useVirtualSimulation,
                                        replyToMessageId = replyTarget?.id ?: 0L,
                                        replyToPayloadId = replyTarget?.messagePayloadId ?: 0,
                                        replyToPreview = replyTarget?.plaintext?.take(48) ?: ""
                                    )
                                    messageInput = ""
                                    replyingToMessage = null
                                    Toast.makeText(context, "پیامک عادی ارسال شد", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "خطا در ارسال پیامک: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Chat, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("ارسال با پیامک عادی (ساده)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text("ارسال بدون رمزنگاری به صورت پیامک معمولی", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSendChoiceDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    // Import Contact Key Dialog
    if (showImportContactKeyDialog) {
        var inputKeyText by remember { mutableStateOf("") }
        var keyError by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showImportContactKeyDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ورود کلید عمومی مخاطب", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            },
            text = {
                Column {
                    Text(
                        text = "کلید عمومی دریافتی از مخاطب (Base64) را در کادر زیر وارد کنید:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inputKeyText,
                        onValueChange = {
                            inputKeyText = it
                            keyError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4,
                        placeholder = { Text("MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQc...") },
                        shape = RoundedCornerShape(14.dp),
                        colors = glassTextFieldColors()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            val clip = clipboardManager.getText()?.text
                            if (!clip.isNullOrBlank()) {
                                inputKeyText = com.example.util.KeySharingHelper.extractBase64Key(clip)
                                keyError = null
                                Toast.makeText(context, "کلید از کلیپ‌بورد جای‌گذاری شد", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "حافظه موقت خالی است", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("جای‌گذاری از کلیپ‌بورد (Paste)", fontSize = 12.sp)
                    }
                    if (keyError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(keyError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            val clean = com.example.util.KeySharingHelper.extractBase64Key(inputKeyText)
                            val parsedKey = CryptoEngine.parsePublicKeyFromBase64(clean)
                            val conv = conversation ?: return@Button
                            coroutineScope.launch {
                                repository.createOrUpdateConversation(
                                    phoneNumber = conv.phoneNumber,
                                    contactName = conv.contactName,
                                    theirPublicKey = parsedKey
                                )
                                showImportContactKeyDialog = false
                                Toast.makeText(context, "کلید مخاطب با موفقیت ذخیره شد", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            keyError = "کلید نامعتبر است: ${e.message}"
                        }
                    }
                ) {
                    Text("ذخیره کلید")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportContactKeyDialog = false }) {
                    Text("لغو")
                }
            }
        )
    }

    // Regenerate My Own Key Dialog
    if (showRegenerateMyKeyDialog) {
        AlertDialog(
            onDismissRequest = { showRegenerateMyKeyDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تولید مجدد کلید اختصاصی من", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            },
            text = {
                Text(
                    text = "آیا مایلید یک جفت کلید امنیتی جدید بسازید؟ پس از ساخت، کلید جدید را برای مخاطبین بفرستید تا چت رمزشده ادامه یابد.",
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
                            val b64 = CryptoEngine.encodePublicKeyToBase64(newKp.public)
                            clipboardManager.setText(AnnotatedString(b64))
                            Toast.makeText(context, "کلید جدید شما با موفقیت ساخته و کپی شد", Toast.LENGTH_LONG).show()
                        }
                    }
                ) {
                    Text("تولید کلید جدید")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRegenerateMyKeyDialog = false }) {
                    Text("لغو")
                }
            }
        )
    }

    if (showShareKeyDialog) {
        KeyShareDialog(
            publicKeyBase64 = myPublicKeyBase64,
            fingerprint = myFingerprint,
            onDismiss = { showShareKeyDialog = false }
        )
    }

    // Envelope Details Dialog (Frosted Glass Code Box)
    if (selectedMessageForInspection != null) {
        val msg = selectedMessageForInspection!!
        AlertDialog(
            onDismissRequest = { selectedMessageForInspection = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("مشخصات بسته رمزگذاری‌شده SMS", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("شناسه پیامک: #${msg.messagePayloadId}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("ترتیب شماره توالی: ${msg.sequenceNumber}", fontSize = 13.sp)
                    Text("تعداد قطعات پیامک: ${msg.fragmentCount} قطعه", fontSize = 13.sp)
                    Text("قالب کدگذاری: ${msg.encodingUsed}", fontSize = 13.sp)
                    Text("وضعیت تحویل: ${msg.deliveryStatus.label}", fontSize = 13.sp)

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("متن خام ارسالی از طریق شبکه پیامک:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        cornerRadius = 14.dp,
                        tintColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = msg.rawCiphertext,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row {
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(msg.rawCiphertext))
                                Toast.makeText(context, "متن خام کپی شد", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("کپی متن خام", fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedMessageForInspection = null }) {
                    Text("بستن")
                }
            }
        )
    }

    // Ciphertext Preview Dialog before sending (Frosted Glass Code Box)
    if (showCiphertextPreviewDialog) {
        AlertDialog(
            onDismissRequest = { showCiphertextPreviewDialog = false },
            title = { Text("پیش‌نمایش محتوای رمزشده پیامک", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "این متن دقیقاً همان کدهایی است که اپراتور تلفن همراه و شبکه‌های مخابراتی مشاهده خواهند کرد:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        cornerRadius = 14.dp,
                        tintColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = when (currentEncoding) {
                                PersianCiphertextCodec.EncodingMode.PERSIAN_LETTERS ->
                                    "پر۱:بپتدثجچحدذرژسشصضطظعغفقکگلمنوهیهدیستبدرسیب..."
                                PersianCiphertextCodec.EncodingMode.PERSIAN_NUMERALS ->
                                    "پن۱:۰۱۲۳۴۵۶۷۸۹؛،؟٪«»..."
                                else ->
                                    "SSMS1:QUVTQzI1Ni1HQ00gRW5jcnlwdGVkIFBheWxvYWQ..."
                            },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "هیچ کاراکتری از پیام اصلی شما به صورت خوانا انتقال نمی‌یابد.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showCiphertextPreviewDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("متوجه شدم", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MessageBubble(
    message: EncryptedStorageRepository.DecryptedMessage,
    isGroup: Boolean,
    isChannel: Boolean,
    subscriberCount: Int,
    scrollVelocityFactor: Float,
    viewportFraction: Float,
    onReply: () -> Unit,
    onReact: (String) -> Unit,
    onClickQuotedReply: (Long) -> Unit,
    onInspectEnvelope: () -> Unit,
    onCopyPlaintext: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeStr = remember(message.timestamp) {
        dateFormat.format(Date(message.timestamp))
    }
    var showMenu by remember { mutableStateOf(false) }
    var expandedStack by remember { mutableStateOf(false) }

    val activeReactions = remember(message.reactions) {
        message.reactions
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    }

    val isOutgoing = message.isOutgoing
    val isIncomplete = message.deliveryStatus == DeliveryStatus.INCOMPLETE
    val isSending = message.deliveryStatus == DeliveryStatus.SENDING

    val isLight = MaterialTheme.colorScheme.surface.luminance() > 0.5f

    val bubbleTint = when {
        isIncomplete -> if (isLight) Color(0xFFFEF3C7) else Color(0xFF78350F)
        !message.isEncrypted -> if (isOutgoing) {
            if (isLight) Color(0xFF0369A1) else Color(0xFF0E7490)
        } else {
            Color.Unspecified
        }
        isOutgoing -> if (isLight) Color(0xFF0284C7) else Color(0xFF059669)
        else -> Color.Unspecified
    }

    val bubbleBorderTint = when {
        isIncomplete -> Color(0xFFFFB703)
        isOutgoing -> if (isLight) Color(0xFF7DD3FC) else Color(0xFF34D399)
        else -> Color.Unspecified
    }

    val textColor = when {
        isIncomplete -> if (isLight) Color(0xFF92400E) else Color(0xFFFFD166)
        isOutgoing -> Color.White
        else -> if (isLight) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    }

    val timeColor = when {
        isOutgoing -> Color.White.copy(alpha = 0.85f)
        isLight -> Color(0xFF64748B)
        else -> Color(0xFF94A3B8)
    }

    val lockIconTint = when {
        !message.isEncrypted -> if (isOutgoing) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.primary
        isIncomplete -> if (isLight) Color(0xFFD97706) else Color(0xFFFFB703)
        isOutgoing -> Color(0xFFA7F3D0)
        isLight -> Color(0xFF059669)
        else -> Color(0xFF10B981)
    }

    val bubbleShape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = if (isOutgoing) 18.dp else 4.dp,
        bottomEnd = if (isOutgoing) 4.dp else 18.dp
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        val alignment = if (isOutgoing) Alignment.End else Alignment.Start

        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalAlignment = alignment
        ) {
            Box {
                Column(horizontalAlignment = alignment) {
                    LiquidMessageBubbleSurface(
                        modifier = Modifier
                            .widthIn(
                                min = 120.dp,
                                max = if (isChannel) 340.dp else 315.dp
                            )
                            .animateContentSize(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ),
                        shape = bubbleShape,
                        isOutgoing = isOutgoing,
                        isIncomplete = isIncomplete,
                        isEncrypted = message.isEncrypted,
                        isChannelPost = isChannel,
                        scrollVelocityFactor = scrollVelocityFactor,
                        viewportFraction = viewportFraction,
                        onClick = { showMenu = true }
                    ) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                // Channel Post Badge or Group Sender Header
                                if (isChannel) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Campaign,
                                            contentDescription = null,
                                            tint = if (isOutgoing) Color(0xFFA7F3D0) else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (message.senderDisplayName.isNotBlank())
                                                message.senderDisplayName
                                            else
                                                "پست کانال",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isOutgoing) Color(0xFFA7F3D0) else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                } else if (isGroup && !isOutgoing && message.senderDisplayName.isNotBlank()) {
                                    Text(
                                        text = message.senderDisplayName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }

                                // Quoted Reply Block if this message is a reply
                                if (message.replyToPreview.isNotBlank()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 6.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isOutgoing) Color.Black.copy(alpha = 0.18f)
                                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                                            )
                                            .clickable {
                                                if (message.replyToMessageId > 0L) {
                                                    onClickQuotedReply(message.replyToMessageId)
                                                }
                                            }
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height(26.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(
                                                    if (isOutgoing) Color(0xFFA7F3D0)
                                                    else MaterialTheme.colorScheme.primary
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.Reply,
                                                    contentDescription = null,
                                                    tint = if (isOutgoing) Color(0xFFA7F3D0) else MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "پاسخ به پیام",
                                                    fontSize = 10.sp,
                                                    lineHeight = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isOutgoing) Color(0xFFA7F3D0) else MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Text(
                                                text = message.replyToPreview,
                                                fontSize = 11.sp,
                                                lineHeight = 15.sp,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                color = textColor.copy(alpha = 0.88f)
                                            )
                                        }
                                    }
                                }

                                // Chunk Stack Header if multi-segment (>1KB)
                                if (message.fragmentCount > 1 || isIncomplete) {
                                    ChunkStackBanner(
                                        fragmentCount = message.fragmentCount,
                                        isIncomplete = isIncomplete,
                                        isSending = isSending,
                                        statusText = message.rawCiphertext,
                                        expanded = expandedStack,
                                        onToggleExpand = { expandedStack = !expandedStack }
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                }

                                // Message text
                                Text(
                                    text = message.plaintext,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textColor,
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // Footer with SMS delivery / Seen status and time
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isChannel) {
                                        Icon(
                                            imageVector = Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = timeColor,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "$subscriberCount",
                                            fontSize = 9.sp,
                                            color = timeColor,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }

                                    Text(
                                        text = timeStr,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = timeColor,
                                        fontSize = 10.sp
                                    )

                                    Spacer(modifier = Modifier.width(4.dp))

                                    Icon(
                                        imageVector = if (!message.isEncrypted) Icons.Default.Chat else if (isIncomplete) Icons.Default.HourglassTop else Icons.Default.Lock,
                                        contentDescription = if (message.isEncrypted) "رمزگذاری‌شده" else "پیامک عادی",
                                        tint = lockIconTint,
                                        modifier = Modifier.size(11.dp)
                                    )

                                    if (isOutgoing) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        AnimatedContent(
                                            targetState = message.deliveryStatus,
                                            transitionSpec = {
                                                (scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn())
                                                    .togetherWith(scaleOut() + fadeOut())
                                            },
                                            label = "smsStatusAnim"
                                        ) { status ->
                                            when (status) {
                                                DeliveryStatus.QUEUED -> {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = Icons.Default.Schedule,
                                                            contentDescription = "در صف",
                                                            tint = timeColor,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(2.dp))
                                                        Text(
                                                            text = "در صف",
                                                            fontSize = 9.sp,
                                                            color = timeColor
                                                        )
                                                    }
                                                }
                                                DeliveryStatus.SENDING -> {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = Icons.Default.Layers,
                                                            contentDescription = "ارسال قطعات",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(2.dp))
                                                        Text(
                                                            text = "در حال ارسال",
                                                            fontSize = 9.sp,
                                                            color = Color.White
                                                        )
                                                    }
                                                }
                                                DeliveryStatus.SEEN -> {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = Icons.Default.DoneAll,
                                                            contentDescription = "دیده شد",
                                                            tint = Color(0xFFA7F3D0),
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(2.dp))
                                                        Text(
                                                            text = "دیده شد",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFFA7F3D0)
                                                        )
                                                    }
                                                }
                                                DeliveryStatus.SENT_TO_CARRIER,
                                                DeliveryStatus.DELIVERED_CARRIER,
                                                DeliveryStatus.DECRYPTED -> {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = "ارسال شده با پیامک",
                                                            tint = lockIconTint,
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(2.dp))
                                                        Text(
                                                            text = "ارسال شد",
                                                            fontSize = 9.sp,
                                                            color = timeColor
                                                        )
                                                    }
                                                }
                                                DeliveryStatus.FAILED -> {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = Icons.Default.Warning,
                                                            contentDescription = "خطا",
                                                            tint = Color(0xFFFCA5A5),
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(2.dp))
                                                        Text(
                                                            text = "خطا",
                                                            fontSize = 9.sp,
                                                            color = Color(0xFFFCA5A5)
                                                        )
                                                    }
                                                }
                                                else -> {}
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Floating Frosted Reaction Pills below the bubble
                    if (activeReactions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            activeReactions.forEach { emoji ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .ultraGlassSurface(
                                            shape = RoundedCornerShape(50),
                                            cornerRadius = 50.dp,
                                            tintColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                            borderTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                                            shadowElevation = 4.dp
                                        )
                                        .clickable { onReact(emoji) }
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = emoji,
                                        fontSize = 13.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            QUICK_REACTION_EMOJIS.forEach { emoji ->
                                val isSelected = activeReactions.contains(emoji)
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                        )
                                        .border(
                                            width = if (isSelected) 1.2.dp else 0.5.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                            else Color.White.copy(alpha = 0.3f),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            showMenu = false
                                            onReact(emoji)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = emoji,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        DropdownMenuItem(
                            text = { Text("پاسخ به این پیام (Reply)", fontWeight = FontWeight.SemiBold) },
                            onClick = {
                                showMenu = false
                                onReply()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Reply,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("کپی متن پیام") },
                            onClick = {
                                showMenu = false
                                onCopyPlaintext()
                            },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("بررسی بسته رمزگذاری‌شده SMS") },
                            onClick = {
                                showMenu = false
                                onInspectEnvelope()
                            },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("حذف پیام") },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Pure Frosted Glass Chunk Stack representation for multi-segment messages (> 1KB)
 */
@Composable
private fun ChunkStackBanner(
    fragmentCount: Int,
    isIncomplete: Boolean,
    isSending: Boolean,
    statusText: String,
    expanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val accent = if (isIncomplete) Color(0xFFFFB703) else Color(0xFF38BDF8)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.18f))
            .border(0.8.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isIncomplete) "چیدمان قطعات ورودی ($fragmentCount قطعه)"
                        else if (isSending) statusText
                        else "بسته چندقطعه‌ای ($fragmentCount بخش)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }

            if (isIncomplete || isSending) {
                val parsedReceived = parseReceivedCount(statusText, fragmentCount)
                LinearProgressIndicator(
                    progress = { if (fragmentCount > 0) parsedReceived.toFloat() / fragmentCount.toFloat() else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = if (isIncomplete) Color(0xFFFFB703) else Color(0xFF00E5A3),
                    trackColor = Color.White.copy(alpha = 0.15f)
                )
            }

            AnimatedVisibility(visible = expanded || isIncomplete || isSending) {
                Column(modifier = Modifier.padding(top = 6.dp)) {
                    val parsedReceived = parseReceivedCount(statusText, fragmentCount)
                    ChunkStackPills(totalChunks = fragmentCount, sentCount = parsedReceived)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChunkStackPills(totalChunks: Int, sentCount: Int) {
    val displayTotal = Math.min(totalChunks, 40)
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (i in 1..displayTotal) {
            val isDone = i <= sentCount || sentCount == 0
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isDone) Color(0xFF00E5A3).copy(alpha = 0.25f)
                        else Color(0xFFFFB703).copy(alpha = 0.2f)
                    )
                    .border(
                        1.dp,
                        if (isDone) Color(0xFF00E5A3) else Color(0xFFFFB703),
                        RoundedCornerShape(6.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$i",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDone) Color(0xFF00E5A3) else Color(0xFFFFB703)
                )
            }
        }
        if (totalChunks > 40) {
            Text(
                text = "+${totalChunks - 40} قطعه دیگر...",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
    }
}

private fun parseReceivedCount(statusText: String, total: Int): Int {
    return try {
        if (statusText.contains("/")) {
            val parts = statusText.substringAfterLast(": ").substringBefore("/").trim()
            parts.toInt()
        } else if (statusText.contains("قطعه")) {
            val parts = statusText.substringAfter("قطعه ").substringBefore(" ").trim()
            parts.toInt()
        } else {
            total
        }
    } catch (e: Exception) {
        total
    }
}
