package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.PersianCiphertextCodec
import com.example.data.ConversationEntity
import com.example.data.ConversationType
import com.example.data.EncryptedStorageRepository
import com.example.data.VerificationStatus
import com.example.ui.components.AvatarPickerDialog
import com.example.ui.components.ContactAvatar
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassHeaderSurface
import com.example.ui.components.LiquidBottomNavigationBar
import com.example.ui.components.LiquidBrandEmblem
import com.example.ui.components.LiquidButton as Button
import com.example.ui.components.LiquidFloatingActionButton as FloatingActionButton
import com.example.ui.components.LiquidIconButton as IconButton
import com.example.ui.components.LiquidNavItem
import com.example.ui.components.LiquidTextButton as TextButton
import com.example.ui.components.glassTextFieldColors
import com.example.ui.components.ultraGlassSurface
import com.example.util.ContactHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class ConversationFilter(val labelFa: String) {
    ALL("همه"),
    DIRECT("شخصی"),
    GROUPS("گروه‌ها"),
    CHANNELS("کانال‌ها")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(
    repository: EncryptedStorageRepository,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onOpenConversation: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenGuideline: () -> Unit,
    onOpenDocumentation: () -> Unit
) {
    val conversationsFlow = remember(repository) { repository.getAllConversationsFlow() }
    val conversations by conversationsFlow.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    var showNewChatDialog by remember { mutableStateOf(false) }
    var initialCreationMode by remember { mutableStateOf(ConversationType.DIRECT) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(ConversationFilter.ALL) }
    var selectedBottomTab by remember { mutableIntStateOf(0) } // 0: Chats, 1: Contacts, 2: Settings
    var avatarEditingConversation by remember { mutableStateOf<ConversationEntity?>(null) }

    BackHandler(enabled = selectedBottomTab != 0) {
        selectedBottomTab = 0
    }

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            repository.syncContactNames(context)
        }
    }

    val filteredList = remember(conversations, searchQuery, selectedFilter) {
        conversations
            .filter { conv ->
                when (selectedFilter) {
                    ConversationFilter.ALL -> true
                    ConversationFilter.DIRECT -> conv.isDirect
                    ConversationFilter.GROUPS -> conv.isGroup
                    ConversationFilter.CHANNELS -> conv.isChannel
                }
            }
            .filter { conv ->
                if (searchQuery.isBlank()) true
                else {
                    conv.contactName.contains(searchQuery, ignoreCase = true) ||
                            conv.phoneNumber.contains(searchQuery) ||
                            conv.description.contains(searchQuery, ignoreCase = true) ||
                            conv.memberNames.contains(searchQuery, ignoreCase = true)
                }
            }
    }

    val groupCount = remember(conversations) { conversations.count { it.isGroup } }
    val channelCount = remember(conversations) { conversations.count { it.isChannel } }
    val directCount = remember(conversations) { conversations.count { it.isDirect } }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            GlassHeaderSurface(
                modifier = Modifier.fillMaxWidth()
            ) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LiquidBrandEmblem(size = 36.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                AnimatedContent(
                                    targetState = when (selectedBottomTab) {
                                        0 -> "پیام‌ها"
                                        1 -> "مخاطبین"
                                        else -> "تنظیمات"
                                    },
                                    label = "topBarTitleAnim"
                                ) { titleText ->
                                    Text(
                                        text = titleText,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        lineHeight = 20.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "رمزنگاری سرتاسری آفلاین • SMS / E2EE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = onToggleTheme,
                            modifier = Modifier.testTag("theme_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = if (isDarkTheme) "حالت روشن" else "حالت تاریک",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = onOpenGuideline,
                            modifier = Modifier.testTag("guideline_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = "راهنمای استفاده",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = onOpenDiagnostics,
                            modifier = Modifier.testTag("diagnostics_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = "آزمایشگاه",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.testTag("settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "تنظیمات",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        },
        bottomBar = {
            val totalUnread = remember(conversations) { conversations.sumOf { it.unreadCount } }
            val navItems = remember(totalUnread) {
                listOf(
                    LiquidNavItem(
                        title = "گفتگوها",
                        icon = Icons.Default.Chat,
                        badgeCount = totalUnread,
                        testTag = "nav_tab_chats"
                    ),
                    LiquidNavItem(
                        title = "مخاطبین",
                        icon = Icons.Default.ContactPhone,
                        testTag = "nav_tab_contacts"
                    ),
                    LiquidNavItem(
                        title = "تنظیمات",
                        icon = Icons.Default.Settings,
                        testTag = "nav_tab_settings"
                    )
                )
            }
            LiquidBottomNavigationBar(
                items = navItems,
                selectedIndex = selectedBottomTab,
                onItemSelected = { index ->
                    selectedBottomTab = index
                }
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = selectedBottomTab == 0,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(),
                exit = scaleOut(
                    animationSpec = spring(stiffness = Spring.StiffnessMedium)
                ) + fadeOut()
            ) {
                FloatingActionButton(
                    onClick = {
                        initialCreationMode = when (selectedFilter) {
                            ConversationFilter.GROUPS -> ConversationType.GROUP
                            ConversationFilter.CHANNELS -> ConversationType.CHANNEL
                            else -> ConversationType.DIRECT
                        }
                        showNewChatDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier.testTag("new_chat_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "گفتگو، گروه یا کانال جدید")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = selectedBottomTab,
                transitionSpec = {
                    val isForward = targetState > initialState
                    val animDuration = 190
                    val easing = androidx.compose.animation.core.FastOutSlowInEasing
                    if (isForward) {
                        (slideInHorizontally(animationSpec = tween(animDuration, easing = easing)) { width -> -width / 4 } +
                                fadeIn(animationSpec = tween(animDuration, easing = easing)))
                            .togetherWith(
                                slideOutHorizontally(animationSpec = tween(animDuration, easing = easing)) { width -> width / 4 } +
                                        fadeOut(animationSpec = tween(animDuration / 2, easing = easing))
                            )
                    } else {
                        (slideInHorizontally(animationSpec = tween(animDuration, easing = easing)) { width -> width / 4 } +
                                fadeIn(animationSpec = tween(animDuration, easing = easing)))
                            .togetherWith(
                                slideOutHorizontally(animationSpec = tween(animDuration, easing = easing)) { width -> -width / 4 } +
                                        fadeOut(animationSpec = tween(animDuration / 2, easing = easing))
                            )
                    }
                },
                label = "bottomTabContentTransition"
            ) { currentTab ->
                when (currentTab) {
                    1 -> {
                        ContactsScreen(
                            repository = repository,
                            onSelectContact = onOpenConversation,
                            onManualNewChat = {
                                initialCreationMode = ConversationType.DIRECT
                                showNewChatDialog = true
                            },
                            onCreateGroup = {
                                initialCreationMode = ConversationType.GROUP
                                showNewChatDialog = true
                            },
                            onCreateChannel = {
                                initialCreationMode = ConversationType.CHANNEL
                                showNewChatDialog = true
                            }
                        )
                    }
                    2 -> {
                        SecuritySettingsScreen(
                            repository = repository,
                            isDarkTheme = isDarkTheme,
                            onToggleTheme = onToggleTheme,
                            onNavigateBack = { selectedBottomTab = 0 },
                            showTopBar = false
                        )
                    }
                    else -> {
                        // Chats Tab
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Clean Executive Search Bar
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .ultraGlassSurface(
                                        shape = RoundedCornerShape(16.dp),
                                        cornerRadius = 16.dp,
                                        shadowElevation = 4.dp
                                    )
                                    .testTag("search_conversations_input"),
                                placeholder = { Text("جستجو در گفتگوها، گروه‌ها و کانال‌ها...", fontSize = 13.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "پاک کردن جستجو",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = glassTextFieldColors(transparentContainer = true)
                            )

                            // Liquid Category Filter Pills + Quick Group/Channel Creation Pills
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ConversationFilter.entries.forEach { filter ->
                                    val isSelected = selectedFilter == filter
                                    val count = when (filter) {
                                        ConversationFilter.ALL -> conversations.size
                                        ConversationFilter.DIRECT -> directCount
                                        ConversationFilter.GROUPS -> groupCount
                                        ConversationFilter.CHANNELS -> channelCount
                                    }
                                    val pillIcon = when (filter) {
                                        ConversationFilter.ALL -> Icons.Default.Chat
                                        ConversationFilter.DIRECT -> Icons.Default.Person
                                        ConversationFilter.GROUPS -> Icons.Default.Groups
                                        ConversationFilter.CHANNELS -> Icons.Default.Campaign
                                    }
                                    LiquidFilterPill(
                                        label = if (count > 0) "${filter.labelFa} ($count)" else filter.labelFa,
                                        icon = pillIcon,
                                        selected = isSelected,
                                        onClick = { selectedFilter = filter },
                                        testTag = "filter_${filter.name.lowercase()}"
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                // Quick Create Group Pill
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .ultraGlassSurface(
                                            shape = RoundedCornerShape(50),
                                            cornerRadius = 50.dp,
                                            tintColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f),
                                            borderTint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.55f)
                                        )
                                        .clickable {
                                            initialCreationMode = ConversationType.GROUP
                                            showNewChatDialog = true
                                        }
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                        .testTag("quick_new_group_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.GroupAdd,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "+ گروه جدید",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                // Quick Create Channel Pill
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .ultraGlassSurface(
                                            shape = RoundedCornerShape(50),
                                            cornerRadius = 50.dp,
                                            tintColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f),
                                            borderTint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f)
                                        )
                                        .clickable {
                                            initialCreationMode = ConversationType.CHANNEL
                                            showNewChatDialog = true
                                        }
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                        .testTag("quick_new_channel_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Campaign,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "+ کانال جدید",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            if (filteredList.isEmpty()) {
                                // Frosted Glass Empty State
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
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
                                                .padding(26.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(68.dp)
                                                    .ultraGlassSurface(
                                                        shape = CircleShape,
                                                        cornerRadius = 34.dp,
                                                        tintColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                                        shadowElevation = 4.dp
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = when (selectedFilter) {
                                                        ConversationFilter.GROUPS -> Icons.Default.Groups
                                                        ConversationFilter.CHANNELS -> Icons.Default.Campaign
                                                        else -> Icons.Default.Lock
                                                    },
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(32.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(14.dp))
                                            Text(
                                                text = when (selectedFilter) {
                                                    ConversationFilter.GROUPS -> "هنوز گروهی ایجاد نشده است"
                                                    ConversationFilter.CHANNELS -> "هنوز کانالی ایجاد نشده است"
                                                    else -> "گفتگویی وجود ندارد"
                                                },
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = when (selectedFilter) {
                                                    ConversationFilter.GROUPS ->
                                                        "با ساخت گروه امن، پیام‌های شما به صورت همزمان برای تمام اعضای گروه رمزگذاری و ارسال می‌شود."
                                                    ConversationFilter.CHANNELS ->
                                                        "با ساخت کانال پیامکی، اطلاعیه‌ها و پیام‌های خود را به صورت یک‌طرفه برای مشترکین پخش کنید."
                                                    else ->
                                                        "یک گفتگوی شخصی، گروه امن یا کانال پیامکی جدید ایجاد کنید."
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(18.dp))
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Button(
                                                    onClick = {
                                                        initialCreationMode = when (selectedFilter) {
                                                            ConversationFilter.GROUPS -> ConversationType.GROUP
                                                            ConversationFilter.CHANNELS -> ConversationType.CHANNEL
                                                            else -> ConversationType.DIRECT
                                                        }
                                                        showNewChatDialog = true
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.primary,
                                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                                    ),
                                                    shape = RoundedCornerShape(14.dp)
                                                ) {
                                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = when (selectedFilter) {
                                                            ConversationFilter.GROUPS -> "ایجاد گروه امن"
                                                            ConversationFilter.CHANNELS -> "ایجاد کانال جدید"
                                                            else -> "ایجاد گفتگو / گروه"
                                                        },
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(filteredList, key = { it.id }) { conversation ->
                                        ConversationItem(
                                            conversation = conversation,
                                            onClick = { onOpenConversation(conversation.id) },
                                            onAvatarClick = { avatarEditingConversation = conversation },
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
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewChatDialog) {
        NewConversationDialog(
            repository = repository,
            initialType = initialCreationMode,
            onDismiss = { showNewChatDialog = false },
            onConversationCreated = { newId ->
                showNewChatDialog = false
                onOpenConversation(newId)
            }
        )
    }

    avatarEditingConversation?.let { targetConv ->
        AvatarPickerDialog(
            contactName = targetConv.contactName,
            phoneNumber = targetConv.phoneNumber,
            currentAvatarUri = targetConv.avatarUri,
            isGroup = targetConv.isGroup,
            isChannel = targetConv.isChannel,
            onAvatarSelected = { newUri ->
                coroutineScope.launch(Dispatchers.IO) {
                    repository.updateConversationAvatar(targetConv.id, newUri)
                }
            },
            onDismiss = { avatarEditingConversation = null }
        )
    }
}

@Composable
private fun LiquidFilterPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.93f
            selected -> 1.03f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "filterPillScale"
    )
    val tintColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
        } else {
            Color.Unspecified
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "filterPillTint"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(50))
            .ultraGlassSurface(
                shape = RoundedCornerShape(50),
                cornerRadius = 50.dp,
                tintColor = tintColor,
                borderTint = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.75f) else Color.Unspecified
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 13.dp, vertical = 7.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ConversationItem(
    conversation: ConversationEntity,
    onClick: () -> Unit,
    onAvatarClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeStr = remember(conversation.lastMessageTimestamp) {
        dateFormat.format(Date(conversation.lastMessageTimestamp))
    }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "conversationItemScale"
    )

    val isLight = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val verifiedColor = if (isLight) Color(0xFF059669) else Color(0xFF00E5A3)
    val warningColor = if (isLight) Color(0xFFDC2626) else Color(0xFFEF476F)
    val groupColor = if (isLight) Color(0xFF6366F1) else Color(0xFF818CF8)
    val channelColor = if (isLight) Color(0xFF0284C7) else Color(0xFF38BDF8)

    val avatarAccentColor = when {
        conversation.isGroup -> groupColor
        conversation.isChannel -> channelColor
        conversation.verificationStatus == VerificationStatus.VERIFIED -> verifiedColor
        conversation.verificationStatus == VerificationStatus.IDENTITY_CHANGED -> warningColor
        else -> MaterialTheme.colorScheme.primary
    }

    val memberCount = remember(conversation.memberPhones) {
        conversation.parsedMemberPhones().size
    }

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .scale(cardScale)
            .testTag("conversation_item_${conversation.id}"),
        shape = RoundedCornerShape(16.dp),
        cornerRadius = 16.dp,
        borderTint = when {
            conversation.unreadCount > 0 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            else -> Color.Unspecified
        },
        shadowElevation = if (isLight) 4.dp else 8.dp,
        onClick = onClick,
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Clean Avatar with corner verification / type indicator dot (tappable to change profile picture)
            Box(modifier = Modifier.size(50.dp)) {
                ContactAvatar(
                    name = conversation.contactName,
                    avatarUri = conversation.avatarUri,
                    size = 48.dp,
                    accentColor = avatarAccentColor,
                    isGroup = conversation.isGroup,
                    isChannel = conversation.isChannel,
                    onClick = onAvatarClick,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .testTag("conversation_avatar_${conversation.id}")
                )

                // Corner security status badge
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.BottomStart)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, avatarAccentColor.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            conversation.verificationStatus == VerificationStatus.VERIFIED -> Icons.Default.VerifiedUser
                            conversation.verificationStatus == VerificationStatus.IDENTITY_CHANGED -> Icons.Default.Warning
                            else -> Icons.Default.Lock
                        },
                        contentDescription = null,
                        tint = when (conversation.verificationStatus) {
                            VerificationStatus.VERIFIED -> verifiedColor
                            VerificationStatus.IDENTITY_CHANGED -> warningColor
                            else -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(11.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = conversation.contactName,
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 15.sp,
                            fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        when {
                            conversation.isGroup -> {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(groupColor.copy(alpha = 0.12f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "گروه • $memberCount",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        lineHeight = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false,
                                        color = groupColor
                                    )
                                }
                            }
                            conversation.isChannel -> {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(channelColor.copy(alpha = 0.12f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "کانال • $memberCount",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        lineHeight = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false,
                                        color = channelColor
                                    )
                                }
                            }
                            conversation.verificationStatus == VerificationStatus.VERIFIED -> {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(verifiedColor.copy(alpha = 0.12f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "تأیید شده",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        lineHeight = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false,
                                        color = verifiedColor
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (conversation.unreadCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (conversation.lastMessageText.isNotBlank()) conversation.lastMessageText else "گفتگوی امن آماده ارسال پیام...",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 13.sp,
                        fontWeight = if (conversation.unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                        color = if (conversation.unreadCount > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (conversation.unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                            Text(
                                text = "${conversation.unreadCount}",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun NewConversationDialog(
    repository: EncryptedStorageRepository,
    initialType: ConversationType = ConversationType.DIRECT,
    onDismiss: () -> Unit,
    onConversationCreated: (Long) -> Unit
) {
    val context = LocalContext.current
    var selectedType by remember { mutableStateOf(initialType) }
    var phoneNumber by remember { mutableStateOf("") }
    var titleOrName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedAvatarUri by remember { mutableStateOf("") }
    var showAvatarPicker by remember { mutableStateOf(false) }
    var memberPhoneInput by remember { mutableStateOf("") }
    var memberNameInput by remember { mutableStateOf("") }
    val addedMembers = remember {
        mutableStateListOf(
            "09121112233" to "امیرحسین رضایی",
            "09354445566" to "سارا محمدی"
        )
    }

    var selectedEncoding by remember { mutableStateOf(PersianCiphertextCodec.EncodingMode.PERSIAN_LETTERS) }
    var encodingMenuExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (selectedType) {
                    ConversationType.DIRECT -> "گفتگوی شخصی جدید"
                    ConversationType.GROUP -> "ایجاد گروه امن پیامکی"
                    ConversationType.CHANNEL -> "ایجاد کانال پخش پیامکی"
                },
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Mode Selector Pills (Direct / Group / Channel)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        Triple(ConversationType.DIRECT, "شخصی", Icons.Default.Person),
                        Triple(ConversationType.GROUP, "گروه", Icons.Default.Groups),
                        Triple(ConversationType.CHANNEL, "کانال", Icons.Default.Campaign)
                    ).forEach { (type, label, icon) ->
                        val active = selectedType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .ultraGlassSurface(
                                    shape = RoundedCornerShape(14.dp),
                                    cornerRadius = 14.dp,
                                    tintColor = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color.Unspecified,
                                    borderTint = if (active) MaterialTheme.colorScheme.primary else Color.Unspecified
                                )
                                .clickable {
                                    selectedType = type
                                    errorMessage = null
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Profile Picture selector for new contact / group / channel
                ContactAvatar(
                    name = titleOrName.ifBlank { phoneNumber.ifBlank { "؟" } },
                    avatarUri = selectedAvatarUri,
                    size = 64.dp,
                    isGroup = selectedType == ConversationType.GROUP,
                    isChannel = selectedType == ConversationType.CHANNEL,
                    showCameraBadge = true,
                    onClick = { showAvatarPicker = true }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "انتخاب عکس پروفایل یا دریافت از مخاطبین",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { showAvatarPicker = true }
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (selectedType == ConversationType.DIRECT) {
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("شماره همراه") },
                        placeholder = { Text("09xxxxxxxxx") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_phone_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = glassTextFieldColors()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = titleOrName,
                        onValueChange = { titleOrName = it },
                        label = { Text("نام مخاطب (اختیاری)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = glassTextFieldColors()
                    )
                } else {
                    OutlinedTextField(
                        value = titleOrName,
                        onValueChange = { titleOrName = it },
                        label = {
                            Text(if (selectedType == ConversationType.GROUP) "نام گروه امن" else "نام کانال پیامکی")
                        },
                        placeholder = {
                            Text(if (selectedType == ConversationType.GROUP) "مثلاً: گروه خانواده / تیم فنی" else "مثلاً: کانال اطلاعیه‌های اضطراری")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_group_title_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = glassTextFieldColors()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("توضیحات (اختیاری)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        shape = RoundedCornerShape(16.dp),
                        colors = glassTextFieldColors()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (selectedType == ConversationType.GROUP) "اعضای گروه (${addedMembers.size} نفر):"
                        else "مشترکین کانال (${addedMembers.size} نفر):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Add member inline row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = memberNameInput,
                            onValueChange = { memberNameInput = it },
                            placeholder = { Text("نام عضو", fontSize = 11.sp) },
                            modifier = Modifier.weight(0.9f),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = glassTextFieldColors()
                        )
                        OutlinedTextField(
                            value = memberPhoneInput,
                            onValueChange = { memberPhoneInput = it },
                            placeholder = { Text("0912...", fontSize = 11.sp) },
                            modifier = Modifier.weight(1.1f),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = glassTextFieldColors()
                        )
                        IconButton(
                            onClick = {
                                val cleanPhone = memberPhoneInput.trim()
                                if (cleanPhone.isNotBlank()) {
                                    val cleanName = memberNameInput.trim().ifBlank { cleanPhone }
                                    if (addedMembers.none { it.first == cleanPhone }) {
                                        addedMembers.add(cleanPhone to cleanName)
                                    }
                                    memberPhoneInput = ""
                                    memberNameInput = ""
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = "افزودن عضو",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Member chips
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        addedMembers.forEach { (phone, name) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .ultraGlassSurface(
                                        shape = RoundedCornerShape(50),
                                        cornerRadius = 50.dp,
                                        tintColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                    )
                                    .clickable {
                                        addedMembers.removeAll { it.first == phone }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "$name ($phone)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "حذف",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Friendly encoding format selector
                ExposedDropdownMenuBox(
                    expanded = encodingMenuExpanded,
                    onExpandedChange = { encodingMenuExpanded = !encodingMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedEncoding.titleFa,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("قالب رمزنگاری پیامک") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = encodingMenuExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(16.dp),
                        colors = glassTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = encodingMenuExpanded,
                        onDismissRequest = { encodingMenuExpanded = false }
                    ) {
                        PersianCiphertextCodec.EncodingMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text("${mode.titleFa} (${mode.titleEn})") },
                                onClick = {
                                    selectedEncoding = mode
                                    encodingMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            if (selectedType == ConversationType.DIRECT) {
                                if (phoneNumber.isBlank()) {
                                    withContext(Dispatchers.Main) {
                                        errorMessage = "شماره همراه الزامی است"
                                    }
                                    return@launch
                                }
                                val finalAvatar = selectedAvatarUri.ifBlank {
                                    ContactHelper.getContactPhotoUriByPhone(context, phoneNumber.trim()) ?: ""
                                }
                                val conv = repository.createOrUpdateConversation(
                                    phoneNumber = phoneNumber.trim(),
                                    contactName = titleOrName.ifBlank { phoneNumber.trim() },
                                    theirPublicKey = null,
                                    preferredEncoding = selectedEncoding.name,
                                    avatarUri = finalAvatar
                                )
                                withContext(Dispatchers.Main) {
                                    onConversationCreated(conv.id)
                                }
                            } else {
                                val finalTitle = titleOrName.trim().ifBlank {
                                    if (selectedType == ConversationType.GROUP) "گروه امن جدید" else "کانال پیامکی جدید"
                                }
                                // Include any pending typed member
                                if (memberPhoneInput.isNotBlank()) {
                                    val cleanP = memberPhoneInput.trim()
                                    val cleanN = memberNameInput.trim().ifBlank { cleanP }
                                    if (addedMembers.none { it.first == cleanP }) {
                                        addedMembers.add(cleanP to cleanN)
                                    }
                                }
                                val conv = repository.createGroupOrChannelConversation(
                                    title = finalTitle,
                                    type = selectedType,
                                    members = addedMembers.toList(),
                                    description = description.trim(),
                                    preferredEncoding = selectedEncoding.name,
                                    avatarUri = selectedAvatarUri
                                )
                                withContext(Dispatchers.Main) {
                                    onConversationCreated(conv.id)
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                errorMessage = "خطا در ایجاد گفتگو: ${e.message}"
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("dialog_confirm_button")
            ) {
                Text(
                    text = when (selectedType) {
                        ConversationType.DIRECT -> "شروع گفتگو"
                        ConversationType.GROUP -> "ایجاد گروه"
                        ConversationType.CHANNEL -> "ایجاد کانال"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("لغو")
            }
        }
    )

    if (showAvatarPicker) {
        AvatarPickerDialog(
            contactName = titleOrName.ifBlank { phoneNumber.ifBlank { "مخاطب جدید" } },
            phoneNumber = phoneNumber,
            currentAvatarUri = selectedAvatarUri,
            isGroup = selectedType == ConversationType.GROUP,
            isChannel = selectedType == ConversationType.CHANNEL,
            onAvatarSelected = { uri -> selectedAvatarUri = uri },
            onDismiss = { showAvatarPicker = false }
        )
    }
}
