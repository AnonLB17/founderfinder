package com.phoenixcorp.founderfinder.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.phoenixcorp.founderfinder.R
import com.phoenixcorp.founderfinder.navigation.Screen
import com.phoenixcorp.founderfinder.ui.screens.Forum

@Composable
fun ForumCard(forum: Forum, navController: NavHostController) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { /* navController.navigate(Screen.CurrentForum.route) */ },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_placeholder),
                contentDescription = "Forum Image",
                modifier = Modifier.size(50.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = forum.title, style = MaterialTheme.typography.titleMedium)
                Text(text = forum.description, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "Created by ${forum.creatorName}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.clickable { /*navController.navigate(Screen.CreatorProfile.route) */ }
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = { /* Pin forum logic */ }) {
                    Icon(painterResource(R.drawable.ic_star), contentDescription = "Favorite")
                }
                IconButton(onClick = { /* Upvote logic */ }) {
                    Icon(painterResource(R.drawable.ic_arrow_up), contentDescription = "Upvote")
                }
                IconButton(onClick = { /* Downvote logic */ }) {
                    Icon(painterResource(R.drawable.ic_arrow_down), contentDescription = "Downvote")
                }
            }
        }
    }
}

