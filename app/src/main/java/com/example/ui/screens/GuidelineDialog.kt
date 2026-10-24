package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import com.example.ui.components.LiquidButton as Button
import com.example.ui.components.LiquidOutlinedButton as OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassCard

@Composable
fun GuidelineDialog(
    onDismiss: () -> Unit,
    onOpenTechnicalDocs: (() -> Unit)? = null
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("راهنمای سریع", "نکات کلیدی")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "راهنمای استفاده",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (selectedTab) {
                        0 -> QuickStartGuide()
                        1 -> KeyTipsGuide()
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("متوجه شدم", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            if (onOpenTechnicalDocs != null) {
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onOpenTechnicalDocs()
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("جزئیات فنی", fontSize = 12.sp)
                }
            }
        }
    )
}

@Composable
private fun QuickStartGuide() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        GuideStepItem(
            stepNumber = "۱",
            icon = Icons.Default.Key,
            title = "تبادل امن کلید با مخاطب",
            description = "کلید عمومی خود را با کد تصویری (QR)، تلگرام، بلوتوث یا سایر برنامه‌ها ارسال کنید و کلید مخاطب را جای‌گذاری نمایید (بدون ارسال با پیامک)."
        )

        GuideStepItem(
            stepNumber = "۲",
            icon = Icons.Default.Lock,
            title = "ارسال پیام رمزگذاری شده",
            description = "متن را بنویسید و ارسال کنید. پیام با رمزنگاری استاندارد AES-256 و کلید مشتق‌شده قفل و ارسال می‌شود."
        )

        GuideStepItem(
            stepNumber = "۳",
            icon = Icons.Default.Layers,
            title = "پیام‌های طولانی",
            description = "پیام‌های بالای ۱ کیلوبایت خودکار قطعه‌بندی و در گوشی مقابل دوباره سرهم می‌شوند."
        )

        GuideStepItem(
            stepNumber = "۴",
            icon = Icons.Default.CloudOff,
            title = "کاملاً آفلاین و ایمن",
            description = "بدون اینترنت و بدون ارسال کلید در شبکه پیامک. کلیدها هرگز از طریق پیامک ارسال نمی‌شوند."
        )
    }
}

@Composable
private fun KeyTipsGuide() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        GuideTipCard(
            icon = Icons.Default.Security,
            title = "اشتراک با تلگرام، بلوتوث و QR",
            description = "می‌توانید کلید عمومی را به صورت مستقیم به تلگرام، با انتقال فایل بلوتوث یا تصویر کد QR با مخاطبین به اشتراک بگذارید."
        )

        GuideTipCard(
            icon = Icons.Default.CheckCircle,
            title = "تولید مجدد کلید امنیتی",
            description = "هر کاربر در هر زمان می‌تواند از بخش تنظیمات یا چت، کلید اختصاصی جدیدی برای خود تولید و جایگزین کند."
        )

        GuideTipCard(
            icon = Icons.Default.Chat,
            title = "پیامک عادی یا امن",
            description = "برای مخاطبانی که هنوز کلید تبادل نکرده‌اید، امکان ارسال پیامک عادی (ساده) نیز فراهم است."
        )

        GuideTipCard(
            icon = Icons.Default.Lock,
            title = "حفظ حریم خصوصی و امنیت محلی",
            description = "پیام‌ها و کلیدهای شما صرفاً به شکل رمزگذاری‌شده بر روی همین دستگاه نگهداری می‌شوند و هیچ اطلاعاتی به سرورهای ابری ارسال نخواهد شد."
        )
    }
}

@Composable
private fun GuideStepItem(
    stepNumber: String,
    icon: ImageVector,
    title: String,
    description: String
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        cornerRadius = 14.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun GuideTipCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        cornerRadius = 14.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
