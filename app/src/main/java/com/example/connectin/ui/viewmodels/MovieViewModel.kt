package com.example.connectin.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.connectin.data.model.Movie
import com.example.connectin.data.model.MovieLog
import com.example.connectin.data.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MovieViewModel : ViewModel() {
    private val repository = MovieRepository()

    private val _recommendations = MutableStateFlow<List<Pair<Movie, Double>>>(emptyList())
    val recommendations: StateFlow<List<Pair<Movie, Double>>> = _recommendations.asStateFlow()

    private val _userMovieLogs = MutableStateFlow<List<MovieLog>>(emptyList())
    val userMovieLogs: StateFlow<List<MovieLog>> = _userMovieLogs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Movie>>(emptyList())
    val searchResults: StateFlow<List<Movie>> = _searchResults.asStateFlow()

    fun loadRecommendations() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.getRecommendedMovies().collect { result ->
                result.fold(
                    onSuccess = { recommendations ->
                        _recommendations.value = recommendations
                    },
                    onFailure = { exception ->
                        _error.value = exception.message
                    }
                )
                _isLoading.value = false
            }
        }
    }

    fun loadUserMovieLogs() {
        viewModelScope.launch {
            repository.getUserMovieLogs().fold(
                onSuccess = { logs ->
                    _userMovieLogs.value = logs
                },
                onFailure = { exception ->
                    _error.value = exception.message
                }
            )
        }
    }

    suspend fun logMovie(movie: Movie, rating: Float? = null) {
        repository.logMovie(movie, rating).fold(
            onSuccess = {
                // Reload logs and recommendations after logging
                loadUserMovieLogs()
                loadRecommendations()
            },
            onFailure = { exception ->
                _error.value = exception.message
            }
        )
    }

    suspend fun searchMovies(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        repository.searchMovies(query).fold(
            onSuccess = { movies ->
                _searchResults.value = movies
            },
            onFailure = { exception ->
                _error.value = exception.message
            }
        )
    }
}