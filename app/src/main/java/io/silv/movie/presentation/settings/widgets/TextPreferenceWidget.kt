package io.silv.movie.presentation.settings.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import io.silv.core_ui.components.shimmer.ShimmerHost
import io.silv.core_ui.components.shimmer.TextPlaceholder
import io.silv.movie.MovieTheme

@Composable
fun TextPreferencePlaceHolder(
    title: String?,
    subtitle: String?,
    icon: ImageVector?,
) {
    ShimmerHost {
        Column(
            Modifier
                .padding(
                    horizontal = SettingsItemsPaddings.Horizontal,
                    vertical = SettingsItemsPaddings.Vertical,
                ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth().padding(vertical = SettingsItemsPaddings.Vertical),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                if (icon != null) {
                    Spacer(modifier = Modifier
                        .padding(horizontal = PrefsHorizontalPadding)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface))

                }
                TextPlaceholder()
            }
            TextPlaceholder(height = 12.dp)
        }
    }
}

@Composable
fun TextPreferenceWidget(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    widget: @Composable (() -> Unit)? = null,
    onPreferenceClick: (() -> Unit)? = null,
) {
    BasePreferenceWidget(
        modifier = modifier,
        title = title,
        subcomponent = if (!subtitle.isNullOrBlank()) {
            {
                Text(
                    text = subtitle,
                    modifier = Modifier
                        .padding(horizontal = PrefsHorizontalPadding)
                        .alpha(0.78f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 10,
                )
            }
        } else {
            null
        },
        icon = if (icon != null) {
            {
                Icon(
                    imageVector = icon,
                    tint = iconTint,
                    contentDescription = null,
                )
            }
        } else {
            null
        },
        onClick = onPreferenceClick,
        widget = widget,
    )
}

@PreviewLightDark
@Composable
private fun TextPreferenceWidgetPreview() {
    MovieTheme {
        Surface {
            Column {
                TextPreferenceWidget(
                    title = "Text preference with icon",
                    subtitle = "Text preference summary",
                    icon = Icons.Filled.Preview,
                    onPreferenceClick = {},
                )
                TextPreferenceWidget(
                    title = "Text preference",
                    subtitle = "Text preference summary",
                    onPreferenceClick = {},
                )
            }
        }
    }
}