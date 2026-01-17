package com.nextserve.serveitpartnernew.ui.viewmodel

/**
 * Provider verification state.
 * Represents the current status of provider verification in Firestore.
 * This is separate from AuthState to allow deterministic navigation decisions.
 */
sealed class ProviderState {
    /**
     * Provider data is still loading from Firestore.
     * Navigation should wait until this resolves.
     */
    object Loading : ProviderState()

    /**
     * Provider needs to complete onboarding.
     * No provider document exists or onboarding is IN_PROGRESS.
     */
    object OnboardingRequired : ProviderState()

    /**
     * Provider has submitted onboarding and is waiting for admin verification.
     * verificationDetails.status == "pending" AND onboardingStatus == "SUBMITTED"
     */
    object PendingVerification : ProviderState()

    /**
     * Provider is verified and approved.
     * verificationDetails.status == "verified"
     */
    object Verified : ProviderState()

    /**
     * Provider was rejected by admin.
     * verificationDetails.status == "rejected"
     */
    data class Rejected(val reason: String?) : ProviderState()
}

