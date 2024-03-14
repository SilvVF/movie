package io.silv.movie.presentation.library.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import io.silv.core_ui.components.topbar.SearchBarInputField
import io.silv.core_ui.components.TooltipIconButton
import io.silv.movie.data.prefrences.PosterDisplayMode

object PosterLargeTopBarDefaults {

    @Composable
    fun BackArrowIcon(
        isKeyboardOpen: Boolean
    ) {

        val navigator = LocalNavigator.current
        val keyboardController = LocalSoftwareKeyboardController.current

        if (navigator?.canPop == true || isKeyboardOpen) {
            IconButton(
                onClick = {
                    if (isKeyboardOpen) {
                        keyboardController?.hide()
                    } else {
                        navigator?.pop()
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null
                )
            }
        }
    }

    context (RowScope)
    @Composable
    fun Actions(
        displayMode: () -> PosterDisplayMode,
        setDisplayMode: (PosterDisplayMode) -> Unit,
        onListOptionClicked: () -> Unit
    ) {
        var dropDownVisible by remember { mutableStateOf(false) }

        Box(contentAlignment = Alignment.BottomCenter) {
            DropdownMenu(
                expanded = dropDownVisible,
                onDismissRequest = { dropDownVisible = false }
            ) {
                PosterDisplayMode.values.forEach {
                    DropdownMenuItem(
                        trailingIcon = {
                            RadioButton(
                                selected = displayMode() == it,
                                onClick = { setDisplayMode(it) }
                            )
                        },
                        text = {
                            Text(
                                remember {
                                    it.toString()
                                        .split(Regex("(?<=[a-z])(?=[A-Z])"))
                                        .joinToString(" ")
                                }
                            )
                        },
                        onClick = { setDisplayMode(it) }
                    )
                }
            }
            TooltipIconButton(
                onClick = { dropDownVisible = true },
                imageVector = when (displayMode()) {
                    PosterDisplayMode.List -> Icons.AutoMirrored.Filled.List
                    else -> Icons.Filled.GridView
                },
                contentDescription = null,
                tooltip = "Display Mode"
            )
        }
        IconButton(onClick = onListOptionClicked) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = null
            )
        }
    }

    @Composable
    fun SearchInputField(
        query: () -> String,
        changeQuery: (String) -> Unit,
        onSearch: (String) -> Unit,
        placeholder: String,
    ) {
        Column(
            Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            val focusManager = LocalFocusManager.current
            SearchBarInputField(
                query = query(),
                placeholder = {
                    Text( placeholder)
                },
                onQueryChange = { changeQuery(it) },
                onSearch = {
                    onSearch(it)
                    focusManager.clearFocus(false)
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AnimatedVisibility(visible = query().isNotEmpty()) {
                            IconButton(onClick = { changeQuery("") }) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = null
                                )
                            }
                        }
                        IconButton(onClick = { onSearch(query()) }) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null
                            )
                        }
                    }
                },
                modifier = Modifier
                    .padding(horizontal = 12.dp)
            )
        }
    }
}