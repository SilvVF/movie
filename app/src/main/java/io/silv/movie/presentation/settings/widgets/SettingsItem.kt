package io.silv.movie.presentation.settings.widgets

import android.view.MotionEvent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.RemoveCircle
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.DisabledByDefault
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.silv.movie.presentation.settings.StringResource
import kotlinx.coroutines.delay

object SettingsItemsPaddings {
    val Horizontal = 24.dp
    val Vertical = 10.dp
}

@Composable
fun HeadingItem(
    labelRes: StringResource,
) {
    HeadingItem(stringResource(labelRes))
}

@Composable
fun HeadingItem(
    text: String,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = SettingsItemsPaddings.Horizontal,
                vertical = SettingsItemsPaddings.Vertical,
            ),
    )
}

@Composable
fun FloatSliderItem(
    label:String,
    value: Float,
    valueText:  @Composable (Float) ->  String,
    onChange: (Float) -> Unit,
    max: Float,
    min: Float = 0f,
    steps: Int,
) {
    val haptic = LocalHapticFeedback.current

    var vcopy by remember{ mutableFloatStateOf(value) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = SettingsItemsPaddings.Horizontal,
                vertical = SettingsItemsPaddings.Vertical,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(modifier = Modifier.weight(0.5f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = valueText(vcopy),
            )
        }

        Slider(
            value = vcopy,
            onValueChangeFinished = {
                onChange(vcopy)
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            },
            onValueChange = {
                vcopy = it
            },
            modifier = Modifier.weight(1f),
            valueRange = min..max,
            steps = steps,
        )
    }
}

