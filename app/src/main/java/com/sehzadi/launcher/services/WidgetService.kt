package com.sehzadi.launcher.services

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class WidgetType {
    LIVE_CLOCK,
    SYSTEM_STATS,
    NOTES,
    NONE
}

@Singleton
class WidgetService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _activeWidget = MutableStateFlow(WidgetType.NONE)
    val activeWidget: StateFlow<WidgetType> = _activeWidget.asStateFlow()

    private val _showWidget = MutableStateFlow(false)
    val showWidget: StateFlow<Boolean> = _showWidget.asStateFlow()

    fun showLiveClock() {
        _activeWidget.value = WidgetType.LIVE_CLOCK
        _showWidget.value = true
    }

    fun showSystemStats() {
        _activeWidget.value = WidgetType.SYSTEM_STATS
        _showWidget.value = true
    }

    fun showLiveNotes() {
        _activeWidget.value = WidgetType.NOTES
        _showWidget.value = true
    }

    fun dismissWidget() {
        _showWidget.value = false
        _activeWidget.value = WidgetType.NONE
    }

    fun toggleWidget(type: WidgetType) {
        if (_activeWidget.value == type && _showWidget.value) {
            dismissWidget()
        } else {
            _activeWidget.value = type
            _showWidget.value = true
        }
    }
}
