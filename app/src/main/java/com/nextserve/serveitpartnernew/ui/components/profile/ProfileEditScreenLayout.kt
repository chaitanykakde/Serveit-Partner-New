package com.nextserve.serveitpartnernew.ui.components.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

/**
 * Reusable screen layout structure extracted from Edit Basic Info screen.
 * Provides consistent Scaffold, TopAppBar, title, subtitle, and scrollable content container.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreenLayout(
    navController: NavController,
    title: String,
    subtitle: String,
    parentPaddingValues: PaddingValues = PaddingValues(),
    content: @Composable () -> Unit
) {
    val scrollState = rememberScrollState()
    val bottomNavPadding = parentPaddingValues.calculateBottomPadding()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { }, // Empty title - title is shown below top bar
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.Black
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Add overflow menu */ }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black,
                    actionIconContentColor = Color.Black
                )
            )
        },
        containerColor = Color.White,
        modifier = Modifier.background(Color.White)
    ) { scaffoldPaddingValues ->
        // Single scrollable Column with all content including button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(scaffoldPaddingValues)
                .background(Color.White)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp + bottomNavPadding)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Title (NOT in top bar)
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF1C1C1C),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Subtitle
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = Color(0xFF6B6B6B),
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Content (fields, buttons, etc.)
            content()
        }
    }
}

