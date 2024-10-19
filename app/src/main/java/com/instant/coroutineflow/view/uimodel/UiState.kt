package com.instant.coroutineflow.view.uimodel

import com.instant.coroutineflow.model.User

// Data class representing the UI state
data class UiState(
    val isLoading: Boolean = false,
    val users: List<User> = emptyList(),
    val errorMessage: String? = null
)
