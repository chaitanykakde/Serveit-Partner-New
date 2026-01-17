package com.nextserve.serveitpartnernew.ui.onboarding.step5

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * FlowRow implementation that naturally wraps chips based on available width.
 * Only displays chips that fit in each row.
 */
@Composable
fun ServiceChipFlowRow(
    items: List<String>,
    isPrimary: Boolean = false,
    horizontalSpacing: androidx.compose.ui.unit.Dp = 8.dp,
    verticalSpacing: androidx.compose.ui.unit.Dp = 8.dp,
    modifier: Modifier = Modifier,
    content: @Composable (String, Boolean) -> Unit
) {
    val density = LocalDensity.current
    Layout(
        content = {
            items.forEach { item ->
                content(item, isPrimary)
            }
        },
        modifier = modifier
    ) { measurables, constraints ->
        val horizontalSpacingPx = with(density) { horizontalSpacing.roundToPx() }
        val verticalSpacingPx = with(density) { verticalSpacing.roundToPx() }
        
        val rows = mutableListOf<List<Placeable>>()
        var currentRow = mutableListOf<Placeable>()
        var currentRowWidth = 0
        
        measurables.forEach { measurable ->
            val placeable = measurable.measure(
                constraints.copy(minWidth = 0, minHeight = 0)
            )
            
            val itemWidth = placeable.width + horizontalSpacingPx
            
            if (currentRow.isEmpty() || currentRowWidth + itemWidth <= constraints.maxWidth) {
                currentRow.add(placeable)
                currentRowWidth += itemWidth
            } else {
                rows.add(currentRow)
                currentRow = mutableListOf(placeable)
                currentRowWidth = itemWidth
            }
        }
        
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
        }
        
        val totalHeight = rows.sumOf { row ->
            row.maxOfOrNull { it.height } ?: 0
        } + if (rows.size > 1) (rows.size - 1) * verticalSpacingPx else 0
        
        val width = rows.maxOfOrNull { row ->
            row.sumOf { it.width } + (row.size - 1) * horizontalSpacingPx
        } ?: 0
        
        layout(width, totalHeight) {
            var yPos = 0
            
            rows.forEach { row ->
                val rowHeight = row.maxOfOrNull { it.height } ?: 0
                var xPos = 0
                
                row.forEach { placeable ->
                    placeable.placeRelative(xPos, yPos)
                    xPos += placeable.width + horizontalSpacingPx
                }
                
                yPos += rowHeight + verticalSpacingPx
            }
        }
    }
}

