package com.malanchuk.svitlo.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.malanchuk.svitlo.R
import com.malanchuk.svitlo.data.ScheduleRepository
import com.malanchuk.svitlo.data.ScheduleStore
import com.malanchuk.svitlo.data.ScheduleUpdateBus
import com.malanchuk.svitlo.model.ScheduleSnapshot
import com.malanchuk.svitlo.util.StartupTrace
import com.malanchuk.svitlo.util.withLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

data class MainUiState(
    val snapshot: ScheduleSnapshot? = null,
    val selectedQueue: String = "1.1",
    val dayOffset: Int = 0,
    val correction30MinutesEnabled: Boolean = false,
    val refreshIntervalMinutes: Int = 15,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
) {
    val selectedDate: LocalDate get() = LocalDate.now().plusDays(dayOffset.toLong())
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ScheduleRepository(application)

    private fun localizedString(resId: Int): String {
        val languageTag = ScheduleStore(getApplication<Application>()).languageTag()
        return getApplication<Application>().withLanguage(languageTag).getString(resId)
    }
    private val _uiState = MutableStateFlow(
        MainUiState(
            snapshot = repository.loadFast(),
            selectedQueue = repository.selectedQueue(),
            correction30MinutesEnabled = repository.correction30MinutesEnabled(),
            refreshIntervalMinutes = repository.refreshIntervalMinutes(),
        ),
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        StartupTrace.mark("MainViewModel initialized")
        viewModelScope.launch {
            ScheduleUpdateBus.updates.collect { snapshot ->
                _uiState.update { state ->
                    if (state.snapshot == null || snapshot.fetchedAt > state.snapshot.fetchedAt) {
                        state.copy(snapshot = snapshot)
                    } else {
                        state
                    }
                }
            }
        }
        if (_uiState.value.snapshot == null) {
            viewModelScope.launch {
                val snapshot = withContext(Dispatchers.IO) { repository.load() }
                _uiState.update { state ->
                    if (state.snapshot == null) state.copy(snapshot = snapshot) else state
                }
                StartupTrace.mark("local snapshot loaded asynchronously")
            }
        }
    }

    fun reloadCachedSchedule() {
        viewModelScope.launch {
            val snapshot = withContext(Dispatchers.IO) { repository.load() }
            if (snapshot != null) {
                _uiState.update { state ->
                    if (state.snapshot == null || snapshot.fetchedAt > state.snapshot.fetchedAt) {
                        state.copy(snapshot = snapshot)
                    } else {
                        state
                    }
                }
            } else if (_uiState.value.snapshot == null) {
                refresh()
            }
        }
    }

    fun refresh() {
        if (_uiState.value.isRefreshing) return
        _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
        StartupTrace.mark("refresh coroutine requested")
        viewModelScope.launch {
            StartupTrace.mark("refresh coroutine started")
            runCatching { repository.refresh() }
                .onSuccess { result ->
                    StartupTrace.mark("refresh completed successfully")
                    _uiState.update { it.copy(snapshot = result.snapshot, isRefreshing = false) }
                }
                .onFailure { error ->
                    StartupTrace.mark("refresh failed: ${error.javaClass.simpleName}")
                    Log.e("Svitlo", "Schedule refresh failed", error)
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            errorMessage = localizedString(R.string.refresh_error),
                        )
                    }
                }
        }
    }

    fun selectQueue(queue: String) {
        repository.setSelectedQueue(queue)
        _uiState.update { it.copy(selectedQueue = queue) }
    }

    fun selectDay(offset: Int) {
        _uiState.update { it.copy(dayOffset = offset.coerceIn(0, 1)) }
    }

    fun setCorrection30MinutesEnabled(enabled: Boolean) {
        repository.setCorrection30MinutesEnabled(enabled)
        _uiState.update { it.copy(correction30MinutesEnabled = enabled) }
    }

    fun setLanguage(languageTag: String) {
        repository.setLanguageTag(languageTag)
    }

    fun setRefreshInterval(minutes: Int): Boolean {
        val accepted = repository.setRefreshIntervalMinutes(minutes)
        if (accepted) _uiState.update { it.copy(refreshIntervalMinutes = minutes) }
        return accepted
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
