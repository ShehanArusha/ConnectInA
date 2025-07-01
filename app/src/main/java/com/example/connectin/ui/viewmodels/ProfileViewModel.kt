package com.example.connectin.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.connectin.data.model.User
import com.example.connectin.data.model.UserSimilarity
import com.example.connectin.data.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {
    private val repository = MovieRepository()

    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile.asStateFlow()

    private val _friendSuggestions = MutableStateFlow<List<UserSimilarity>>(emptyList())
    val friendSuggestions: StateFlow<List<UserSimilarity>> = _friendSuggestions.asStateFlow()

    private val _movieCount = MutableStateFlow(0)
    val movieCount: StateFlow<Int> = _movieCount.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // Get current user info
                val currentUser = repository.getCurrentUser()
                currentUser?.let { userInfo ->
                    // For now, create a simple user profile
                    // In a real app, you'd fetch this from Supabase profiles table
                    _userProfile.value = User(
                        id = userInfo.id,
                        email = userInfo.email ?: "",
                        username = userInfo.userMetadata?.get("username") as? String ?: "User",
                        avatarUrl = null,
                        bio = "Movie enthusiast discovering great films!"
                    )
                }

                // Get movie count
                repository.getUserMovieLogs().fold(
                    onSuccess = { logs ->
                        _movieCount.value = logs.size
                    },
                    onFailure = { exception ->
                        _error.value = exception.message
                    }
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadFriendSuggestions() {
        viewModelScope.launch {
            repository.getFriendSuggestions().fold(
                onSuccess = { suggestions ->
                    _friendSuggestions.value = suggestions
                },
                onFailure = { exception ->
                    _error.value = exception.message
                }
            )
        }
    }

    suspend fun signOut() {
        repository.signOut()
    }
}