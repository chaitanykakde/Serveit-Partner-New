package com.nextserve.serveitpartnernew.domain.onboarding

/**
 * Domain model representing onboarding steps.
 * Type-safe replacement for magic step numbers.
 */
enum class OnboardingStep(val stepNumber: Int, val displayName: String) {
    BASIC_INFO(1, "Basic Information"),
    SERVICE_SELECTION(2, "Service Selection"),
    LOCATION(3, "Location"),
    VERIFICATION(4, "Verification"),
    REVIEW(5, "Review & Submit");

    companion object {
        fun fromStepNumber(stepNumber: Int): OnboardingStep {
            return values().find { it.stepNumber == stepNumber }
                ?: throw IllegalArgumentException("Invalid step number: $stepNumber")
        }

        fun isValidStep(stepNumber: Int): Boolean {
            return stepNumber in 1..5
        }
    }
}
