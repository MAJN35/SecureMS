package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

data class LiquidNavItem(
    val title: String,
    val icon: ImageVector,
    val badgeCount: Int = 0,
    val testTag: String = ""
)

@Composable
fun LiquidBottomNavigationBar(
    items: List<LiquidNavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val itemBounds = remember { mutableStateMapOf<Int, Pair<Float, Float>>() }
    val targetBounds = itemBounds[selectedIndex]
    val isLight = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val primaryColor = MaterialTheme.colorScheme.primary

    val targetLeft = targetBounds?.first ?: 0f
    val targetRight = targetBounds?.second ?: 0f

    val animatedLeft by animateFloatAsState(
        targetValue = targetLeft,
        animationSpec = spring(
            dampingRatio = 0.68f,
            stiffness = 380f
        ),
        label = "liquidLeft"
    )

    val animatedRight by animateFloatAsState(
        targetValue = targetRight,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = 300f
        ),
        label = "liquidRight"
    )

    val currentWidth = (animatedRight - animatedLeft).coerceAtLeast(0f)
    val restWidth = (targetRight - targetLeft).coerceAtLeast(1f)
    val stretchRatio = (abs(currentWidth - restWidth) / restWidth).coerceIn(0f, 0.35f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .ultraGlassSurface(
                    shape = RoundedCornerShape(30.dp),
                    cornerRadius = 30.dp,
                    shadowElevation = if (isLight) 10.dp else 16.dp,
                    surfaceAlphaMultiplier = 1.04f
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 6.dp, vertical = 6.dp)
                    .drawBehind {
                        if (targetBounds != null && animatedRight > animatedLeft) {
                            val verticalSqueeze = size.height * stretchRatio * 0.22f
                            val pillTop = verticalSqueeze / 2f
                            val pillHeight = (size.height - verticalSqueeze).coerceAtLeast(12f)
                            val pillLeft = animatedLeft.coerceAtMost(animatedRight)
                            val pillWidth = abs(animatedRight - animatedLeft).coerceAtLeast(24f)
                            val cornerRadius = CornerRadius(pillHeight / 2f, pillHeight / 2f)

                            // Core liquid sliding pill
                            drawRoundRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        primaryColor,
                                        if (isLight) Color(0xFF0284C7) else Color(0xFF059669)
                                    ),
                                    start = Offset(pillLeft, pillTop),
                                    end = Offset(pillLeft + pillWidth, pillTop + pillHeight)
                                ),
                                topLeft = Offset(pillLeft, pillTop),
                                size = Size(pillWidth, pillHeight),
                                cornerRadius = cornerRadius
                            )
                        }
                    },
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    val isSelected = selectedIndex == index
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()

                    val iconScale by animateFloatAsState(
                        targetValue = when {
                            isPressed -> 0.92f
                            isSelected -> 1.03f
                            else -> 1.0f
                        },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "iconScale_$index"
                    )

                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "navItemColor_$index"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .onGloballyPositioned { coords ->
                                val pos = coords.positionInParent()
                                itemBounds[index] = Pair(pos.x, pos.x + coords.size.width)
                            }
                            .clip(RoundedCornerShape(24.dp))
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = { onItemSelected(index) }
                            )
                            .padding(horizontal = 10.dp)
                            .let { mod ->
                                if (item.testTag.isNotEmpty()) mod.testTag(item.testTag) else mod
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.scale(iconScale)
                        ) {
                            Box {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = contentColor,
                                    modifier = Modifier.size(20.dp)
                                )

                                if (item.badgeCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEF4444))
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = item.title,
                                color = contentColor,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
