package com.example.connectin.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

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

@Serializable
data class MovieLog(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    @SerialName("movie_id")
    val movieId: Int,
    val title: String,
    @SerialName("genre_ids")
    val genreIds: List<Int>,
    @SerialName("poster_path")
    val posterPath: String?,
    @SerialName("watched_date")
    val watchedDate: String,
    val rating: Float? = null
)

@Serializable
data class User(
    val id: String,
    val email: String,
    val username: String,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    val bio: String? = null
)

data class UserSimilarity(
    val user: User,
    val similarityScore: Float,
    val sharedMovies: Int
)