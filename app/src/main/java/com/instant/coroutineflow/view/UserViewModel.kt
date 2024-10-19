package com.instant.coroutineflow.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.instant.coroutineflow.model.UIResources
import com.instant.coroutineflow.model.User
import com.instant.coroutineflow.repository.UserRepository
import com.instant.coroutineflow.view.uimodel.UiState
import com.instant.coroutineflow.view.uimodel.UserIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class UserViewModel : ViewModel() {

    private val repository = UserRepository()

    // MutableStateFlow for managing UI state and triggering UI updates
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    // SharedFlow for one-time events like task progress (used to emit events once)
    private val _sharedFlow = MutableSharedFlow<String>()
    val sharedFlow: SharedFlow<String> = _sharedFlow

    // Channel for Snackbar events to handle fire-and-forget events
    private val _snackbarChannel = Channel<String>()
    val snackbarFlow = _snackbarChannel.receiveAsFlow()

    // SupervisorJob ensures failure in one coroutine doesn’t cancel others
    private var supervisorJob = SupervisorJob()

    // To handle long-running tasks, we store the job here for cancellation if needed
    private var longRunningJob: Job? = null

    // Function to process various user intents (e.g., API requests, task actions)
    fun processIntent(intent: UserIntent) {
        when (intent) {
            is UserIntent.BasicLaunchExample -> basicLaunchExample()
            is UserIntent.APIRequestExample -> simulateAPIRequest()
            is UserIntent.StartLongRunningTask -> startLongRunningTask()
            is UserIntent.CancelLongRunningTask -> cancelLongRunningTask()
            is UserIntent.FetchSequential -> fetchSequential()
            is UserIntent.FetchParallel -> fetchParallel()
            is UserIntent.FetchWithError -> fetchWithError()
            is UserIntent.RetryFailedRequest -> retryFailedRequest()
            is UserIntent.ChainedCoroutines -> chainedCoroutines()
            is UserIntent.TaskWithTimeout -> taskWithTimeout()
            is UserIntent.RetryFlowExample -> retryFlowExample()
            is UserIntent.FlowTransformationExample -> flowTransformationExample()
            is UserIntent.CombineFlowsExample -> combineFlowsExample()
            is UserIntent.DebounceExample -> debounceExample()
        }
    }

    // Example of a simple coroutine launch with a delay
    private fun basicLaunchExample() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            delay(2000L) // Simulate a delay for demonstration purposes
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                users = listOf(User(1, "Test User")),
                errorMessage = null
            )
        }
    }

    // Example of fetching users with simulated API request, using SupervisorJob
    private fun simulateAPIRequest() {
        viewModelScope.launch(Dispatchers.IO + supervisorJob) {
            repository.fetchUsers().collect { result ->
                withContext(Dispatchers.Main) {
                    when (result) {
                        is UIResources.Loading -> {
                            _uiState.value = _uiState.value.copy(isLoading = true)
                        }
                        is UIResources.Success -> {
                            _uiState.value = _uiState.value.copy(isLoading = false, users = result.data)
                            _sharedFlow.emit("Fetched ${result.data.size} users")
                        }
                        is UIResources.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = result.message
                            )
                            _snackbarChannel.send(result.message)
                        }
                    }
                }
            }
        }
    }

    // Example of starting a long-running task with repeated steps
    private fun startLongRunningTask() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            longRunningJob = launch {
                repeat(5) { // Repeat task 5 times
                    delay(1000L) // Simulate each task taking 1 second
                    _sharedFlow.emit("Task Step ${it + 1} Complete")
                }
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    // Cancel the long-running task if it's still running
    private fun cancelLongRunningTask() {
        longRunningJob?.cancel() // Cancel the job if it's active
        _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Task Cancelled")
        viewModelScope.launch {
            _snackbarChannel.send("Task Cancelled")
        }
    }

    // Fetch users sequentially with delays between calls
    private fun fetchSequential() {
        viewModelScope.launch(Dispatchers.IO + supervisorJob) {
            repository.fetchSequentialUsers().collect { result ->
                withContext(Dispatchers.Main) {
                    when (result) {
                        is UIResources.Loading -> {
                            _uiState.value = _uiState.value.copy(isLoading = true)
                        }
                        is UIResources.Success -> {
                            _uiState.value = _uiState.value.copy(isLoading = false, users = result.data)
                        }
                        is UIResources.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = result.message
                            )
                            _snackbarChannel.send(result.message)
                        }
                    }
                }
            }
        }
    }

    // Fetch two user lists in parallel and combine their results
    private fun fetchParallel() {
        viewModelScope.launch(Dispatchers.IO + supervisorJob) {
            repository.fetchParallelUsers().collect { result ->
                withContext(Dispatchers.Main) {
                    val firstResult = result.first
                    val secondResult = result.second

                    when {
                        firstResult is UIResources.Loading || secondResult is UIResources.Loading -> {
                            _uiState.value = _uiState.value.copy(isLoading = true)
                        }
                        firstResult is UIResources.Success && secondResult is UIResources.Success -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                users = firstResult.data + secondResult.data
                            )
                        }
                        firstResult is UIResources.Error || secondResult is UIResources.Error -> {
                            val errorMessage = (firstResult as? UIResources.Error)?.message
                                ?: (secondResult as? UIResources.Error)?.message ?: "Unknown error"
                            _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = errorMessage)
                            _snackbarChannel.send("Error fetching parallel data: $errorMessage")
                        }
                    }
                }
            }
        }
    }

    // Example of fetching with an error
    private fun fetchWithError() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Simulated Error")
            _snackbarChannel.send("Simulated Error Occurred")
        }
    }

    // Example of retrying a failed request multiple times
    private fun retryFailedRequest() {
        viewModelScope.launch(Dispatchers.IO + supervisorJob) {
            repository.retryFetchingUsers().collect { result ->
                withContext(Dispatchers.Main) {
                    when (result) {
                        is UIResources.Loading -> {
                            _uiState.value = _uiState.value.copy(isLoading = true)
                        }
                        is UIResources.Success -> {
                            _uiState.value = _uiState.value.copy(isLoading = false, users = result.data)
                        }
                        is UIResources.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = result.message
                            )
                            _snackbarChannel.send(result.message)
                        }
                    }
                }
            }
        }
    }

    // Example of chained coroutines fetching users and then fetching more after a delay
    private fun chainedCoroutines() {
        viewModelScope.launch(Dispatchers.IO + supervisorJob) {
            repository.fetchUsers().collect { usersResult ->
                withContext(Dispatchers.Main) {
                    when (usersResult) {
                        is UIResources.Loading -> {
                            _uiState.value = _uiState.value.copy(isLoading = true)
                        }
                        is UIResources.Success -> {
                            delay(1000L) // Simulate waiting before fetching more
                            repository.fetchUsers().collect { moreUsersResult ->
                                withContext(Dispatchers.Main) {
                                    when (moreUsersResult) {
                                        is UIResources.Loading -> {
                                            _uiState.value = _uiState.value.copy(isLoading = true)
                                        }
                                        is UIResources.Success -> {
                                            _uiState.value = _uiState.value.copy(
                                                isLoading = false,
                                                users = usersResult.data + moreUsersResult.data
                                            )
                                        }
                                        is UIResources.Error -> {
                                            _uiState.value = _uiState.value.copy(
                                                isLoading = false,
                                                errorMessage = moreUsersResult.message
                                            )
                                            _snackbarChannel.send("Failed to fetch more users: ${moreUsersResult.message}")
                                        }
                                    }
                                }
                            }
                        }
                        is UIResources.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = usersResult.message
                            )
                            _snackbarChannel.send("Failed to fetch users: ${usersResult.message}")
                        }
                    }
                }
            }
        }
    }

    // Example of a long-running task that may timeout
    private fun taskWithTimeout() {
        viewModelScope.launch(Dispatchers.IO + supervisorJob) {
            try {
                withTimeout(3000L) { // Set timeout of 3 seconds
                    delay(4000L) // Simulate a task that takes too long
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            } catch (e: TimeoutCancellationException) {
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Task Timed Out")
                    _snackbarChannel.send("Task Timed Out")
                }
            }
        }
    }

    // Example of retrying flow-based fetching
    private fun retryFlowExample() {
        viewModelScope.launch(Dispatchers.IO + supervisorJob) {
            repository.fetchUsers()
                .retry(3) { // Retry 3 times with a delay
                    delay(1000L)
                    true // Continue retrying
                }
                .collect { result ->
                    withContext(Dispatchers.Main) {
                        when (result) {
                            is UIResources.Loading -> {
                                _uiState.value = _uiState.value.copy(isLoading = true)
                            }
                            is UIResources.Success -> {
                                _uiState.value = _uiState.value.copy(isLoading = false, users = result.data)
                            }
                            is UIResources.Error -> {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    errorMessage = result.message
                                )
                                _snackbarChannel.send(result.message)
                            }
                        }
                    }
                }
        }
    }

    // Example of transforming flow values
    private fun flowTransformationExample() {
        viewModelScope.launch(Dispatchers.IO + supervisorJob) {
            flowOf(1, 2, 3, 4)
                .filter { it > 2 }
                .map { "Transformed Item: $it" }
                .collect {
                    _sharedFlow.emit(it)
                }
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    // Example of combining two flows into one
    private fun combineFlowsExample() {
        viewModelScope.launch(Dispatchers.IO + supervisorJob) {
            val flow1 = flowOf(1, 2, 3)
            val flow2 = flowOf("A", "B", "C")
            flow1.combine(flow2) { number, letter ->
                "$number -> $letter"
            }.collect { combined ->
                _sharedFlow.emit(combined)
            }
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    // Example of debouncing flow emissions
    private fun debounceExample() {
        viewModelScope.launch(Dispatchers.IO + supervisorJob) {
            flowOf(1, 2, 3, 4)
                .debounce(300L) // Emit values only if no new value arrives within 300ms
                .collect { item ->
                    _sharedFlow.emit("Debounced Item: $item")
                }
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}


