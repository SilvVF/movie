package io.silv.movie.presentation.movie.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun FilterBottomSheet(
    onDismissRequest: () -> Unit,
    onApplyFilter: () -> Unit,
    onResetFilter: () -> Unit
) {
    ModalBottomSheet(
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        ),
        onDismissRequest = onDismissRequest,
        windowInsets = WindowInsets(0)
    ) {
        val screenHeight = LocalConfiguration.current.screenHeightDp.dp
        Column(modifier = Modifier.heightIn(max = screenHeight * 0.8f)) {
            Column(
                Modifier
                    .height(IntrinsicSize.Min)
                    .verticalScroll(rememberScrollState())
            ) {
                repeat(5) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Filter $it")
                        IconButton(onClick = { /*TODO*/ }) {
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowDown,
                                contentDescription = null
                            )
                        }
                    }
                }
            }
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .padding(
                        bottom = with(LocalDensity.current) {
                            WindowInsets.systemBars.getBottom(this).toDp()
                        }
                    )
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onResetFilter) {
                    Text("Reset")
                }
                Button(onClick = onApplyFilter) {
                    Text(text = "Filter")
                }
            }
        }
    }
}