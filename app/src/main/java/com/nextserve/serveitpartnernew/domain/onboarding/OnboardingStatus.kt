package com.nextserve.serveitpartnernew.domain.onboarding

/**
 * Domain model representing onboarding status.
 * Type-safe replacement for magic strings.
 */
enum class OnboardingStatus(val statusString: String, val displayName: String) {
    NOT_STARTED("NOT_STARTED", "Not Started"),
    IN_PROGRESS("IN_PROGRESS", "In Progress"),
    SUBMITTED("SUBMITTED", "Submitted");

    companion object {
        fun fromStatusString(statusString: String?): OnboardingStatus {
            return values().find { it.statusString == statusString } ?: NOT_STARTED
        }
    }
}
