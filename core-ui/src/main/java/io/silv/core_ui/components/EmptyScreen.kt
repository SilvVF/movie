package io.silv.core_ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExploreOff
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.silv.core_ui.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun NoResultsEmptyScreen(contentPaddingValues: PaddingValues) {
    EmptyScreen(
        icon = Icons.Filled.ExploreOff,
        iconSize = 176.dp,
        message = stringResource(id = R.string.no_results_found),
        contentPadding = contentPaddingValues,
    )
}

@Composable
fun EmptyScreen(
    icon: ImageVector,
    iconSize: Dp = 24.dp,
    message: String? = null,
    actions: ImmutableList<Action> = persistentListOf(),
    contentPadding: PaddingValues = PaddingValues(),
) {
    val iconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .45f)
    EmptyScreen(message, actions, contentPadding) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = iconColor,
        )
    }
}

object PrimaryColorRippleTheme : RippleTheme {

    @Composable override fun defaultColor(): Color = MaterialTheme.colorScheme.primary

    @Composable
    override fun rippleAlpha() =
        RippleAlpha(
            draggedAlpha = 0.9f,
            focusedAlpha = 0.9f,
            hoveredAlpha = 0.9f,
            pressedAlpha = 0.9f,
        )
}

@Composable
private fun EmptyScreen(
    message: String? = null,
    actions: ImmutableList<Action> = persistentListOf(),
    contentPadding: PaddingValues,
    icon: @Composable () -> Unit,
) {
    val iconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .45f)
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding()),
    ) {
        val top = maxHeight / 2
        Column(
            modifier = Modifier.fillMaxSize().paddingFromBaseline(top = top),
            Arrangement.Top,
            Alignment.CenterHorizontally,
        ) {
            icon()
            message?.let {
                Text(
                    text = message,
                    color = iconColor,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    textAlign = TextAlign.Center,
                )
            }
            CompositionLocalProvider(
                LocalRippleTheme provides PrimaryColorRippleTheme
            ) {
                actions.forEach { action ->
                    Spacer(modifier = Modifier.size(16.dp))
                    TextButton(onClick = action.onClick) {
                        Text(
                            text = stringResource(id = action.resId),
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun EmptyViewPreview() {
    EmptyScreen(
        icon = Icons.Filled.ExploreOff,
        iconSize = 72.dp,
        message = stringResource(id = R.string.no_results_found),
        actions = persistentListOf(Action(R.string.retry)),
    )
}

data class Action(
    @StringRes val resId: Int,
    val onClick: () -> Unit = {},
)