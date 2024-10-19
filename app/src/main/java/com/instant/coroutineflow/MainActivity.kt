package com.instant.coroutineflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.instant.coroutineflow.model.User
import com.instant.coroutineflow.view.UserViewModel
import com.instant.coroutineflow.view.uimodel.UserIntent
import kotlinx.coroutines.launch


// MainActivity that initializes the ViewModel and sets the Compose UI content
class MainActivity : ComponentActivity() {
    // Using Jetpack ViewModel from the activity's scope
    private val viewModel: UserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Setting the content to the CoroutineApp composable function
        setContent {
            CoroutineApp(viewModel)
        }
    }
}

// Extension function to insert an item in LazyListScope from outside
fun LazyListScope.addOutsideItem(content: @Composable () -> Unit) {
    // Adds a single item (external content) to LazyList scope
    item {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class) // Opting in for Material3 API
@Composable
fun CoroutineApp(viewModel: UserViewModel) {
    // Collecting the UI state from the ViewModel's state flow
    val uiState by viewModel.uiState.collectAsState()

    // Snackbar state to manage snackbar display
    val snackbarHostState = remember { SnackbarHostState() }

    // Coroutine scope for launching coroutines in Compose
    val coroutineScope = rememberCoroutineScope()

    // Mutable list to collect SharedFlow events for task progress and other messages
    val sharedFlowEvents = remember { mutableStateListOf<String>() }

    // Collecting SharedFlow for events and adding them to the mutable list
    LaunchedEffect(Unit) {
        viewModel.sharedFlow.collect { event ->
            sharedFlowEvents.add(event)
        }
    }

    // Collecting Snackbar messages from the Channel and showing them in a snackbar
    LaunchedEffect(Unit) {
        viewModel.snackbarFlow.collect { message ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    Scaffold(
        // Setting up the snackbar host for displaying messages
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(8.dp)
            )
        },
        // Top AppBar for the screen title
        topBar = {
            TopAppBar(title = {
                Text(
                    "MVI with Coroutines & Flow",
                    color = MaterialTheme.colorScheme.onPrimary
                )
            })
        },
        // Content section containing a LazyColumn to display dynamic UI
        content = { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize() // Filling the screen size
                    .padding(innerPadding)
                    .padding(16.dp), // Adding padding to content
                horizontalAlignment = Alignment.CenterHorizontally // Aligning items in the center
            ) {
                // Adding an external item at the top using the LazyListScope extension
                addOutsideItem {
                    Text(
                        "Welcome to the Coroutine Example",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Show a loading indicator if the UI state is loading
                if (uiState.isLoading) {
                    item {
                        CircularProgressIndicator() // Loading spinner
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Display the list of users if available in the UI state
                if (uiState.users.isNotEmpty()) {
                    items(uiState.users) { user ->
                        UserCard(user)
                    }
                }

                // Display error message if present in the UI state
                uiState.errorMessage?.let { error ->
                    item {
                        Text("Error: $error", color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Adding buttons for different actions (API requests, long-running tasks, etc.)
                addOutsideItem {
                    CoroutineAppButtons(viewModel = viewModel)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Display SharedFlow events from the list
                if (sharedFlowEvents.isNotEmpty()) {
                    item {
                        Text("SharedFlow Events:", style = MaterialTheme.typography.titleMedium)
                    }
                    items(sharedFlowEvents) { event ->
                        Text(event)
                    }
                }
            }
        }
    )
}

// Composable function to display user information in a card layout
@Composable
fun UserCard(user: User) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp), // Adding vertical padding between cards
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), // Card background color
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // Elevation for shadow effect
    ) {
        // Displaying user details inside the card
        Column(modifier = Modifier.padding(16.dp)) {
            Text("User ID: ${user.id}", style = MaterialTheme.typography.titleMedium)
            Text("Name: ${user.name}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// Composable function to display action buttons for triggering various coroutines
@Composable
fun CoroutineAppButtons(viewModel: UserViewModel) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Button triggering a basic launch coroutine example
        TextButton(onClick = { viewModel.processIntent(UserIntent.BasicLaunchExample) }) {
            Text("Basic Launch Example")
        }
        // Button for simulating an API request using coroutines
        TextButton(onClick = { viewModel.processIntent(UserIntent.APIRequestExample) }) {
            Text("Simulate API Request")
        }
        // Button to start a long-running task with coroutines
        TextButton(onClick = { viewModel.processIntent(UserIntent.StartLongRunningTask) }) {
            Text("Start Long-Running Task")
        }
        // Button to cancel the ongoing long-running task
        TextButton(onClick = { viewModel.processIntent(UserIntent.CancelLongRunningTask) }) {
            Text("Cancel Long-Running Task")
        }
        // Button to fetch data sequentially using coroutines
        TextButton(onClick = { viewModel.processIntent(UserIntent.FetchSequential) }) {
            Text("Fetch Sequential")
        }
        // Button to fetch data in parallel using coroutines
        TextButton(onClick = { viewModel.processIntent(UserIntent.FetchParallel) }) {
            Text("Fetch Parallel")
        }
        // Button to trigger a task that intentionally causes an error
        TextButton(onClick = { viewModel.processIntent(UserIntent.FetchWithError) }) {
            Text("Fetch With Error")
        }
        // Button to retry a failed request with retries
        TextButton(onClick = { viewModel.processIntent(UserIntent.RetryFailedRequest) }) {
            Text("Retry Failed Request")
        }
        // Button to trigger chained coroutines
        TextButton(onClick = { viewModel.processIntent(UserIntent.ChainedCoroutines) }) {
            Text("Chained Coroutines")
        }
        // Button to trigger a task that times out
        TextButton(onClick = { viewModel.processIntent(UserIntent.TaskWithTimeout) }) {
            Text("Task With Timeout")
        }
        // Button for retrying flow examples
        TextButton(onClick = { viewModel.processIntent(UserIntent.RetryFlowExample) }) {
            Text("Retry Flow Example")
        }
        // Button for demonstrating flow transformations
        TextButton(onClick = { viewModel.processIntent(UserIntent.FlowTransformationExample) }) {
            Text("Flow Transformation Example")
        }
        // Button for combining multiple flows
        TextButton(onClick = { viewModel.processIntent(UserIntent.CombineFlowsExample) }) {
            Text("Combine Flows Example")
        }
        // Button to demonstrate debouncing with flows
        TextButton(onClick = { viewModel.processIntent(UserIntent.DebounceExample) }) {
            Text("Debounce Example")
        }
    }
}
