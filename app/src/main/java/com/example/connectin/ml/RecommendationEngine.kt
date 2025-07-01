package com.example.connectin.ml

import com.example.connectin.data.model.Movie
import com.example.connectin.data.model.User
import com.example.connectin.data.model.UserSimilarity
import kotlin.math.sqrt

/**
 * ML-based recommendation engine using content-based filtering
 * Uses genre vectors and cosine similarity for movie recommendations
 */
object RecommendationEngine {

    // Genre IDs from TMDb API
    private val GENRE_MAP: Map<Int, String> = mapOf(
        28 to "Action",
        12 to "Adventure",
        16 to "Animation",
        35 to "Comedy",
        80 to "Crime",
        99 to "Documentary",
        18 to "Drama",
        10751 to "Family",
        14 to "Fantasy",
        36 to "History",
        27 to "Horror",
        10402 to "Music",
        9648 to "Mystery",
        10749 to "Romance",
        878 to "Science Fiction",
        10770 to "TV Movie",
        53 to "Thriller",
        10752 to "War",
        37 to "Western"
    )

    /**
     * Convert a movie to a feature vector based on genres
     * This is the ML representation of the movie
     */
    fun movieToVector(movie: Movie): DoubleArray {
        val vector = DoubleArray(GENRE_MAP.size) { 0.0 }
        val genreIndices = GENRE_MAP.keys.toList()

        movie.genreIds.forEach { genreId ->
            val index = genreIndices.indexOf(genreId)
            if (index != -1) {
                vector[index] = 1.0
            }
        }

        return vector
    }

    /**
     * Calculate cosine similarity between two vectors
     * This is a core ML similarity metric
     */
    fun cosineSimilarity(vectorA: DoubleArray, vectorB: DoubleArray): Double {
        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0

        for (i in vectorA.indices) {
            dotProduct += vectorA[i] * vectorB[i]
            normA += vectorA[i] * vectorA[i]
            normB += vectorB[i] * vectorB[i]
        }

        return if (normA == 0.0 || normB == 0.0) {
            0.0
        } else {
            dotProduct / (sqrt(normA) * sqrt(normB))
        }
    }

    /**
     * Create a user profile vector based on their watched movies
     * This aggregates user preferences using ML techniques
     */
    fun createUserProfile(watchedMovies: List<Movie>): DoubleArray {
        if (watchedMovies.isEmpty()) {
            return DoubleArray(GENRE_MAP.size) { 0.0 }
        }

        val profileVector = DoubleArray(GENRE_MAP.size) { 0.0 }

        // Aggregate genre preferences
        watchedMovies.forEach { movie ->
            val movieVector = movieToVector(movie)
            movieVector.forEachIndexed { index, value ->
                profileVector[index] = profileVector[index] + value
            }
        }

        // Normalize by the number of movies
        val movieCount = watchedMovies.size.toDouble()
        return DoubleArray(GENRE_MAP.size) { index ->
            profileVector[index] / movieCount
        }
    }

    /**
     * Get movie recommendations based on user profile
     * Uses content-based filtering with cosine similarity
     */
    fun getRecommendations(
        userWatchedMovies: List<Movie>,
        candidateMovies: List<Movie>,
        topN: Int = 10
    ): List<Pair<Movie, Double>> {
        // Create user profile vector
        val userProfile = createUserProfile(userWatchedMovies)

        // Filter out already watched movies
        val watchedIds = userWatchedMovies.map { it.id }.toSet()
        val unwatchedMovies = candidateMovies.filter { it.id !in watchedIds }

        // Calculate similarity scores for each candidate movie
        val movieScores = unwatchedMovies.map { movie ->
            val movieVector = movieToVector(movie)
            val similarity = cosineSimilarity(userProfile, movieVector)

            // Boost score based on movie popularity (hybrid approach)
            val popularityBoost = movie.popularity / 1000.0 // Normalize popularity
            val finalScore = (similarity * 0.7) + (popularityBoost * 0.3)

            movie to finalScore
        }

        // Sort by score and return top N
        return movieScores
            .sortedByDescending { it.second }
            .take(topN)
    }

    /**
     * Calculate user similarity based on movie preferences
     * Used for friend suggestions (collaborative filtering)
     */
    fun calculateUserSimilarity(
        userAMovies: List<Movie>,
        userBMovies: List<Movie>
    ): Double {
        if (userAMovies.isEmpty() || userBMovies.isEmpty()) {
            return 0.0
        }

        // Create genre preference vectors for both users
        val userAProfile = createUserProfile(userAMovies)
        val userBProfile = createUserProfile(userBMovies)

        // Calculate cosine similarity between user profiles
        val cosineSim = cosineSimilarity(userAProfile, userBProfile)

        // Calculate Jaccard similarity based on movie overlap
        val movieIdsA = userAMovies.map { it.id }.toSet()
        val movieIdsB = userBMovies.map { it.id }.toSet()
        val intersection = movieIdsA.intersect(movieIdsB).size.toDouble()
        val union = movieIdsA.union(movieIdsB).size.toDouble()
        val jaccardSim = if (union > 0) intersection / union else 0.0

        // Combine both similarities (hybrid approach)
        return (cosineSim * 0.6) + (jaccardSim * 0.4)
    }

    /**
     * Get friend suggestions based on movie preferences
     * Implements collaborative filtering
     */
    fun getFriendSuggestions(
        currentUserMovies: List<Movie>,
        otherUsers: List<Pair<User, List<Movie>>>,
        topN: Int = 5
    ): List<UserSimilarity> {
        val suggestions = otherUsers.map { (user, movies) ->
            val similarity = calculateUserSimilarity(currentUserMovies, movies)
            val sharedMovies = currentUserMovies.map { it.id }.toSet()
                .intersect(movies.map { it.id }.toSet()).size

            UserSimilarity(
                user = user,
                similarityScore = similarity.toFloat(),
                sharedMovies = sharedMovies
            )
        }

        return suggestions
            .filter { it.similarityScore > 0.1 } // Minimum threshold
            .sortedByDescending { it.similarityScore }
            .take(topN)
    }
}