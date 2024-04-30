package io.silv.movie.presentation.components.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val BottomSheetStartPadding = 12.dp

@Composable
fun BottomSheetDragHandlerNoPadding(
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
            alpha = 0.4f
        ),
        shape = RoundedCornerShape(28.0.dp)
    ) {
        Box(
            Modifier
                .size(
                    width =  32.0.dp,
                    height = 4.0.dp
                )
        )
    }
}

@Composable
fun ColumnScope.BottomSheetHeader(
    poster: @Composable () -> Unit,
    title: @Composable () -> Unit,
    description: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = BottomSheetStartPadding)
            .minimumInteractiveComponentSize()
            .height(72.dp)
    ) {
        poster()
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier
                .weight(1f)
        ) {
            ProvideTextStyle(value = MaterialTheme.typography.labelLarge) {
                CompositionLocalProvider(
                    LocalContentColor provides AlertDialogDefaults.titleContentColor,
                    content = title
                )
            }
            Spacer(Modifier.height(2.dp))
            ProvideTextStyle(value = MaterialTheme.typography.labelSmall) {
                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colorScheme.onBackground.copy(
                        alpha = 0.78f
                    ),
                    content = description
                )
            }
        }
    }
}

@Composable
fun ColumnScope.BottomSheetItem(
    title: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Row(
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp, horizontal = BottomSheetStartPadding)
            .minimumInteractiveComponentSize()
    ) {
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onBackground.copy(
                alpha = 0.78f
            ),
            content = icon
        )
        Spacer(Modifier.width(12.dp))
        ProvideTextStyle(value = MaterialTheme.typography.labelLarge) {
            CompositionLocalProvider(
                LocalContentColor provides AlertDialogDefaults.titleContentColor,
                content = title
            )
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            trailingIcon?.invoke()
        }
    }
}
