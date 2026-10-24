package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import com.example.ui.components.LiquidButton as Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SecurityDocumentationDialog(
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("مدل تهدید", "معماری رمزنگاری", "پروتکل پیامک", "حریم خصوصی")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF00E5A3))
                Spacer(modifier = Modifier.width(8.dp))
                Text("مستندات معماری امنیتی SecureSMS", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
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
                            text = { Text(title, fontSize = 11.sp, maxLines = 1) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (selectedTab) {
                        0 -> ThreatModelContent()
                        1 -> CryptoArchitectureContent()
                        2 -> SmsProtocolContent()
                        3 -> PrivacyPolicyContent()
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
                Text("بستن", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun ThreatModelContent() {
    Column {
        Text("حفاظت‌های ارائه شده توسط سیستم:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
        Text("• جلوگیری از خواندن محتوای پیام توسط اپراتورهای همراه، گیت‌وی‌ها و ناظران شبکه.", fontSize = 12.sp)
        Text("• رمزگذاری داده‌ها در حافظه محلی با کلید سخت‌افزاری Android Keystore.", fontSize = 12.sp)
        Text("• جلوگیری از دستکاری، جعل، تزریق پیام و حملات بازپخش (Replay Attacks) با استفاده از تگ AEAD و شمارنده صعودی توالی.", fontSize = 12.sp)
        Text("• کشف حملات مرد میانی (MITM) از طریق کدهای امنیتی ۶۰ رقمی و هشدارهای تغییر کلید هویتی.", fontSize = 12.sp)

        Spacer(modifier = Modifier.height(12.dp))

        com.example.ui.components.GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            cornerRadius = 14.dp,
            tintColor = Color(0xFFFFB703).copy(alpha = 0.16f),
            borderTint = Color(0xFFFFB703).copy(alpha = 0.50f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تهدیداتی که رمزگذاری پیامک نمی‌تواند ممانعت کند:", fontWeight = FontWeight.Bold, color = Color(0xFFD97706), fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("• تحلیل ترافیک (Traffic Analysis): شماره تلفن‌ها، زمان و حجم پیامک‌ها برای اپراتور مخابراتی قابل مشاهده است.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("• تعویض سیم‌کارت (SIM Swap): بازپس‌گیری شماره توسط افراد غیرمجاز.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("• سیستم‌عامل آلوده به کی‌لاگر یا دسترسی فیزیکی مستقیم به دستگاه قفل‌نشده.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("• فیلترینگ یا حذف انتخابی پیامک‌ها توسط اپراتور (Denial of Service).", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun CryptoArchitectureContent() {
    Column {
        Text("مشخصات استانداردهای رمزنگاری:", fontWeight = FontWeight.Bold, color = Color(0xFF00E5A3), fontSize = 13.sp)
        Text("• منحنی بیضوی: NIST P-256 (secp256r1) پشتیبانی‌شده در لایه بومی تمامی نسخه‌های اندروید.", fontSize = 12.sp)
        Text("• توافق کلید: ECDH (Elliptic Curve Diffie-Hellman) با تولید محلی روی دستگاه.", fontSize = 12.sp)
        Text("• استخراج کلید: تابع توسعه HKDF-SHA256 طبق RFC 5869 با داده‌های وابسته به نشست.", fontSize = 12.sp)
        Text("• رمزنگاری متقارن اصالت‌سنجی‌شده: AES-256-GCM با بردار اولیه (IV) ۱۲ بایتی تصادفی و تگ احراز هویت ۱۲۸ بیتی.", fontSize = 12.sp)
        Text("• داده‌های احراز هویت اضافی (AAD): اتصال مستقیم شناسه فرستنده، گیرنده، نسخه و شماره توالی به تگ اصالت‌سنجی جهت ممانعت از تغییر زمینه پیام.", fontSize = 12.sp)
        Text("• ذخیره‌سازی کلیدها: کلید ارشد در سخت‌افزار امن AndroidKeyStore تولید و نگهداری می‌شود.", fontSize = 12.sp)
    }
}

@Composable
private fun SmsProtocolContent() {
    Column {
        Text("طراحی فریم بسته‌های پیامکی (Wire Protocol):", fontWeight = FontWeight.Bold, color = Color(0xFF00E5A3), fontSize = 13.sp)
        Text("• سرآیند ۲۹ بایتی شامل: مجیک بایت‌ها (SS)، نسخه (1)، نوع پیام (DATA/KEY)، شناسه پیام، شاخص و تعداد کل قطعات، هش ۸ بایتی کلید فرستنده و گیرنده، و شماره توالی.", fontSize = 12.sp)
        Text("• قطعه‌بندی تطبیقی (Adaptive Fragmentation): شکستن خودکار محتوای رمزشده به اندازه متناسب با گنجایش پیامک (حدود ۶۵ کاراکتر در حالت الفبای فارسی جهت انطباق با UCS-2).", fontSize = 12.sp)
        Text("• مدیریت قطعات نامرتب و تکراری با مهلت انقضای ۲۴ ساعته جهت جلوگیری از پر شدن حافظه.", fontSize = 12.sp)
        Text("• حالت حروف فارسی: تبدیل دقیق ۵ بیت به ۵ بیت روی ۳۲ حرف فارسی با نرمال‌سازی خودکار حروف عربی (کاف و یا) برای خنثی‌سازی تغییرات کیبورد و اپراتور.", fontSize = 12.sp)
    }
}

@Composable
private fun PrivacyPolicyContent() {
    Column {
        Text("بیانیه شفافیت و حریم خصوصی:", fontWeight = FontWeight.Bold, color = Color(0xFF00E5A3), fontSize = 13.sp)
        Text("• هیچ‌گونه دسترسی به اینترنت در مانیفست برنامه درخواست نشده است (مجوز INTERNET وجود ندارد).", fontSize = 12.sp)
        Text("• هیچ تلمتری، آمار، گزارش خرابی یا داده‌ای به هیچ سرور خارجی یا ابری ارسال نمی‌شود.", fontSize = 12.sp)
        Text("• دفترچه مخاطبین صرفاً در صورت اعطای مجوز توسط کاربر و منحصراً بر روی گوشی به کار گرفته می‌شود.", fontSize = 12.sp)
        Text("• متن پیام‌ها در حافظه پایگاه داده با استاندارد رمزنگاری محافظت می‌شود.", fontSize = 12.sp)
    }
}
