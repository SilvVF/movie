package io.silv.core_ui.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind

fun Modifier.selectedBackground(isSelected: Boolean): Modifier = if (isSelected) {
    composed {
        val alpha = if (isSystemInDarkTheme()) 0.16f else 0.22f
        val color = MaterialTheme.colorScheme.secondary.copy(alpha = alpha)
        Modifier.drawBehind {
            drawRect(color)
        }
    }
} else {
    this
}



fun Modifier.clickableNoIndication(
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier = this.composed {
    combinedClickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onLongClick = onLongClick,
        onClick = onClick,
    )
}