package com.instant.coroutineflow.view.uimodel

// Sealed Class for Snackbar Event Channel
sealed class SnackbarEffect {
    data class ShowSnackbar(val message: String) : SnackbarEffect()
}