package com.instant.coroutineflow.model

// Sealed class to manage API responses (Success, Error, Loading)
sealed class UIResources<out T> {
    // Success state which holds the data of type T
    data class Success<T>(val data: T) : UIResources<T>()

    // Error state which holds the error message in case of failure
    data class Error(val message: String) : UIResources<Nothing>()

    // Loading state which represents the ongoing process or loading indicator
    object Loading : UIResources<Nothing>()
}
