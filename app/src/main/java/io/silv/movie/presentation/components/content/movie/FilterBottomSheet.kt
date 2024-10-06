package io.silv.movie.presentation.components.content.movie

import FilterChipWrapper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import io.silv.core_ui.util.colorClickable
import io.silv.core_ui.util.conditional
import io.silv.movie.R
import io.silv.movie.data.content.Genre
import io.silv.movie.data.content.GenreMode
import io.silv.movie.data.content.SearchItem
import io.silv.movie.data.content.SortingOption
import kotlinx.coroutines.launch


@Composable
fun MovieFilterBottomSheet(
    onDismissRequest: () -> Unit,
    onApplyFilter: () -> Unit,
    onResetFilter: () -> Unit,
    searchItems: List<SearchItem>,
    onSortingItemSelected: (SortingOption) -> Unit,
    selectedSortingOption: SortingOption,
    onGenreSelected: (Genre) -> Unit,
    genreMode: GenreMode,
    changeGenreMode: (GenreMode) -> Unit,
    genres: List<Pair<Boolean, Genre>>,
) {
    var selectedSearchItemIdx by remember { mutableIntStateOf(0) }
    ModalBottomSheet(
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        ),
        onDismissRequest = onDismissRequest,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        val screenHeight = LocalConfiguration.current.screenHeightDp.dp
        val selectedSearchItem by remember {
            derivedStateOf { searchItems[selectedSearchItemIdx] }
        }
        Column(modifier = Modifier.heightIn(max = screenHeight * 0.8f)) {
            Column(
                Modifier
                    .padding(2.dp)
                    .animateContentSize()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(id = R.string.filter_text_ignored_hint),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(4.dp)
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    searchItems.fastForEachIndexed { idx, item ->
                        FilterChipWrapper(
                            idx == selectedSearchItemIdx,
                            { selectedSearchItemIdx = idx },
                            item.label,
                        )
                    }
                }
                selectedSearchItem.error.value?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.error
                        )
                    )
                }
                SearchFooter(
                    title = selectedSearchItem.text.value,
                    showDivider = false,
                    labelText = selectedSearchItem.label,
                    keyboardType = selectedSearchItem.keyboardType,
                    isError = selectedSearchItem.error.value != null,
                    textChanged = { selectedSearchItem.text.value = it },
                    search = { onApplyFilter() }
                )
                FilterDropDown(label = stringResource(id = R.string.genres)) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        genres.fastForEach { (selected, genre) ->
                            FilterChipWrapper(
                                selected = selected,
                                onClick = { onGenreSelected(genre) },
                                name = genre.name
                            )
                        }
                    }
                }
                FilterDropDown(label = stringResource(id = R.string.sort)) {
                    val items = remember { SortingOption.entries }
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                       items.fastForEach {
                            FilterChipWrapper(
                                selected = selectedSortingOption == it,
                                onClick = { onSortingItemSelected(it) },
                                name = it.title
                            )
                        }
                    }
                }
                FilterDropDown(label = stringResource(id = R.string.genre_mode)) {
                    Column {
                        ToggleItem(
                            label = stringResource(id = R.string.or),
                            checked = genreMode == GenreMode.Or,
                            onCheckChange = { changeGenreMode(GenreMode.Or) }
                        )
                        ToggleItem(
                            label = stringResource(id = R.string.and),
                            checked = genreMode == GenreMode.And,
                            onCheckChange = { changeGenreMode(GenreMode.And) }
                        )
                    }
                }
            }
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .padding(
                        bottom = with(LocalDensity.current) {
                            WindowInsets.systemBars
                                .getBottom(this)
                                .toDp()
                        }
                    )
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onResetFilter) {
                    Text(stringResource(id = R.string.reset))
                }
                Button(
                    onClick = onApplyFilter,
                    enabled = searchItems.fastAll { it.error.value == null }
                ) {
                    Text(text = stringResource(id = R.string.filter))
                }
            }
        }
        Spacer(
            Modifier.height(
                with(LocalDensity.current) {
                    WindowInsets.systemBars.getBottom(LocalDensity.current).toDp()
                }
            )
        )
    }
}

@Composable
private fun ToggleItem(
    label: String,
    checked: Boolean,
    onCheckChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .colorClickable {
                onCheckChange(!checked)
            },
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            enabled = true,
            onClick = { onCheckChange(!checked) },
            selected = checked
        )
        Spacer(Modifier.padding(12.dp))
        Text(label)
    }
}

@Composable
private fun FilterDropDown(
    label: String,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label)
            IconButton(onClick = { expanded = !expanded }) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.conditional(expanded) {
                        rotate(180f)
                    }
                )
            }
        }
        AnimatedVisibility(visible = expanded) {
            content()
        }
    }
}

@Composable
private fun ColumnScope.SearchFooter(
    title: String,
    labelText: String,
    keyboardType: KeyboardType,
    enabled: Boolean = true,
    isError: Boolean = false,
    showDivider: Boolean = true,
    textChanged: (String) -> Unit,
    search: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val bringIntoViewRequester = BringIntoViewRequester()
    val scope = rememberCoroutineScope()

    if (showDivider) {
        HorizontalDivider()
        Spacer(Modifier.height(2.dp))
    }

    OutlinedTextField(
        modifier =
        Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusEvent {
                if (it.isFocused || it.hasFocus) {
                    scope.launch { bringIntoViewRequester.bringIntoView() }
                }
            }
            .padding(horizontal = 2.dp),
        value = title,
        enabled = enabled,
        singleLine = true,
        label = { Text(text = labelText, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        trailingIcon = {
            if (isError) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            } else if (title.isNotEmpty()) {
                IconButton(onClick = { textChanged("") }) {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = stringResource(id = R.string.cancel),
                        tint = MaterialTheme.colorScheme.primaryContainer
                    )
                }
            }
        },
        isError = isError,
        onValueChange = { textChanged(it) },
        colors =
        OutlinedTextFieldDefaults.colors(
            cursorColor = MaterialTheme.colorScheme.primaryContainer,
            errorCursorColor = MaterialTheme.colorScheme.error,
            focusedBorderColor = MaterialTheme.colorScheme.primaryContainer,
            errorBorderColor = MaterialTheme.colorScheme.error,
            focusedLabelColor = MaterialTheme.colorScheme.primaryContainer,
            errorLabelColor = MaterialTheme.colorScheme.error,
        ),
        keyboardOptions =
        KeyboardOptions.Default.copy(
            imeAction = ImeAction.Search,
            keyboardType = keyboardType
        ),
        keyboardActions =
        KeyboardActions(
            onSearch = {
                if (!isError) {
                    focusManager.clearFocus()
                    search(title)
                }
            },
        ),
    )
}