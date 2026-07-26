package com.focustrack.app

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.glance.appwidget.updateAll
import com.focustrack.app.data.CategoryKind
import com.focustrack.app.data.CategoryOverrides
import com.focustrack.app.usage.DailySummary
import com.focustrack.app.usage.DayStat
import com.focustrack.app.usage.UsagePermission
import com.focustrack.app.usage.UsageRepository
import com.focustrack.app.widget.FocusWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Top-level UI state for the main screen. */
sealed interface DataState {
    data object Loading : DataState
    data object NoAccess : DataState
    data class Ready(val summary: DailySummary, val week: List<DayStat>) : DataState
}

class MainViewModel(private val appContext: Context) : ViewModel() {

    private val repo = UsageRepository(appContext)
    private val overrides = CategoryOverrides(appContext)

    private val _state = MutableStateFlow<DataState>(DataState.Loading)
    val state: StateFlow<DataState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val granted = UsagePermission.hasAccess(appContext)
            if (!granted) {
                _state.value = DataState.NoAccess
                return@launch
            }
            // Keep showing current content while re-loading; only show the
            // spinner on the very first load.
            if (_state.value !is DataState.Ready) _state.value = DataState.Loading
            val (summary, week) = withContext(Dispatchers.IO) { repo.loadAll() }
            _state.value = DataState.Ready(summary, week)
            // Push fresh data to the home-screen widget immediately.
            runCatching { FocusWidget().updateAll(appContext) }
        }
    }

    /** Persists a user-chosen category for an app and recomputes state. */
    fun setCategory(packageName: String, kind: CategoryKind) {
        overrides.setKind(packageName, kind)
        refresh()
    }

    /** Removes a user override, reverting the app to its default category. */
    fun clearCategory(packageName: String) {
        overrides.clear(packageName)
        refresh()
    }

    class Factory(private val appContext: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(appContext) as T
        }
    }
}
