package com.example.connectin.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.connectin.data.model.Movie
import com.example.connectin.data.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val repository = MovieRepository()

    private val _popularMovies = MutableStateFlow<List<Movie>>(emptyList())
    val popularMovies: StateFlow<List<Movie>> = _popularMovies.asStateFlow()

    private val _topRatedMovies = MutableStateFlow<List<Movie>>(emptyList())
    val topRatedMovies: StateFlow<List<Movie>> = _topRatedMovies.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    suspend fun loadMovies() {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                // Load popular movies
                repository.getPopularMovies().fold(
                    onSuccess = { movies ->
                        _popularMovies.value = movies
                    },
                    onFailure = { exception ->
                        _error.value = exception.message
                    }
                )

                // Load top rated movies
                repository.getTopRatedMovies().fold(
                    onSuccess = { movies ->
                        _topRatedMovies.value = movies
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
}