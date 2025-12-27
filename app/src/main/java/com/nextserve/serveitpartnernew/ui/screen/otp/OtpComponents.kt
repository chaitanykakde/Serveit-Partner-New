package com.nextserve.serveitpartnernew.ui.screen.otp

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nextserve.serveitpartnernew.R
import com.nextserve.serveitpartnernew.ui.theme.OrangeAccent
import com.nextserve.serveitpartnernew.ui.util.Dimens

object OtpComponents {
    
    @Composable
    fun ServeitLogo() {
        // Use splash screen logo image - centered
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.serveit_partner_logo_light),
                contentDescription = "Serveit Partner Logo",
                modifier = Modifier.size(150.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
        }
    }
}

