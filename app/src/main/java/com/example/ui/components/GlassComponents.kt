package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Refined architectural ambient backdrop that provides subtle depth for frosted glass surfaces
 * while maintaining executive readability and high contrast in both Light and Dark modes.
 */
@Composable
fun GlassAuroraBackground(
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "auroraMotion")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "auroraPhase"
    )

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val dx = sin(phase) * w * 0.04f
            val dy = cos(phase) * h * 0.03f

            // 1. Base architectural canvas gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = if (isDarkTheme) {
                        listOf(
                            Color(0xFF080D18),
                            Color(0xFF0C1424),
                            Color(0xFF0F172A),
                            Color(0xFF090D16)
                        )
                    } else {
                        listOf(
                            Color(0xFFE6EEF8),
                            Color(0xFFEEF4FA),
                            Color(0xFFF4F7FB),
                            Color(0xFFEDF2F8)
                        )
                    },
                    startY = 0f,
                    endY = h
                )
            )

            // 2. Subtle Top-Right Sapphire Ambient Glow
            val c1 = Offset(w * 0.85f + dx, h * 0.14f + dy)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = if (isDarkTheme) {
                        listOf(
                            Color(0xFF0284C7).copy(alpha = 0.18f),
                            Color(0xFF0369A1).copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    } else {
                        listOf(
                            Color(0xFF38BDF8).copy(alpha = 0.14f),
                            Color(0xFF60A5FA).copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    },
                    center = c1,
                    radius = w * 0.75f
                ),
                radius = w * 0.75f,
                center = c1
            )

            // 3. Subtle Lower-Left Emerald / Teal Ambient Glow
            val c2 = Offset(w * 0.18f - dx, h * 0.78f - dy)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = if (isDarkTheme) {
                        listOf(
                            Color(0xFF10B981).copy(alpha = 0.14f),
                            Color(0xFF0D9488).copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    } else {
                        listOf(
                            Color(0xFF0D9488).copy(alpha = 0.10f),
                            Color(0xFF38BDF8).copy(alpha = 0.04f),
                            Color.Transparent
                        )
                    },
                    center = c2,
                    radius = w * 0.78f
                ),
                radius = w * 0.78f,
                center = c2
            )
        }

        content()
    }
}

/**
 * Clean vector Shield Emblem for headers and onboarding with zero rectangular background artifacts.
 */
@Composable
fun LiquidBrandEmblem(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    val isLight = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val primary = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(size * 0.30f)

    Box(
        modifier = modifier
            .size(size)
            .ultraGlassSurface(
                shape = shape,
                cornerRadius = size * 0.30f,
                tintColor = primary.copy(alpha = if (isLight) 0.12f else 0.20f),
                borderTint = primary.copy(alpha = if (isLight) 0.32f else 0.45f),
                shadowElevation = 0.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = "SecureSMS Emblem",
            tint = primary,
            modifier = Modifier.size(size * 0.56f)
        )
    }
}

private fun Outline.toComposePath(): Path {
    return when (this) {
        is Outline.Generic -> this.path
        is Outline.Rounded -> Path().apply { addRoundRect(this@toComposePath.roundRect) }
        is Outline.Rectangle -> Path().apply { addRect(this@toComposePath.rect) }
    }
}

/**
 * Executive frosted glass surface modifier:
 * - Zero hardware RenderNode rectangular shadow artifacts
 * - Exact Outline path matching for any Shape
 * - High-clarity frosted fill so typography remains razor-sharp
 * - Subtle 1.dp architectural rim border
 */
