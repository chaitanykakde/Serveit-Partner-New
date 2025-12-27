package com.nextserve.serveitpartnernew.ui.screen.welcome

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import com.nextserve.serveitpartnernew.R
import com.nextserve.serveitpartnernew.ui.components.PrimaryButton
import com.nextserve.serveitpartnernew.ui.theme.OrangeAccent
import com.nextserve.serveitpartnernew.ui.util.Dimens

@Composable
fun WelcomeScreen(
    onJoinClick: () -> Unit = {}
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White) // White background
    ) {
        // Top content with padding
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.paddingLg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Spacer (minimal)
            Spacer(modifier = Modifier.height(Dimens.spacingMd))
            
            // Logo - Use splash screen logo
            Image(
                painter = painterResource(id = R.drawable.serveit_partner_logo_light),
                contentDescription = "Serveit Partner Logo",
                modifier = Modifier.size(100.dp),
                contentScale = ContentScale.Fit
            )
            
            Spacer(modifier = Modifier.height(Dimens.spacingSm))
            
            // Headline (2 rows, centered)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Win more jobs.",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Earn more money.",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(Dimens.spacingXs))
            
            // Subtitle (centered)
            Text(
                text = "Join ServeIt as a professional and grow your business.",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(Dimens.spacingMd))
        }
        
        // Hero Image - Full screen width (no padding)
        Image(
            painter = painterResource(id = R.drawable.serveit_partner_onboarding_hero),
            contentDescription = "Professional handyman illustration",
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.FillWidth // Fill width, adjust height proportionally
        )
        
        // Bottom content with padding
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = Dimens.paddingLg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(Dimens.spacingMd))
            
            // 3 Benefit Items (below image, no overlap)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
            ) {
                BenefitItem(
                    iconRes = R.drawable.serveit_partner_onb_growth,
                    title = "Grow your income",
                    description = "Get access to high-quality jobs near you."
                )
                
                BenefitItem(
                    iconRes = R.drawable.serveit_partner_onb_verified,
                    title = "Work on your terms",
                    description = "Choose your schedule and service areas."
                )
                
                BenefitItem(
                    iconRes = R.drawable.serveit_partner_onb_payments,
                    title = "Fast, secure payments",
                    description = "Get paid quickly and securely."
                )
            }
            
            // Spacer (weight to push button down)
            Spacer(modifier = Modifier.weight(1f))
            
            // Join Button (bottom)
            PrimaryButton(
                text = "Join ServeIt",
                onClick = {
                    // Mark welcome as seen
                    val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                    prefs.edit().putBoolean("has_seen_welcome", true).apply()
                    onJoinClick()
                },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            )
            
            Spacer(modifier = Modifier.height(Dimens.spacingXs))
            
            // Footer Text (below button)
            Text(
                text = "By joining, you agree to our Terms of Service",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Dimens.spacingMd)
            )
        }
    }
}

@Composable
private fun BenefitItem(
    iconRes: Int,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMd),
        verticalAlignment = Alignment.Top
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                contentScale = ContentScale.Fit
            )
        }
        
        // Text
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(Dimens.spacingXs))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
