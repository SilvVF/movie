package io.silv.movie.presentation.covers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import io.silv.core_ui.components.TooltipIconButton
import io.silv.core_ui.util.clickableNoIndication
import io.silv.movie.R
import io.silv.movie.data.content.lists.ContentItem
import io.silv.movie.data.content.lists.ContentList
import io.silv.movie.presentation.components.content.ContentPreviewDefaults
import io.silv.movie.presentation.components.content.rememberListUri
import io.silv.movie.presentation.toPoster
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable

@Composable
fun LibraryCoverDialog(
    list: ContentList,
    items: List<ContentItem>,
    isCustomCover: Boolean,
    snackbarHostState: SnackbarHostState,
    onShareClick: () -> Unit,
    onSaveClick: () -> Unit,
    onEditClick: ((EditCoverAction) -> Unit)?,
    onDismissRequest: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Column {
            Scaffold(
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                containerColor = Color.Transparent,
                modifier = Modifier.weight(1f),
                bottomBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                    ) {
                        ActionsPill {
                            IconButton(onClick = onDismissRequest) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = stringResource(R.string.close),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        ActionsPill {
                            TooltipIconButton(
                                tooltip = stringResource(R.string.share),
                                imageVector = Icons.Outlined.Share,
                                onClick = onShareClick,
                            )
                            TooltipIconButton(
                                tooltip = stringResource(R.string.save),
                                imageVector = Icons.Outlined.Save,
                                onClick = onSaveClick,
                            )
                            if (onEditClick != null) {
                                Box {
                                    var expanded by remember { mutableStateOf(false) }
                                    IconButton(
                                        onClick = {
                                            if (isCustomCover) {
                                                expanded = true
                                            } else {
                                                onEditClick(EditCoverAction.EDIT)
                                            }
                                        },
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Edit,
                                            contentDescription = stringResource(R.string.edit,),
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false },
                                        offset = DpOffset(8.dp, 0.dp),
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(text = stringResource(R.string.edit)) },
                                            onClick = {
                                                onEditClick(EditCoverAction.EDIT)
                                                expanded = false
                                            },
                                        )
                                        DropdownMenuItem(
                                            text = { Text(text = stringResource(R.string.delete)) },
                                            onClick = {
                                                onEditClick(EditCoverAction.DELETE)
                                                expanded = false
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
            ) { contentPadding ->
                val context = LocalContext.current
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickableNoIndication(onClick = onDismissRequest),
                ) {
                    val listUri = rememberListUri(list = list)

                    if (listUri != null) {
                        val state = rememberZoomableState()
                        AsyncImage(
                            imageLoader = LocalContext.current.imageLoader,
                            model = ImageRequest.Builder(context)
                                .data(listUri.uri)
                                .diskCacheKey("${listUri.hash},${list.lastModified}")
                                .memoryCacheKey("${listUri.hash},${list.lastModified}")
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .zoomable(state)
                                .padding(
                                    top = contentPadding.calculateTopPadding(),
                                    bottom = contentPadding.calculateBottomPadding()
                                ),
                        )
                    } else {
                        when {
                            items.isEmpty() -> {
                                ContentPreviewDefaults.PlaceholderPoster(
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            items.size < 4 -> {
                                val state = rememberZoomableState()
                                AsyncImage(
                                    imageLoader = LocalContext.current.imageLoader,
                                    model = ImageRequest.Builder(context)
                                        .data(
                                            remember(items) {
                                                items.first().toPoster()
                                            }
                                        )
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .zoomable(state)
                                        .padding(
                                            top = contentPadding.calculateTopPadding(),
                                            bottom = contentPadding.calculateBottomPadding()
                                        ),
                                )
                            }
                            else -> {
                                ContentPreviewDefaults.MultiItemPoster(
                                    modifier = Modifier.fillMaxSize(),
                                    items = items
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun ListViewCoverDialog(
    list: ContentList,
    items: List<ContentItem>,
    isCustomCover: Boolean,
    snackbarHostState: SnackbarHostState,
    onShareClick: () -> Unit,
    onSaveClick: () -> Unit,
    onEditClick: ((EditCoverAction) -> Unit)?,
    onDismissRequest: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Column {
            Scaffold(
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                containerColor = Color.Transparent,
                modifier = Modifier.weight(1f),
                bottomBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                    ) {
                        ActionsPill {
                            IconButton(onClick = onDismissRequest) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = stringResource(R.string.close),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        ActionsPill {
                            TooltipIconButton(
                                tooltip = stringResource(R.string.share),
                                imageVector = Icons.Outlined.Share,
                                onClick = onShareClick,
                            )
                            TooltipIconButton(
                                tooltip = stringResource(R.string.save),
                                imageVector = Icons.Outlined.Save,
                                onClick = onSaveClick,
                            )
                            if (onEditClick != null) {
                                Box {
                                    var expanded by remember { mutableStateOf(false) }
                                    IconButton(
                                        onClick = {
                                            if (isCustomCover) {
                                                expanded = true
                                            } else {
                                                onEditClick(EditCoverAction.EDIT)
                                            }
                                        },
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Edit,
                                            contentDescription = stringResource(R.string.edit,),
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false },
                                        offset = DpOffset(8.dp, 0.dp),
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(text = stringResource(R.string.edit)) },
                                            onClick = {
                                                onEditClick(EditCoverAction.EDIT)
                                                expanded = false
                                            },
                                        )
                                        DropdownMenuItem(
                                            text = { Text(text = stringResource(R.string.delete)) },
                                            onClick = {
                                                onEditClick(EditCoverAction.DELETE)
                                                expanded = false
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
            ) { contentPadding ->
                val context = LocalContext.current
                val listUri = rememberListUri(list = list)

                if (listUri != null) {
                    val state = rememberZoomableState()
                    AsyncImage(
                        imageLoader = LocalContext.current.imageLoader,
                        model = ImageRequest.Builder(context)
                            .data(listUri.uri)
                            .diskCacheKey("${listUri.hash},${list.posterLastModified}")
                            .memoryCacheKey("${listUri.hash},${list.posterLastModified}")
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .zoomable(state)
                            .padding(
                                top = contentPadding.calculateTopPadding(),
                                bottom = contentPadding.calculateBottomPadding()
                            ),
                    )
                } else {
                    when {
                        items.isEmpty() -> {
                            ContentPreviewDefaults.PlaceholderPoster(
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        items.size < 4 -> {
                            val state = rememberZoomableState()
                            AsyncImage(
                                imageLoader = LocalContext.current.imageLoader,
                                model = ImageRequest.Builder(context)
                                    .data(items.first().toPoster())
                                    .crossfade(1_000)
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .zoomable(state)
                                    .padding(
                                        top = contentPadding.calculateTopPadding(),
                                        bottom = contentPadding.calculateBottomPadding()
                                    ),
                            )
                        }
                        else -> {
                            ContentPreviewDefaults.MultiItemPoster(
                                modifier = Modifier.fillMaxSize(),
                                items = items
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}


@Composable
private fun ActionsPill(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f)),
    ) {
        content()
    }
}