package io.silv.core_ui.components

import androidx.compose.animation.core.EaseInOutBounce
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import io.silv.core_ui.theme.SeededMaterialTheme

private val StateLayerSize = 40.0.dp
private val BlockWidth = 4.dp
private val BlockPadding = 2.dp
private val BlockMaxHeight = 20.dp
private val BlockMinHeight = 10.dp

@Preview
@Composable
fun AnimatedEqualizerPreview() {
    SeededMaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedEqualizer(
                onClick = { /*TODO*/ },
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
fun AnimatedEqualizer(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(StateLayerSize)
            .clip(CircleShape)
            .clickable(
                onClick = onClick,
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = ripple(
                    bounded = false,
                    radius = StateLayerSize / 2
                )
            ),
        contentAlignment = Alignment.Center
    ) {

        val infiniteTransition = rememberInfiniteTransition("block-transition")

        val b1p = infiniteTransition.animateBlockProgress(Pos.Start)
        val b2p = infiniteTransition.animateBlockProgress(Pos.Mid)
        val b3p = infiniteTransition.animateBlockProgress(Pos.End)

        Canvas(Modifier.fillMaxSize()) {
            drawEqualizerBlock(
                progress = b1p,
                colors.contentColor,
                Pos.Start
            )
            drawEqualizerBlock(
                progress = b2p,
                colors.contentColor,
                Pos.Mid
            )

            drawEqualizerBlock(
                progress = b3p,
                colors.contentColor,
                Pos.End
            )
        }
    }
}

private enum class Pos { Start, Mid, End }

@Composable
private fun InfiniteTransition.animateBlockProgress(p: Pos): Float {
    return animateFloat(
        label = "block-progress",
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when(p) {
                    Pos.Start -> 250
                    Pos.Mid -> 300
                    Pos.End -> 275
                },
                easing = when(p) {
                    Pos.Start -> EaseInOutCubic
                    Pos.Mid -> LinearOutSlowInEasing
                    Pos.End -> EaseInOutBounce
                }
            ),
            repeatMode = RepeatMode.Reverse,
        )
    )
        .value
}

private fun DrawScope.drawEqualizerBlock(
    progress: Float,
    color: Color,
    p: Pos
) {

    val middle = size.width / 2f
    val x = when(p) {
        Pos.Start -> middle  - BlockPadding.toPx() - BlockWidth.toPx()
        Pos.Mid -> middle
        Pos.End -> middle + BlockPadding.toPx() + BlockWidth.toPx()
    }

    val yStart = (size.height / 2f) + (BlockMaxHeight.toPx() / 2f)
    val height = lerp(BlockMinHeight.toPx(), BlockMaxHeight.toPx(), progress)

    drawLine(
        color = color,
        strokeWidth = BlockWidth.toPx(),
        start = Offset(x, yStart),
        end = Offset(
            x,
            yStart - height
        )
    )
}