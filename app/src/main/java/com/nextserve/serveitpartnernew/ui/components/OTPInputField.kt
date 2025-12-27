package com.nextserve.serveitpartnernew.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextserve.serveitpartnernew.ui.theme.OTPBoxShape

/**
 * OTP input field with support for paste, SMS autofill, and improved backspace handling.
 * @param otpLength The length of OTP (default 6)
 * @param onOtpChange Callback when OTP changes
 * @param modifier Modifier for the component
 */
@Composable
fun OTPInputField(
    otpLength: Int = 6,
    onOtpChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var otpText by remember { mutableStateOf("") }
    val focusRequesters = remember { List(otpLength) { FocusRequester() } }
    val keyboardController = LocalSoftwareKeyboardController.current
    val isFocusedStates = remember { mutableStateOf(List(otpLength) { false }) }

    LaunchedEffect(otpText) {
        onOtpChange(otpText)
        if (otpText.length == otpLength) {
            keyboardController?.hide()
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(otpLength) { index ->
            val char = if (index < otpText.length) otpText[index].toString() else ""
            val isFocused = isFocusedStates.value[index]

            val colorScheme = MaterialTheme.colorScheme
            val borderColor = when {
                isFocused -> colorScheme.primary
                char.isNotEmpty() -> colorScheme.primary
                else -> colorScheme.outline
            }
            val backgroundColor = if (char.isNotEmpty()) {
                colorScheme.primaryContainer.copy(alpha = 0.2f)
            } else {
                colorScheme.surface
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .background(
                        color = backgroundColor,
                        shape = OTPBoxShape
                    )
                    .border(
                        width = 2.dp,
                        color = borderColor,
                        shape = OTPBoxShape
                    )
                    .focusRequester(focusRequesters[index])
                    .onFocusChanged { focusState ->
                        val newStates = isFocusedStates.value.toMutableList()
                        newStates[index] = focusState.isFocused
                        isFocusedStates.value = newStates
                    }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (char.isNotEmpty()) {
                    Text(
                        text = char,
                        style = TextStyle(
                            fontSize = 24.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
                BasicTextField(
                    value = TextFieldValue(
                        text = "",
                        selection = TextRange(0)
                    ),
                    onValueChange = { newValue ->
                        val input = newValue.text.filter { it.isDigit() }
                        
                        // Handle paste: if input length matches or exceeds OTP length, take first N digits
                        if (input.length >= otpLength) {
                            val pastedOtp = input.take(otpLength)
                            otpText = pastedOtp
                            // Focus last box after paste
                            if (pastedOtp.length == otpLength) {
                                focusRequesters[otpLength - 1].requestFocus()
                                keyboardController?.hide()
                            }
                            return@BasicTextField
                        }
                        
                        if (input.isNotEmpty()) {
                            val digit = input.last().toString()
                            // Only add if current box is empty or we're at the right position
                            if (otpText.length == index) {
                                val newOtp = otpText + digit
                                if (newOtp.length <= otpLength) {
                                    otpText = newOtp
                                    if (index < otpLength - 1) {
                                        focusRequesters[index + 1].requestFocus()
                                    } else {
                                        keyboardController?.hide()
                                    }
                                }
                            } else if (otpText.length < index) {
                                // Fill gap if user skipped boxes
                                val newOtp = otpText + "0".repeat(index - otpText.length) + digit
                                if (newOtp.length <= otpLength) {
                                    otpText = newOtp
                                    if (index < otpLength - 1) {
                                        focusRequesters[index + 1].requestFocus()
                                    } else {
                                        keyboardController?.hide()
                                    }
                                }
                            }
                        } else {
                            // Backspace handling: improved logic
                            if (otpText.length > index) {
                                // Delete current character
                                otpText = otpText.substring(0, index) + otpText.substring(index + 1)
                                if (index > 0) {
                                    focusRequesters[index - 1].requestFocus()
                                }
                            } else if (otpText.length == index && index > 0) {
                                // Delete previous character if current is empty
                                otpText = otpText.substring(0, index - 1)
                                focusRequesters[index - 1].requestFocus()
                            }
                        }
                    },
                    textStyle = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = androidx.compose.ui.graphics.Color.Transparent // Hide the actual text, we show it above
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = if (index == otpLength - 1) ImeAction.Done else ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                        }
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Auto-focus first box
    LaunchedEffect(Unit) {
        focusRequesters[0].requestFocus()
    }
}

