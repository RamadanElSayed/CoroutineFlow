package com.instant.coroutineflow.view.uimodel

// Sealed class representing user intents
sealed class UserIntent {
    object BasicLaunchExample : UserIntent()
    object APIRequestExample : UserIntent()
    object StartLongRunningTask : UserIntent()
    object CancelLongRunningTask : UserIntent()
    object FetchSequential : UserIntent()
    object FetchParallel : UserIntent()
    object FetchWithError : UserIntent()
    object RetryFailedRequest : UserIntent()
    object ChainedCoroutines : UserIntent()
    object TaskWithTimeout : UserIntent()
    object RetryFlowExample : UserIntent()
    object FlowTransformationExample : UserIntent()
    object CombineFlowsExample : UserIntent()
    object DebounceExample : UserIntent()
}
