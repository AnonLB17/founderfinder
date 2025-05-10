package com.phoenixcorp.founderfinder.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.phoenixcorp.founderfinder.R
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.components.ScreenBanner
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestorSearchScreen(navController: NavHostController) {
    val investors = remember { mutableStateListOf(sampleInvestors).flatten().toMutableList() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { ScreenBanner(
            title = "Investor Search",
            navController = navController,
            showBackButton = true
        ) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (investors.isNotEmpty()) {
                val investor = investors.first()
                InvestorCard(investor, navController, onSwipe = {
                    scope.launch {
                        investors.removeFirstOrNull()
                    }
                })
            } else {
                Text("No more investors to show", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
fun InvestorCard(investor: Investor, navController: NavHostController, onSwipe: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(12.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_profile_placeholder),
                contentDescription = "Investor Image",
                modifier = Modifier.size(100.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = investor.name, style = MaterialTheme.typography.headlineSmall)
            Text(text = "Industry: ${investor.industry}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Investment Philosophy: ${investor.philosophy}", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = onSwipe) {
                    Icon(Icons.Filled.Close, contentDescription = "Skip")
                }
                IconButton(onClick = { /* navController.navigate( Screen.InvestorProfile.route */ }) {
                    Icon(Icons.Filled.Info, contentDescription = "View Profile")
                }
                IconButton(onClick = { /* Logic to send documents */ }) {
                    Icon(Icons.Filled.Send, contentDescription = "Send Documents")
                }
            }
        }
    }
}

data class Investor(val name: String, val industry: String, val philosophy: String)

val sampleInvestors = listOf(
    Investor("John Doe", "Technology", "Long-term innovation"),
    Investor("Jane Smith", "Healthcare", "Disruptive biotech"),
    Investor("Michael Lee", "Finance", "Sustainable investing")
)
