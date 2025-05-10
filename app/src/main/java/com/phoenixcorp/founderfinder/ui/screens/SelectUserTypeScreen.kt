package com.phoenixcorp.founderfinder.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.phoenixcorp.founderfinder.navigation.Screen

@Composable
fun SelectUserTypeScreen(navController: NavHostController) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Select User Type", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Welcome new user! All users must start as a Regular User")
        Spacer(modifier = Modifier
            .height(8.dp)
        )

        Button(onClick = { navController.navigate(Screen.UserInfo.route) }) {
            Text("Regular User")
        }
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Join as an investor and finance people's ambitions.")
        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = { navController.navigate(Screen.InvestorInfo.route) }) {
            Text("Investor User")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SelectUserTypeScreenPreview() {
    SelectUserTypeScreen(navController = rememberNavController())
}
