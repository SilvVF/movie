package io.silv.movie.presentation.settings.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import io.silv.core_ui.theme.SeededMaterialTheme
import io.silv.movie.MovieTheme

@Composable
internal fun InfoWidget(text: String) {
    Column(
        modifier = Modifier
            .padding(
                horizontal = PrefsHorizontalPadding,
                vertical = 8.dp,
            )
            .alpha(0.78f),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@PreviewLightDark
@Composable
private fun InfoWidgetPreview() {
    MovieTheme {
        Surface {
            InfoWidget(text = "kdjflkasjf;asjk;dfjl;adsfjkl")
        }
    }
}