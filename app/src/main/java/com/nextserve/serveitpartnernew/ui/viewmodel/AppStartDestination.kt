package com.nextserve.serveitpartnernew.ui.viewmodel

/**
 * Single source of truth for app startup navigation destination.
 * 
 * This is the ONLY place that determines where the app should navigate
 * after splash screen. All navigation decisions are made here based on
 * combined auth, provider, and language states.
 * 
 * NO SCREEN is allowed to make independent navigation decisions.
 */
sealed class AppStartDestination {
    /**
     * Stay on splash screen - waiting for state resolution.
     */
    object Splash : AppStartDestination()

    /**
     * Navigate to mobile number (login) screen.
     * User is logged out and needs to authenticate.
     */
    object MobileNumber : AppStartDestination()

    /**
     * Navigate to language selection screen.
     * User is authenticated, needs onboarding, but hasn't selected language.
     */
    object LanguageSelection : AppStartDestination()

    /**
     * Navigate to onboarding screen.
     * User is authenticated, needs onboarding, and has selected language.
     * Also used for pending verification (shows step 5 - review/verification status).
     */
    object Onboarding : AppStartDestination()

    /**
     * Navigate to home screen.
     * User is verified OR rejected.
     * Inner app screens handle the specific UI (rejected).
     */
    object Home : AppStartDestination()
}

