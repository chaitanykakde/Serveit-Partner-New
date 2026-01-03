package com.nextserve.serveitpartnernew.data.repository

import com.nextserve.serveitpartnernew.data.model.Job
import com.nextserve.serveitpartnernew.data.model.OptimizedRoute
import com.nextserve.serveitpartnernew.data.model.RouteWaypoint
import kotlin.math.*

data class OptimizedRoute(
    val waypoints: List<RouteWaypoint>,
    val totalDistance: Double,
    val totalDuration: Long, // in minutes
    val optimizationScore: Double // 0-1, higher is better
)

data class RouteWaypoint(
    val job: Job,
    val order: Int, // 0-based index in optimized route
    val distanceToNext: Double? = null, // km to next waypoint
    val durationToNext: Long? = null // minutes to next waypoint
)

class RouteOptimizationRepository(
    private val locationRepository: LocationRepository
) {

    /**
     * Optimize route for multiple jobs using nearest neighbor algorithm
     * This is a simple heuristic - for production, consider using Google Directions API
     */
    suspend fun optimizeRoute(
        jobs: List<Job>,
        currentLocation: android.location.Location
    ): Result<OptimizedRoute> {
        return try {
            if (jobs.isEmpty()) {
                return Result.success(
                    OptimizedRoute(
                        waypoints = emptyList(),
                        totalDistance = 0.0,
                        totalDuration = 0L,
                        optimizationScore = 1.0
                    )
                )
            }

            if (jobs.size == 1) {
                val singleWaypoint = RouteWaypoint(
                    job = jobs[0],
                    order = 0
                )
                return Result.success(
                    OptimizedRoute(
                        waypoints = listOf(singleWaypoint),
                        totalDistance = 0.0,
                        totalDuration = 0L,
                        optimizationScore = 1.0
                    )
                )
            }

            // Use nearest neighbor algorithm for route optimization
            val optimizedOrder = nearestNeighborOptimization(jobs, currentLocation)
            val waypoints = createWaypoints(jobs, optimizedOrder, currentLocation)

            // Calculate route metrics
            val totalDistance = waypoints.sumOf { it.distanceToNext ?: 0.0 }
            val totalDuration = waypoints.sumOf { it.durationToNext ?: 0L }

            // Calculate optimization score (0-1, higher is better)
            val score = calculateOptimizationScore(jobs, optimizedOrder, currentLocation)

            Result.success(
                OptimizedRoute(
                    waypoints = waypoints,
                    totalDistance = totalDistance,
                    totalDuration = totalDuration,
                    optimizationScore = score
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Nearest neighbor algorithm for route optimization
     */
    private fun nearestNeighborOptimization(
        jobs: List<Job>,
        startLocation: android.location.Location
    ): List<Int> {
        val unvisited = jobs.indices.toMutableList()
        val route = mutableListOf<Int>()

        // Start from current location
        var currentLat = startLocation.latitude
        var currentLng = startLocation.longitude

        while (unvisited.isNotEmpty()) {
            // Find nearest unvisited job
            var nearestIndex = -1
            var minDistance = Double.MAX_VALUE

            unvisited.forEach { index ->
                val job = jobs[index]
                val jobLat = job.jobCoordinates?.latitude ?: return@forEach
                val jobLng = job.jobCoordinates?.longitude ?: return@forEach

                val distance = locationRepository.calculateDistance(
                    currentLat, currentLng, jobLat, jobLng
                )

                if (distance < minDistance) {
                    minDistance = distance
                    nearestIndex = index
                }
            }

            if (nearestIndex == -1) break

            route.add(nearestIndex)
            unvisited.remove(nearestIndex)

            // Update current location to this job
            val nextJob = jobs[nearestIndex]
            currentLat = nextJob.jobCoordinates?.latitude ?: currentLat
            currentLng = nextJob.jobCoordinates?.longitude ?: currentLng
        }

        return route
    }

    /**
     * Create waypoints from optimized job order
     */
    private fun createWaypoints(
        jobs: List<Job>,
        optimizedOrder: List<Int>,
        startLocation: android.location.Location
    ): List<RouteWaypoint> {
        val waypoints = mutableListOf<RouteWaypoint>()
        var currentLat = startLocation.latitude
        var currentLng = startLocation.longitude

        optimizedOrder.forEachIndexed { order, jobIndex ->
            val job = jobs[jobIndex]
            val jobLat = job.jobCoordinates?.latitude ?: return@forEachIndexed
            val jobLng = job.jobCoordinates?.longitude ?: return@forEachIndexed

            // Calculate distance and duration to this job
            val distance = locationRepository.calculateDistance(
                currentLat, currentLng, jobLat, jobLng
            )

            // Estimate duration (assuming average speed of 30 km/h for urban areas)
            val duration = ((distance / 30.0) * 60).toLong() // minutes

            val waypoint = RouteWaypoint(
                job = job,
                order = order,
                distanceToNext = if (order < optimizedOrder.size - 1) {
                    // Calculate distance to next job
                    val nextJobIndex = optimizedOrder[order + 1]
                    val nextJob = jobs[nextJobIndex]
                    val nextLat = nextJob.jobCoordinates?.latitude ?: jobLat
                    val nextLng = nextJob.jobCoordinates?.longitude ?: jobLng
                    locationRepository.calculateDistance(jobLat, jobLng, nextLat, nextLng)
                } else null,
                durationToNext = if (order < optimizedOrder.size - 1) {
                    val nextDistance = waypoints.lastOrNull()?.distanceToNext ?: distance
                    ((nextDistance / 30.0) * 60).toLong()
                } else null
            )

            waypoints.add(waypoint)

            // Update current location
            currentLat = jobLat
            currentLng = jobLng
        }

        return waypoints
    }

    /**
     * Calculate optimization score (0-1)
     * Higher score means better route optimization
     */
    private fun calculateOptimizationScore(
        jobs: List<Job>,
        route: List<Int>,
        startLocation: android.location.Location
    ): Double {
        if (route.size <= 1) return 1.0

        // Calculate total route distance
        var totalDistance = 0.0
        var currentLat = startLocation.latitude
        var currentLng = startLocation.longitude

        route.forEach { jobIndex ->
            val job = jobs[jobIndex]
            val jobLat = job.jobCoordinates?.latitude ?: return@forEach
            val jobLng = job.jobCoordinates?.longitude ?: return@forEach

            totalDistance += locationRepository.calculateDistance(
                currentLat, currentLng, jobLat, jobLng
            )

            currentLat = jobLat
            currentLng = jobLng
        }

        // Calculate worst-case distance (sum of all distances from start to each job)
        var worstCaseDistance = 0.0
        jobs.forEach { job ->
            val jobLat = job.jobCoordinates?.latitude ?: return@forEach
            val jobLng = job.jobCoordinates?.longitude ?: return@forEach

            worstCaseDistance += locationRepository.calculateDistance(
                startLocation.latitude, startLocation.longitude, jobLat, jobLng
            )
        }

        // Score is inverse of normalized distance (closer to 0 means better optimization)
        val normalizedDistance = totalDistance / worstCaseDistance
        return max(0.0, 1.0 - normalizedDistance)
    }

    /**
     * Get route directions using Google Maps (opens external app)
     */
    fun openRouteInMaps(
        context: android.content.Context,
        waypoints: List<RouteWaypoint>,
        currentLocation: android.location.Location
    ) {
        if (waypoints.isEmpty()) return

        val uri = if (waypoints.size == 1) {
            // Single destination
            val job = waypoints[0].job
            val lat = job.jobCoordinates?.latitude ?: return
            val lng = job.jobCoordinates?.longitude ?: return
            "geo:$lat,$lng?q=$lat,$lng"
        } else {
            // Multiple waypoints
            val waypointsParam = waypoints.joinToString("|") { waypoint ->
                val job = waypoint.job
                "${job.jobCoordinates?.latitude},${job.jobCoordinates?.longitude}"
            }
            "https://www.google.com/maps/dir/?api=1&origin=${currentLocation.latitude},${currentLocation.longitude}&destination=${waypoints.last().job.jobCoordinates?.latitude},${waypoints.last().job.jobCoordinates?.longitude}&waypoints=$waypointsParam"
        }

        val intent = android.content.Intent(
            android.content.Intent.ACTION_VIEW,
            android.net.Uri.parse(uri)
        )
        intent.setPackage("com.google.android.apps.maps")
        context.startActivity(intent)
    }
}