@Composable
fun SliderItem(
    label: String,
    value: Int,
    valueText: @Composable (Int) -> String,
    onChange: (Int) -> Unit,
    max: Int,
    min: Int = 0,
) {
    val haptic = LocalHapticFeedback.current
    var vcopy by remember{ mutableIntStateOf(value) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = SettingsItemsPaddings.Horizontal,
                vertical = SettingsItemsPaddings.Vertical,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(modifier = Modifier.weight(0.5f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(valueText(vcopy))
        }

        Slider(
            value = vcopy.toFloat(),
            onValueChange = {
                val newValue = it.toInt()
                if (newValue != value) {
                    vcopy = newValue
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            },
            onValueChangeFinished = {
                onChange(vcopy)
            },
            modifier = Modifier.weight(1.5f),
            valueRange = min.toFloat()..max.toFloat(),
            steps = max - min,
        )
    }
}

@Composable
fun SelectItem(
    label: String,
    options: Array<out Any?>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryEditable, true)
                .fillMaxWidth()
                .padding(
                    horizontal = SettingsItemsPaddings.Horizontal,
                    vertical = SettingsItemsPaddings.Vertical,
                ),
            label = { Text(text = label) },
            value = options[selectedIndex].toString(),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded,
                )
            },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
        )

        ExposedDropdownMenu(
            modifier = Modifier.exposedDropdownSize(matchTextFieldWidth = true),
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEachIndexed { index, text ->
                DropdownMenuItem(
                    text = { Text(text.toString()) },
                    onClick = {
                        onSelect(index)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

enum class TriState {
    DISABLED, // Disable filter
    ENABLED_IS, // Enabled with "is" filter
    ENABLED_NOT, // Enabled with "not" filter
    ;

    fun next(): TriState {
        return when (this) {
            DISABLED -> ENABLED_IS
            ENABLED_IS -> ENABLED_NOT
            ENABLED_NOT -> DISABLED
        }
    }
}

@Composable
fun TriStateItem(
    label: String,
    state: TriState,
    enabled: Boolean = true,
    onClick: ((TriState) -> Unit)?,
) {
    Row(
        modifier = Modifier
            .clickable(
                enabled = enabled && onClick != null,
                onClick = {
                    when (state) {
                        TriState.DISABLED -> onClick?.invoke(TriState.ENABLED_IS)
                        TriState.ENABLED_IS -> onClick?.invoke(TriState.ENABLED_NOT)
                        TriState.ENABLED_NOT -> onClick?.invoke(TriState.DISABLED)
                    }
                },
            )
            .fillMaxWidth()
            .padding(
                horizontal = SettingsItemsPaddings.Horizontal,
                vertical = SettingsItemsPaddings.Vertical,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        val stateAlpha = if (enabled && onClick != null) 1f else 0.38f

        Icon(
            imageVector = when (state) {
                TriState.DISABLED -> Icons.Rounded.CheckBoxOutlineBlank
                TriState.ENABLED_IS -> Icons.Rounded.CheckBox
                TriState.ENABLED_NOT -> Icons.Rounded.DisabledByDefault
            },
            contentDescription = null,
            tint = if (!enabled || state == TriState.DISABLED) {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = stateAlpha)
            } else {
                when (onClick) {
                    null -> MaterialTheme.colorScheme.onSurface.copy(alpha =  0.38f)
                    else -> MaterialTheme.colorScheme.primary
                }
            },
        )
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = stateAlpha),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
fun <T> SelectItem(
    label: String,
    options: Array<T>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    onSelect: (Int) -> Unit,
    toString: (T) -> String = { it.toString() },
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        modifier = modifier,
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .padding(
                    horizontal = SettingsItemsPaddings.Horizontal,
                    vertical = SettingsItemsPaddings.Vertical,
                ),
            label = { Text(text = label) },
            value = toString(options[selectedIndex]),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded,
                )
            },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
        )

        ExposedDropdownMenu(
            modifier = Modifier.exposedDropdownSize(),
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(toString(option)) },
                    onClick = {
                        onSelect(index)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
fun RepeatingIconButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    maxDelayMillis: Long = 750,
    minDelayMillis: Long = 5,
    delayDecayFactor: Float = .25f,
    content: @Composable () -> Unit,
) {
    val currentClickListener by rememberUpdatedState(onClick)
    var pressed by remember { mutableStateOf(false) }

    IconButton(
        modifier = modifier.pointerInteropFilter {
            pressed = when (it.action) {
                MotionEvent.ACTION_DOWN -> true

                else -> false
            }

            true
        },
        onClick = {},
        enabled = enabled,
        interactionSource = interactionSource,
        content = content,
    )

    LaunchedEffect(pressed, enabled) {
        var currentDelayMillis = maxDelayMillis

        while (enabled && pressed) {
            currentClickListener()
            delay(currentDelayMillis)
            currentDelayMillis =
                (currentDelayMillis - (currentDelayMillis * delayDecayFactor))
                    .toLong().coerceAtLeast(minDelayMillis)
        }
    }
}

@Composable
fun OutlinedNumericChooser(
    label: String,
    placeholder: String,
    suffix: String,
    value: Int,
    step: Int,
    min: Int? = null,
    onValueChanged: (Int) -> Unit,
) {
    var currentValue = value

    val updateValue: (Boolean) -> Unit = {
        currentValue += if (it) step else -step

        if (min != null) currentValue = if (currentValue < min) min else currentValue

        onValueChanged(currentValue)
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        RepeatingIconButton(
            onClick = { updateValue(false) },
        ) { Icon(imageVector = Icons.Outlined.RemoveCircle, contentDescription = null) }

        OutlinedTextField(
            value = "%d".format(currentValue),
            modifier = Modifier.widthIn(min = 140.dp),

            onValueChange = {
                // Don't allow multiple decimal points, non-numeric characters, or leading zeros
                currentValue = it.trim().replace(Regex("[^-\\d.]"), "").toIntOrNull()
                    ?: currentValue
                onValueChanged(currentValue)
            },

            label = { Text(text = label) },
            placeholder = { Text(text = placeholder) },
            suffix = { Text(text = suffix) },

            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )

        RepeatingIconButton(
            onClick = { updateValue(true) },
        ) { Icon(imageVector = Icons.Outlined.AddCircle, contentDescription = null) }
    }
}

@Composable
fun TextItem(
    label: String,
    value: String,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SettingsItemsPaddings.Horizontal, vertical = 4.dp),
        label = { Text(text = label) },
        value = value,
        onValueChange = onChange,
        singleLine = true,
    )
}

@Composable
fun SettingsChipRow(
    labelRes: StringResource,
    content: @Composable FlowRowScope.() -> Unit,
) {
    Column {
        HeadingItem(labelRes)
        FlowRow(
            modifier = Modifier.padding(
                start = SettingsItemsPaddings.Horizontal,
                top = 0.dp,
                end = SettingsItemsPaddings.Horizontal,
                bottom = SettingsItemsPaddings.Vertical,
            ),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            content = content,
        )
    }
}

@Composable
fun SettingsIconGrid(labelRes: StringResource, content: LazyGridScope.() -> Unit) {
    Column {
        HeadingItem(labelRes)
        LazyVerticalGrid(
            columns = GridCells.Adaptive(128.dp),
            modifier = Modifier.padding(
                start = SettingsItemsPaddings.Horizontal,
                end = SettingsItemsPaddings.Horizontal,
                bottom = SettingsItemsPaddings.Vertical,
            ),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            content = content,
        )
    }
}

@Composable
private fun BaseSettingsItem(
    label: String,
    widget: @Composable RowScope.() -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .fillMaxWidth()
            .padding(
                horizontal = SettingsItemsPaddings.Horizontal,
                vertical = SettingsItemsPaddings.Vertical,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        widget(this)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}