package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.example.ui.components.LiquidButton as Button
import com.example.ui.components.LiquidOutlinedButton as OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.components.GlassCard
import com.example.ui.components.ultraGlassSurface

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val send = permissions[Manifest.permission.SEND_SMS] == true
        val receive = permissions[Manifest.permission.RECEIVE_SMS] == true
        hasSmsPermission = send && receive
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Pure Vector Liquid Glass Brand Emblem (no solid square background)
        com.example.ui.components.LiquidBrandEmblem(size = 90.dp)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "SecureSMS",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "پیام‌رسان آفلاین با رمزنگاری سرتاسری و پنهان‌سازی فارسی",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Security Pillars
        SecurityFeatureCard(
            icon = Icons.Default.CellTower,
            title = "ارتباط کاملاً آفلاین (SMS Only)",
            description = "بدون اینترنت، سرور، یا ابری. پیام‌ها صرفاً از طریق شبکه سلولار SMS منتقل می‌شوند."
        )

        Spacer(modifier = Modifier.height(12.dp))

        SecurityFeatureCard(
            icon = Icons.Default.EnhancedEncryption,
            title = "رمزنگاری معتبر AES-256-GCM + ECDH",
            description = "کلیدها منحصراً روی گوشی شما در Android Keystore ذخیره می‌شوند و هیچ اپراتوری امکان خواندن متن را ندارد."
        )

        Spacer(modifier = Modifier.height(12.dp))

        SecurityFeatureCard(
            icon = Icons.Default.Language,
            title = "پشتیبانی از قالب حروف فارسی (Base32)",
            description = "متن‌های رمزشده می‌توانند به حروف و الفبای فارسی تبدیل شوند تا شبیه به متون معمولی به نظر برسند."
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Threat Model Limitations Disclaimer (Frosted Amber Glass)
        val isLight = MaterialTheme.colorScheme.background.red > 0.5f
        val warningTint = if (isLight) Color(0xFFFEF3C7) else Color(0xFF78350F)
        val warningTitle = if (isLight) Color(0xFFB45309) else Color(0xFFFFB703)
        val warningBody = if (isLight) Color(0xFF78350F) else Color(0xFFE2E8F0)

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            cornerRadius = 18.dp,
            tintColor = warningTint,
            borderTint = warningTitle.copy(alpha = 0.65f)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Security Note",
                    tint = warningTitle,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "ملاحظات امنیتی و مدل تهدید",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = warningTitle
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "رمزنگاری محتوا را محافظت می‌کند اما فراداده‌ها (شماره فرستنده/گیرنده و زمان ارسال) برای اپراتور قابل مشاهده است. همچنین در صورت سرقت فیزیکی دستگاه قفل‌نشده، محافظت تضمین نمی‌شود.",
                        style = MaterialTheme.typography.bodySmall,
                        color = warningBody,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SMS Permission Button
        if (!hasSmsPermission) {
            OutlinedButton(
                onClick = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.SEND_SMS,
                            Manifest.permission.RECEIVE_SMS,
                            Manifest.permission.READ_CONTACTS
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("request_permission_button"),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("اعطای مجوز پیامک و مخاطبین", fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Enter app button
        Button(
            onClick = onFinished,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("enter_app_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "ورود به محیط امن و ساخت شناسه",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun SecurityFeatureCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        cornerRadius = 18.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .ultraGlassSurface(
                        shape = RoundedCornerShape(12.dp),
                        cornerRadius = 12.dp,
                        tintColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        shadowElevation = 2.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )
            }
        }
    }
}
