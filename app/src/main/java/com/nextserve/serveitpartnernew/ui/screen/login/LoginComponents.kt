package com.nextserve.serveitpartnernew.ui.screen.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nextserve.serveitpartnernew.R
import com.nextserve.serveitpartnernew.ui.theme.OrangeAccent
import com.nextserve.serveitpartnernew.ui.util.Dimens

object LoginComponents {
    
    @Composable
    fun ServeitLogo() {
        // Use splash screen logo image - centered
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.serveit_partner_logo_light),
                contentDescription = "Serveit Partner Logo",
                modifier = Modifier.size(150.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
    
    @Composable
    fun PhoneNumberInput(
        phoneNumber: String,
        onPhoneNumberChange: (String) -> Unit,
        modifier: Modifier = Modifier,
        isError: Boolean = false,
        errorMessage: String? = null
    ) {
        Row(
            modifier = modifier
                .height(Dimens.inputFieldHeight)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // +91 Prefix Box (Gray) with India flag
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(Dimens.inputFieldHeight)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(
                            topStart = Dimens.inputFieldCornerRadius,
                            bottomStart = Dimens.inputFieldCornerRadius,
                            topEnd = 0.dp,
                            bottomEnd = 0.dp
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(
                            topStart = Dimens.inputFieldCornerRadius,
                            bottomStart = Dimens.inputFieldCornerRadius,
                            topEnd = 0.dp,
                            bottomEnd = 0.dp
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🇮🇳",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "+91",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                    )
                }
            }
            
            // Phone Number Input Field with border
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(Dimens.inputFieldHeight)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(
                            topStart = 0.dp,
                            bottomStart = 0.dp,
                            topEnd = Dimens.inputFieldCornerRadius,
                            bottomEnd = Dimens.inputFieldCornerRadius
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(
                            topStart = 0.dp,
                            bottomStart = 0.dp,
                            topEnd = Dimens.inputFieldCornerRadius,
                            bottomEnd = Dimens.inputFieldCornerRadius
                        )
                    )
            ) {
                TextField(
                    value = phoneNumber,
                    onValueChange = onPhoneNumberChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.inputFieldHeight),
                    placeholder = {
                        Text(
                            text = "Enter mobile number",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        errorContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        errorIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(
                        topStart = 0.dp,
                        bottomStart = 0.dp,
                        topEnd = Dimens.inputFieldCornerRadius,
                        bottomEnd = Dimens.inputFieldCornerRadius
                    ),
                    singleLine = true,
                    isError = isError,
                    supportingText = if (isError && errorMessage != null) {
                        { Text(text = errorMessage) }
                    } else null
                )
            }
        }
    }
}

