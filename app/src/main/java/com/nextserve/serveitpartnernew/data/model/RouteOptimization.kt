package com.nextserve.serveitpartnernew.data.model

import com.nextserve.serveitpartnernew.data.model.Job

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
