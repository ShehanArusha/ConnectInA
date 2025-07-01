package com.example.connectin.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.connectin.data.repository.MovieRepository
import io.github.jan.supabase.gotrue.user.UserInfo
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val repository = MovieRepository()

    suspend fun signIn(email: String, password: String): Result<UserInfo> {
        return repository.signIn(email, password)
    }

    suspend fun signUp(email: String, password: String, username: String): Result<UserInfo> {
        return repository.signUp(email, password, username)
    }

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
        }
    }

    fun getCurrentUser() = repository.getCurrentUser()
}