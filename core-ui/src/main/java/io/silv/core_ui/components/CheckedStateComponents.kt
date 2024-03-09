
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/*
Copyright 2015 Javier Tomás
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Modifications copyright (C) 2019 NekoMangaOrg
 */

@Composable
fun FilterChipWrapper(
    selected: Boolean,
    onClick: () -> Unit,
    name: String,
    modifier: Modifier = Modifier,
    hideIcons: Boolean = false,
    labelStyle: TextStyle =
        MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
) {
    TriStateFilterChip(
        modifier = modifier,
        state = ToggleableState(selected),
        hideIcons = hideIcons,
        toggleState = { _ -> onClick() },
        name = name,
        labelTextStyle = labelStyle,
    )
}

@Composable
fun TriStateFilterChip(
    state: ToggleableState,
    toggleState: (ToggleableState) -> Unit,
    name: String,
    modifier: Modifier = Modifier,
    hideIcons: Boolean = false,
    labelTextStyle: TextStyle =
        MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
) {
    FilterChip(
        modifier = modifier,
        selected = state == ToggleableState.On || state == ToggleableState.Indeterminate,
        onClick = { toggleStateIfAble(false, state, toggleState) },
        leadingIcon = {
            if (!hideIcons) {
                if (state == ToggleableState.On) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                } else if (state == ToggleableState.Indeterminate) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = null)
                }
            }
        },
        shape = RoundedCornerShape(100),
        label = { Text(text = name, style = labelTextStyle) },
        colors =
        FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            selectedContainerColor =
            MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp),
            selectedLabelColor = MaterialTheme.colorScheme.primary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
        ),
        border =
        FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = false,
            borderColor = MaterialTheme.colorScheme.onSurface.copy(0.1f),
            selectedBorderColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp),
        ),
    )
}

private fun toggleStateIfAble(
    disabled: Boolean,
    state: ToggleableState,
    toggleState: (ToggleableState) -> Unit
) {
    if (!disabled) {
        val newState =
            when (state) {
                ToggleableState.On -> ToggleableState.Indeterminate
                ToggleableState.Indeterminate -> ToggleableState.Off
                ToggleableState.Off -> ToggleableState.On
            }
        toggleState(newState)
    }
}