@Composable
fun Modifier.ultraGlassSurface(
    shape: Shape = RoundedCornerShape(18.dp),
    cornerRadius: Dp = 18.dp,
    tintColor: Color = Color.Unspecified,
    borderTint: Color = Color.Unspecified,
    shadowElevation: Dp = 6.dp,
    surfaceAlphaMultiplier: Float = 1f
): Modifier {
    val isLight = MaterialTheme.colorScheme.surface.luminance() > 0.5f

    val topLeftFill = when {
        tintColor != Color.Unspecified -> {
            val baseSurface = if (isLight) Color.White else Color(0xFF162033)
            val tintWeight = tintColor.alpha.coerceIn(0.08f, 0.35f)
            Color(
                red = baseSurface.red * (1f - tintWeight) + tintColor.red * tintWeight,
                green = baseSurface.green * (1f - tintWeight) + tintColor.green * tintWeight,
                blue = baseSurface.blue * (1f - tintWeight) + tintColor.blue * tintWeight,
                alpha = ((if (isLight) 0.94f else 0.88f) * surfaceAlphaMultiplier).coerceIn(0f, 1f)
            )
        }
        isLight -> Color.White.copy(alpha = (0.94f * surfaceAlphaMultiplier).coerceIn(0f, 1f))
        else -> Color(0xFF162033).copy(alpha = (0.88f * surfaceAlphaMultiplier).coerceIn(0f, 1f))
    }

    val bottomRightFill = when {
        tintColor != Color.Unspecified -> {
            val baseSurface = if (isLight) Color(0xFFF8FAFC) else Color(0xFF111827)
            val tintWeight = (tintColor.alpha * 0.8f).coerceIn(0.06f, 0.28f)
            Color(
                red = baseSurface.red * (1f - tintWeight) + tintColor.red * tintWeight,
                green = baseSurface.green * (1f - tintWeight) + tintColor.green * tintWeight,
                blue = baseSurface.blue * (1f - tintWeight) + tintColor.blue * tintWeight,
                alpha = ((if (isLight) 0.90f else 0.82f) * surfaceAlphaMultiplier).coerceIn(0f, 1f)
            )
        }
        isLight -> Color(0xFFF8FAFC).copy(alpha = (0.90f * surfaceAlphaMultiplier).coerceIn(0f, 1f))
        else -> Color(0xFF111827).copy(alpha = (0.82f * surfaceAlphaMultiplier).coerceIn(0f, 1f))
    }

    val strokeColorTop = when {
        borderTint != Color.Unspecified -> borderTint.copy(alpha = borderTint.alpha.coerceAtMost(0.65f))
        isLight -> Color(0xFFCBD5E1).copy(alpha = 0.85f)
        else -> Color.White.copy(alpha = 0.14f)
    }

    val strokeColorBottom = when {
        borderTint != Color.Unspecified -> borderTint.copy(alpha = (borderTint.alpha * 0.55f).coerceAtMost(0.45f))
        isLight -> Color(0xFFCBD5E1).copy(alpha = 0.55f)
        else -> Color.White.copy(alpha = 0.07f)
    }

    return this
        .clip(shape)
        .drawWithCache {
            val outline = shape.createOutline(size, layoutDirection, this)
            val outlinePath = outline.toComposePath()

            val fillBrush = Brush.linearGradient(
                colors = listOf(topLeftFill, bottomRightFill),
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height)
            )
            val topSheenBrush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (isLight) 0.22f else 0.05f),
                    Color.Transparent
                ),
                startY = 0f,
                endY = size.height * 0.28f
            )
            val borderBrush = Brush.linearGradient(
                colors = listOf(strokeColorTop, strokeColorBottom),
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height)
            )
            val strokeWidthPx = 1.dp.toPx()

            onDrawBehind {
                drawPath(
                    path = outlinePath,
                    brush = fillBrush
                )
                clipPath(outlinePath) {
                    drawRect(
                        brush = topSheenBrush,
                        size = Size(size.width, size.height * 0.28f)
                    )
                }
                drawPath(
                    path = outlinePath,
                    brush = borderBrush,
                    style = Stroke(width = strokeWidthPx)
                )
            }
        }
}

/**
 * Reusable Glass Card container with clean geometry and subtle tactile press feedback.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    cornerRadius: Dp = 18.dp,
    tintColor: Color = Color.Unspecified,
    borderTint: Color = Color.Unspecified,
    shadowElevation: Dp = 6.dp,
    onClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable BoxScope.() -> Unit
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.982f else 1f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 480f),
        label = "glassCardScale"
    )

    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = ripple(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
            onClick = onClick
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                this.scaleX = scale
                this.scaleY = scale
            }
            .ultraGlassSurface(
                shape = shape,
                cornerRadius = cornerRadius,
                tintColor = tintColor,
                borderTint = borderTint,
                shadowElevation = shadowElevation
            )
            .then(clickModifier),
        content = content
    )
}

/**
 * Liquid-Glass Message Bubble surface for ChatScreen:
 * - Touch-reactive 2D liquid ripple & specular caustic wave originating from the exact touch point
 * - Smooth spring press cushion + subtle vertical fluid elasticity on scroll
 * - Executive high-contrast message bubble colors in both Light and Dark themes
 */
