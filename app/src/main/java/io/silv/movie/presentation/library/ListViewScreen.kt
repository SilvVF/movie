package io.silv.movie.presentation.library

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import io.silv.core_ui.components.FastScrollLazyColumn
import io.silv.movie.data.lists.ContentItem
import io.silv.movie.data.lists.ContentList
import io.silv.movie.data.lists.ContentListRepository
import io.silv.movie.presentation.library.components.ListViewTopBar
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf


class ListViewScreenModel(
    contentListRepository: ContentListRepository,
    listId: Long
): StateScreenModel<ListViewState>(ListViewState.Loading) {

    private val stateSuccessTrigger =
        state.map { it.success?.list?.id }.filterNotNull().distinctUntilChanged()

    init {
        screenModelScope.launch {
            val list = runCatching { contentListRepository.getList(listId) }.getOrNull()
            if (list != null) {
                mutableState.value = ListViewState.Success(list = list)
            } else {
                mutableState.value = ListViewState.Error("No list found")
            }
        }

        contentListRepository.observeListById(listId)
            .combine(stateSuccessTrigger) { a, b ->  a  }
            .onEach { list ->
                mutableState.updateSuccess { state ->
                    state.copy(list = list)
                }
            }
            .launchIn(screenModelScope)

        contentListRepository.observeListItemsByListId(listId)
            .combine(stateSuccessTrigger) { a, b ->  a  }
            .onEach { content ->
                val items = content.toImmutableList()
                mutableState.updateSuccess { state ->
                    state.copy(items = items)
                }
            }
            .launchIn(screenModelScope)
    }

    private fun MutableStateFlow<ListViewState>.updateSuccess(
        function: (ListViewState.Success) -> ListViewState.Success
    ) {
        update {
            when (it) {
                is ListViewState.Success -> function(it)
                else -> it
            }
        }
    }
}

sealed interface ListViewState {
    data object Loading: ListViewState
    data class Error(val message: String): ListViewState
    data class Success(
        val list: ContentList,
        val items: ImmutableList<ContentItem> = persistentListOf()
    ): ListViewState

    val success: Success?
        get() = this as? Success
}
data class ListViewScreen(
    private val listId: Long
): Screen {

    @Composable
    override fun Content() {

        val screenModel = getScreenModel<ListViewScreenModel> { parametersOf(listId) }

        val state by screenModel.state.collectAsStateWithLifecycle()

        when (val s = state) {
            is ListViewState.Error -> {

            }
            ListViewState.Loading -> {

            }
            is ListViewState.Success -> {
                SuccessScreenContent(state = s)
            }
        }
    }
}

@Composable
private fun SuccessScreenContent(
    state: ListViewState.Success
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            ListViewTopBar(
                scrollBehavior = scrollBehavior,
                query = { "" },
                changeQuery = {},
                onSearch = {},
                items = { state.items },
                contentListProvider = { state.list }
            )
        }
    ) { paddingValues ->
        FastScrollLazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues
        ) {

        }
    }
}