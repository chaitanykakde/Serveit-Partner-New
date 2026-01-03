package com.nextserve.serveitpartnernew.ui.screen.payout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nextserve.serveitpartnernew.ui.viewmodel.BankAccount
import com.nextserve.serveitpartnernew.ui.viewmodel.MonthlySettlement
import com.nextserve.serveitpartnernew.ui.viewmodel.PayoutRequest
import com.nextserve.serveitpartnernew.ui.viewmodel.PayoutStatus
import com.nextserve.serveitpartnernew.ui.viewmodel.SettlementStatus
import com.nextserve.serveitpartnernew.utils.CurrencyUtils
import java.time.format.DateTimeFormatter

/**
 * Bank account section showing account status and details.
 */
@Composable
fun BankAccountSection(
    bankAccount: BankAccount?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "🏦",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Bank Account",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.weight(1f))

                // Status for cash payments
                AssistChip(
                    onClick = { /* Cash payments - no setup needed */ },
                    label = { Text("Cash Payments") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFF4CAF50).copy(alpha = 0.1f),
                        labelColor = androidx.compose.ui.graphics.Color(0xFF4CAF50)
                    )
                )
            }

            // Cash payment information
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Cash payments are processed directly",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Payouts are made in cash at the service location",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "No bank account setup required",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Available balance summary card.
 */
@Composable
fun AvailableBalanceCard(
    settlements: List<MonthlySettlement>,
    modifier: Modifier = Modifier
) {
    val availableBalance = settlements.sumOf { it.availableForPayout }

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Available for Payout",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = CurrencyUtils.formatCurrency(availableBalance),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            if (availableBalance > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Ready to withdraw",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/**
 * Monthly settlement card with payout option.
 */
@Composable
fun SettlementCard(
    settlement: MonthlySettlement,
    onRequestPayout: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPayoutDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with period and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = settlement.displayPeriod,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                SettlementStatusChip(status = settlement.settlementStatus)
            }

            // Summary stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatItem(
                    label = "Jobs",
                    value = settlement.completedJobs.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    label = "Earned",
                    value = CurrencyUtils.formatCurrency(settlement.partnerShare),
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    label = "Paid",
                    value = CurrencyUtils.formatCurrency(settlement.paidAmount),
                    modifier = Modifier.weight(1f)
                )
            }

            // Payout section
            if (settlement.availableForPayout > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Available: ${CurrencyUtils.formatCurrency(settlement.availableForPayout)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    OutlinedButton(
                        onClick = { onRequestPayout(settlement.availableForPayout) },
                        enabled = settlement.settlementStatus == SettlementStatus.READY
                    ) {
                        Text("Request Payout")
                    }
                }
            }
        }
    }
}

/**
 * Payout request card showing request status and details.
 */
@Composable
fun PayoutRequestCard(
    request: PayoutRequest,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = CurrencyUtils.formatCurrency(request.requestedAmount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
                Text(
                    text = request.requestedAt.format(dateFormatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            PayoutStatusChip(status = request.requestStatus)
        }
    }
}

/**
 * Settlement status chip.
 */
@Composable
fun SettlementStatusChip(status: SettlementStatus) {
    val (text, color) = when (status) {
        SettlementStatus.PENDING -> "Calculating" to Color(0xFFFF9800) // Orange
        SettlementStatus.READY -> "Ready" to Color(0xFF4CAF50) // Green
        SettlementStatus.REQUESTED -> "Requested" to Color(0xFF2196F3) // Blue
        SettlementStatus.PROCESSING -> "Processing" to Color(0xFFFF9800) // Orange
        SettlementStatus.SETTLED -> "Settled" to Color(0xFF4CAF50) // Green
        SettlementStatus.FAILED -> "Failed" to Color(0xFFF44336) // Red
    }

    AssistChip(
        onClick = { /* No action */ },
        label = { Text(text) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color.copy(alpha = 0.1f),
            labelColor = color
        )
    )
}

/**
 * Payout status chip.
 */
@Composable
fun PayoutStatusChip(status: PayoutStatus) {
    val (text, color) = when (status) {
        PayoutStatus.PENDING -> "Pending" to Color(0xFFFF9800) // Orange
        PayoutStatus.APPROVED -> "Approved" to Color(0xFF2196F3) // Blue
        PayoutStatus.PROCESSING -> "Processing" to Color(0xFFFF9800) // Orange
        PayoutStatus.COMPLETED -> "Completed" to Color(0xFF4CAF50) // Green
        PayoutStatus.FAILED -> "Failed" to Color(0xFFF44336) // Red
        PayoutStatus.CANCELLED -> "Cancelled" to Color(0xFF9E9E9E) // Grey
    }

    AssistChip(
        onClick = { /* No action */ },
        label = { Text(text) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color.copy(alpha = 0.1f),
            labelColor = color
        )
    )
}

/**
 * Statistic item for settlement cards.
 */
@Composable
private fun StatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Empty state when no payouts or settlements exist.
 */
@Composable
fun PayoutEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "💰",
            style = MaterialTheme.typography.displayLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No Payout History",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Complete jobs and request payouts to see your payment history here",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Skeleton loading state for the payout screen.
 */
@Composable
fun PayoutSkeleton() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Bank account skeleton
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(120.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Loading bank account...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Balance card skeleton
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(100.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Loading balance...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Settlement cards skeleton
        repeat(2) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(140.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Loading settlements...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