@Composable
fun LiquidMessageBubbleSurface(
    isOutgoing: Boolean,
    isIncomplete: Boolean,
    isEncrypted: Boolean,
    isChannelPost: Boolean = false,
    scrollVelocityFactor: Float = 0f,
    viewportFraction: Float = 0.5f,
    shape: Shape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isLight = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val primaryColor = MaterialTheme.colorScheme.primary
    val interactionSource = remember { MutableInteractionSource() }

    val scaleXAnim = remember { Animatable(1f) }
    val scaleYAnim = remember { Animatable(1f) }
    val rippleProgress = remember { Animatable(0f) }
    val rippleAlpha = remember { Animatable(0f) }
    var touchPoint by remember { mutableStateOf(Offset.Unspecified) }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collectLatest { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    touchPoint = interaction.pressPosition
                    launch {
                        scaleXAnim.animateTo(
                            targetValue = 0.965f,
                            animationSpec = spring(dampingRatio = 0.52f, stiffness = 480f)
                        )
                    }
                    launch {
                        scaleYAnim.animateTo(
                            targetValue = 0.950f,
                            animationSpec = spring(dampingRatio = 0.56f, stiffness = 420f)
                        )
                    }
                    launch {
                        rippleProgress.snapTo(0.06f)
                        rippleProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing)
                        )
                    }
                    launch {
                        rippleAlpha.snapTo(0.38f)
                        rippleAlpha.animateTo(
                            targetValue = 0.14f,
                            animationSpec = tween(durationMillis = 380)
                        )
                    }
                }
                is PressInteraction.Release,
                is PressInteraction.Cancel -> {
                    launch {
                        scaleXAnim.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(dampingRatio = 0.46f, stiffness = 320f)
                        )
                    }
                    launch {
                        scaleYAnim.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(dampingRatio = 0.50f, stiffness = 280f)
                        )
                    }
                    launch {
                        rippleProgress.animateTo(
                            targetValue = 1.28f,
                            animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                        )
                    }
                    launch {
                        rippleAlpha.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(durationMillis = 320)
                        )
                    }
                }
            }
        }
    }

    // Subtle vertical fluid elasticity during scroll without crooked rotation
    val clampedScroll = scrollVelocityFactor.coerceIn(-1f, 1f)
    val absScroll = abs(clampedScroll)
    val scrollStretchY = 1f + (absScroll * 0.036f)
    val scrollSqueezeX = 1f - (absScroll * 0.016f)

    val (gradStart, gradMid, gradEnd) = when {
        isIncomplete -> if (isLight) {
            Triple(
                Color(0xFFFEF3C7),
                Color(0xFFFDE68A),
                Color(0xFFFCD34D)
            )
        } else {
            Triple(
                Color(0xFF78350F).copy(alpha = 0.90f),
                Color(0xFF451A03).copy(alpha = 0.86f),
                Color(0xFF3B1502).copy(alpha = 0.82f)
            )
        }
        isChannelPost && isOutgoing -> if (isLight) {
            Triple(
                Color(0xFF0052CC),
                Color(0xFF0284C7),
                Color(0xFF0369A1)
            )
        } else {
            Triple(
                Color(0xFF0F4C81),
                Color(0xFF075985),
                Color(0xFF064E3B)
            )
        }
        !isEncrypted && isOutgoing -> if (isLight) {
            Triple(
                Color(0xFF1D4ED8),
                Color(0xFF1E40AF),
                Color(0xFF1E3A8A)
            )
        } else {
            Triple(
                Color(0xFF1E3A8A),
                Color(0xFF172554),
                Color(0xFF0F172A)
            )
        }
        isOutgoing -> if (isLight) {
            // Executive Cobalt -> Sapphire gradient
            Triple(
                Color(0xFF0052CC),
                Color(0xFF0066FF),
                Color(0xFF0284C7)
            )
        } else {
            // Refined Emerald-Teal -> Deep Cyan gradient
            Triple(
                Color(0xFF065F46),
                Color(0xFF047857),
                Color(0xFF0F766E)
            )
        }
        else -> if (isLight) {
            // Crisp Elevated White Card for incoming messages in Light Mode
            Triple(
                Color(0xFFFFFFFF),
                Color(0xFFF8FAFC),
                Color(0xFFF1F5F9)
            )
        } else {
            // Elevated Obsidian Slate for incoming messages in Dark Mode
            Triple(
                Color(0xFF1E293B).copy(alpha = 0.94f),
                Color(0xFF172033).copy(alpha = 0.90f),
                Color(0xFF111827).copy(alpha = 0.88f)
            )
        }
    }

    val rimTopColor = when {
        isIncomplete -> Color(0xFFF59E0B)
        isOutgoing -> if (isLight) Color(0xFF60A5FA).copy(alpha = 0.55f) else Color(0xFF34D399).copy(alpha = 0.45f)
        isLight -> Color(0xFFCBD5E1).copy(alpha = 0.85f)
        else -> Color(0xFF334155).copy(alpha = 0.85f)
    }

    val rimBottomColor = when {
        isOutgoing -> if (isLight) Color(0xFF0284C7).copy(alpha = 0.45f) else Color(0xFF0D9488).copy(alpha = 0.35f)
        isLight -> Color(0xFFCBD5E1).copy(alpha = 0.65f)
        else -> Color(0xFF1E293B).copy(alpha = 0.75f)
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                this.scaleX = scaleXAnim.value * scrollSqueezeX
                this.scaleY = scaleYAnim.value * scrollStretchY
            }
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .drawWithCache {
                val outline = shape.createOutline(size, layoutDirection, this)
                val outlinePath = outline.toComposePath()
                val strokeWidthPx = 1.dp.toPx()

                onDrawBehind {
                    val shiftY = (viewportFraction - 0.5f) * size.height * 0.25f
                    val bodyBrush = Brush.linearGradient(
                        colors = listOf(gradStart, gradMid, gradEnd),
                        start = Offset(0f, shiftY),
                        end = Offset(size.width, size.height + shiftY)
                    )

                    // 1. Bubble surface body
                    drawPath(
                        path = outlinePath,
                        brush = bodyBrush
                    )

                    // 2. Subtle top specular highlight & touch-point liquid ripple wave
                    clipPath(outlinePath) {
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isOutgoing) 0.14f else if (isLight) 0.40f else 0.06f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = size.height * 0.32f
                            ),
                            size = Size(size.width, size.height * 0.32f)
                        )

                        val rAlpha = rippleAlpha.value
                        if (rAlpha > 0.005f) {
                            val center = if (touchPoint != Offset.Unspecified) {
                                Offset(
                                    touchPoint.x.coerceIn(0f, size.width),
                                    touchPoint.y.coerceIn(0f, size.height)
                                )
                            } else {
                                Offset(size.width / 2f, size.height / 2f)
                            }
                            val maxRadius = maxOf(size.width, size.height) * 1.1f
                            val currentRadius = (maxRadius * rippleProgress.value).coerceAtLeast(8f)

                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = rAlpha * 0.50f),
                                        (if (isOutgoing) Color(0xFF7DD3FC) else primaryColor).copy(alpha = rAlpha * 0.30f),
                                        Color.Transparent
                                    ),
                                    center = center,
                                    radius = currentRadius
                                ),
                                center = center,
                                radius = currentRadius
                            )

                            drawCircle(
                                color = Color.White.copy(alpha = rAlpha * 0.55f),
                                center = center,
                                radius = currentRadius * 0.70f,
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                        }
                    }

                    // 3. Crisp 1.dp border
                    drawPath(
                        path = outlinePath,
                        brush = Brush.linearGradient(
                            colors = listOf(rimTopColor, rimBottomColor),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, size.height)
                        ),
                        style = Stroke(width = strokeWidthPx)
                    )
                }
            },
        content = content
    )
}

