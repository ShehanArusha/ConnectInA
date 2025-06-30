package com.example.connectin.data.api

import com.example.connectin.data.model.Genre
import com.example.connectin.data.model.Movie
import retrofit2.http.GET
import retrofit2.http.Query

// Response wrapper classes
data class MovieResponse(
    val page: Int,
    val results: List<MovieDto>,
    val total_pages: Int,
    val total_results: Int
)

data class MovieDto(
    val id: Int,
    val title: String,
    val overview: String,
    val poster_path: String?,
    val backdrop_path: String?,
    val release_date: String,
    val vote_average: Double,
    val genre_ids: List<Int>,
    val popularity: Double
) {
    fun toMovie() = Movie(
        id = id,
        title = title,
        overview = overview,
        posterPath = poster_path,
        backdropPath = backdrop_path,
        releaseDate = release_date,
        voteAverage = vote_average,
        genreIds = genre_ids,
        popularity = popularity
    )
}

data class GenreResponse(
    val genres: List<Genre>
)

interface TmdbApi {
    @GET("movie/popular")
    suspend fun getPopularMovies(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1
    ): MovieResponse

    @GET("movie/top_rated")
    suspend fun getTopRatedMovies(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1
    ): MovieResponse

    @GET("movie/now_playing")
    suspend fun getNowPlayingMovies(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1
    ): MovieResponse

    @GET("search/movie")
    suspend fun searchMovies(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("page") page: Int = 1
    ): MovieResponse

    @GET("genre/movie/list")
    suspend fun getGenres(
        @Query("api_key") apiKey: String
    ): GenreResponse

    @GET("discover/movie")
    suspend fun discoverMovies(
        @Query("api_key") apiKey: String,
        @Query("with_genres") genres: String? = null,
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("page") page: Int = 1
    ): MovieResponse
}