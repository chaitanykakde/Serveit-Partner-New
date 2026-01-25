package com.nextserve.serveitpartnernew.ui.auth.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Privacy Policy Dialog for Serveit Partner App
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Privacy Policy",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Last Updated: January 2026",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                PrivacySection(
                    title = "1. Introduction",
                    content = "Welcome to Serveit Partner. This Privacy Policy explains how we collect, use, disclose, and safeguard your information when you use our mobile application and services. By using Serveit Partner, you consent to the data practices described in this policy."
                )
                
                PrivacySection(
                    title = "2. Information We Collect",
                    content = """
                        We collect the following types of information:
                        
                        • Personal Information: Mobile number, name, email address, profile photo, address, and location data
                        • Service Information: Service categories, skills, certifications, work history, and ratings
                        • Financial Information: Bank account details for payouts, earnings, and transaction history
                        • Device Information: Device ID, operating system, app version, and usage analytics
                        • Location Data: Real-time location for job matching and service delivery
                        • Communication Data: Messages, call logs, and notifications exchanged through the platform
                    """.trimIndent()
                )
                
                PrivacySection(
                    title = "3. How We Use Your Information",
                    content = """
                        We use your information to:
                        
                        • Provide and improve our services, including job matching and notifications
                        • Verify your identity and professional credentials
                        • Process payments and manage earnings
                        • Send job notifications, updates, and important service communications
                        • Match you with relevant service requests based on location and skills
                        • Maintain service quality through ratings and reviews
                        • Comply with legal obligations and prevent fraud
                        • Analyze usage patterns to enhance user experience
                    """.trimIndent()
                )
                
                PrivacySection(
                    title = "4. Location Services",
                    content = "We collect and use your location data to match you with nearby service requests, calculate distances, and enable real-time tracking during active jobs. Location data is shared with customers only when you accept a job and during active service delivery. You can control location permissions through your device settings."
                )
                
                PrivacySection(
                    title = "5. Data Sharing",
                    content = """
                        We share your information with:
                        
                        • Customers: Basic profile, location (during active jobs), and service history
                        • Payment Processors: Bank details for secure payout processing
                        • Service Providers: Analytics, cloud storage, and communication services
                        • Legal Authorities: When required by law or to protect rights and safety
                        
                        We do not sell your personal information to third parties.
                    """.trimIndent()
                )
                
                PrivacySection(
                    title = "6. Data Security",
                    content = "We implement industry-standard security measures including encryption, secure authentication, and regular security audits to protect your personal information. However, no method of transmission over the internet is 100% secure."
                )
                
                PrivacySection(
                    title = "7. Your Rights",
                    content = """
                        You have the right to:
                        
                        • Access and update your personal information
                        • Request deletion of your account and data
                        • Opt-out of non-essential communications
                        • Withdraw consent for location tracking
                        • Request a copy of your data
                        
                        To exercise these rights, contact us through the app settings or support.
                    """.trimIndent()
                )
                
                PrivacySection(
                    title = "8. Data Retention",
                    content = "We retain your information for as long as your account is active or as needed to provide services. After account deletion, we may retain certain information for legal compliance, dispute resolution, and fraud prevention for up to 7 years."
                )
                
                PrivacySection(
                    title = "9. Children's Privacy",
                    content = "Serveit Partner is not intended for users under 18 years of age. We do not knowingly collect personal information from children."
                )
                
                PrivacySection(
                    title = "10. Changes to Privacy Policy",
                    content = "We may update this Privacy Policy from time to time. We will notify you of significant changes through the app or via email. Continued use of the service after changes constitutes acceptance of the updated policy."
                )
                
                PrivacySection(
                    title = "11. Contact Us",
                    content = "If you have questions about this Privacy Policy or our data practices, please contact us through the app support section or email us at privacy@serveit.com"
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        modifier = Modifier.fillMaxWidth(0.9f)
    )
}

@Composable
private fun PrivacySection(
    title: String,
    content: String
) {
    Column(
        modifier = Modifier.padding(bottom = 20.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 20.sp
        )
    }
}
