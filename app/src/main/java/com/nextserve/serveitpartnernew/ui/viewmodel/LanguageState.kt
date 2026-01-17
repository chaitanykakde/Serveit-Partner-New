package com.nextserve.serveitpartnernew.ui.viewmodel

/**
 * Language selection state.
 * Represents whether user has selected a language preference.
 */
sealed class LanguageState {
    /**
     * Language preference is unknown or not yet selected.
     * User needs to select a language.
     */
    object Unknown : LanguageState()

    /**
     * Language has been explicitly selected by user.
     * Can be any language including "en".
     */
    object Selected : LanguageState()
}

