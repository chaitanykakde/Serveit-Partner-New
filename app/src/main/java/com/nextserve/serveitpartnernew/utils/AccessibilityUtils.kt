package com.nextserve.serveitpartnernew.utils

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * Utility class for accessibility improvements in the app
 */
object AccessibilityUtils {

    /**
     * Adds content description for screen readers
     */
    fun Modifier.contentDescription(description: String): Modifier {
        return this.semantics {
            contentDescription = description
        }
    }

    /**
     * Adds accessibility information for clickable elements
     */
    fun Modifier.accessibleClickable(
        label: String,
        roleDescription: String? = null
    ): Modifier {
        return this.semantics {
            contentDescription = label
            // roleDescription is not available in current Compose version
        }
    }

    /**
     * Adds accessibility information for images
     */
    fun Modifier.accessibleImage(
        contentDescription: String,
        isDecorative: Boolean = false
    ): Modifier {
        return this.semantics {
            if (!isDecorative) {
                this.contentDescription = contentDescription
            }
        }
    }

    /**
     * Adds accessibility information for progress indicators
     */
    fun Modifier.accessibleProgress(
        currentValue: Int,
        maxValue: Int,
        label: String
    ): Modifier {
        return this.semantics {
            contentDescription = "$label: $currentValue of $maxValue"
        }
    }
}