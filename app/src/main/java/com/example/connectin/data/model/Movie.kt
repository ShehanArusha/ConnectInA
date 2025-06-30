package com.example.connectin.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Movie(
    val id: Int,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val releaseDate: String,
    val voteAverage: Double,
    val genreIds: List<Int>,
    val popularity: Double
) : Parcelable {
    val fullPosterPath: String
        get() = if (posterPath != null) {
            "https://image.tmdb.org/t/p/w500$posterPath"
        } else {
            ""
        }

    val fullBackdropPath: String
        get() = if (backdropPath != null) {
            "https://image.tmdb.org/t/p/w780$backdropPath"
        } else {
            ""
        }
}

@Parcelize
data class Genre(
    val id: Int,
    val name: String
) : Parcelable

data class MovieLog(
    val id: String? = null,
    val userId: String,
    val movieId: Int,
    val title: String,
    val genreIds: List<Int>,
    val posterPath: String?,
    val watchedDate: String,
    val rating: Float? = null
)

data class User(
    val id: String,
    val email: String,
    val username: String,
    val avatarUrl: String? = null,
    val bio: String? = null
)

data class UserSimilarity(
    val user: User,
    val similarityScore: Float,
    val sharedMovies: Int
)