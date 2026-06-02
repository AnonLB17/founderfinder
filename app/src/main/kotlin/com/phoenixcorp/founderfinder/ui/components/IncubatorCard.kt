package com.phoenixcorp.founderfinder.ui.components

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.phoenixcorp.founderfinder.domain.model.Incubator

@Composable
fun IncubatorCard(incubator: Incubator) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(incubator.websiteUrl))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Handle invalid URL or no browser
                    Log.e("IncubatorCard", "Error opening URL: ${e.message}", e)
                }
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = incubator.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}