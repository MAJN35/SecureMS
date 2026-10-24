package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassCard
import com.example.ui.components.LiquidButton as Button
import com.example.ui.components.LiquidIconButton as IconButton
import com.example.ui.components.LiquidOutlinedButton as OutlinedButton
import com.example.ui.components.glassTextFieldColors
import com.example.ui.components.ultraGlassSurface

data class TweetDraftTemplate(
    val title: String,
    val subtitle: String,
    val text: String
)

val DEFAULT_TWEET_DRAFTS = listOf(
    TweetDraftTemplate(
        title = "رهایی و امید",
        subtitle = "عمیق، روان و با مفهوم اصیل آزادی و حق گفتگو",
        text = "آزادی از جایی آغاز می‌شه که ترسی از شنیده شدن حرف‌هامون نداشته باشیم.\n\nوقتی درها بسته می‌شن و شبکه‌ها خاموش، سکوت تنها انتخاب نیست. این اپلیکیشن رو نوشتم تا حتی در اوج قطعی‌ها، پیام‌های ما امن و دست‌نخورده به هم برسه؛ مستقیم از گوشی به گوشی، بدون واسطه و بدون نیاز به اجازه از کسی.\n\nارتباط، امتیاز اعطایی نیست؛ حق نفس کشیدن و زندگی ماست. 🌱🕊️\n\n#آزادی_بیان #حق_ارتباط #اینترنت_آزاد"
    ),
    TweetDraftTemplate(
        title = "شجاعانه و نسل جدید",
        subtitle = "پرانرژی، صریح و بدون پیچیدگی‌های فنی",
        text = "دیوار کشیدن جلوی حقیقت هیچ‌وقت جواب نداده و نمیده.\n\nهر بار راه‌ها رو بستن، یاد گرفتیم راه‌های جدید بسازیم. برای همین این پیام‌رسان رو توسعه دادم: بدون نیاز به یک ثانیه اینترنت، بدون هیچ سرور واسطه‌ای، کلماتمون با پیامک معمولی اما در کمال امنیت جابه‌جا میشن و هیچ غریبه‌ای بهش راه نداره.\n\nاین سهم من از ایستادن پای حق گفت‌وگو و آزادی ماست ✌️🤍\n\n#آزادی #حق_ارتباط #نه_به_سانسور"
    ),
    TweetDraftTemplate(
        title = "کوتاه و استوار",
        subtitle = "موجز، شاعرانه و ماندگار",
        text = "صدایی که ریشه در حقیقت داشته باشه، پشت هیچ سدی نمی‌مونه.\n\nاپلیکیشنی ساختم برای روزهایی که راه‌ها رو می‌بندن؛ تا پیوندمون به سیم و سوئیچ و فیلتر بند نباشه. پیام‌هایی که در تاریکی هم به مقصد می‌رسند—آزاد، امن، و بی‌واسطه.\n\nروشنایی از همین گفت‌وگوها جوانه می‌زنه 🕊️🌱\n#حق_ارتباط #آزادی_بیان"
    ),
    TweetDraftTemplate(
        title = "حق سخن گفتن",
        subtitle = "بیانیه‌ای قاطع درباره حریم خصوصی و اراده جوانان",
        text = "آزادی یعنی حقِ این‌که بی‌هراس با هم سخن بگوییم.\n\nدر روزگاری که ارتباط را مشروط و تحت نظارت می‌خواهند، راه رهایی در ساختن ابزارهای خودمان است. این پیام‌رسان رو توسعه دادم تا ارتباط انسانی ما در هر شرایطی نفوذناپذیر و امن باقی بمونه.\n\nتقدیم به تمام کسانی که باور دارند آزادی خاموش‌شدنی نیست. 🤍\n#آزادی_بیان #حق_گفتگو"
    )
)

@Composable
fun TweetShareDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var tweetContent by remember {
        mutableStateOf(DEFAULT_TWEET_DRAFTS[0].text)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("tweet_share_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1DA1F2).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = Color(0xFF1DA1F2),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "نشر صدای آزادی (توییت)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "معرفی اپلیکیشن به نسل جوان در X / توییتر",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "بستن")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Draft template selector pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DEFAULT_TWEET_DRAFTS.forEachIndexed { index, draft ->
                        val isSelected = selectedTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .clickable {
                                    selectedTab = index
                                    tweetContent = draft.text
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = draft.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = DEFAULT_TWEET_DRAFTS[selectedTab].subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Editable Tweet Box
                OutlinedTextField(
                    value = tweetContent,
                    onValueChange = { tweetContent = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .testTag("tweet_content_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = glassTextFieldColors(transparentContainer = true)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "طول کاراکتر: ${tweetContent.length}",
                        fontSize = 11.sp,
                        color = if (tweetContent.length > 280) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "می‌تونید متن رو آزادانه ویرایش کنید",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val tweetUrl = "https://twitter.com/intent/tweet?text=" + Uri.encode(tweetContent)
                    val tweetIntent = Intent(Intent.ACTION_VIEW, Uri.parse(tweetUrl)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        context.startActivity(tweetIntent)
                    } catch (e: Exception) {
                        // Fallback to generic share intent
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, tweetContent)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "اشتراک‌گذاری در شبکه اجتماعی"))
                    }
                    onDismiss()
                },
                modifier = Modifier.testTag("confirm_tweet_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DA1F2))
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("توییت در X / توییتر", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(tweetContent))
                    Toast.makeText(context, "متن توییت کپی شد!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.testTag("copy_tweet_button")
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("کپی متن", fontSize = 13.sp)
            }
        }
    )
}