/**
 * Frosted glass top bar container with high-contrast status bar backing and bottom hairline divider.
 * In Light Mode, uses a cool slate-tinted translucent surface (#E2E8F0 -> #F1F5F9) so the dark
 * Android OS status bar clock, battery, and icons have crisp contrast and the top edge is never white.
 */
@Composable
fun GlassHeaderSurface(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isLight = MaterialTheme.colorScheme.surface.luminance() > 0.5f

    Box(
        modifier = modifier
            .drawWithCache {
                val fillBrush = Brush.verticalGradient(
                    colors = if (isLight) {
                        listOf(
                            Color(0xFFE2E8F0).copy(alpha = 0.96f),
                            Color(0xFFF1F5F9).copy(alpha = 0.94f),
                            Color(0xFFF8FAFC).copy(alpha = 0.92f)
                        )
                    } else {
                        listOf(
                            Color(0xFF090D16).copy(alpha = 0.95f),
                            Color(0xFF0F172A).copy(alpha = 0.90f)
                        )
                    }
                )
                val bottomBorderColor = if (isLight) {
                    Color(0xFFCBD5E1).copy(alpha = 0.90f)
                } else {
                    Color(0xFF1E293B).copy(alpha = 0.95f)
                }
                onDrawBehind {
                    drawRect(brush = fillBrush)
                    drawLine(
                        color = bottomBorderColor,
                        start = Offset(0f, size.height - 1.dp.toPx()),
                        end = Offset(size.width, size.height - 1.dp.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            },
        content = content
    )
}

/**
 * Consistent frosted glass OutlinedTextField colors for search bars and chat inputs.
 */
@Composable
fun glassTextFieldColors(
    transparentContainer: Boolean = false
): TextFieldColors {
    val isLight = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = if (transparentContainer) Color.Transparent else if (isLight) Color.White.copy(alpha = 0.95f) else Color(0xFF162033).copy(alpha = 0.88f),
        unfocusedContainerColor = if (transparentContainer) Color.Transparent else if (isLight) Color(0xFFF8FAFC).copy(alpha = 0.90f) else Color(0xFF111827).copy(alpha = 0.78f),
        disabledContainerColor = Color.Transparent,
        focusedBorderColor = if (transparentContainer) Color.Transparent else MaterialTheme.colorScheme.primary.copy(alpha = 0.80f),
        unfocusedBorderColor = if (transparentContainer) Color.Transparent else if (isLight) Color(0xFFCBD5E1) else Color(0xFF334155),
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
