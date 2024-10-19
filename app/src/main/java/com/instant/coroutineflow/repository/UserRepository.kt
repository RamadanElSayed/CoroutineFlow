package com.instant.coroutineflow.repository

import com.instant.coroutineflow.model.UIResources
import com.instant.coroutineflow.model.User
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

class UserRepository {

    // Function to simulate fetching users with Flow
    fun fetchUsers(): Flow<UIResources<List<User>>> = flow {

        // Emit the Loading state initially to indicate the operation has started
        emit(UIResources.Loading)
        try {
            // Simulate a delay as if a network request is being made
            delay(2000L)

            // Simulate a list of users fetched from the server
            val users = listOf(User(1, "John"), User(2, "Jane"), User(3, "Bob"))

            // Emit the Success state with the list of users
            emit(UIResources.Success(users))
        } catch (e: Exception) {
            // If an exception occurs, emit the Error state with a failure message
            emit(UIResources.Error("Failed to fetch users"))
        }
    }

    // Function to simulate fetching users in a sequential manner
    fun fetchSequentialUsers(): Flow<UIResources<List<User>>> = flow {
        // Emit the Loading state initially to indicate the operation has started
        emit(UIResources.Loading)
        try {
            // Simulate a delay for the first part of data fetching
            delay(1000L)
            val users = listOf(User(1, "Alice"))

            // Simulate a delay for the second part of data fetching
            delay(1000L)
            val moreUsers = listOf(User(2, "Bob"))

            // Emit Success with the combined list of users fetched sequentially
            emit(UIResources.Success(users + moreUsers))
        } catch (e: Exception) {
            // If an exception occurs, emit the Error state with a failure message
            emit(UIResources.Error("Failed to fetch sequential users"))
        }
    }

    // Function to simulate fetching two lists of users in parallel
    fun fetchParallelUsers(): Flow<Pair<UIResources<List<User>>, UIResources<List<User>>>> = flow {
        // Emit the Loading state for both parallel requests
        emit(UIResources.Loading to UIResources.Loading)
        try {
            // Fetch the two lists of users in parallel by calling fetchUsers() twice
            val users = fetchUsers()
            val moreUsers = fetchUsers()

            // Collect the first emission from each flow and emit them as a pair
            emit(users.first() to moreUsers.first())
        } catch (e: Exception) {
            // If an exception occurs, emit Error states for both parallel requests
            emit(
                UIResources.Error("Failed to fetch users") to UIResources.Error("Failed to fetch more users")
            )
        }
    }

    // Function to retry fetching users with a given retry count
    fun retryFetchingUsers(retryCount: Int = 3): Flow<UIResources<List<User>>> = flow {
        var attempts = 0
        var success = false

        // Retry loop that continues until the maximum retry count is reached or success is achieved
        while (attempts < retryCount && !success) {
            try {
                // Call fetchUsers() and collect the result
                fetchUsers().collect { result ->
                    when (result) {
                        // If the fetch is successful, emit the result and mark success
                        is UIResources.Success -> {
                            emit(result)
                            success = true
                        }
                        // If an error occurs and this is the last attempt, emit the error
                        is UIResources.Error -> {
                            if (attempts == retryCount - 1) {
                                emit(result)
                            }
                        }
                        // Emit the Loading state if it's encountered
                        is UIResources.Loading -> {
                            emit(result)
                        }
                    }
                }
            } catch (e: Exception) {
                // If an exception is thrown, emit an error message
                emit(UIResources.Error("Failed to retry fetching users: ${e.message}"))
            }

            // Increment the attempt count and introduce a delay before the next retry attempt
            attempts++
            if (!success) delay(1000L)
        }

        // If after all attempts we still haven't succeeded, emit an error message
        if (!success) {
            emit(UIResources.Error("Retry failed after $retryCount attempts"))
        }
    }
}



