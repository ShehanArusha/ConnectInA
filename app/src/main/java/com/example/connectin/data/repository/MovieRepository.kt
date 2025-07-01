package com.example.connectin.data.repository

import com.example.connectin.BuildConfig
import com.example.connectin.data.api.TmdbApi
import com.example.connectin.data.model.Movie
import com.example.connectin.data.model.MovieLog
import com.example.connectin.data.model.User
import com.example.connectin.data.supabase.SupabaseClient
import com.example.connectin.ml.RecommendationEngine
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.user.UserInfo
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class MovieRepository {

    private val tmdbApi: TmdbApi = Retrofit.Builder()
        .baseUrl("https://api.themoviedb.org/3/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(TmdbApi::class.java)

    private val supabase = SupabaseClient.client
    private val apiKey = BuildConfig.TMDB_API_KEY

    // ===== Authentication =====

    suspend fun signUp(email: String, password: String, username: String): Result<UserInfo> {
        return try {
            val result = supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
                data = buildJsonObject {
                    put("username", username)
                }
            }

            result?.let {
                // Create user profile in database
                supabase.from("profiles").insert(
                    mapOf(
                        "id" to it.id,
                        "email" to email,
                        "username" to username
                    )
                )
                Result.success(it)
            } ?: Result.failure(Exception("Sign up failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, password: String): Result<UserInfo> {
        return try {
            val result = supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            result?.let {
                Result.success(it)
            } ?: Result.failure(Exception("Sign in failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        supabase.auth.signOut()
    }

    fun getCurrentUser() = supabase.auth.currentUserOrNull()

    // ===== Movie Data =====

    suspend fun getPopularMovies(): Result<List<Movie>> {
        return try {
            val response = tmdbApi.getPopularMovies(apiKey)
            Result.success(response.results.map { it.toMovie() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTopRatedMovies(): Result<List<Movie>> {
        return try {
            val response = tmdbApi.getTopRatedMovies(apiKey)
            Result.success(response.results.map { it.toMovie() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchMovies(query: String): Result<List<Movie>> {
        return try {
            val response = tmdbApi.searchMovies(apiKey, query)
            Result.success(response.results.map { it.toMovie() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun discoverMoviesByGenre(genreId: Int): Result<List<Movie>> {
        return try {
            val response = tmdbApi.discoverMovies(apiKey, genres = genreId.toString())
            Result.success(response.results.map { it.toMovie() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== Movie Logging =====

    suspend fun logMovie(movie: Movie, rating: Float? = null): Result<Unit> {
        return try {
            val userId = getCurrentUser()?.id ?: return Result.failure(Exception("User not logged in"))

            val movieLog = MovieLog(
                userId = userId,
                movieId = movie.id,
                title = movie.title,
                genreIds = movie.genreIds,
                posterPath = movie.posterPath,
                watchedDate = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
                rating = rating
            )

            supabase.from("movie_logs").insert(movieLog)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserMovieLogs(): Result<List<MovieLog>> {
        return try {
            val userId = getCurrentUser()?.id ?: return Result.failure(Exception("User not logged in"))

            val logs = supabase.from("movie_logs")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<MovieLog>()

            Result.success(logs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== ML-Based Recommendations =====

    suspend fun getRecommendedMovies(): Flow<Result<List<Pair<Movie, Double>>>> = flow {
        try {
            // Get user's watched movies
            val userLogs = getUserMovieLogs().getOrNull() ?: emptyList()
            if (userLogs.isEmpty()) {
                emit(Result.success(emptyList()))
                return@flow
            }

            // Convert logs to Movie objects
            val watchedMovies = userLogs.mapNotNull { log ->
                Movie(
                    id = log.movieId,
                    title = log.title,
                    overview = "",
                    posterPath = log.posterPath,
                    backdropPath = null,
                    releaseDate = "",
                    voteAverage = 0.0,
                    genreIds = log.genreIds,
                    popularity = 0.0
                )
            }

            // Get candidate movies from different sources
            val popularMovies = getPopularMovies().getOrNull() ?: emptyList()
            val topRatedMovies = getTopRatedMovies().getOrNull() ?: emptyList()

            // Combine candidate movies
            val candidateMovies = (popularMovies + topRatedMovies).distinctBy { it.id }

            // Get ML-based recommendations
            val recommendations = RecommendationEngine.getRecommendations(
                userWatchedMovies = watchedMovies,
                candidateMovies = candidateMovies,
                topN = 20
            )

            emit(Result.success(recommendations))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    // ===== Friend Suggestions =====

    suspend fun getFriendSuggestions(): Result<List<com.example.connectin.data.model.UserSimilarity>> {
        return try {
            val currentUserId = getCurrentUser()?.id ?: return Result.failure(Exception("User not logged in"))

            // Get current user's movies
            val currentUserLogs = getUserMovieLogs().getOrNull() ?: return Result.success(emptyList())
            val currentUserMovies = currentUserLogs.map { log ->
                Movie(
                    id = log.movieId,
                    title = log.title,
                    overview = "",
                    posterPath = log.posterPath,
                    backdropPath = null,
                    releaseDate = "",
                    voteAverage = 0.0,
                    genreIds = log.genreIds,
                    popularity = 0.0
                )
            }

            // Get all users except current user
            val allUsers = supabase.from("profiles")
                .select()
                .decodeList<User>()
                .filter { it.id != currentUserId }

            // Get movie logs for all other users
            val otherUsersWithMovies = allUsers.mapNotNull { user ->
                val userLogs = supabase.from("movie_logs")
                    .select {
                        filter {
                            eq("user_id", user.id)
                        }
                    }
                    .decodeList<MovieLog>()

                if (userLogs.isNotEmpty()) {
                    val userMovies = userLogs.map { log ->
                        Movie(
                            id = log.movieId,
                            title = log.title,
                            overview = "",
                            posterPath = log.posterPath,
                            backdropPath = null,
                            releaseDate = "",
                            voteAverage = 0.0,
                            genreIds = log.genreIds,
                            popularity = 0.0
                        )
                    }
                    user to userMovies
                } else {
                    null
                }
            }

            // Get ML-based friend suggestions
            val suggestions = RecommendationEngine.getFriendSuggestions(
                currentUserMovies = currentUserMovies,
                otherUsers = otherUsersWithMovies,
                topN = 10
            )

            Result.success(suggestions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}