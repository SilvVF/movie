package io.silv.core_ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider

@Composable
fun TooltipIconButton(
    onClick: () -> Unit,
    tooltip: String,
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tint: Color = LocalContentColor.current,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    tooltipAlignment: Alignment = Alignment.BottomCenter
) {
    val state =  rememberTooltipState()

    TooltipBox(
        tooltip = {
            PlainTooltip(
                containerColor = tint
            ) {
                Text(text = tooltip, style = MaterialTheme.typography.labelSmall)
            }
        },
        state = state,
        positionProvider = remember(tooltipAlignment) {
            AlignmentOffsetPositionProvider(
                tooltipAlignment,
                offset = IntOffset(0, 32)
            )
        }
    ) {
        IconButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            colors = colors,
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                tint = tint
            )
        }
    }
}

@Immutable
@Stable
private class AlignmentOffsetPositionProvider(
    val alignment: Alignment,
    val offset: IntOffset
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        // TODO: Decide which is the best way to round to result without reimplementing Alignment.align

        val anchorAlignmentPoint = alignment.align(
            IntSize.Zero,
            anchorBounds.size,
            layoutDirection
        )
        // Note the negative sign. Popup alignment point contributes negative offset.
        val popupAlignmentPoint = -alignment.align(
            IntSize.Zero,
            popupContentSize,
            layoutDirection
        )
        val resolvedUserOffset = IntOffset(
            offset.x * (if (layoutDirection == LayoutDirection.Ltr) 1 else -1),
            offset.y
        )

        return anchorBounds.topLeft +
                anchorAlignmentPoint +
                popupAlignmentPoint +
                resolvedUserOffset
    }
}
