package io.silv.movie.presentation.components.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.silv.core_ui.components.topbar.SearchBarInputField
import io.silv.movie.R

object PosterLargeTopBarDefaults {

    @Composable
    fun SearchInputField(
        query: () -> String,
        changeQuery: (String) -> Unit,
        onSearch: (String) -> Unit,
        placeholder: String,
        modifier: Modifier = Modifier,
    ) {
        Column(
            modifier
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
                    Text(placeholder)
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
                                contentDescription = stringResource(id = R.string.search)
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