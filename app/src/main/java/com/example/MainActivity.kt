package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.ContactSecurityScreen
import com.example.ui.screens.ConversationsScreen
import com.example.ui.screens.DiagnosticsScreen
import com.example.ui.screens.GuidelineDialog
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SecurityDocumentationDialog
import com.example.ui.screens.SecuritySettingsScreen
import com.example.ui.theme.SecureSmsTheme

sealed class Screen(val depth: Int) {
    object Onboarding : Screen(0)
    object Conversations : Screen(1)
    data class Chat(val conversationId: Long) : Screen(2)
    data class ContactSecurity(val conversationId: Long) : Screen(3)
    object SecuritySettings : Screen(2)
    object Diagnostics : Screen(2)
}

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val locale = java.util.Locale("fa")
        java.util.Locale.setDefault(locale)
        val config = android.content.res.Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("securesms_prefs", Context.MODE_PRIVATE)
        val initialDark = prefs.getBoolean("is_dark_theme", false)
        if (initialDark) {
            enableEdgeToEdge(
                statusBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                navigationBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            )
        } else {
            enableEdgeToEdge(
                statusBarStyle = androidx.activity.SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT
                ),
                navigationBarStyle = androidx.activity.SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT
                )
            )
        }
        val hasCompletedOnboarding = prefs.getBoolean("onboarding_complete", false)

        setContent {
            var isDarkTheme by remember {
                mutableStateOf(initialDark) // Light theme by default
            }

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                SecureSmsTheme(darkTheme = isDarkTheme) {
                    com.example.ui.components.GlassAuroraBackground(
                        isDarkTheme = isDarkTheme,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        var currentScreen by remember {
                            mutableStateOf<Screen>(
                                if (hasCompletedOnboarding) Screen.Conversations else Screen.Onboarding
                            )
                        }
                        var showDocumentationDialog by remember { mutableStateOf(false) }
                        var showGuidelineDialog by remember { mutableStateOf(false) }

                        val app = application as SecureSmsApp
                        val repository = app.repository
                        val transportManager = app.smsTransportManager
                        val fragmentationEngine = app.fragmentationEngine
                        val smsProcessor = app.smsProcessor

                        val toggleTheme = {
                            val next = !isDarkTheme
                            isDarkTheme = next
                            prefs.edit().putBoolean("is_dark_theme", next).apply()
                        }

                        val senderFromNotification = intent.getStringExtra("senderPhone")
                        androidx.compose.runtime.LaunchedEffect(senderFromNotification) {
                            if (!senderFromNotification.isNullOrBlank()) {
                                val conv = repository.getConversationByPhone(senderFromNotification)
                                if (conv != null) {
                                    currentScreen = Screen.Chat(conv.id)
                                }
                            }
                        }

                        // Telegram-inspired fluid spring screen transition
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = {
                                val isForward = targetState.depth >= initialState.depth
                                val animDuration = 220
                                val easing = androidx.compose.animation.core.FastOutSlowInEasing
                                if (isForward) {
                                    (slideInHorizontally(animationSpec = tween(animDuration, easing = easing)) { fullWidth -> -fullWidth / 4 } +
                                            fadeIn(animationSpec = tween(animDuration, easing = easing)))
                                        .togetherWith(
                                            slideOutHorizontally(animationSpec = tween(animDuration, easing = easing)) { fullWidth -> fullWidth / 4 } +
                                                    fadeOut(animationSpec = tween(animDuration / 2, easing = easing))
                                        )
                                } else {
                                    (slideInHorizontally(animationSpec = tween(animDuration, easing = easing)) { fullWidth -> fullWidth / 4 } +
                                            fadeIn(animationSpec = tween(animDuration, easing = easing)))
                                        .togetherWith(
                                            slideOutHorizontally(animationSpec = tween(animDuration, easing = easing)) { fullWidth -> -fullWidth / 4 } +
                                                    fadeOut(animationSpec = tween(animDuration / 2, easing = easing))
                                        )
                                }
                            },
                            label = "screenTransition"
                        ) { screen ->
                            when (screen) {
                                is Screen.Onboarding -> {
                                    OnboardingScreen(
                                        onFinished = {
                                            prefs.edit().putBoolean("onboarding_complete", true).apply()
                                            currentScreen = Screen.Conversations
                                        }
                                    )
                                }

                                is Screen.Conversations -> {
                                    ConversationsScreen(
                                        repository = repository,
                                        isDarkTheme = isDarkTheme,
                                        onToggleTheme = toggleTheme,
                                        onOpenConversation = { id ->
                                            currentScreen = Screen.Chat(id)
                                        },
                                        onOpenSettings = {
                                            currentScreen = Screen.SecuritySettings
                                        },
                                        onOpenDiagnostics = {
                                            currentScreen = Screen.Diagnostics
                                        },
                                        onOpenGuideline = {
                                            showGuidelineDialog = true
                                        },
                                        onOpenDocumentation = {
                                            showDocumentationDialog = true
                                        }
                                    )
                                }

                                is Screen.Chat -> {
                                    BackHandler {
                                        currentScreen = Screen.Conversations
                                    }
                                    ChatScreen(
                                        conversationId = screen.conversationId,
                                        repository = repository,
                                        transportManager = transportManager,
                                        onNavigateBack = {
                                            currentScreen = Screen.Conversations
                                        },
                                        onOpenContactSecurity = { id ->
                                            currentScreen = Screen.ContactSecurity(id)
                                        }
                                    )
                                }

                                is Screen.ContactSecurity -> {
                                    BackHandler {
                                        currentScreen = Screen.Chat(screen.conversationId)
                                    }
                                    ContactSecurityScreen(
                                        conversationId = screen.conversationId,
                                        repository = repository,
                                        onNavigateBack = {
                                            currentScreen = Screen.Chat(screen.conversationId)
                                        }
                                    )
                                }

                                is Screen.SecuritySettings -> {
                                    BackHandler {
                                        currentScreen = Screen.Conversations
                                    }
                                    SecuritySettingsScreen(
                                        repository = repository,
                                        isDarkTheme = isDarkTheme,
                                        onToggleTheme = toggleTheme,
                                        onNavigateBack = {
                                            currentScreen = Screen.Conversations
                                        }
                                    )
                                }

                                is Screen.Diagnostics -> {
                                    BackHandler {
                                        currentScreen = Screen.Conversations
                                    }
                                    DiagnosticsScreen(
                                        repository = repository,
                                        fragmentationEngine = fragmentationEngine,
                                        transportManager = transportManager,
                                        smsProcessor = smsProcessor,
                                        onNavigateBack = {
                                            currentScreen = Screen.Conversations
                                        }
                                    )
                                }
                            }
                        }

                        if (showGuidelineDialog) {
                            GuidelineDialog(
                                onDismiss = { showGuidelineDialog = false },
                                onOpenTechnicalDocs = {
                                    showGuidelineDialog = false
                                    showDocumentationDialog = true
                                }
                            )
                        }

                        if (showDocumentationDialog) {
                            SecurityDocumentationDialog(
                                onDismiss = { showDocumentationDialog = false }
                            )
                        }
                    }
                }
            }
        }
    }
}
