package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingActionButtonElevation
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.SelectableChipElevation
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Universal IndicationNodeFactory that gives any clickable element a liquid squash-and-bounce
 * spring animation and a soft specular highlight aura on press.
 */
object LiquidBounceIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode {
        return LiquidBounceIndicationNode(interactionSource)
    }

    override fun hashCode(): Int = -104928471
    override fun equals(other: Any?): Boolean = other === this
}

private class LiquidBounceIndicationNode(
    private val interactionSource: InteractionSource
) : Modifier.Node(), DrawModifierNode {

    private val scaleX = Animatable(1f)
    private val scaleY = Animatable(1f)
    private val glowAlpha = Animatable(0f)

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collectLatest { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        launch {
                            scaleX.animateTo(
                                targetValue = 0.94f,
                                animationSpec = spring(
                                    dampingRatio = 0.58f,
                                    stiffness = 520f
                                )
                            )
                        }
                        launch {
                            scaleY.animateTo(
                                targetValue = 0.90f,
                                animationSpec = spring(
                                    dampingRatio = 0.62f,
                                    stiffness = 420f
                                )
                            )
                        }
                        launch {
                            glowAlpha.animateTo(
                                targetValue = 0.22f,
                                animationSpec = spring(stiffness = 500f)
                            )
                        }
                    }
                    is PressInteraction.Release,
                    is PressInteraction.Cancel -> {
                        launch {
                            scaleX.animateTo(
                                targetValue = 1f,
                                animationSpec = spring(
                                    dampingRatio = 0.48f,
                                    stiffness = 360f
                                )
                            )
                        }
                        launch {
                            scaleY.animateTo(
                                targetValue = 1f,
                                animationSpec = spring(
                                    dampingRatio = 0.52f,
                                    stiffness = 320f
                                )
                            )
                        }
                        launch {
                            glowAlpha.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(stiffness = 260f)
                            )
                        }
                    }
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        val sx = scaleX.value
        val sy = scaleY.value
        val alpha = glowAlpha.value
        scale(scaleX = sx, scaleY = sy, pivot = center) {
            this@draw.drawContent()
            if (alpha > 0.005f) {
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = alpha),
                            Color(0xFF00E5A3).copy(alpha = alpha * 0.5f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.maxDimension * 0.7f
                    ),
                    cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                )
            }
        }
    }
}

/**
 * Modifier applying lightweight hardware-accelerated fluid press animation to buttons.
 */
@Composable
fun Modifier.liquidSpringEffect(
    interactionSource: MutableInteractionSource,
    accentColor: Color = MaterialTheme.colorScheme.primary
): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = 500f
        ),
        label = "liquidBtnScale"
    )

    return this.graphicsLayer {
        this.scaleX = scale
        this.scaleY = scale
    }
}

@Composable
fun LiquidButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    androidx.compose.material3.Button(
        onClick = onClick,
        modifier = modifier.liquidSpringEffect(interactionSource),
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}

@Composable
fun LiquidOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.outlinedShape,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        modifier = modifier.liquidSpringEffect(interactionSource),
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}

@Composable
fun LiquidTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.textShape,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    androidx.compose.material3.TextButton(
        onClick = onClick,
        modifier = modifier.liquidSpringEffect(interactionSource),
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}

@Composable
fun LiquidIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    androidx.compose.material3.IconButton(
        onClick = onClick,
        modifier = modifier.liquidSpringEffect(interactionSource),
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
        content = content
    )
}

@Composable
fun LiquidFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = FloatingActionButtonDefaults.shape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    androidx.compose.material3.FloatingActionButton(
        onClick = onClick,
        modifier = modifier.liquidSpringEffect(interactionSource),
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = elevation,
        interactionSource = interactionSource,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiquidFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = FilterChipDefaults.shape,
    colors: SelectableChipColors = FilterChipDefaults.filterChipColors(),
    elevation: SelectableChipElevation? = FilterChipDefaults.filterChipElevation(),
    border: BorderStroke? = FilterChipDefaults.filterChipBorder(enabled, selected),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    androidx.compose.material3.FilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        modifier = modifier.liquidSpringEffect(interactionSource),
        enabled = enabled,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        interactionSource = interactionSource
    )
}
